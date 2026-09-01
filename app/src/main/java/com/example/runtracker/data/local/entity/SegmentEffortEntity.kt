package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Một lần đi qua segment. Leaderboard = ORDER BY elapsedSeconds ASC (KHÔNG giới hạn top 10 như Strava free).
 * `rank` chỉ là cache để hiển thị nhanh.
 */
@Entity(
    tableName = "segment_efforts",
    indices = [Index("segmentId"), Index("userId")]
)
data class SegmentEffortEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val segmentId: String,
    val activityId: String,
    val userId: String,
    val elapsedSeconds: Double,
    val startTime: Long,
    val avgHeartRate: Int?,
    val rank: Int? = null
)
