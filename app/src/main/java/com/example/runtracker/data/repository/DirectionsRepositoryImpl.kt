package com.example.runtracker.data.repository

import com.example.runtracker.BuildConfig
import com.example.runtracker.data.remote.DirectionsApi
import com.example.runtracker.data.remote.DirectionsRequest
import com.example.runtracker.domain.geo.PolylineCodec
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.RouteStep
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.domain.repository.DirectionsRepository
import com.example.runtracker.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectionsRepositoryImpl @Inject constructor(
    private val api: DirectionsApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DirectionsRepository {

    override suspend fun route(
        waypoints: List<GeoPoint>,
        mode: TravelMode,
    ): Result<PlannedRoute> = withContext(io) {
        runCatching {
            require(waypoints.size >= 2) { "cần ít nhất 2 điểm" }
            require(BuildConfig.ORS_API_KEY.isNotBlank()) { "thiếu ORS_API_KEY" }

            val response = api.directions(
                profile = mode.orsProfile,
                apiKey = BuildConfig.ORS_API_KEY,
                body = DirectionsRequest(
                    coordinates = waypoints.map { listOf(it.longitude, it.latitude) },
                ),
            )

            val route = response.routes.firstOrNull() ?: error("không có route trả về")
            val polyline = PolylineCodec.decode(route.geometry)
            require(polyline.size >= 2) { "geometry rỗng" }

            PlannedRoute(
                polyline = polyline,
                distanceMeters = route.summary.distance,
                steps = route.segments.flatMap { segment ->
                    segment.steps.map { step ->
                        val at = step.wayPoints.firstOrNull()?.coerceIn(polyline.indices) ?: 0
                        RouteStep(
                            instruction = step.instruction.trim(),
                            location = polyline[at],
                            distanceMeters = step.distance,
                        )
                    }
                },
                snappedToRoads = true,
            )
        }
    }
}
