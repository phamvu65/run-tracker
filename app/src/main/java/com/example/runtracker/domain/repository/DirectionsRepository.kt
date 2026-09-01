package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.TravelMode

interface DirectionsRepository {

    /** Bám đường cho một chuỗi điểm. Thất bại (mất mạng / thiếu key) -> [Result.failure]. */
    suspend fun route(waypoints: List<GeoPoint>, mode: TravelMode): Result<PlannedRoute>
}
