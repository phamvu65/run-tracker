package com.example.runtracker.tracking

import com.example.runtracker.core.formatClock
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingStateTest {

    @Test
    fun `avg pace is moving time over distance in km`() {
        val state = TrackingState(distanceMeters = 2_000.0, movingTimeSeconds = 600)
        assertEquals(300.0, state.avgPaceSecPerKm, 0.001) // 5:00 /km
    }

    @Test
    fun `avg pace is zero before any distance`() {
        assertEquals(0.0, TrackingState(movingTimeSeconds = 120).avgPaceSecPerKm, 0.0)
    }

    @Test
    fun `formatClock switches to hours when needed`() {
        assertEquals("0:00", formatClock(0))
        assertEquals("9:05", formatClock(545))
        assertEquals("1:00:00", formatClock(3_600))
        assertEquals("2:03:04", formatClock(7_384))
    }
}
