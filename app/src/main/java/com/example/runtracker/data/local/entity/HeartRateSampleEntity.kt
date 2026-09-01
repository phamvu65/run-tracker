package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mẫu nhịp tim, tách riêng RoutePoint vì tần suất lấy mẫu HR khác GPS.
 */
@Entity(
    tableName = "heart_rate_samples",
    foreignKeys = [ForeignKey(
        entity = ActivityEntity::class,
        parentColumns = ["id"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("activityId")]
)
data class HeartRateSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: String,
    val bpm: Int,
    val timestamp: Long
)
