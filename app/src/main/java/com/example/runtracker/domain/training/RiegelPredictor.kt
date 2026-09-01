package com.example.runtracker.domain.training

import kotlin.math.pow

/**
 * Riegel formula — ngoại suy thành tích từ MỘT nỗ lực gần đây:
 *
 *   T2 = T1 × (D2 / D1) ^ 1.06
 *
 * Đáng tin trong khoảng ~3km đến ~cự ly marathon; càng xa cự ly gốc sai số càng lớn.
 * Thuần JVM.
 */
object RiegelPredictor {

    const val FATIGUE_EXPONENT = 1.06

    fun predictSeconds(
        baseDistanceMeters: Double,
        baseTimeSeconds: Double,
        targetDistanceMeters: Double,
    ): Double {
        require(baseDistanceMeters > 0 && baseTimeSeconds > 0) { "base effort must be positive" }
        return baseTimeSeconds * (targetDistanceMeters / baseDistanceMeters).pow(FATIGUE_EXPONENT)
    }
}

/** Các cự ly chuẩn để dự đoán (khớp `distanceLabel` của entity). */
enum class RaceDistance(val label: String, val meters: Double) {
    FIVE_K("5K", 5_000.0),
    TEN_K("10K", 10_000.0),
    HALF("HALF", 21_097.5),
    FULL("FULL", 42_195.0);

    companion object {
        fun fromLabel(label: String): RaceDistance? = entries.firstOrNull { it.label == label }
    }
}
