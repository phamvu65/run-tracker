package com.example.runtracker.data.local.entity

import androidx.room.Entity

/**
 * CTL / ATL / TSB (mô hình Banister — "Fitness & Freshness" của Strava).
 * Tính đệ quy (EWMA) mỗi ngày bằng WorkManager job, KHÔNG tính lại toàn bộ lịch sử mỗi lần:
 *   CTL_today = CTL_yest + (TRIMP_today - CTL_yest) / 42
 *   ATL_today = ATL_yest + (TRIMP_today - ATL_yest) / 7
 *   TSB_today = CTL_yest - ATL_yest
 */
@Entity(
    tableName = "fitness_freshness_snapshots",
    primaryKeys = ["userId", "date"]
)
data class FitnessFreshnessSnapshotEntity(
    val userId: String,
    val date: String,          // "yyyy-MM-dd"
    val ctl: Double,           // Chronic Training Load = "Fitness", EWMA 42 ngày
    val atl: Double,           // Acute Training Load = "Fatigue", EWMA 7 ngày
    val tsb: Double,           // Training Stress Balance = "Form" = CTL(hôm qua) - ATL(hôm qua)
    val computedAt: Long
)
