package com.example.runtracker.domain.tracking

import com.example.runtracker.domain.model.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class GpsTrackFilterTest {

    private val base: Instant = Instant.parse("2026-09-01T00:00:00Z")

    /** ~0.00005 deg latitude ≈ 5.6 m — dưới ngưỡng 7 m/s khi cách nhau 1 s. */
    private fun point(
        latOffsetSteps: Int,
        secondsFromBase: Long,
        accuracy: Float? = 5f,
    ) = RoutePoint(
        latitude = 10.0 + latOffsetSteps * 0.00005,
        longitude = 106.0,
        altitude = 0.0,
        speedMps = null,
        accuracyMeters = accuracy,
        timestamp = base.plusSeconds(secondsFromBase),
    )

    private fun List<RoutePoint>.seconds() = map { it.timestamp.epochSecond - base.epochSecond }

    @Test
    fun `drops points with poor accuracy`() {
        val points = listOf(
            point(latOffsetSteps = 0, secondsFromBase = 1, accuracy = 8f),
            point(latOffsetSteps = 1, secondsFromBase = 2, accuracy = 40f), // > 25 m -> drop
            point(latOffsetSteps = 2, secondsFromBase = 3, accuracy = 8f),
        )

        val kept = GpsTrackFilter.sanitize(previous = null, candidates = points)

        assertEquals(listOf(1L, 3L), kept.seconds())
    }

    @Test
    fun `drops implausible speed jumps`() {
        val jump = point(latOffsetSteps = 0, secondsFromBase = 2).copy(latitude = 10.01) // ~1.1 km in 1 s
        val points = listOf(
            point(latOffsetSteps = 0, secondsFromBase = 1),
            jump,
            point(latOffsetSteps = 1, secondsFromBase = 3),
        )

        val kept = GpsTrackFilter.sanitize(previous = null, candidates = points)

        assertEquals(listOf(1L, 3L), kept.seconds())
    }

    @Test
    fun `drops duplicate timestamps`() {
        val points = listOf(
            point(latOffsetSteps = 0, secondsFromBase = 5),
            point(latOffsetSteps = 1, secondsFromBase = 5), // same timestamp -> drop
            point(latOffsetSteps = 1, secondsFromBase = 6),
        )

        val kept = GpsTrackFilter.sanitize(previous = null, candidates = points)

        assertEquals(listOf(5L, 6L), kept.seconds())
    }

    @Test
    fun `uses previous point for continuity across batches`() {
        val previous = point(latOffsetSteps = 0, secondsFromBase = 0)
        val batch = listOf(
            point(latOffsetSteps = 0, secondsFromBase = 1).copy(latitude = 10.01), // jump -> drop
            point(latOffsetSteps = 1, secondsFromBase = 2),
        )

        val kept = GpsTrackFilter.sanitize(previous = previous, candidates = batch)

        assertEquals(listOf(2L), kept.seconds())
    }

    @Test
    fun `empty input yields empty output`() {
        assertEquals(
            emptyList<RoutePoint>(),
            GpsTrackFilter.sanitize(previous = null, candidates = emptyList()),
        )
    }
}
