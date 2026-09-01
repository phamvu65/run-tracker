package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Đoạn đường (segment) để so kè thành tích. `polyline` (encoded) dùng để match GPS trace.
 */
@Entity(tableName = "segments")
data class SegmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startLat: Double, val startLng: Double,
    val endLat: Double, val endLng: Double,
    val distanceMeters: Double,
    val avgGrade: Double,
    val polyline: String,
    val createdByUserId: String,
    val isPublic: Boolean = true
)
