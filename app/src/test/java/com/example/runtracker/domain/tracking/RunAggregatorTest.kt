package com.example.runtracker.domain.tracking

import com.example.runtracker.domain.model.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class RunAggregatorTest {

    private val base: Instant = Instant.parse("2026-09-01T00:00:00Z")
    private val metersPerDegree = 6_371_000.0 * Math.PI / 180.0

    private fun point(meters: Double, atSecond: Long, altitude: Double = 0.0) = RoutePoint(
        latitude = 0.0,
        longitude = meters / metersPerDegree,
        altitude = altitude,
        speedMps = null,
        accuracyMeters = 5f,
        timestamp = base.plusSeconds(atSecond),
    )

    @Test
    fun `sums distance and counts only moving segments`() {
        val aggregate = RunAggregator.fromPoints(
            listOf(
                point(0.0, 0),
                point(300.0, 100),      // 3 m/s -> moving, +100s
                point(305.0, 400),      // ~0.017 m/s over 300s -> stopped, +0s
                point(605.0, 500),      // 3 m/s -> moving, +100s
            ),
        )

        assertEquals(605.0, aggregate.distanceMeters, 1.0)
        assertEquals(200, aggregate.movingTimeSeconds)
    }

    @Test
    fun `elevation gain and loss ignore sub-threshold noise`() {
        val aggregate = RunAggregator.fromPoints(
            listOf(
                point(0.0, 0, altitude = 100.0),
                point(100.0, 30, altitude = 100.5),   // +0.5m -> ignored
                point(200.0, 60, altitude = 110.0),   // +9.5m -> gain
                point(300.0, 90, altitude = 104.0),   // -6m   -> loss
            ),
        )

        assertEquals(9.5, aggregate.elevationGainMeters, 0.001)
        assertEquals(6.0, aggregate.elevationLossMeters, 0.001)
    }

    @Test
    fun `empty or single point yields zeros`() {
        val aggregate = RunAggregator.fromPoints(listOf(point(0.0, 0)))
        assertEquals(0.0, aggregate.distanceMeters, 0.0)
        assertEquals(0, aggregate.movingTimeSeconds)
    }
}
