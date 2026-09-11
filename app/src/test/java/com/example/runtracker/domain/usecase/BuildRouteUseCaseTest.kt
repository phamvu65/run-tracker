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

    @Test
    fun `fromSketch snaps the trace to roads when routing succeeds`() = runTest {
        val snapped = PlannedRoute(listOf(geo(0.0), geo(400.0)), 390.0, emptyList(), snappedToRoads = true)
        val useCase = BuildRouteUseCase(FakeDirections(Result.success(snapped)))

        val result = useCase.fromSketch(listOf(geo(0.0), geo(200.0), geo(400.0)), TravelMode.WALKING)

        assertEquals(snapped, result)
    }

    @Test
    fun `fromSketch falls back to a straight line when routing fails`() = runTest {
        val useCase = BuildRouteUseCase(FakeDirections(Result.failure(RuntimeException("offline"))))

        val result = useCase.fromSketch(listOf(geo(0.0), geo(300.0), geo(700.0)), TravelMode.WALKING)

        assertFalse(result.snappedToRoads)
        assertEquals(700.0, result.distanceMeters, 1.0)
    }

    @Test
    fun `fromSketch rejects a snapped route that wanders far off the sketch`() = runTest {
        // Đường "bám" dài gấp đôi nét vẽ và vòng ra cách nét vẽ ~1 km — đúng kiểu zigzag
        // qua nhiều phố mà ta muốn loại.
        val detour = PlannedRoute(
            polyline = listOf(geo(0.0), GeoPoint(0.01, 0.0), geo(800.0)),
            distanceMeters = 1_600.0,
            steps = emptyList(),
            snappedToRoads = true,
        )
        val fake = FakeDirections(Result.success(detour))
        val useCase = BuildRouteUseCase(fake)
        val sketch = listOf(geo(0.0), geo(400.0), geo(800.0))

        val result = useCase.fromSketch(sketch, TravelMode.WALKING)

        assertFalse(result.snappedToRoads)
        assertEquals(sketch, result.polyline)
        // Thử lại với mật độ điểm khác (3 mức trong VIA_SPACING_M) rồi mới bỏ cuộc.
        assertEquals(3, fake.calls)
    }

    @Test
    fun `fromSketch sends only a bounded number of via points and forbids u-turns`() = runTest {
        val metersPerDegreeLat = 111_320.0
        // Nét vẽ dày: 200 điểm cách nhau 10 m (2 km) — không được gửi hết cho router.
        val sketch = (0 until 200).map { GeoPoint(it * 10.0 / metersPerDegreeLat, 0.0) }
        val snapped = PlannedRoute(sketch, 2_000.0, emptyList(), snappedToRoads = true)
        val fake = FakeDirections(Result.success(snapped))

        BuildRouteUseCase(fake).fromSketch(sketch, TravelMode.WALKING)

        assertTrue("gửi ${fake.lastWaypointCount} điểm", fake.lastWaypointCount in 3..40)
        assertFalse(fake.lastAllowUTurns)
    }

    @Test
    fun `fromSketch constrains each via point to a small snap radius`() = runTest {
        val snapped = PlannedRoute(listOf(geo(0.0), geo(400.0)), 390.0, emptyList(), snappedToRoads = true)
        val fake = FakeDirections(Result.success(snapped))

        BuildRouteUseCase(fake).fromSketch(listOf(geo(0.0), geo(200.0), geo(400.0)), TravelMode.WALKING)

        assertEquals(60.0, fake.lastRadiusMeters!!, 0.0)
    }

    @Test
    fun `plain invoke does not constrain the snap radius`() = runTest {
        val planned = PlannedRoute(listOf(geo(0.0), geo(500.0)), 480.0, emptyList(), snappedToRoads = true)
        val fake = FakeDirections(Result.success(planned))

        BuildRouteUseCase(fake).invoke(listOf(geo(0.0), geo(500.0)), TravelMode.WALKING)

        assertEquals(null, fake.lastRadiusMeters)
    }

    @Test
    fun `plain invoke does not constrain bearing`() = runTest {
        val planned = PlannedRoute(listOf(geo(0.0), geo(500.0)), 480.0, emptyList(), snappedToRoads = true)
        val fake = FakeDirections(Result.success(planned))

        BuildRouteUseCase(fake).invoke(listOf(geo(0.0), geo(500.0)), TravelMode.WALKING)

        assertEquals(null, fake.lastBearingsDegrees)
    }

    @Test
    fun `fromSketch constrains each via point to the sketch's local direction`() = runTest {
        val snapped = PlannedRoute(listOf(geo(0.0), geo(400.0)), 390.0, emptyList(), snappedToRoads = true)
        val fake = FakeDirections(Result.success(snapped))

        // Nét vẽ đi thẳng theo hướng đông (kinh độ tăng, vĩ độ không đổi) -> bearing ~90.
        BuildRouteUseCase(fake).fromSketch(listOf(geo(0.0), geo(200.0), geo(400.0)), TravelMode.WALKING)

        val bearings = fake.lastBearingsDegrees
        assertEquals(3, bearings?.size)
        bearings!!.forEach { assertEquals(90.0, it, 1.0) }
        assertEquals(45.0, fake.lastBearingRangeDegrees, 0.0)
    }

    private class FakeDirections(private val route: Result<PlannedRoute>) : DirectionsRepository {
        var calls = 0
            private set
        var lastWaypointCount = 0
            private set
        var lastAllowUTurns = true
            private set
        var lastRadiusMeters: Double? = null
            private set
        var lastBearingsDegrees: List<Double>? = null
            private set
        var lastBearingRangeDegrees: Double = 0.0
            private set

        override suspend fun route(
            waypoints: List<GeoPoint>,
            mode: TravelMode,
            allowUTurns: Boolean,
            radiusMeters: Double?,
            bearingsDegrees: List<Double>?,
            bearingRangeDegrees: Double,
        ): Result<PlannedRoute> {
            calls++
            lastWaypointCount = waypoints.size
            lastAllowUTurns = allowUTurns
            lastRadiusMeters = radiusMeters
            lastBearingsDegrees = bearingsDegrees
            lastBearingRangeDegrees = bearingRangeDegrees
            return route
        }
    }
}
