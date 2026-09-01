package com.example.runtracker.data.repository

import com.example.runtracker.data.local.dao.UserZoneSettingsDao
import com.example.runtracker.data.mapper.toDomain
import com.example.runtracker.data.mapper.toEntity
import com.example.runtracker.domain.repository.ZoneSettingsRepository
import com.example.runtracker.domain.training.HeartRateZones
import com.example.runtracker.domain.training.ZoneCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZoneSettingsRepositoryImpl @Inject constructor(
    private val dao: UserZoneSettingsDao,
) : ZoneSettingsRepository {

    override fun observeZones(userId: String): Flow<HeartRateZones?> =
        dao.observeForUser(userId).map { it?.toDomain() }

    override suspend fun getZones(userId: String): HeartRateZones? =
        dao.getForUser(userId)?.toDomain()

    override suspend fun ensureZones(userId: String, maxHr: Int?): HeartRateZones {
        dao.getForUser(userId)?.let { return it.toDomain() }
        val zones = HeartRateZones(
            userId = userId,
            zones = ZoneCalculator.defaultZones(maxHr ?: ZoneCalculator.FALLBACK_MAX_HR),
            thresholdPaceSecPerKm = null,
            updatedAt = System.currentTimeMillis(),
        )
        dao.upsert(zones.toEntity())
        return zones
    }

    override suspend fun saveZones(zones: HeartRateZones) {
        dao.upsert(zones.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }
}
