package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.HeartRateSample
import kotlin.math.roundToInt

/** Một vùng nhịp tim (bpm). `index` 1..5. */
data class HeartRateZone(val index: Int, val minBpm: Int, val maxBpm: Int)

data class HeartRateZones(
    val userId: String,
    val zones: List<HeartRateZone>, // đúng 5 vùng, index 1..5
    val thresholdPaceSecPerKm: Double?,
    val updatedAt: Long,
)

/** Thời gian (giây) ở một vùng. */
data class ZoneTime(val zoneIndex: Int, val seconds: Long)

object ZoneCalculator {

    const val ZONE_COUNT = 5
    const val FALLBACK_MAX_HR = 190

    /** Ranh giới mặc định theo % nhịp tim tối đa: 50/60/70/80/90/100. */
    private val DEFAULT_BOUNDS = listOf(0.50, 0.60, 0.70, 0.80, 0.90, 1.00)

    fun defaultZones(maxHr: Int): List<HeartRateZone> =
        (1..ZONE_COUNT).map { i ->
            HeartRateZone(
                index = i,
                minBpm = (maxHr * DEFAULT_BOUNDS[i - 1]).roundToInt(),
                maxBpm = (maxHr * DEFAULT_BOUNDS[i]).roundToInt(),
            )
        }
}

object ZoneDistribution {

    /** Khoảng cách tối đa giữa 2 mẫu HR được tính vào 1 vùng (giây) — tránh gap dữ liệu làm lệch. */
    const val MAX_INTERVAL_SECONDS = 30L

    fun compute(samples: List<HeartRateSample>, zones: List<HeartRateZone>): List<ZoneTime> {
        if (samples.size < 2 || zones.isEmpty()) return emptyList()

        val sorted = samples.sortedBy { it.timestamp }
        val secondsPerZone = LongArray(zones.size)

        for (i in 1 until sorted.size) {
            val dt = (sorted[i].timestamp.toEpochMilli() - sorted[i - 1].timestamp.toEpochMilli()) / 1000
            if (dt <= 0) continue
            val capped = dt.coerceAtMost(MAX_INTERVAL_SECONDS)
            secondsPerZone[zoneIndexOf(sorted[i - 1].bpm, zones)] += capped
        }

        return zones.mapIndexed { i, zone -> ZoneTime(zone.index, secondsPerZone[i]) }
    }

    private fun zoneIndexOf(bpm: Int, zones: List<HeartRateZone>): Int {
        zones.forEachIndexed { i, zone -> if (bpm <= zone.maxBpm) return i }
        return zones.lastIndex
    }
}
