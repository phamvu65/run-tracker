package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.tracking.RunAggregator

/**
 * Kỷ lục tính trên TOÀN BỘ 1 buổi tập (khác [EffortDistance] — không cần cửa sổ trượt trên GPS
 * trace, chỉ so sánh trực tiếp field đã tổng hợp sẵn của [Activity]).
 */
enum class ActivityRecordType(val label: String, val unit: String, val higherIsBetter: Boolean) {
    FASTEST_PACE("Pace nhanh nhất", "giây/km", higherIsBetter = false),
    LONGEST_DISTANCE("Quãng đường dài nhất", "m", higherIsBetter = true),
    MOST_ELEVATION_GAIN("Độ cao lên nhiều nhất", "m", higherIsBetter = true),
}

/**
 * Giá trị của 1 buổi tập cho 1 loại kỷ lục — null nếu buổi không đủ điều kiện tính. Có ngưỡng tối
 * thiểu để loại buổi chỉ toàn nhiễu GPS (đứng yên vẫn cộng dồn vài mét do haversine giữa các fix
 * nhiễu — xem [com.example.runtracker.domain.tracking.RunAggregator]), tránh kỷ lục vô nghĩa kiểu
 * "quãng đường dài nhất: 0,00 km".
 */
object ActivityRecordCalculator {
    /** Dưới mức này coi như chưa thực sự di chuyển — cùng tinh thần ngưỡng "chưa di chuyển" ở màn Ghi. */
    private const val MIN_MEANINGFUL_DISTANCE_M = 100.0

    fun valueFor(type: ActivityRecordType, activity: Activity): Double? = when (type) {
        ActivityRecordType.FASTEST_PACE ->
            activity.avgPaceSecPerKm.takeIf { it > 0.0 && activity.distanceMeters >= MIN_MEANINGFUL_DISTANCE_M }
        ActivityRecordType.LONGEST_DISTANCE ->
            activity.distanceMeters.takeIf { it >= MIN_MEANINGFUL_DISTANCE_M }
        ActivityRecordType.MOST_ELEVATION_GAIN ->
            activity.elevationGainMeters.takeIf { it >= RunAggregator.ELEVATION_THRESHOLD_M }
    }
}
