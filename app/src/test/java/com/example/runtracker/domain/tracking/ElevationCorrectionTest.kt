package com.example.runtracker.domain.tracking

import org.junit.Assert.assertEquals
import org.junit.Test

class ElevationCorrectionTest {

    @Test
    fun `sampleIndices keeps everything under the cap`() {
        val indices = ElevationCorrection.sampleIndices(size = 50, maxSamples = 100)
        assertEquals((0 until 50).toList(), indices)
    }

    @Test
    fun `sampleIndices spans the whole trace and stays under the cap`() {
        val indices = ElevationCorrection.sampleIndices(size = 1340, maxSamples = 100)
        assertEquals(0, indices.first())
        assertEquals(1339, indices.last())
        assert(indices.size <= 100)
        assertEquals(indices.sorted(), indices)
    }

    @Test
    fun `interpolate produces a linear ramp between two samples`() {
        val result = ElevationCorrection.interpolate(
            size = 5,
            sampleIndices = listOf(0, 4),
            sampleElevations = listOf(0.0, 40.0),
        )
        assertEquals(listOf(0.0, 10.0, 20.0, 30.0, 40.0), result)
    }

    @Test
    fun `interpolate passes through multiple samples exactly`() {
        val result = ElevationCorrection.interpolate(
            size = 7,
            sampleIndices = listOf(0, 3, 6),
            sampleElevations = listOf(10.0, 10.0, 40.0),
        )
        assertEquals(10.0, result[0], 0.001)
        assertEquals(10.0, result[3], 0.001)
        assertEquals(40.0, result[6], 0.001)
        assertEquals(20.0, result[4], 0.001)
    }

    @Test
    fun `interpolate with a single sample fills every point with it`() {
        val result = ElevationCorrection.interpolate(
            size = 3,
            sampleIndices = listOf(1),
            sampleElevations = listOf(5.0),
        )
        assertEquals(listOf(5.0, 5.0, 5.0), result)
    }

    @Test
    fun `interpolate with no samples returns zeros without crashing`() {
        val result = ElevationCorrection.interpolate(size = 3, sampleIndices = emptyList(), sampleElevations = emptyList())
        assertEquals(listOf(0.0, 0.0, 0.0), result)
    }
}
