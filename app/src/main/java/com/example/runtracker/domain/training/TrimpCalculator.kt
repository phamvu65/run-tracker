package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.Sex
import com.example.runtracker.domain.model.User
import com.example.runtracker.domain.model.maxHeartRateOrEstimate
import kotlin.math.exp

/**
 * TRIMP (Training Impulse) — Banister. Một con số "độ nặng" cho mỗi buổi tập,
 * nguyên liệu cho CTL/ATL/TSB.
 *
 *   ΔHR_ratio = (avgHR − restingHR) / (maxHR − restingHR)         [kẹp về 0..1]
 *   TRIMP = phút × ratio × 0.64 × e^(1.92 × ratio)   [nam / không rõ]
 *   TRIMP = phút × ratio × 0.86 × e^(1.67 × ratio)   [nữ]
 *
 * Không đủ dữ liệu HR → fallback RPE: `phút × RPE` (RPE 1-10). Không có cả hai → null.
 *
 * Thuần JVM.
 */
object TrimpCalculator {

    private const val MALE_A = 0.64
    private const val MALE_B = 1.92
    private const val FEMALE_A = 0.86
    private const val FEMALE_B = 1.67

    fun calculate(
        durationMinutes: Double,
        avgHeartRate: Int?,
        restingHeartRate: Int?,
        maxHeartRate: Int?,
        sex: Sex?,
        perceivedExertion: Int?,
    ): Double? {
        if (durationMinutes <= 0.0) return 0.0

        heartRateTrimp(durationMinutes, avgHeartRate, restingHeartRate, maxHeartRate, sex)
            ?.let { return it }

        val rpe = perceivedExertion?.takeIf { it in 1..10 } ?: return null
        return durationMinutes * rpe
    }

    /** Tiện ích cho một [Activity] cụ thể; dùng moving time, fallback tổng thời gian. */
    fun forActivity(activity: Activity, user: User?, year: Int): Double? = calculate(
        durationMinutes = durationMinutesOf(activity),
        avgHeartRate = activity.avgHeartRate,
        restingHeartRate = user?.restingHeartRate,
        maxHeartRate = user?.maxHeartRateOrEstimate(year),
        sex = user?.sex,
        perceivedExertion = activity.perceivedExertion,
    )

    fun durationMinutesOf(activity: Activity): Double {
        val seconds = activity.movingTime.inWholeSeconds.takeIf { it > 0 }
            ?: activity.duration.inWholeSeconds
        return seconds / 60.0
    }

    private fun heartRateTrimp(
        durationMinutes: Double,
        avgHeartRate: Int?,
        restingHeartRate: Int?,
        maxHeartRate: Int?,
        sex: Sex?,
    ): Double? {
        if (avgHeartRate == null || restingHeartRate == null || maxHeartRate == null) return null
        if (maxHeartRate <= restingHeartRate) return null

        val ratio = ((avgHeartRate - restingHeartRate).toDouble() / (maxHeartRate - restingHeartRate))
            .coerceIn(0.0, 1.0)
        val (a, b) = if (sex == Sex.FEMALE) FEMALE_A to FEMALE_B else MALE_A to MALE_B
        return durationMinutes * ratio * a * exp(b * ratio)
    }
}
