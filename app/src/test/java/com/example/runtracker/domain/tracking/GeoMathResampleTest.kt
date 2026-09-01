package com.example.runtracker.domain.tracking

import com.example.runtracker.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathResampleTest {

    private val metersPerDegree = 6_371_000.0 * Math.PI / 180.0
    private fun geo(meters: Double) = GeoPoint(0.0, meters / metersPerDegree)

    @Test
    fun `resample returns evenly spaced points along the path`() {
        val dense = (0..100).map { geo(it * 10.0) } // 0..1000 m, 101 điểm

        val sampled = GeoMath.resample(dense, count = 5)

        assertEquals(5, sampled.size)
        val spacings = (1 until sampled.size).map {
            GeoMath.distanceMeters(sampled[it - 1], sampled[it])
        }
        spacings.forEach { assertEquals(250.0, it, 5.0) }
    }

    @Test
    fun `resample keeps the list when already short`() {
        val points = listOf(geo(0.0), geo(500.0))
        assertEquals(points, GeoMath.resample(points, count = 10))
    }
}
