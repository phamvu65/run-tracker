package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.domain.repository.DirectionsRepository
import com.example.runtracker.domain.tracking.GeoMath
import javax.inject.Inject

/**
 * Dựng route từ các điểm người dùng chấm. Ưu tiên bám đường qua Directions;
 * nếu lỗi (mất mạng / thiếu key) thì fallback đường thẳng nối các điểm.
 */
class BuildRouteUseCase @Inject constructor(
    private val directionsRepository: DirectionsRepository,
) {
    suspend operator fun invoke(waypoints: List<GeoPoint>, mode: TravelMode): PlannedRoute {
        if (waypoints.size < 2) {
            return PlannedRoute(waypoints, 0.0, emptyList(), snappedToRoads = false)
        }
        return directionsRepository.route(waypoints, mode).getOrElse {
            PlannedRoute(
                polyline = waypoints,
                distanceMeters = GeoMath.pathDistanceMeters(waypoints),
                steps = emptyList(),
                snappedToRoads = false,
            )
        }
    }
}
