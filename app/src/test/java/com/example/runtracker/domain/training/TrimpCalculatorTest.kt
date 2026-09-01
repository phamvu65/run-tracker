package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.model.Sex
import com.example.runtracker.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

class TrimpCalculatorTest {

    // ratio = (150-50)/(200-50) = 0.6667
    @Test
    fun `heart-rate trimp for male`() {
        val trimp = TrimpCalculator.calculate(
            durationMinutes = 60.0,
            avgHeartRate = 150, restingHeartRate = 50, maxHeartRate = 200,
            sex = Sex.MALE, perceivedExertion = null,
        )
        assertEquals(92.07, trimp!!, 0.05)
    }

    @Test
    fun `heart-rate trimp for female uses different coefficients`() {
        val trimp = TrimpCalculator.calculate(
            durationMinutes = 60.0,
            avgHeartRate = 150, restingHeartRate = 50, maxHeartRate = 200,
            sex = Sex.FEMALE, perceivedExertion = null,
        )
        assertEquals(104.73, trimp!!, 0.1)
    }

    @Test
    fun `null sex is treated as male`() {
        val male = TrimpCalculator.calculate(60.0, 150, 50, 200, Sex.MALE, null)
        val unknown = TrimpCalculator.calculate(60.0, 150, 50, 200, null, null)
        assertEquals(male!!, unknown!!, 0.0001)
    }

    @Test
    fun `heart rate at or below resting yields zero`() {
        val trimp = TrimpCalculator.calculate(30.0, 45, 50, 190, Sex.MALE, null)
        assertEquals(0.0, trimp!!, 0.0001)
    }

    @Test
    fun `falls back to rpe when heart rate data incomplete`() {
        assertEquals(180.0, TrimpCalculator.calculate(30.0, null, 50, 200, Sex.MALE, 6)!!, 0.0)
        assertEquals(280.0, TrimpCalculator.calculate(40.0, 150, null, 200, Sex.MALE, 7)!!, 0.0)
    }

    @Test
    fun `null when no heart rate and no valid rpe`() {
        assertNull(TrimpCalculator.calculate(30.0, null, 50, 200, Sex.MALE, null))
        assertNull(TrimpCalculator.calculate(30.0, null, null, null, null, 0))
        assertNull(TrimpCalculator.calculate(30.0, null, null, null, null, 15))
    }

    @Test
    fun `zero duration is zero`() {
        assertEquals(0.0, TrimpCalculator.calculate(0.0, 150, 50, 200, Sex.MALE, 8)!!, 0.0)
    }

    @Test
    fun `forActivity uses moving time and estimated max heart rate`() {
        val activity = activity(movingMinutes = 20, totalMinutes = 60, avgHr = null, rpe = 5)
        val user = User(
            id = "u", displayName = "x", email = null, birthYear = 1990, weightKg = null,
            restingHeartRate = 50, maxHeartRate = null, sex = Sex.MALE,
        )
        // no HR -> RPE path: movingMinutes 20 * rpe 5
        assertEquals(100.0, TrimpCalculator.forActivity(activity, user, year = 2026)!!, 0.0)
    }

    private fun activity(movingMinutes: Int, totalMinutes: Int, avgHr: Int?, rpe: Int?) = Activity(
        id = "a", userId = "u", type = ActivityType.RUNNING,
        startTime = Instant.EPOCH, endTime = Instant.EPOCH,
        distanceMeters = 3000.0,
        duration = totalMinutes.minutes, movingTime = movingMinutes.minutes,
        avgPaceSecPerKm = 0.0, avgSpeedKmh = 0.0,
        elevationGainMeters = 0.0, elevationLossMeters = 0.0,
        avgHeartRate = avgHr, maxHeartRate = null, calories = null, avgCadence = null,
        perceivedExertion = rpe, weatherTempC = null, gpxRawPath = null,
    )
}
