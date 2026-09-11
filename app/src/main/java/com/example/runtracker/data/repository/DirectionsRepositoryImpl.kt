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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Routing qua OSRM của FOSSGIS (`routing.openstreetmap.de`) — miễn phí, không key.
 * Dùng `/route` cho cả điểm chấm lẫn nét vẽ tay (nét vẽ đã được rút thành điểm cách
 * đều nên đường đi bám sát hình vẽ). `/match` của instance này giới hạn số điểm quá
 * thấp nên không dùng.
 */
@Singleton
class DirectionsRepositoryImpl @Inject constructor(
    private val api: DirectionsApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DirectionsRepository {

    override suspend fun route(
        waypoints: List<GeoPoint>,
        mode: TravelMode,
        allowUTurns: Boolean,
        radiusMeters: Double?,
        bearingsDegrees: List<Double>?,
        bearingRangeDegrees: Double,
    ): Result<PlannedRoute> = withContext(io) {
        runCatching {
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
            val res = api.route(url)
            val route = res.routes.firstOrNull()
            require(res.code == "Ok" && route != null) { "OSRM: ${res.code}" }
            route.toPlannedRoute()
        }
    }

    private fun OsrmRoute.toPlannedRoute(): PlannedRoute {
        val polyline = PolylineCodec.decode(geometry)
        require(polyline.size >= 2) { "geometry rỗng" }
        val steps = legs.flatMap { it.steps }.mapNotNull { step ->
            val loc = step.maneuver.location
            if (loc.size < 2) return@mapNotNull null
            val instruction = instructionOf(step.maneuver, step.name)
            if (instruction.isBlank()) return@mapNotNull null
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
