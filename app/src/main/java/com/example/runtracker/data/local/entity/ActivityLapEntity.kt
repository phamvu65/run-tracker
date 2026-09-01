package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lap của một activity, chia tự động theo km hoặc thủ công.
 */
@Entity(tableName = "activity_laps")
data class ActivityLapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: String,
    val lapIndex: Int,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val avgPaceSecPerKm: Double,
    val avgHeartRate: Int?
)
