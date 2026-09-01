package com.example.runtracker.domain.tracking

import com.example.runtracker.domain.model.RoutePoint

/**
 * Lọc nhiễu GPS TRƯỚC khi lưu RoutePoint (yêu cầu kỹ thuật trong CLAUDE.md):
 *  - bỏ điểm có `accuracyMeters` quá kém
 *  - bỏ điểm khiến tốc độ giữa 2 điểm liên tiếp bất thường (jump vị trí)
 *  - bỏ điểm trùng / lệch thứ tự thời gian
 *
 * Thuần logic, không phụ thuộc Android — test trên JVM.
 */
object GpsTrackFilter {

    /** Ngưỡng accuracy tối đa (m). Điểm kém hơn bị loại. */
    const val MAX_ACCURACY_METERS = 25f

    /** Tốc độ tối đa hợp lý cho chạy bộ (~25 km/h). Vượt -> coi là nhiễu. */
    const val MAX_PLAUSIBLE_SPEED_MPS = 7.0

    /**
     * @param previous điểm cuối cùng đã được chấp nhận trước batch này (null nếu là đầu activity).
     * @param candidates các điểm mới, sẽ được sắp theo timestamp trước khi lọc.
     * @return danh sách điểm giữ lại, theo thứ tự thời gian tăng dần.
     */
    fun sanitize(
        previous: RoutePoint?,
        candidates: List<RoutePoint>,
        maxAccuracyMeters: Float = MAX_ACCURACY_METERS,
        maxSpeedMps: Double = MAX_PLAUSIBLE_SPEED_MPS,
    ): List<RoutePoint> {
        if (candidates.isEmpty()) return emptyList()

        val kept = ArrayList<RoutePoint>(candidates.size)
        var last = previous

        for (point in candidates.sortedBy { it.timestamp }) {
            val accuracy = point.accuracyMeters
            if (accuracy != null && accuracy > maxAccuracyMeters) continue

            val prev = last
            if (prev != null) {
                val dtSeconds = (point.timestamp.toEpochMilli() - prev.timestamp.toEpochMilli()) / 1000.0
                if (dtSeconds <= 0.0) continue // trùng hoặc lệch thứ tự

                val displacement = GeoMath.distanceMeters(
                    prev.latitude, prev.longitude, point.latitude, point.longitude,
                )
                if (displacement / dtSeconds > maxSpeedMps) continue
            }

            kept += point
            last = point
        }
        return kept
    }
}
