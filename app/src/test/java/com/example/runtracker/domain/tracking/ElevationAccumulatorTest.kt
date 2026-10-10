package com.example.runtracker.domain.tracking

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ElevationAccumulatorTest {
    private fun accumulate(values: List<Double?>): ElevationAccumulator = ElevationAccumulator().also { accumulator ->
        values.forEachIndexed { index, value -> accumulator.add(value, Instant.ofEpochSecond(index.toLong())) }
    }

    @Test fun `gentle climb accumulates instead of disappearing below threshold`() {
        val values = listOf(100.0) + (0..100).map { 100.0 + it * 0.5 } + 150.0
        assertEquals(50.0, accumulate(values).gainMeters, 0.001)
    }

    @Test fun `GPS oscillation on flat ground is not repeated ascent`() {
        val result = accumulate(List(101) { if (it % 2 == 0) 100.0 else 102.0 })
        assertEquals(0.0, result.gainMeters, 0.001)
        assertEquals(0.0, result.lossMeters, 0.001)
    }

    @Test fun `climb then descent retains both slopes`() {
        val values = listOf(100.0) + (0..20).map { 100.0 + it * 0.5 } +
            listOf(110.0) + (1..20).map { 110.0 - it * 0.5 } + 100.0
        val result = accumulate(values)
        assertEquals(10.0, result.gainMeters, 0.001)
        assertEquals(10.0, result.lossMeters, 0.001)
    }

    @Test fun `missing elevations break the baseline instead of creating a climb`() {
        val result = accumulate(listOf(0.0, 0.0, 0.0, null, 100.0, 100.0, 100.0))
        assertEquals(0.0, result.gainMeters, 0.001)
    }

    @Test fun `isolated GPS spike does not create climb or descent`() {
        val result = accumulate(listOf(100.0, 100.0, 100.0, 180.0, 100.0, 100.0))
        assertEquals(0.0, result.gainMeters, 0.001)
        assertEquals(0.0, result.lossMeters, 0.001)
    }

    @Test fun `gap in tracking does not join unrelated height baselines`() {
        val result = accumulate(listOf(100.0, 100.0, 100.0))
        repeat(3) { result.add(200.0, Instant.ofEpochSecond(100L + it)) }
        assertEquals(0.0, result.gainMeters, 0.001)
    }
}
