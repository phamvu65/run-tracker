package com.example.runtracker.domain.model

import java.time.Instant

/**
 * Một điểm GPS trong trace. `id` = 0 khi chưa lưu (Room autogenerate).
 */
data class RoutePoint(
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speedMps: Float?,
    val accuracyMeters: Float?,
    val timestamp: Instant,
)
