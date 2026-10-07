package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.*
import com.example.runtracker.domain.repository.DirectionsRepository
import com.example.runtracker.domain.tracking.GeoMath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class BuildRouteUseCaseTest {
    private fun p(x: Double, y: Double = 0.0) = GeoPoint(y / 111_195.0, x / 111_195.0)
    private fun road(points: List<GeoPoint>) = PlannedRoute(points, GeoMath.pathDistanceMeters(points), emptyList(), true)

    private class Fake(val results: List<Result<PlannedRoute>>) : DirectionsRepository {
        var calls = 0
        var anchors = emptyList<GeoPoint>()
        var radius: Double? = null
        var bearings: List<Double>? = null
        var uTurns = true
        override suspend fun route(waypoints: List<GeoPoint>, mode: TravelMode, allowUTurns: Boolean,
            radiusMeters: Double?, bearingsDegrees: List<Double>?, bearingRangeDegrees: Double): Result<PlannedRoute> {
            anchors = waypoints
            radius = radiusMeters
            bearings = bearingsDegrees
            uTurns = allowUTurns
            return results[(calls++).coerceAtMost(results.lastIndex)]
        }
    }

    @Test fun `tap route uses road geometry`() = runTest {
        val route = road(listOf(p(0.0), p(500.0)))
        assertEquals(route, BuildRouteUseCase(Fake(listOf(Result.success(route))))(route.polyline, TravelMode.WALKING))
    }

    @Test fun `network failure never creates a straight route through water`() = runTest {
        val builder = BuildRouteUseCase(Fake(listOf(Result.failure(Exception("offline")))))
        val points = listOf(p(0.0), p(700.0))
        for (result in listOf(builder(points, TravelMode.WALKING), builder.fromSketch(points, TravelMode.WALKING))) {
            assertFalse(result.snappedToRoads)
            assertTrue(result.polyline.isEmpty())
            assertTrue(result.steps.isEmpty())
            assertEquals(0.0, result.distanceMeters, 0.0)
        }
    }

    @Test fun `single or invalid point cannot become a route`() = runTest {
        val fake = Fake(listOf(Result.failure(Exception())))
        val builder = BuildRouteUseCase(fake)
        assertFalse(builder(listOf(p(0.0)), TravelMode.WALKING).snappedToRoads)
        assertFalse(builder(listOf(p(0.0), GeoPoint(Double.NaN, 0.0)), TravelMode.WALKING).snappedToRoads)
        assertEquals(0, fake.calls)
    }

    @Test fun `tap point cannot snap to a distant shore`() = runTest {
        val fake = Fake(listOf(Result.success(road(listOf(p(150.0), p(500.0))))))
        assertFalse(BuildRouteUseCase(fake)(listOf(p(0.0), p(500.0)), TravelMode.WALKING).snappedToRoads)
        assertEquals(50.0, fake.radius!!, 0.0)
    }

    @Test fun `corner anchors retain the shape of a loop`() {
        val builder = BuildRouteUseCase(Fake(listOf(Result.failure(Exception()))))
        val loop = listOf(p(0.0), p(300.0), p(300.0, 300.0), p(0.0, 300.0), p(0.0))
        val anchors = builder.anchors(loop, 12.0)
        assertTrue(anchors.containsAll(loop))
        assertEquals(loop.first(), anchors.first())
        assertEquals(loop.last(), anchors.last())
        assertTrue(anchors.size <= 40)
    }

    @Test fun `dense noisy drawing does not force hard bearings at every point`() = runTest {
        val points = (0..200).map { p(it * 10.0, if (it % 2 == 0) 0.0 else 3.0) }
        val fake = Fake(listOf(Result.success(road(listOf(p(0.0), p(2000.0))))))
        val result = BuildRouteUseCase(fake).fromSketch(points, TravelMode.WALKING)
        assertTrue(result.snappedToRoads)
        assertTrue(fake.anchors.size in 2..40)
        assertFalse(fake.uTurns)
        assertNull(fake.bearings)
    }

    @Test fun `tries relaxed constraints after no segment`() = runTest {
        val points = listOf(p(0.0), p(300.0), p(600.0))
        val fake = Fake(listOf(Result.failure(Exception("NoSegment")), Result.success(road(points))))
        assertTrue(BuildRouteUseCase(fake).fromSketch(points, TravelMode.WALKING).snappedToRoads)
        assertEquals(2, fake.calls)
        assertEquals(60.0, fake.radius!!, 0.0)
    }

    @Test fun `rejects shortcut across a lake inside a drawn curve`() = runTest {
        val curve = listOf(p(0.0), p(0.0, 180.0), p(400.0, 180.0), p(400.0))
        val fake = Fake(listOf(Result.success(road(listOf(curve.first(), curve.last())))))
        val result = BuildRouteUseCase(fake).fromSketch(curve, TravelMode.WALKING)
        assertFalse(result.snappedToRoads)
        assertTrue(result.polyline.isEmpty())
    }

    @Test fun `rejects excursion even if sketch vertices are all on the route`() = runTest {
        val sketch = listOf(p(0.0), p(200.0), p(600.0))
        val excursion = road(listOf(p(0.0), p(200.0), p(200.0, 100.0), p(200.0), p(600.0)))
        assertFalse(BuildRouteUseCase(Fake(listOf(Result.success(excursion))))
            .fromSketch(sketch, TravelMode.WALKING).snappedToRoads)
    }

    @Test fun `uses a better candidate after a detour`() = runTest {
        val sketch = listOf(p(0.0), p(300.0), p(600.0))
        val detour = road(listOf(p(0.0), p(300.0, 200.0), p(600.0)))
        val correct = road(sketch)
        val fake = Fake(listOf(Result.success(detour), Result.success(correct)))
        assertEquals(correct, BuildRouteUseCase(fake).fromSketch(sketch, TravelMode.WALKING))
    }

    @Test fun `preserves actual winding road around a lake`() = runTest {
        val loop = listOf(p(0.0), p(300.0), p(300.0, 300.0), p(0.0, 300.0), p(0.0))
        val actual = road(loop)
        assertEquals(actual, BuildRouteUseCase(Fake(listOf(Result.success(actual))))
            .fromSketch(loop, TravelMode.WALKING))
    }

    @Test fun `rejects short backtracking close to the intended street`() = runTest {
        val sketch = listOf(p(0.0), p(600.0))
        val backtrack = road(listOf(p(0.0), p(300.0), p(190.0), p(600.0)))
        assertFalse(BuildRouteUseCase(Fake(listOf(Result.success(backtrack))))
            .fromSketch(sketch, TravelMode.WALKING).snappedToRoads)
    }

    @Test fun `unverified and patched candidates cannot be saved as roads`() = runTest {
        val points = listOf(p(0.0), p(300.0))
        val patched = road(points).copy(gapPolylines = listOf(points))
        assertFalse(BuildRouteUseCase(Fake(listOf(Result.success(patched))))
            .fromSketch(points, TravelMode.WALKING).snappedToRoads)
    }

    @Test fun `cancellation is propagated`() = runTest {
        val fake = object : DirectionsRepository {
            override suspend fun route(waypoints: List<GeoPoint>, mode: TravelMode, allowUTurns: Boolean,
                radiusMeters: Double?, bearingsDegrees: List<Double>?, bearingRangeDegrees: Double): Result<PlannedRoute> {
                throw CancellationException("cancelled")
            }
        }
        try {
            BuildRouteUseCase(fake).fromSketch(listOf(p(0.0), p(300.0)), TravelMode.WALKING)
            fail("Cancellation was swallowed")
        } catch (_: CancellationException) { }
    }
}
