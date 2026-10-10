package com.example.runtracker.data.repository

import com.example.runtracker.data.remote.DirectionsApi
import com.example.runtracker.data.remote.OsrmLeg
import com.example.runtracker.data.remote.OsrmManeuver
import com.example.runtracker.data.remote.OsrmRoute
import com.example.runtracker.di.IoDispatcher
import com.example.runtracker.domain.geo.PolylineCodec
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.RouteLeg
import com.example.runtracker.domain.model.RouteStep
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.domain.repository.DirectionsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Routing qua OSRM của FOSSGIS (`routing.openstreetmap.de`) — miễn phí, không key.
 * Dùng `/route` cho điểm chấm và các mốc giữ hình dạng nét vẽ do use-case chọn.
 * Tuần tự hoá request và giãn cách tối thiểu một giây theo chính sách máy chủ.
 */
@Singleton
class DirectionsRepositoryImpl @Inject constructor(
    private val api: DirectionsApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DirectionsRepository {
    private val requestMutex = Mutex()
    private var lastRequestNanos = 0L

    private suspend fun request(url: String) = requestMutex.withLock {
        val elapsedMs = (System.nanoTime() - lastRequestNanos) / 1_000_000
        if (lastRequestNanos != 0L && elapsedMs < 1_050) delay(1_050 - elapsedMs)
        lastRequestNanos = System.nanoTime()
        api.route(url)
    }

    override suspend fun matchSketch(points: List<GeoPoint>, mode: TravelMode, radiusMeters: Double): Result<PlannedRoute> =
        withContext(io) {
            try {
                require(points.size in 2..10 && radiusMeters.isFinite() && radiusMeters > 0)
                val coords = points.joinToString(";") { "${it.longitude},${it.latitude}" }
                val radiuses = points.joinToString(";") { radiusMeters.toString() }
                val res = request("${DirectionsApi.BASE_URL}${mode.osrmHost}/match/v1/${mode.osrmProfile}/$coords" +
                    "?overview=full&geometries=polyline&steps=true&gaps=ignore&tidy=false&radiuses=$radiuses")
                // OSRM may drop outliers, split traces, or omit the first/last points.
                // Only one connected route covering both endpoints may be used.
                require(res.code == "Ok" && res.matchings.size == 1)
                require(res.tracepoints.size == points.size)
                require(res.tracepoints.first()?.matchings_index == 0 && res.tracepoints.last()?.matchings_index == 0)
                require(res.tracepoints.count { it?.matchings_index == 0 } >= points.size * 0.7)
                require(res.tracepoints.filterNotNull().all { it.matchings_index == 0 })
                Result.success(res.matchings.single().toPlannedRoute())
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) { Result.failure(error) }
        }

    override suspend fun route(
        waypoints: List<GeoPoint>,
        mode: TravelMode,
        allowUTurns: Boolean,
        radiusMeters: Double?,
        bearingsDegrees: List<Double>?,
        bearingRangeDegrees: Double,
    ): Result<PlannedRoute> = withContext(io) {
        try {
            require(waypoints.size >= 2) { "cần ít nhất 2 điểm" }
            require(bearingsDegrees == null || bearingsDegrees.size == waypoints.size) {
                "bearingsDegrees phải có cùng số điểm với waypoints"
            }
            val coords = waypoints.joinToString(";") { "${it.longitude},${it.latitude}" }
            val radiuses = radiusMeters?.let { r ->
                "&radiuses=" + waypoints.joinToString(";") { r.toString() }
            }.orEmpty()
            val bearings = bearingsDegrees?.let { degs ->
                "&bearings=" + degs.joinToString(";") { d ->
                    "${d.roundToInt()},${bearingRangeDegrees.roundToInt()}"
                }
            }.orEmpty()
            val url = "${DirectionsApi.BASE_URL}${mode.osrmHost}/route/v1/${mode.osrmProfile}/$coords" +
                "?overview=full&geometries=polyline&steps=true" +
                "&continue_straight=${if (allowUTurns) "false" else "true"}" +
                radiuses + bearings
            val res = request(url)
            val route = res.routes.firstOrNull()
            require(res.code == "Ok" && route != null) { "OSRM: ${res.code}" }
            Result.success(route.toPlannedRoute())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun OsrmRoute.toPlannedRoute(): PlannedRoute {
        val polyline = PolylineCodec.decode(geometry)
        require(polyline.size >= 2) { "geometry rỗng" }
        require(distance.isFinite() && distance > 0.0) { "distance không hợp lệ" }
        val allSteps = legs.flatMap { it.steps }
        require(allSteps.none { it.mode == "ferry" }) { "Lộ trình cần đi phà, không phù hợp buổi chạy." }
        val steps = allSteps.mapIndexedNotNull { index, step ->
            // Intermediate via points are not destinations or new departures.
            if (step.maneuver.type == "depart" ||
                (step.maneuver.type == "arrive" && index != allSteps.lastIndex)) return@mapIndexedNotNull null
            val loc = step.maneuver.location
            if (loc.size < 2) return@mapIndexedNotNull null
            val instruction = instructionOf(step.maneuver, step.name)
            if (instruction.isBlank()) return@mapIndexedNotNull null
            RouteStep(
                instruction = instruction,
                location = GeoPoint(loc[1], loc[0]),
                distanceMeters = step.distance,
            )
        }
        return PlannedRoute(
            polyline = polyline,
            distanceMeters = distance,
            steps = steps,
            snappedToRoads = true,
            legs = legs.map { it.toRouteLeg() },
        )
    }

    /** Nối polyline của từng step trong leg — step liền kề chia sẻ 1 điểm nên bỏ điểm trùng. */
    private fun OsrmLeg.toRouteLeg(): RouteLeg {
        val points = ArrayList<GeoPoint>()
        for (step in steps) {
            val stepPoints = PolylineCodec.decode(step.geometry)
            for (p in stepPoints) {
                if (points.isEmpty() || points.last() != p) points += p
            }
        }
        return RouteLeg(polyline = points, distanceMeters = distance)
    }

    /** Chuyển maneuver của OSRM sang câu chỉ đường tiếng Việt ngắn gọn. */
    private fun instructionOf(m: OsrmManeuver, name: String): String {
        val where = if (name.isNotBlank()) " vào $name" else ""
        val dir = when (m.modifier) {
            "left", "slight left", "sharp left" -> "trái"
            "right", "slight right", "sharp right" -> "phải"
            else -> ""
        }
        return when (m.type) {
            "depart" -> "Xuất phát"
            "arrive" -> "Đến nơi"
            "turn", "end of road", "fork", "on ramp", "off ramp" ->
                if (dir.isNotBlank()) "Rẽ $dir$where" else "Đi tiếp$where"
            "roundabout", "rotary", "roundabout turn" -> "Vào vòng xuyến$where"
            "merge", "continue", "new name" -> "Đi thẳng$where"
            else -> if (name.isNotBlank()) "Theo $name" else ""
        }
    }
}
