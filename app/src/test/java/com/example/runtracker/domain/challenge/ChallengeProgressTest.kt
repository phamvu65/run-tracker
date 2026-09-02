package com.example.runtracker.domain.challenge

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ChallengeGoalType
import com.example.runtracker.domain.model.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ChallengeProgressTest {

    private val runs = listOf(
        activity(distance = 5_000.0, elevation = 40.0, moving = 25.minutes),
        activity(distance = 8_000.0, elevation = 120.0, moving = 44.minutes),
        activity(distance = 3_000.0, elevation = 10.0, moving = 15.minutes),
    )

    @Test
    fun `total distance sums meters`() {
        assertEquals(16_000.0, ChallengeProgress.compute(ChallengeGoalType.TOTAL_DISTANCE, runs), 0.001)
    }

    @Test
    fun `total activities counts entries`() {
        assertEquals(3.0, ChallengeProgress.compute(ChallengeGoalType.TOTAL_ACTIVITIES, runs), 0.0)
    }

    @Test
    fun `total elevation sums gain`() {
        assertEquals(170.0, ChallengeProgress.compute(ChallengeGoalType.TOTAL_ELEVATION, runs), 0.001)
    }

    @Test
    fun `total duration uses moving time in seconds`() {
        assertEquals(
            (25 + 44 + 15) * 60.0,
            ChallengeProgress.compute(ChallengeGoalType.TOTAL_DURATION, runs),
            0.001,
        )
    }

    @Test
    fun `total duration falls back to duration when moving time is zero`() {
        val stalled = activity(distance = 1_000.0, elevation = 0.0, moving = 0.seconds, total = 10.minutes)
        assertEquals(600.0, ChallengeProgress.compute(ChallengeGoalType.TOTAL_DURATION, listOf(stalled)), 0.001)
    }

    @Test
    fun `empty list is zero progress`() {
        assertEquals(0.0, ChallengeProgress.compute(ChallengeGoalType.TOTAL_DISTANCE, emptyList()), 0.0)
    }

    private fun activity(
        distance: Double,
        elevation: Double,
        moving: Duration,
        total: Duration = moving,
    ) = Activity(
        id = "a", userId = "u", type = ActivityType.RUNNING,
        startTime = Instant.EPOCH, endTime = Instant.EPOCH,
        distanceMeters = distance,
        duration = total, movingTime = moving,
        avgPaceSecPerKm = 0.0, avgSpeedKmh = 0.0,
        elevationGainMeters = elevation, elevationLossMeters = 0.0,
        avgHeartRate = null, maxHeartRate = null, calories = null, avgCadence = null,
        perceivedExertion = null, weatherTempC = null, gpxRawPath = null,
    )
}
