package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.model.BadgeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes

class BadgeEvaluatorTest {

    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    private fun activityOn(date: LocalDate, distanceMeters: Double, hour: Int = 7): Activity {
        val start = date.atStartOfDay(zone).plusHours(hour.toLong()).toInstant()
        return Activity(
            id = "a-$date-$distanceMeters-$hour",
            userId = "u",
            type = ActivityType.RUNNING,
            startTime = start,
            endTime = start.plusSeconds(1800),
            distanceMeters = distanceMeters,
            duration = 30.minutes,
            movingTime = 28.minutes,
            avgPaceSecPerKm = 300.0,
            avgSpeedKmh = 10.0,
            elevationGainMeters = 0.0,
            elevationLossMeters = 0.0,
            avgHeartRate = null,
            maxHeartRate = null,
            calories = null,
            steps = null,
            avgCadence = null,
            perceivedExertion = null,
            weather = null,
            gpxRawPath = null,
        )
    }

    @Test
    fun `first activity unlocks FIRST_ACTIVITY`() {
        val activities = listOf(activityOn(LocalDate.of(2026, 9, 1), 3_000.0))
        val newly = BadgeEvaluator.evaluateForLatest(activities, emptySet(), zone)
        assertTrue(BadgeType.FIRST_ACTIVITY in newly)
    }

    @Test
    fun `total distance crossing 10km unlocks DISTANCE_10_KM`() {
        val activities = listOf(
            activityOn(LocalDate.of(2026, 9, 1), 6_000.0),
            activityOn(LocalDate.of(2026, 9, 3), 5_000.0),
        )
        val newly = BadgeEvaluator.evaluateForLatest(
            activities,
            alreadyUnlocked = setOf(BadgeType.FIRST_ACTIVITY),
            zoneId = zone,
        )
        assertTrue(BadgeType.DISTANCE_10_KM in newly)
    }

    @Test
    fun `does not re-unlock a badge already in alreadyUnlocked`() {
        val activities = listOf(activityOn(LocalDate.of(2026, 9, 1), 3_000.0))
        val newly = BadgeEvaluator.evaluateForLatest(
            activities,
            alreadyUnlocked = setOf(BadgeType.FIRST_ACTIVITY),
            zoneId = zone,
        )
        assertFalse(BadgeType.FIRST_ACTIVITY in newly)
    }

    @Test
    fun `marathon distance unlocks FIRST_MARATHON`() {
        val activities = listOf(activityOn(LocalDate.of(2026, 9, 1), 42_195.0))
        val newly = BadgeEvaluator.evaluateForLatest(activities, emptySet(), zone)
        assertTrue(BadgeType.FIRST_MARATHON in newly)
    }

    @Test
    fun `activity starting before 6am unlocks EARLY_BIRD`() {
        val activities = listOf(activityOn(LocalDate.of(2026, 9, 1), 3_000.0, hour = 5))
        val newly = BadgeEvaluator.evaluateForLatest(activities, emptySet(), zone)
        assertTrue(BadgeType.EARLY_BIRD in newly)
    }

    @Test
    fun `activity starting at 6am or later does not unlock EARLY_BIRD`() {
        val activities = listOf(activityOn(LocalDate.of(2026, 9, 1), 3_000.0, hour = 6))
        val newly = BadgeEvaluator.evaluateForLatest(activities, emptySet(), zone)
        assertFalse(BadgeType.EARLY_BIRD in newly)
    }

    @Test
    fun `7 consecutive days unlocks STREAK_7`() {
        val start = LocalDate.of(2026, 9, 1)
        val activities = (0 until 7).map { activityOn(start.plusDays(it.toLong()), 2_000.0) }
        val newly = BadgeEvaluator.evaluateForLatest(activities, emptySet(), zone)
        assertTrue(BadgeType.STREAK_7 in newly)
        assertFalse(BadgeType.STREAK_30 in newly)
    }

    @Test
    fun `a gap in days resets the streak`() {
        val activities = listOf(
            activityOn(LocalDate.of(2026, 9, 1), 2_000.0),
            activityOn(LocalDate.of(2026, 9, 2), 2_000.0),
            // lỗ ngày 3 - 8
            activityOn(LocalDate.of(2026, 9, 9), 2_000.0),
        )
        val newly = BadgeEvaluator.evaluateForLatest(activities, emptySet(), zone)
        assertFalse(BadgeType.STREAK_7 in newly)
    }

    @Test
    fun `empty activity list unlocks nothing`() {
        val newly = BadgeEvaluator.evaluateForLatest(emptyList(), emptySet(), zone)
        assertEquals(emptyList<BadgeType>(), newly)
    }
}
