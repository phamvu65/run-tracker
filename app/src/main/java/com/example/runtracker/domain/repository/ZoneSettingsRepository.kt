package com.example.runtracker.domain.repository

import com.example.runtracker.domain.training.HeartRateZones
import kotlinx.coroutines.flow.Flow

interface ZoneSettingsRepository {

    fun observeZones(userId: String): Flow<HeartRateZones?>

    suspend fun getZones(userId: String): HeartRateZones?

    /** Trả về cấu hình vùng; tạo mặc định theo [maxHr] nếu chưa có. */
    suspend fun ensureZones(userId: String, maxHr: Int?): HeartRateZones

    suspend fun saveZones(zones: HeartRateZones)
}
