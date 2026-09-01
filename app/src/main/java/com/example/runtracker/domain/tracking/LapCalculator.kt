package com.example.runtracker.domain.tracking

import com.example.runtracker.domain.model.ActivityLap
import com.example.runtracker.domain.model.RoutePoint
import kotlin.time.Duration.Companion.seconds

/**
 * Chia trace GPS thành các lap đều theo quãng đường (mặc định 1 km).
 * Thời điểm cắt lap được nội suy tuyến tính trong đoạn chứa mốc, nên mỗi lap đầy
 * đúng `lapMeters`; lap cuối có thể ngắn hơn. `avgHeartRate` để null (chưa ghi HR).
 *
 * Thuần JVM.
 */
object LapCalculator {

    const val DEFAULT_LAP_METERS = 1_000.0

    fun splitByDistance(
        points: List<RoutePoint>,
        lapMeters: Double = DEFAULT_LAP_METERS,
    ): List<ActivityLap> {
        if (points.size < 2 || lapMeters <= 0.0) return emptyList()

        val laps = mutableListOf<ActivityLap>()
        var lapIndex = 1
        var lapStartDist = 0.0
        var lapStartTimeMs = points.first().timestamp.toEpochMilli().toDouble()
        var cumulative = 0.0

        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val segStart = cumulative
            val segDist = GeoMath.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
            val segEnd = segStart + segDist
            val aMs = a.timestamp.toEpochMilli().toDouble()
            val bMs = b.timestamp.toEpochMilli().toDouble()

            var boundary = lapStartDist + lapMeters
            while (segDist > 0.0 && boundary <= segEnd) {
                val fraction = (boundary - segStart) / segDist
                val crossMs = aMs + fraction * (bMs - aMs)
                laps += lap(lapIndex, lapMeters, crossMs - lapStartTimeMs)
                lapIndex++
                lapStartDist = boundary
                lapStartTimeMs = crossMs
                boundary += lapMeters
            }
            cumulative = segEnd
        }

        val remaining = cumulative - lapStartDist
        if (remaining > 1.0) {
            val endMs = points.last().timestamp.toEpochMilli().toDouble()
            laps += lap(lapIndex, remaining, endMs - lapStartTimeMs)
        }
        return laps
    }

    private fun lap(index: Int, distanceMeters: Double, durationMs: Double): ActivityLap {
        val seconds = (durationMs / 1_000.0).coerceAtLeast(0.0)
        val pace = if (distanceMeters > 0.0) seconds / (distanceMeters / 1_000.0) else 0.0
        return ActivityLap(
            lapIndex = index,
            distanceMeters = distanceMeters,
            duration = seconds.seconds,
            avgPaceSecPerKm = pace,
            avgHeartRate = null,
        )
    }
}
