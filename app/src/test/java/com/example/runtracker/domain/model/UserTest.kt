package com.example.runtracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserTest {

    private fun user(birthYear: Int? = null, maxHeartRate: Int? = null) = User(
        id = "u", displayName = "x", email = null,
        birthYear = birthYear, weightKg = null, restingHeartRate = null,
        maxHeartRate = maxHeartRate, sex = null,
    )

    @Test
    fun `measured max heart rate wins over estimate`() {
        assertEquals(190, user(birthYear = 1990, maxHeartRate = 190).maxHeartRateOrEstimate(2026))
    }

    @Test
    fun `falls back to 220 minus age`() {
        assertEquals(220 - 36, user(birthYear = 1990).maxHeartRateOrEstimate(2026))
    }

    @Test
    fun `null when neither measured nor birth year present`() {
        assertNull(user().maxHeartRateOrEstimate(2026))
    }
}
