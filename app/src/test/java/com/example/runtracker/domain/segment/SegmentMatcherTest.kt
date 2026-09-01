package com.example.runtracker.domain.segment

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class SegmentMatcherTest {

    private val base: Instant = Instant.parse("2026-09-01T00:00:00Z")
    private val metersPerDegree = 6_371_000.0 * Math.PI / 180.0

    /** Điểm trên xích đạo, cách gốc `meters` về phía đông. */
    private fun routePoint(meters: Double, second: Long) = RoutePoint(
        latitude = 0.0,
        longitude = meters / metersPerDegree,
        altitude = 0.0,
        speedMps = null,
        accuracyMeters = 5f,
        timestamp = base.plusSeconds(second),
    )

    private fun geo(meters: Double) = GeoPoint(0.0, meters / metersPerDegree)

    @Test
    fun `matches a traversal and returns the elapsed time between endpoints`() {
        val route = (0..10).map { routePoint(it * 100.0, it * 30L) } // 1000 m trong 300 s

        val match = SegmentMatcher.match(
            route = route,
            segmentStart = geo(200.0),
            segmentEnd = geo(800.0),
            segmentDistanceMeters = 600.0,
        )

        assertNotNull(match)
        assertEquals(180.0, match!!.elapsedSeconds, 0.001) // (800-200)/100 * 30s
        assertEquals(base.plusSeconds(60), match.startTime)
    }

    @Test
    fun `no match when the route never reaches the segment start`() {
        val route = (0..5).map { routePoint(it * 100.0, it * 30L) } // chỉ tới 500 m

        val match = SegmentMatcher.match(
            route = route,
            segmentStart = geo(2_000.0),
            segmentEnd = geo(2_600.0),
            segmentDistanceMeters = 600.0,
        )

        assertNull(match)
    }

    @Test
    fun `matches when the route shape follows the segment polyline`() {
        val route = (0..10).map { routePoint(it * 100.0, it * 30L) }
        val polyline = (0..10).map { geo(it * 100.0) }

        val match = SegmentMatcher.match(
            route = route,
            segmentStart = geo(0.0),
            segmentEnd = geo(1_000.0),
            segmentDistanceMeters = 1_000.0,
            segmentPolyline = polyline,
        )

        assertNotNull(match)
    }

    @Test
    fun `rejects when the route bulges away from the segment polyline`() {
        val northDegrees = 200.0 / 111_320.0 // ~200 m về phía bắc
        val route = listOf(
            routePoint(0.0, 0),
            routePoint(250.0, 60),
            RoutePoint(
                latitude = northDegrees,
                longitude = 500.0 / metersPerDegree,
                altitude = 0.0,
                speedMps = null,
                accuracyMeters = 5f,
                timestamp = base.plusSeconds(120),
            ),
            routePoint(750.0, 180),
            routePoint(1_000.0, 240),
        )
        val polyline = (0..10).map { geo(it * 100.0) } // đường thẳng y = 0

        val match = SegmentMatcher.match(
            route = route,
            segmentStart = geo(0.0),
            segmentEnd = geo(1_000.0),
            segmentDistanceMeters = 1_000.0,
            segmentPolyline = polyline,
        )

        assertNull(match)
    }

    @Test
    fun `no match when traversed distance is far from the segment distance`() {
        val route = (0..10).map { routePoint(it * 100.0, it * 30L) }

        // endpoints 200m..800m nhưng khai báo segment dài 2000m -> lệch quá 25%
        val match = SegmentMatcher.match(
            route = route,
            segmentStart = geo(200.0),
            segmentEnd = geo(800.0),
            segmentDistanceMeters = 2_000.0,
        )

        assertNull(match)
    }
}
