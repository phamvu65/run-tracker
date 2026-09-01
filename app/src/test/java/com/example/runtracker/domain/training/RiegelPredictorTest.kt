package com.example.runtracker.domain.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RiegelPredictorTest {

    @Test
    fun `same distance returns the base time`() {
        assertEquals(
            1200.0,
            RiegelPredictor.predictSeconds(5_000.0, 1_200.0, 5_000.0),
            1e-6,
        )
    }

    @Test
    fun `doubling the distance scales by two to the 1_06`() {
        // 1200 * 2^1.06 ≈ 2501.9
        assertEquals(
            2501.9,
            RiegelPredictor.predictSeconds(5_000.0, 1_200.0, 10_000.0),
            0.5,
        )
    }

    @Test
    fun `half marathon from a 5k effort`() {
        // 1200 * (21097.5/5000)^1.06 ≈ 5520.2 s  (~1:32:00)
        assertEquals(
            5520.24,
            RiegelPredictor.predictSeconds(5_000.0, 1_200.0, 21_097.5),
            0.1,
        )
    }

    @Test
    fun `non-positive base is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            RiegelPredictor.predictSeconds(0.0, 1_200.0, 5_000.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RiegelPredictor.predictSeconds(5_000.0, 0.0, 5_000.0)
        }
    }
}
