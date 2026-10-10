package com.example.runtracker.domain.tracking

import com.example.runtracker.domain.model.RoutePoint
import kotlin.math.roundToLong

/** Số liệu tổng hợp tính từ trace GPS đã qua lọc nhiễu. */
data class RunAggregate(
    val distanceMeters: Double,
    val movingTimeSeconds: Long,
    val elevationGainMeters: Double,
    val elevationLossMeters: Double,
)

/**
 * Tính lại toàn bộ aggregate từ danh sách điểm — dùng khi kết thúc buổi tập (số liệu
 * chuẩn, không lệ thuộc trạng thái tích luỹ trong bộ nhớ) và khi khôi phục buổi bị gián đoạn.
 * Thuần JVM.
 */
object RunAggregator {

    /** Tốc độ tối thiểu coi là "đang di chuyển" (m/s). */
    const val MOVING_SPEED_MPS = 0.6

    /** Minimum total ascent for elevation records; not a per-point noise filter. */
    const val ELEVATION_THRESHOLD_M = 1.0

    fun fromPoints(points: List<RoutePoint>): RunAggregate {
        var distance = 0.0
        var movingSeconds = 0L
        val elevation = ElevationAccumulator()
        points.forEach { elevation.add(it.altitude, it.timestamp) }

        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val meters = GeoMath.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
            val dtSeconds = (b.timestamp.toEpochMilli() - a.timestamp.toEpochMilli()) / 1000.0

            distance += meters
            if (dtSeconds > 0 && meters / dtSeconds >= MOVING_SPEED_MPS) {
                movingSeconds += dtSeconds.roundToLong()
            }
        }

        return RunAggregate(distance, movingSeconds, elevation.gainMeters, elevation.lossMeters)
    }
}
