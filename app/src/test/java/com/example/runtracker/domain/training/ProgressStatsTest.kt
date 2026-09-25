package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes

class ProgressStatsTest {

    private val zone = ZoneId.systemDefault()

    private fun activityOn(date: LocalDate, distanceMeters: Double, movingMinutes: Long = 30): Activity {
        val start = date.atStartOfDay(zone).plusHours(7).toInstant()
        return Activity(
            id = "a-$date-$distanceMeters",
            userId = "u",
            type = ActivityType.RUNNING,
            startTime = start,
            endTime = start.plusSeconds(movingMinutes * 60),
            distanceMeters = distanceMeters,
            duration = movingMinutes.minutes,
            movingTime = movingMinutes.minutes,
            avgPaceSecPerKm = 0.0,
            avgSpeedKmh = 0.0,
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
    fun `weekly buckets group by Monday-start week, oldest first`() {
        val monday = LocalDate.of(2026, 9, 14)
        val activities = listOf(
            activityOn(monday.plusDays(2), 5_000.0), // tuần hiện tại
            activityOn(monday.minusDays(5), 3_000.0), // tuần trước (thứ Tư tuần trước)
        )

        val buckets = ProgressStats.buckets(activities, TrendPeriod.WEEK, count = 2, endDate = monday)

        assertEquals(2, buckets.size)
        assertEquals(3_000.0, buckets[0].distanceMeters, 0.001) // tuần trước, cũ nhất trước
        assertEquals(5_000.0, buckets[1].distanceMeters, 0.001) // tuần hiện tại
    }

    @Test
    fun `monthly buckets sum distance within the calendar month`() {
        val activities = listOf(
            activityOn(LocalDate.of(2026, 8, 15), 4_000.0),
            activityOn(LocalDate.of(2026, 9, 1), 6_000.0),
            activityOn(LocalDate.of(2026, 9, 20), 2_000.0),
        )

        val buckets = ProgressStats.buckets(
            activities,
            TrendPeriod.MONTH,
            count = 2,
            endDate = LocalDate.of(2026, 9, 25),
        )

        assertEquals(2, buckets.size)
        assertEquals(4_000.0, buckets[0].distanceMeters, 0.001) // tháng 8
        assertEquals(8_000.0, buckets[1].distanceMeters, 0.001) // tháng 9
    }

    @Test
    fun `bucket with no activity has null avgPace`() {
        val buckets = ProgressStats.buckets(
            emptyList(),
            TrendPeriod.YEAR,
            count = 1,
            endDate = LocalDate.of(2026, 1, 1),
        )

        assertEquals(0.0, buckets[0].distanceMeters, 0.001)
        assertNull(buckets[0].avgPaceSecPerKm)
    }

    @Test
    fun `compare computes distance delta percent and pace delta`() {
        val currentRange = LocalDate.of(2026, 9, 1)..LocalDate.of(2026, 9, 30)
        val previousRange = LocalDate.of(2026, 6, 1)..LocalDate.of(2026, 6, 30)
        val activities = listOf(
            // Tháng 9: 10km trong 3000s -> pace 300 s/km
            activityOn(LocalDate.of(2026, 9, 10), 10_000.0, movingMinutes = 50),
            // Tháng 6: 5km trong 1800s -> pace 360 s/km (chậm hơn tháng 9)
            activityOn(LocalDate.of(2026, 6, 10), 5_000.0, movingMinutes = 30),
        )

        val result = ProgressStats.compare(activities, currentRange, previousRange)

        assertEquals(10_000.0, result.currentDistanceMeters, 0.001)
        assertEquals(5_000.0, result.previousDistanceMeters, 0.001)
        assertEquals(100.0, result.distanceDeltaPct!!, 0.001) // gấp đôi -> +100%
        assertEquals(300.0, result.currentAvgPaceSecPerKm!!, 0.001)
        assertEquals(360.0, result.previousAvgPaceSecPerKm!!, 0.001)
        assertEquals(-60.0, result.paceDeltaSecPerKm!!, 0.001) // âm = nhanh hơn 60s/km
    }

    @Test
    fun `compare returns null delta when previous period has no data`() {
        val currentRange = LocalDate.of(2026, 9, 1)..LocalDate.of(2026, 9, 30)
        val previousRange = LocalDate.of(2026, 6, 1)..LocalDate.of(2026, 6, 30)
        val activities = listOf(activityOn(LocalDate.of(2026, 9, 10), 10_000.0))

        val result = ProgressStats.compare(activities, currentRange, previousRange)

        assertNull(result.distanceDeltaPct)
        assertNull(result.paceDeltaSecPerKm)
        assertTrue(result.previousAvgPaceSecPerKm == null)
    }
}
