package com.example.runtracker.domain.tracking

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.model.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

class GpxWriterTest {

    private val activity = Activity(
        id = "a1",
        userId = "u1",
        type = ActivityType.RUNNING,
        startTime = Instant.parse("2026-09-20T17:29:00Z"),
        endTime = Instant.parse("2026-09-20T18:05:00Z"),
        distanceMeters = 4780.0,
        duration = 36.minutes,
        movingTime = 36.minutes,
        avgPaceSecPerKm = 452.0,
        avgSpeedKmh = 8.0,
        elevationGainMeters = 10.0,
        elevationLossMeters = 8.0,
        avgHeartRate = null,
        maxHeartRate = null,
        calories = null,
        steps = null,
        avgCadence = null,
        perceivedExertion = null,
        weather = null,
        gpxRawPath = null,
    )

    private val points = listOf(
        RoutePoint(
            latitude = 20.9,
            longitude = 105.8,
            altitude = 12.5,
            speedMps = null,
            accuracyMeters = null,
            timestamp = Instant.parse("2026-09-20T17:29:00Z"),
        ),
        RoutePoint(
            latitude = 20.901,
            longitude = 105.801,
            altitude = 13.0,
            speedMps = null,
            accuracyMeters = null,
            timestamp = Instant.parse("2026-09-20T17:29:03Z"),
        ),
    )

    @Test
    fun `writes well-formed gpx with track points`() {
        val xml = GpxWriter.write(activity, points)

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(xml.contains("<gpx version=\"1.1\""))
        assertEquals(2, Regex("<trkpt ").findAll(xml).count())
        assertTrue(xml.contains("lat=\"20.9\" lon=\"105.8\""))
        assertTrue(xml.contains("<ele>12.5</ele>"))
        assertTrue(xml.contains("<time>2026-09-20T17:29:00Z</time>"))
        assertTrue(xml.contains("<name>RUNNING 2026-09-20T17:29:00Z</name>"))
    }

    @Test
    fun `writes an empty track segment when there are no points`() {
        val xml = GpxWriter.write(activity, emptyList())
        assertTrue(xml.contains("<trkseg>\n    </trkseg>"))
        assertEquals(0, Regex("<trkpt ").findAll(xml).count())
    }
}
