package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.ActivityType
import kotlin.math.roundToInt

/**
 * Ước tính calo tiêu hao bằng công thức MET chuẩn ACSM, theo tốc độ trung bình + cân nặng:
 *
 *   VO2 (chạy)   = 0.2 × tốc_độ(m/phút) + 3.5
 *   VO2 (đi bộ)  = 0.1 × tốc_độ(m/phút) + 3.5
 *   MET = VO2 / 3.5
 *   kcal/phút = MET × 3.5 × cân_nặng(kg) / 200
 *
 * Đạp xe/khác dùng MET cố định (không có công thức ACSM theo tốc độ đơn giản tương đương).
 * Cần cân nặng người dùng — không có fallback theo dân số trung bình (khác `maxHeartRateOrEstimate`
 * dùng tuổi), nên trả về null nếu chưa khai cân nặng trong hồ sơ.
 *
 * Thuần JVM.
 */
object CalorieEstimator {

    private const val CYCLING_MET = 8.0
    private const val OTHER_MET = 6.0

    fun estimate(
        type: ActivityType,
        distanceMeters: Double,
        movingTimeSeconds: Long,
        weightKg: Double?,
    ): Int? {
        if (weightKg == null || weightKg <= 0.0) return null
        if (movingTimeSeconds <= 0 || distanceMeters <= 0.0) return null

        val minutes = movingTimeSeconds / 60.0
        val speedKmh = (distanceMeters / 1000.0) / (movingTimeSeconds / 3600.0)
        val met = metFor(type, speedKmh)
        val kcalPerMinute = met * 3.5 * weightKg / 200.0
        return (kcalPerMinute * minutes).roundToInt()
    }

    private fun metFor(type: ActivityType, speedKmh: Double): Double {
        val speedMPerMin = speedKmh * 1000.0 / 60.0
        val met = when (type) {
            ActivityType.RUNNING -> (0.2 * speedMPerMin + 3.5) / 3.5
            ActivityType.WALKING -> (0.1 * speedMPerMin + 3.5) / 3.5
            ActivityType.CYCLING -> CYCLING_MET
            ActivityType.OTHER -> OTHER_MET
        }
        return met.coerceAtLeast(1.0)
    }
}
