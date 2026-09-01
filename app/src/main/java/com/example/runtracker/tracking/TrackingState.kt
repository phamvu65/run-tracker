package com.example.runtracker.tracking

import java.time.Instant

enum class TrackingStatus { IDLE, TRACKING, PAUSED }

/**
 * Ảnh chụp trạng thái buổi ghi hiện tại, do [LocationTrackingService] cập nhật và
 * UI quan sát qua [TrackingSession]. Aggregates tính tăng dần từ các điểm GPS đã qua lọc nhiễu.
 */
data class TrackingState(
    val status: TrackingStatus = TrackingStatus.IDLE,
    val activityId: String? = null,
    val startedAt: Instant? = null,
    val elapsedSeconds: Long = 0,
    val movingTimeSeconds: Long = 0,
    val distanceMeters: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
    val pointCount: Int = 0,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val lastUpdate: Instant? = null,
) {
    val isActive: Boolean get() = status != TrackingStatus.IDLE

    /** Pace trung bình (giây/km) tính theo moving time; 0 nếu chưa đủ dữ liệu. */
    val avgPaceSecPerKm: Double
        get() = if (distanceMeters > 0) movingTimeSeconds / (distanceMeters / 1000.0) else 0.0
}
