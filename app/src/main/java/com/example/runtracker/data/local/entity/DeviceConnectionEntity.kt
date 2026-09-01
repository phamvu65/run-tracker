package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Kết nối đồng hồ / vòng đeo qua Health Connect hoặc BLE.
 */
@Entity(tableName = "device_connections")
data class DeviceConnectionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val deviceType: String,     // "HEALTH_CONNECT", "BLE_HR_STRAP", "GARMIN"...
    val deviceName: String,
    val isActive: Boolean,
    val lastSyncedAt: Long?
)
