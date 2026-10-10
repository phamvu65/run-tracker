package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.Route
import com.example.runtracker.domain.model.RouteWaypoint
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.domain.repository.RouteRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Chỉ lưu kết quả bám đường. Giữ đầu vào gốc riêng với các bước rẽ để mở lại chỉnh sửa.
 */
class SaveRouteUseCase @Inject constructor(
    private val routeRepository: RouteRepository,
) {
    suspend operator fun invoke(
        name: String,
        planned: PlannedRoute,
        tappedPoints: List<GeoPoint>,
        mode: TravelMode,
        drawnFromSketch: Boolean = false,
        existing: Route? = null,
    ): String {
        require(planned.snappedToRoads && planned.polyline.size >= 2) {
            "Chỉ lưu lộ trình đã bám đường."
        }
        require(planned.distanceMeters.isFinite() && planned.distanceMeters > 0.0)
        val id = existing?.id ?: UUID.randomUUID().toString()
        val waypoints = if (planned.steps.isNotEmpty()) {
            planned.steps.mapIndexed { i, step ->
                RouteWaypoint(orderIndex = i, location = step.location, instruction = step.instruction)
            }
        } else {
            tappedPoints.mapIndexed { i, p -> RouteWaypoint(orderIndex = i, location = p, instruction = null) }
        }

        routeRepository.saveRoute(
            Route(
                id = id,
                userId = LOCAL_USER_ID,
                name = name.trim().ifEmpty { "Route" },
                distanceMeters = planned.distanceMeters,
                elevationGainMeters = 0.0,
                polyline = planned.polyline,
                isPublic = existing?.isPublic ?: false,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                waypoints = waypoints,
                travelMode = mode,
                sourcePoints = tappedPoints.toList(),
                drawnFromSketch = drawnFromSketch,
                snappedToRoads = true,
            ),
        )
        return id
    }
}
