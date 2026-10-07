package com.example.runtracker.domain.navigation

import com.example.runtracker.domain.model.*
import com.example.runtracker.domain.tracking.GeoMath
import org.junit.Assert.*
import org.junit.Test

class NavigationSessionTest {
    private fun p(x: Double, y: Double = 0.0) = GeoPoint(y / 111_195.0, x / 111_195.0)
    private fun route(mode: TravelMode = TravelMode.CYCLING) = Route(
        "r", "u", "route", 1000.0, 0.0, (0..10).map { p(it * 100.0) }, false, 0,
        listOf(RouteWaypoint(0, p(1000.0), "Finish")), mode, snappedToRoads = true)
    private fun result(request: NavigationSession.RerouteRequest): PlannedRoute {
        val line = listOf(request.origin, request.destination)
        return PlannedRoute(line, GeoMath.pathDistanceMeters(line), emptyList(), true)
    }

    @Test fun `reroute uses original travel mode and retains remaining route`() {
        val session = NavigationSession()
        val original = route()
        session.start(original)
        session.update(p(0.0))
        session.update(p(100.0, 100.0))
        val request = session.beginReroute()!!
        assertEquals(TravelMode.CYCLING, request.mode)
        assertTrue(session.applyReroute(request, result(request)))
        val update = session.update(request.origin)!!
        assertEquals(request.origin, update.polyline.first())
        assertEquals(original.polyline.last(), update.polyline.last())
        assertTrue(update.polyline.contains(p(900.0)))
        assertFalse(update.progress.offRoute)
    }

    @Test fun `late request after stop or new session cannot revive old route`() {
        val session = NavigationSession()
        session.start(route())
        session.update(p(0.0, 100.0))
        val request = session.beginReroute()!!
        session.start(null)
        assertFalse(session.applyReroute(request, result(request)))
        assertNull(session.update(p(0.0)))
        session.start(route())
        assertFalse(session.applyReroute(request, result(request)))
    }

    @Test fun `returning to route invalidates pending response`() {
        val session = NavigationSession()
        session.start(route())
        session.update(p(0.0, 100.0))
        val request = session.beginReroute()!!
        session.invalidateRequests()
        assertFalse(session.applyReroute(request, result(request)))
    }

    @Test fun `rejects snapped rejoin on another shore`() {
        val session = NavigationSession()
        session.start(route())
        session.update(p(0.0, 100.0))
        val request = session.beginReroute()!!
        assertFalse(session.applyReroute(request,
            PlannedRoute(listOf(request.origin, p(300.0, 100.0)), 300.0, emptyList(), true)))
    }

    @Test fun `legacy unverified route cannot start navigation`() {
        val session = NavigationSession()
        session.start(route().copy(snappedToRoads = false))
        assertNull(session.update(p(0.0)))
    }
}
