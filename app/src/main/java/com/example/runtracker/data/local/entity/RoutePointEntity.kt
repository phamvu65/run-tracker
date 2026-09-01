package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * GPS trace thô của một activity. Lọc nhiễu (accuracy > 20-30m, tốc độ bất thường)
 * TRƯỚC khi insert để không làm sai distanceMeters.
 */
@Entity(
    tableName = "route_points",
    foreignKeys = [ForeignKey(
        entity = ActivityEntity::class,
        parentColumns = ["id"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("activityId")]
)
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speedMps: Float?,
    val accuracyMeters: Float?,
    val timestamp: Long
)
