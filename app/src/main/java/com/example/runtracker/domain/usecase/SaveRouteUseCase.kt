package com.example.runtracker.domain.usecase

import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.Route
import com.example.runtracker.domain.model.RouteWaypoint
import com.example.runtracker.domain.repository.RouteRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Lưu [PlannedRoute] thành [Route]. Waypoint = các điểm rẽ (steps) nếu bám đường,
 * ngược lại là các điểm người dùng đã chấm.
 */
class SaveRouteUseCase @Inject constructor(
    private val routeRepository: RouteRepository,
) {
    suspend operator fun invoke(
        name: String,
        planned: PlannedRoute,
        tappedPoints: List<GeoPoint>,
    ): String {
        val id = UUID.randomUUID().toString()
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
                isPublic = false,
                createdAt = System.currentTimeMillis(),
                waypoints = waypoints,
            ),
        )
        return id
    }
}
