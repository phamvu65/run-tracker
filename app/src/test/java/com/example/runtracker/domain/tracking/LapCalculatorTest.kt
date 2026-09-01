package com.example.runtracker.domain.tracking

import com.example.runtracker.domain.model.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LapCalculatorTest {

    private val base: Instant = Instant.parse("2026-09-01T00:00:00Z")

    /** Điểm nằm trên xích đạo, cách gốc `meters` về phía đông (khớp haversine của GeoMath). */
    private val metersPerDegree = 6_371_000.0 * Math.PI / 180.0

    private fun point(meters: Double, atSecond: Long) = RoutePoint(
        latitude = 0.0,
        longitude = meters / metersPerDegree,
        altitude = 0.0,
        speedMps = null,
        accuracyMeters = 5f,
        timestamp = base.plusSeconds(atSecond),
    )

    @Test
    fun `splits into full km laps plus a partial final lap`() {
        val laps = LapCalculator.splitByDistance(
            listOf(
                point(0.0, 0),
                point(1_000.0, 300),
                point(2_000.0, 650),
                point(2_500.0, 800),
            ),
        )

        assertEquals(3, laps.size)
        assertEquals(1_000.0, laps[0].distanceMeters, 0.5)
        assertEquals(300, laps[0].duration.inWholeSeconds)
        assertEquals(300.0, laps[0].avgPaceSecPerKm, 0.5)
        assertEquals(350, laps[1].duration.inWholeSeconds)
        assertEquals(500.0, laps[2].distanceMeters, 2.0)
        assertEquals(300.0, laps[2].avgPaceSecPerKm, 3.0)
        assertEquals(listOf(1, 2, 3), laps.map { it.lapIndex })
    }

    @Test
    fun `interpolates multiple boundaries inside one segment`() {
        val laps = LapCalculator.splitByDistance(
            listOf(point(0.0, 0), point(3_000.0, 900)),
        )

        assertEquals(3, laps.size)
        laps.forEach {
            assertEquals(1_000.0, it.distanceMeters, 0.5)
            assertEquals(300, it.duration.inWholeSeconds)
        }
    }

    @Test
    fun `single sub-kilometre run yields one lap`() {
        val laps = LapCalculator.splitByDistance(
            listOf(point(0.0, 0), point(400.0, 120)),
        )

        assertEquals(1, laps.size)
        assertEquals(400.0, laps[0].distanceMeters, 1.0)
        assertTrue(laps[0].avgPaceSecPerKm in 290.0..310.0)
    }

    @Test
    fun `fewer than two points yields no laps`() {
        assertEquals(emptyList<Any>(), LapCalculator.splitByDistance(listOf(point(0.0, 0))))
        assertEquals(emptyList<Any>(), LapCalculator.splitByDistance(emptyList()))
    }
}
