package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cấu hình vùng nhịp tim / pace theo từng người — nền tảng cho mọi phân tích Phase 2.
 * Tách bảng riêng thay vì hard-code: zone phải cấu hình được theo từng user.
 */
@Entity(tableName = "user_zone_settings")
data class UserZoneSettingsEntity(
    @PrimaryKey val userId: String,
    val zone1Min: Int, val zone1Max: Int,
    val zone2Min: Int, val zone2Max: Int,
    val zone3Min: Int, val zone3Max: Int,
    val zone4Min: Int, val zone4Max: Int,
    val zone5Min: Int, val zone5Max: Int,
    val thresholdPaceSecPerKm: Double?, // pace ngưỡng (FTP-pace), dùng cho Performance Prediction
    val updatedAt: Long
)
