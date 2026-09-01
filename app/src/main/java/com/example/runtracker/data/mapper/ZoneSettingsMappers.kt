package com.example.runtracker.data.mapper

import com.example.runtracker.data.local.entity.UserZoneSettingsEntity
import com.example.runtracker.domain.training.HeartRateZone
import com.example.runtracker.domain.training.HeartRateZones

fun UserZoneSettingsEntity.toDomain(): HeartRateZones = HeartRateZones(
    userId = userId,
    zones = listOf(
        HeartRateZone(1, zone1Min, zone1Max),
        HeartRateZone(2, zone2Min, zone2Max),
        HeartRateZone(3, zone3Min, zone3Max),
        HeartRateZone(4, zone4Min, zone4Max),
        HeartRateZone(5, zone5Min, zone5Max),
    ),
    thresholdPaceSecPerKm = thresholdPaceSecPerKm,
    updatedAt = updatedAt,
)

fun HeartRateZones.toEntity(): UserZoneSettingsEntity {
    require(zones.size == 5) { "expected exactly 5 heart-rate zones, got ${zones.size}" }
    val z = zones.sortedBy { it.index }
    return UserZoneSettingsEntity(
        userId = userId,
        zone1Min = z[0].minBpm, zone1Max = z[0].maxBpm,
        zone2Min = z[1].minBpm, zone2Max = z[1].maxBpm,
        zone3Min = z[2].minBpm, zone3Max = z[2].maxBpm,
        zone4Min = z[3].minBpm, zone4Max = z[3].maxBpm,
        zone5Min = z[4].minBpm, zone5Max = z[4].maxBpm,
        thresholdPaceSecPerKm = thresholdPaceSecPerKm,
        updatedAt = updatedAt,
    )
}
