package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.domain.repository.DirectionsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildRouteUseCaseTest {

    private val metersPerDegree = 6_371_000.0 * Math.PI / 180.0
    private fun geo(meters: Double) = GeoPoint(0.0, meters / metersPerDegree)

    @Test
    fun `uses directions result when available`() = runTest {
        val planned = PlannedRoute(listOf(geo(0.0), geo(500.0)), 480.0, emptyList(), snappedToRoads = true)
        val useCase = BuildRouteUseCase(FakeDirections(Result.success(planned)))

        val result = useCase.invoke(listOf(geo(0.0), geo(500.0)), TravelMode.WALKING)

        assertEquals(planned, result)
    }

    @Test
    fun `falls back to straight-line distance when directions fails`() = runTest {
        val useCase = BuildRouteUseCase(FakeDirections(Result.failure(RuntimeException("offline"))))

        val result = useCase.invoke(listOf(geo(0.0), geo(300.0), geo(700.0)), TravelMode.WALKING)

        assertFalse(result.snappedToRoads)
        assertEquals(700.0, result.distanceMeters, 1.0)
        assertTrue(result.steps.isEmpty())
    }

    @Test
    fun `fewer than two points yields an empty route`() = runTest {
        val useCase = BuildRouteUseCase(FakeDirections(Result.failure(RuntimeException())))
        val result = useCase.invoke(listOf(geo(0.0)), TravelMode.WALKING)
        assertEquals(0.0, result.distanceMeters, 0.0)
    }

    private class FakeDirections(private val response: Result<PlannedRoute>) : DirectionsRepository {
        override suspend fun route(waypoints: List<GeoPoint>, mode: TravelMode) = response
    }
}
