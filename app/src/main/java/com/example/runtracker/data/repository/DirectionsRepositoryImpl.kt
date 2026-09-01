package com.example.runtracker.data.repository

import androidx.core.text.HtmlCompat
import com.example.runtracker.BuildConfig
import com.example.runtracker.data.remote.DirectionsApi
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
            require(BuildConfig.MAPS_API_KEY.isNotBlank()) { "thiếu MAPS_API_KEY" }

            val response = api.directions(
                origin = waypoints.first().toParam(),
                destination = waypoints.last().toParam(),
                waypoints = waypoints.drop(1).dropLast(1)
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString("|") { it.toParam() },
                mode = mode.apiValue,
                key = BuildConfig.MAPS_API_KEY,
            )

            val route = response.routes.firstOrNull()
                ?: error("Directions status=${response.status}")

            PlannedRoute(
                polyline = PolylineCodec.decode(route.overviewPolyline.points),
                distanceMeters = route.legs.sumOf { it.distance.value },
                steps = route.legs.flatMap { leg ->
                    leg.steps.map { step ->
                        RouteStep(
                            instruction = step.htmlInstructions.stripHtml(),
                            location = GeoPoint(step.startLocation.lat, step.startLocation.lng),
                            distanceMeters = step.distance.value,
                        )
                    }
                },
                snappedToRoads = true,
            )
        }
    }

    private fun GeoPoint.toParam() = "$latitude,$longitude"

    private fun String.stripHtml(): String =
        HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
}
