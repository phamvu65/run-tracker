package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.HeartRateSample
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HeartRateZonesTest {

    @Test
    fun `default zones split max heart rate into 10 percent bands`() {
        val zones = ZoneCalculator.defaultZones(maxHr = 200)

        assertEquals(5, zones.size)
        assertEquals(HeartRateZone(1, 100, 120), zones[0])
        assertEquals(HeartRateZone(3, 140, 160), zones[2])
        assertEquals(HeartRateZone(5, 180, 200), zones[4])
    }

    private val base: Instant = Instant.parse("2026-09-01T00:00:00Z")
    private fun sample(bpm: Int, second: Long) =
        HeartRateSample(bpm = bpm, timestamp = base.plusSeconds(second))

    private val zones = ZoneCalculator.defaultZones(200) // Z1 100-120 ... Z5 180-200

    @Test
    fun `distribution attributes each interval to the zone of the earlier sample`() {
        val samples = listOf(
            sample(110, 0),   // Z1
            sample(150, 10),  // interval [0,10] -> Z1 (10s)
            sample(190, 20),  // interval [10,20] -> Z3 (10s)
            sample(190, 30),  // interval [20,30] -> Z5 (10s)
        )

        val dist = ZoneDistribution.compute(samples, zones).associate { it.zoneIndex to it.seconds }

        assertEquals(10L, dist[1])
        assertEquals(0L, dist[2])
        assertEquals(10L, dist[3])
        assertEquals(0L, dist[4])
        assertEquals(10L, dist[5])
    }

    @Test
    fun `long gaps between samples are capped`() {
        val samples = listOf(sample(150, 0), sample(150, 600)) // 10 phút gap
        val dist = ZoneDistribution.compute(samples, zones).associate { it.zoneIndex to it.seconds }
        assertEquals(ZoneDistribution.MAX_INTERVAL_SECONDS, dist[3])
    }

    @Test
    fun `fewer than two samples yields nothing`() {
        assertEquals(emptyList<ZoneTime>(), ZoneDistribution.compute(listOf(sample(150, 0)), zones))
    }
}
