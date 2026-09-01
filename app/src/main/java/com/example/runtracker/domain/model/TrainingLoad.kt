package com.example.runtracker.domain.model

import java.time.LocalDate

/** Tổng TRIMP + thống kê của một ngày cho một user (1 dòng/ngày). */
data class DailyTrainingLoad(
    val userId: String,
    val date: LocalDate,
    val trimpScore: Double,
    val activityCount: Int,
    val totalDurationSeconds: Long,
    val updatedAt: Long,
)

/** CTL (Fitness) / ATL (Fatigue) / TSB (Form) tại một ngày. */
data class FitnessFreshnessSnapshot(
    val userId: String,
    val date: LocalDate,
    val ctl: Double,
    val atl: Double,
    val tsb: Double,
    val computedAt: Long,
)
