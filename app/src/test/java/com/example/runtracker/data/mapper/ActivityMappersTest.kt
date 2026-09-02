package com.example.runtracker.data.mapper

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.model.ActivityWeather
import com.example.runtracker.domain.model.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ActivityMappersTest {

    @Test
    fun `activity survives domain to entity round trip`() {
        val activity = Activity(
            id = "act-1",
            userId = "user-1",
            type = ActivityType.RUNNING,
            startTime = Instant.parse("2026-09-01T06:00:00Z"),
            endTime = Instant.parse("2026-09-01T06:30:00Z"),
            distanceMeters = 5123.4,
            duration = 30.minutes,
            movingTime = 28.minutes + 30.seconds,
            avgPaceSecPerKm = 333.7,
            avgSpeedKmh = 10.8,
            elevationGainMeters = 42.0,
            elevationLossMeters = 40.0,
            avgHeartRate = 155,
            maxHeartRate = 178,
            calories = 320,
            avgCadence = 168,
            perceivedExertion = 6,
            weather = ActivityWeather(
                temperatureC = 27.5,
                apparentTemperatureC = 30.1,
                humidityPct = 78,
                windSpeedMps = 3.4,
                windDirectionDeg = 210,
                weatherCode = 2,
            ),
            gpxRawPath = null,
        )

        val restored = activity.toEntity(updatedAt = 1_000L).toDomain()

        assertEquals(activity, restored)
    }

    @Test
    fun `null weather round trips as null`() {
        val activity = Activity(
            id = "x", userId = "u", type = ActivityType.RUNNING,
            startTime = Instant.EPOCH, endTime = Instant.EPOCH,
            distanceMeters = 0.0, duration = 0.seconds, movingTime = 0.seconds,
            avgPaceSecPerKm = 0.0, avgSpeedKmh = 0.0,
            elevationGainMeters = 0.0, elevationLossMeters = 0.0,
            avgHeartRate = null, maxHeartRate = null, calories = null, avgCadence = null,
            perceivedExertion = null, weather = null, gpxRawPath = null,
        )
        assertEquals(null, activity.toEntity(updatedAt = 0L).toDomain().weather)
    }

    @Test
    fun `unknown activity type maps to OTHER`() {
        val entity = Activity(
            id = "x", userId = "u", type = ActivityType.OTHER,
            startTime = Instant.EPOCH, endTime = Instant.EPOCH,
            distanceMeters = 0.0, duration = 0.seconds, movingTime = 0.seconds,
            avgPaceSecPerKm = 0.0, avgSpeedKmh = 0.0,
            elevationGainMeters = 0.0, elevationLossMeters = 0.0,
            avgHeartRate = null, maxHeartRate = null, calories = null, avgCadence = null,
            perceivedExertion = null, weather = null, gpxRawPath = null,
        ).toEntity(updatedAt = 0L).copy(type = "SWIMMING")

        assertEquals(ActivityType.OTHER, entity.toDomain().type)
    }

    @Test
    fun `route point survives round trip and keeps activity id`() {
        val point = RoutePoint(
            latitude = 10.762622,
            longitude = 106.660172,
            altitude = 12.5,
            speedMps = 3.1f,
            accuracyMeters = 4.0f,
            timestamp = Instant.parse("2026-09-01T06:05:00Z"),
        )

        val entity = point.toEntity(activityId = "act-1")

        assertEquals("act-1", entity.activityId)
        assertEquals(point.copy(id = 0), entity.toDomain().copy(id = 0))
    }
}
