package com.example.runtracker.domain.model

import java.time.Instant
import kotlin.time.Duration

/**
 * Domain model của một buổi tập — không mang chi tiết đồng bộ (`isSynced`, `updatedAt` nằm ở entity).
 */
data class Activity(
    val id: String,
    val userId: String,
    val type: ActivityType,
    val startTime: Instant,
    val endTime: Instant,
    val distanceMeters: Double,
    val duration: Duration,
    val movingTime: Duration,
    val avgPaceSecPerKm: Double,
    val avgSpeedKmh: Double,
    val elevationGainMeters: Double,
    val elevationLossMeters: Double,
    val avgHeartRate: Int?,
    val maxHeartRate: Int?,
    val calories: Int?,
    val avgCadence: Int?,
    val perceivedExertion: Int?,   // RPE 1-10, fallback cho TRIMP khi thiếu HR
    val weatherTempC: Double?,
    val gpxRawPath: String?,
)

enum class ActivityType(val raw: String) {
    RUNNING("RUNNING"),
    CYCLING("CYCLING"),
    WALKING("WALKING"),
    OTHER("OTHER");

    companion object {
        fun fromRaw(raw: String): ActivityType =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: OTHER
    }
}
