package com.example.runtracker.domain.tracking

import org.junit.Assert.assertEquals
import org.junit.Test

class ElevationCorrectionTest {

    @Test
    fun `sampleIndices keeps everything under the cap`() {
        val indices = ElevationCorrection.sampleIndices(distances = (0 until 50).map { it.toDouble() }, maxSamples = 100)
        assertEquals((0 until 50).toList(), indices)
    }

    @Test
    fun `sampleIndices spans the whole trace and stays under the cap`() {
        val indices = ElevationCorrection.sampleIndices(distances = (0 until 1340).map { it.toDouble() }, maxSamples = 100)
        assertEquals(0, indices.first())
        assertEquals(1339, indices.last())
        assert(indices.size <= 100)
        assertEquals(indices.sorted(), indices)
    }

    @Test
    fun `interpolate produces a linear ramp between two samples`() {
        val result = ElevationCorrection.interpolate(
            distances = (0 until 5).map { it.toDouble() },
            sampleIndices = listOf(0, 4),
            sampleElevations = listOf(0.0, 40.0),
        )
        assertEquals(listOf(0.0, 10.0, 20.0, 30.0, 40.0), result)
    }

    @Test
    fun `interpolate passes through multiple samples exactly`() {
        val result = ElevationCorrection.interpolate(
            distances = (0 until 7).map { it.toDouble() },
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
            distances = (0 until 3).map { it.toDouble() },
            sampleIndices = listOf(1),
            sampleElevations = listOf(5.0),
        )
        assertEquals(listOf(5.0, 5.0, 5.0), result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing DEM samples cannot silently fabricate sea level`() {
        ElevationCorrection.interpolate(listOf(0.0, 3.0), emptyList(), emptyList())
    }

    @Test fun `interpolation follows distance not point count`() {
        val result = ElevationCorrection.interpolate(listOf(0.0, 1.0, 90.0, 100.0),
            listOf(0, 3), listOf(100.0, 110.0))
        assertEquals(100.1, result[1], 0.001)
        assertEquals(109.0, result[2], 0.001)
    }

    @Test fun `sampling covers sparse parts as well as dense GPS clusters`() {
        val distances = (0..1000).map { it / 1000.0 } + listOf(100.0, 200.0, 300.0)
        assertEquals(listOf(0, 1001, 1002, 1003), ElevationCorrection.sampleIndices(distances, 4))
    }
}