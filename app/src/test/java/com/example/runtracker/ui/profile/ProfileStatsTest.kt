package com.example.runtracker.ui.profile

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ProfileStatsTest {

    private val zone = ZoneId.systemDefault()

    private fun activityOn(date: LocalDate, distanceMeters: Double, steps: Int = 0, avgHeartRate: Int? = null): Activity {
        val start = date.atStartOfDay(zone).plusHours(7).toInstant()
        return Activity(
            id = "a-$date-$distanceMeters",
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
            avgHeartRate = avgHeartRate,
            maxHeartRate = null,
            calories = null,
            steps = steps,
            avgCadence = null,
            perceivedExertion = null,
            weather = null,
            gpxRawPath = null,
        )
    }

    @Test
    fun `weekDays returns exactly 7 days Monday to Sunday`() {
        val monday = LocalDate.of(2026, 9, 14) // biết trước là Thứ Hai
        val activities = listOf(activityOn(monday.plusDays(3), 5_000.0, steps = 6_000))

        val days = weekDays(activities, monday)

        assertEquals(7, days.size)
        assertEquals(monday, days.first().date)
        assertEquals(monday.plusDays(6), days.last().date)
        assertEquals(5_000.0, days[3].distanceMeters, 0.001)
        assertEquals(6_000, days[3].steps)
        assertTrue(days[3].hasActivity)
        assertTrue(!days[0].hasActivity)
    }

    @Test
    fun `monthDays only includes days that had an activity`() {
        val month = YearMonth.of(2026, 9)
        val activities = listOf(
            activityOn(month.atDay(5), 3_000.0),
            activityOn(month.atDay(20), 7_000.0),
            activityOn(month.minusMonths(1).atDay(28), 1_000.0), // tháng trước, phải bị loại
        )

        val grid = monthDays(activities, month)

        assertEquals(2, grid.size)
        assertEquals(3_000.0, grid.getValue(month.atDay(5)).distanceMeters, 0.001)
        assertEquals(7_000.0, grid.getValue(month.atDay(20)).distanceMeters, 0.001)
    }

    @Test
    fun `periodStat computes delta percent against the previous range`() {
        val weekStart = LocalDate.of(2026, 9, 14)
        val prevWeekStart = weekStart.minusDays(7)
        val activities = listOf(
            activityOn(weekStart.plusDays(1), 10_000.0),
            activityOn(prevWeekStart.plusDays(1), 5_000.0),
        )

        val stat = periodStat(
            activities,
            range = weekStart..weekStart.plusDays(6),
            previousRange = prevWeekStart..prevWeekStart.plusDays(6),
        )

        assertEquals(10_000.0, stat.distanceMeters, 0.001)
        assertEquals(100.0, stat.deltaPct!!, 0.001) // gấp đôi tuần trước = +100%
    }

    @Test
    fun `periodStat delta is null when the previous range has no data`() {
        val weekStart = LocalDate.of(2026, 9, 14)
        val activities = listOf(activityOn(weekStart.plusDays(1), 10_000.0))

        val stat = periodStat(
            activities,
            range = weekStart..weekStart.plusDays(6),
            previousRange = weekStart.minusDays(7)..weekStart.minusDays(1),
        )

        assertNull(stat.deltaPct)
    }

    @Test
    fun `monthlyTrend sums distance per calendar month`() {
        val thisMonth = YearMonth.now()
        val activities = listOf(
            activityOn(thisMonth.atDay(1), 4_000.0),
            activityOn(thisMonth.atDay(2), 6_000.0),
        )

        val trend = monthlyTrend(activities, months = 3)

        assertEquals(3, trend.size)
        assertEquals(10.0, trend.last(), 0.001) // tháng hiện tại là phần tử cuối
    }
}
