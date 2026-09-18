package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalorieEstimatorTest {

    @Test
    fun `running 5km in 30 minutes at 70kg burns a plausible amount`() {
        val calories = CalorieEstimator.estimate(
            type = ActivityType.RUNNING,
            distanceMeters = 5_000.0,
            movingTimeSeconds = 30 * 60L,
            weightKg = 70.0,
        )
        // ~10 km/h pace, 70kg, 30 min -> khoảng 300-400 kcal là hợp lý cho công thức ACSM.
        assertEquals(true, calories != null && calories in 250..450)
    }

    @Test
    fun `heavier person burns more calories for the same run`() {
        val light = CalorieEstimator.estimate(ActivityType.RUNNING, 5_000.0, 1_800L, 55.0)!!
        val heavy = CalorieEstimator.estimate(ActivityType.RUNNING, 5_000.0, 1_800L, 90.0)!!
        assertEquals(true, heavy > light)
    }

    @Test
    fun `null weight returns null`() {
        assertNull(CalorieEstimator.estimate(ActivityType.RUNNING, 5_000.0, 1_800L, null))
    }

    @Test
    fun `zero distance returns null`() {
        assertNull(CalorieEstimator.estimate(ActivityType.RUNNING, 0.0, 1_800L, 70.0))
    }

    @Test
    fun `cycling uses a fixed moderate MET`() {
        val calories = CalorieEstimator.estimate(
            type = ActivityType.CYCLING,
            distanceMeters = 20_000.0,
            movingTimeSeconds = 60 * 60L,
            weightKg = 70.0,
        )
        assertEquals(true, calories != null && calories > 0)
    }
}
