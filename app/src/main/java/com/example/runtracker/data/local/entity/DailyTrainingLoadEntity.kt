package com.example.runtracker.data.local.entity

import androidx.room.Entity

/**
 * Mỗi user 1 dòng/ngày. `trimpScore` = tổng TRIMP của mọi activity trong ngày —
 * nguyên liệu cho FitnessFreshnessSnapshot (CTL/ATL/TSB).
 */
@Entity(
    tableName = "daily_training_load",
    primaryKeys = ["userId", "date"]
)
data class DailyTrainingLoadEntity(
    val userId: String,
    val date: String,          // "yyyy-MM-dd"
    val trimpScore: Double,
    val activityCount: Int,
    val totalDurationSeconds: Long,
    val updatedAt: Long
)
