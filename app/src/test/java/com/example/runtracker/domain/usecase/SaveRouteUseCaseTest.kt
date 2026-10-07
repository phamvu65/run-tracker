package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.*
import com.example.runtracker.domain.repository.RouteRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SaveRouteUseCaseTest {
    @Test fun `unverified straight-line route cannot enter saved routes`() = runTest {
        var written = false
        val repository = object : RouteRepository {
            override fun observeRoutes(userId: String) = flowOf(emptyList<Route>())
            override suspend fun getRoute(routeId: String): Route? = null
            override suspend fun saveRoute(route: Route) { written = true }
            override suspend fun deleteRoute(routeId: String) = Unit
        }
        val points = listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.01))
        try {
            SaveRouteUseCase(repository)("Lake", PlannedRoute(points, 1000.0, emptyList(), false), points, TravelMode.WALKING)
            fail("Unverified route was accepted")
        } catch (_: IllegalArgumentException) { }
        assertFalse(written)
    }
}
