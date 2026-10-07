package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

/**
 * Route do người dùng tự dựng (Route Builder) — khác RoutePoint đã ghi khi chạy.
 */
@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val distanceMeters: Double,
    val elevationGainMeters: Double,
    val polyline: String,
    val isPublic: Boolean = false,
    val createdAt: Long,
    val travelMode: String? = null,
    val sourcePolyline: String? = null,
    @ColumnInfo(defaultValue = "0") val drawnFromSketch: Boolean = false,
    @ColumnInfo(defaultValue = "0") val snappedToRoads: Boolean = false,
)
