package com.example.runtracker.domain.navigation

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.RouteWaypoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteNavigatorTest {

    private val metersPerDegree = 6_371_000.0 * Math.PI / 180.0
    private fun geo(meters: Double) = GeoPoint(0.0, meters / metersPerDegree)

    private val polyline = (0..10).map { geo(it * 100.0) } // đường thẳng 0..1000 m trên xích đạo
    private val steps = listOf(
        RouteWaypoint(0, geo(300.0), "Rẽ phải"),
        RouteWaypoint(1, geo(700.0), "Rẽ trái"),
        RouteWaypoint(2, geo(1_000.0), "Về đích"),
    )

    @Test
    fun `points to the current step with distance`() {
        val p = RouteNavigator.progress(geo(100.0), steps, polyline, currentStepIndex = 0)

        assertEquals(0, p.stepIndex)
        assertEquals("Rẽ phải", p.nextInstruction)
        assertEquals(200.0, p.distanceToNextMeters!!, 1.0)
        assertFalse(p.offRoute)
        assertFalse(p.arrived)
    }

    @Test
    fun `advances past a step once inside its radius`() {
        val p = RouteNavigator.progress(geo(305.0), steps, polyline, currentStepIndex = 0)

        assertEquals(1, p.stepIndex)
        assertEquals("Rẽ trái", p.nextInstruction)
    }

    @Test
    fun `flags off-route when far from the polyline`() {
        // cách polyline ~110 m về phía bắc
        val off = GeoPoint(0.001, geo(400.0).longitude)
        val p = RouteNavigator.progress(off, steps, polyline, currentStepIndex = 1)

        assertTrue(p.offRoute)
    }

    @Test
    fun `arrives after the last step`() {
        val p = RouteNavigator.progress(geo(1_001.0), steps, polyline, currentStepIndex = 2)

        assertTrue(p.arrived)
        assertNull(p.nextInstruction)
        assertEquals(steps.size, p.stepIndex)
    }

    @Test
    fun `no steps yields no instruction`() {
        val p = RouteNavigator.progress(geo(100.0), emptyList(), polyline, currentStepIndex = 0)
        assertNull(p.nextInstruction)
        assertFalse(p.arrived)
    }

    @Test fun `advances a turn skipped during a GPS gap`() {
        val p = RouteNavigator.progress(geo(360.0), steps, polyline, 0, previousAlongMeters = 240.0)
        assertEquals(1, p.stepIndex)
        assertEquals(340.0, p.distanceToNextMeters!!, 1.0)
    }

    @Test fun `distance follows the curved route instead of a chord`() {
        val corner = GeoPoint(0.001, 0.0)
        val finish = GeoPoint(0.001, 0.001)
        val path = listOf(GeoPoint(0.0, 0.0), corner, finish)
        val p = RouteNavigator.progress(path.first(), listOf(RouteWaypoint(0, finish, "Finish")), path, 0)
        assertEquals(222.4, p.distanceToNextMeters!!, 1.0)
    }

    @Test fun `closed loop does not arrive at the start`() {
        val path = listOf(geo(0.0), geo(300.0), GeoPoint(0.003, geo(300.0).longitude), GeoPoint(0.003, 0.0), geo(0.0))
        val p = RouteNavigator.progress(path.first(), listOf(RouteWaypoint(0, path.last(), "Finish")), path, 0)
        assertFalse(p.arrived)
        assertTrue(p.distanceToNextMeters!! > 1000.0)
    }

    @Test fun `off-route fix cannot advance a turn`() {
        val p = RouteNavigator.progress(GeoPoint(0.002, geo(800.0).longitude), steps, polyline, 0, 200.0)
        assertTrue(p.offRoute)
        assertEquals(0, p.stepIndex)
        assertEquals(200.0, p.alongMeters, 0.0)
    }
}
