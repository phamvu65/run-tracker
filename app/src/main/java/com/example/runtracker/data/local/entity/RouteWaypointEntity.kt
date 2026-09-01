package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Điểm mốc trong route, có `instruction` cho turn-by-turn navigation.
 */
@Entity(
    tableName = "route_waypoints",
    foreignKeys = [ForeignKey(
        entity = RouteEntity::class,
        parentColumns = ["id"],
        childColumns = ["routeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("routeId")]
)
data class RouteWaypointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: String,
    val orderIndex: Int,
    val latitude: Double,
    val longitude: Double,
    val instruction: String?   // "Rẽ trái vào Nguyễn Trãi"
)
