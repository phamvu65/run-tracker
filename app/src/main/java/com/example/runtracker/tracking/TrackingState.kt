package com.example.runtracker.tracking

import java.time.Instant
import com.example.runtracker.domain.model.GeoPoint

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
    /**
     * Tín hiệu GPS còn tốt hay không (chỉ có ý nghĩa khi [status] == TRACKING): false khi
     * [android.location.LocationListener] báo hết khả năng định vị, hoặc lâu rồi không có fix mới.
     * Không phản ánh việc đã cấp quyền vị trí hay chưa (xem UI dùng `hasPermission` riêng cho việc đó).
     */
    val gpsSignalOk: Boolean = true,
    /** Nhịp tim live từ đai BLE (null nếu không kết nối). */
    val liveHeartRateBpm: Int? = null,
    /** Nhịp bước/phút, tính theo cửa sổ trượt ngắn từ cảm biến bước chân (null nếu chưa đủ dữ liệu). */
    val liveCadenceSpm: Int? = null,
    // Turn-by-turn navigation (null nếu không theo route nào)
    val navRouteName: String? = null,
    val navInstruction: String? = null,
    val navDistanceMeters: Double? = null,
    val navOffRoute: Boolean = false,
    val navStepIndex: Int = 0,
    val navStepCount: Int = 0,
    val navPolyline: List<GeoPoint> = emptyList(),
    val navRerouting: Boolean = false,
    /** true khi [status] == PAUSED do tự động phát hiện đứng yên (khác tạm dừng bằng nút bấm). */
    val autoPaused: Boolean = false,
) {
    val isActive: Boolean get() = status != TrackingStatus.IDLE

    /** Pace trung bình (giây/km) tính theo moving time; 0 nếu chưa đủ dữ liệu. */
    val avgPaceSecPerKm: Double
        get() = if (distanceMeters > 0) movingTimeSeconds / (distanceMeters / 1000.0) else 0.0
}
