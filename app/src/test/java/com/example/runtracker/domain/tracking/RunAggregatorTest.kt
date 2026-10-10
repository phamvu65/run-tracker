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
    fun `elevation gain and loss preserve gradual climbs after DEM interpolation`() {
        val aggregate = RunAggregator.fromPoints(
            listOf(
                point(0.0, 0, altitude = 100.0),
            ) + (0..20).map { point((it + 1) * 3.0, it + 1L, 100.0 + it * 0.5) } +
                point(66.0, 22, 110.0),
        )

        assertEquals(10.0, aggregate.elevationGainMeters, 0.001)
        assertEquals(0.0, aggregate.elevationLossMeters, 0.001)
    }

    @Test
    fun `empty or single point yields zeros`() {
        val aggregate = RunAggregator.fromPoints(listOf(point(0.0, 0)))
        assertEquals(0.0, aggregate.distanceMeters, 0.0)
        assertEquals(0, aggregate.movingTimeSeconds)
    }

    @Test fun `live accumulation and restoration match final calculation`() {
        val points = (0..200).map { i ->
            point(i * 3.0, i.toLong(), if (i <= 100) 100.0 + i * 0.5 else 200.0 - i * 0.5)
        }
        val live = ElevationAccumulator()
        val restored = ElevationAccumulator()
        points.take(80).forEach { restored.add(it.altitude, it.timestamp) }
        points.drop(80).forEach { restored.add(it.altitude, it.timestamp) }
        points.forEach { live.add(it.altitude, it.timestamp) }
        val final = RunAggregator.fromPoints(points)
        assertEquals(final.elevationGainMeters, live.gainMeters, 0.001)
        assertEquals(final.elevationLossMeters, live.lossMeters, 0.001)
        assertEquals(final.elevationGainMeters, restored.gainMeters, 0.001)
        assertEquals(final.elevationLossMeters, restored.lossMeters, 0.001)
    }
}
