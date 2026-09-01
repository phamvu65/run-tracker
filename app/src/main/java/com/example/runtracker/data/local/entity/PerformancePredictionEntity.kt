package com.example.runtracker.data.local.entity

import androidx.room.Entity

/**
 * Dự đoán thành tích theo cự ly. Công thức Riegel: T2 = T1 * (D2 / D1) ^ 1.06
 * với T1/D1 lấy từ activity tốt nhất gần đây.
 */
@Entity(
    tableName = "performance_predictions",
    primaryKeys = ["userId", "distanceLabel"]
)
data class PerformancePredictionEntity(
    val userId: String,
    val distanceLabel: String,     // "5K", "10K", "HALF", "FULL"
    val predictedSeconds: Double,
    val basedOnActivityId: String,
    val computedAt: Long
)
