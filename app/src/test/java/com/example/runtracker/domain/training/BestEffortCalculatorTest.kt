package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class BestEffortCalculatorTest {

    private val base: Instant = Instant.parse("2026-09-01T00:00:00Z")
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
    fun `steady pace over exactly 5km yields that elapsed time`() {
        val results = BestEffortCalculator.compute(
            listOf(point(0.0, 0), point(5_000.0, 1_500)),
            distances = listOf(EffortDistance.FIVE_K),
        )

        assertEquals(1, results.size)
        assertEquals(EffortDistance.FIVE_K, results[0].distance)
        assertEquals(1_500L, results[0].elapsedSeconds)
    }

    @Test
    fun `finds the fastest 1km window even in the middle of a longer run`() {
        // 0-1km chậm (300s), 1-2km nhanh (200s), 2-3km chậm lại (300s)
        val results = BestEffortCalculator.compute(
            listOf(
                point(0.0, 0),
                point(1_000.0, 300),
                point(2_000.0, 500),
                point(3_000.0, 800),
            ),
            distances = listOf(EffortDistance.ONE_K),
        )

        assertEquals(1, results.size)
        assertEquals(200L, results[0].elapsedSeconds)
    }

    @Test
    fun `distance longer than the run is not achievable`() {
        val results = BestEffortCalculator.compute(
            listOf(point(0.0, 0), point(2_000.0, 600)),
            distances = listOf(EffortDistance.FIVE_K),
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun `only returns achievable distances out of a mixed request`() {
        val results = BestEffortCalculator.compute(
            listOf(point(0.0, 0), point(6_000.0, 1_800)),
            distances = listOf(EffortDistance.ONE_K, EffortDistance.FIVE_K, EffortDistance.TEN_K),
        )

        assertEquals(setOf(EffortDistance.ONE_K, EffortDistance.FIVE_K), results.map { it.distance }.toSet())
    }

    @Test
    fun `fewer than two points yields no results`() {
        assertEquals(emptyList<BestEffortWindow>(), BestEffortCalculator.compute(listOf(point(0.0, 0))))
        assertEquals(emptyList<BestEffortWindow>(), BestEffortCalculator.compute(emptyList()))
    }

    @Test
    fun `interpolates the crossing point inside a segment`() {
        // 1 điểm mỗi 500m, tốc độ đều 5 m/s -> 1km mất đúng 200s bất kể mốc cắt rơi giữa đoạn nào.
        val results = BestEffortCalculator.compute(
            listOf(
                point(0.0, 0),
                point(500.0, 100),
                point(1_000.0, 200),
                point(1_500.0, 300),
                point(2_000.0, 400),
            ),
            distances = listOf(EffortDistance.ONE_K),
        )

        assertEquals(200L, results[0].elapsedSeconds)
    }
}
