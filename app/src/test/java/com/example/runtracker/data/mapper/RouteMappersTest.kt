package com.example.runtracker.data.mapper

import com.example.runtracker.data.local.entity.RouteEntity
import com.example.runtracker.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class RouteMappersTest {
    @Test fun `route roundtrip retains input geometry separately from turn instructions`() {
        val points = listOf(GeoPoint(10.0, 106.0), GeoPoint(10.01, 106.01))
        val route = Route("r", "u", "Lake", 100.0, 0.0, points, false, 1L,
            listOf(RouteWaypoint(0, points.last(), "Finish")), TravelMode.CYCLING,
            points.reversed(), drawnFromSketch = true, snappedToRoads = true)
        assertEquals(route, route.toRouteEntity().toDomain(route.toWaypointEntities()))
    }

    @Test fun `old route defaults stay unverified rather than guessing its travel mode`() {
        val entity = RouteEntity("r", "u", "old", 10.0, 0.0, "", false, 1L)
        val route = entity.toDomain(emptyList())
        assertNull(route.travelMode)
        assertTrue(route.sourcePoints.isEmpty())
        assertFalse(route.snappedToRoads)
    }
}
