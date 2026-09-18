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
    val steps: Int?,
    val avgCadence: Int?,
    val perceivedExertion: Int?,   // RPE 1-10, fallback cho TRIMP khi thiếu HR
    val weather: ActivityWeather?,
    val gpxRawPath: String?,
)

/**
 * Thời tiết tại điểm xuất phát, thời điểm bắt đầu buổi tập (Phase 3 — weather overlay).
 * Lấy từ Open-Meteo lúc xem chi tiết; null nếu chưa lấy được.
 */
data class ActivityWeather(
    val temperatureC: Double,
    val apparentTemperatureC: Double?,
    val humidityPct: Int?,
    val windSpeedMps: Double?,
    val windDirectionDeg: Int?,
    /** Mã thời tiết WMO (0 = quang, 1-3 = mây, 45/48 = sương mù, 51-67 = mưa, 71-77 = tuyết, 80-99 = mưa rào/dông). */
    val weatherCode: Int?,
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
