package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.RouteLeg
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
    fun `fromSketch uses the best snapped route even when it deviates a lot from the sketch`() = runTest {
        // Đường "bám" dài gấp đôi nét vẽ và vòng ra cách nét vẽ ~1 km — vẫn là một route thật
        // trên mạng đường, nên được ưu tiên hơn là rơi về nét vẽ tay thô cắt ngang nhà cửa.
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

        assertEquals(detour, result)
        // Không mật độ điểm nào (3 mức trong VIA_SPACING_M) đạt GOOD_SCORE nên thử hết cả 3.
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
        assertEquals(60.0, fake.lastBearingRangeDegrees, 0.0)
    }

    @Test
    fun `fromSketch replaces a leg that detours far past an unmapped road with a straight chord`() = runTest {
        // Chặng via0->via1 bám đường đúng ý (đường thẳng khớp nét vẽ). Chặng via1->via2 router
        // phải đi vòng gấp 3 lần đường thẳng nối 2 via đó — đúng kiểu lối ven hồ chưa được vẽ
        // hết trong OSM, buộc router vòng qua khu dân cư kế bên để nối tiếp.
        val via0 = geo(0.0)
        val via1 = geo(400.0)
        val via2 = geo(800.0)
        val goodLeg = RouteLeg(polyline = listOf(via0, via1), distanceMeters = 400.0)
        val detourLeg = RouteLeg(
            polyline = listOf(via1, GeoPoint(0.01, via1.longitude), via2),
            distanceMeters = 1_200.0,
        )
        val routeWithGap = PlannedRoute(
            polyline = goodLeg.polyline + detourLeg.polyline.drop(1),
            distanceMeters = goodLeg.distanceMeters + detourLeg.distanceMeters,
            steps = emptyList(),
            snappedToRoads = true,
            legs = listOf(goodLeg, detourLeg),
        )
        val fake = FakeDirections(Result.success(routeWithGap))
        val sketch = listOf(via0, via1, via2)

        val result = BuildRouteUseCase(fake).fromSketch(sketch, TravelMode.WALKING)

        // Chặng tốt giữ nguyên; chặng vòng xa được thay bằng đoạn thẳng via1 -> via2.
        assertEquals(listOf(via0, via1, via2), result.polyline)
        assertEquals(800.0, result.distanceMeters, 1.0)
        assertTrue(result.snappedToRoads)
        // Đoạn đã vá được ghi lại riêng để UI vẽ nét đứt.
        assertEquals(listOf(listOf(via1, via2)), result.gapPolylines)
    }

    @Test
    fun `fromSketch refines a moderately detoured leg with an extra via point`() = runTest {
        // Chặng dài hơn chord ~20% (< LEG_DETOUR_RATIO nhưng > MODERATE_DETOUR_RATIO) — đúng
        // kiểu router chọn nhầm nhánh hơi vòng hơn ở ngã ba nhiều đường ngắn giao nhau.
        val via0 = geo(0.0)
        val via1 = geo(200.0)
        val moderateLeg = RouteLeg(
            polyline = listOf(via0, GeoPoint(0.001, via0.longitude), via1),
            distanceMeters = 240.0,
        )
        val initialRoute = PlannedRoute(
            polyline = moderateLeg.polyline,
            distanceMeters = moderateLeg.distanceMeters,
            steps = emptyList(),
            snappedToRoads = true,
            legs = listOf(moderateLeg),
        )
        // Kết quả tinh chỉnh (gọi lại với 1 via point chèn giữa) tìm được nhánh ngắn hơn.
        val refined = PlannedRoute(
            polyline = listOf(via0, via1),
            distanceMeters = 205.0,
            steps = emptyList(),
            snappedToRoads = true,
        )
        val fake = SequencedFakeDirections(listOf(Result.success(initialRoute), Result.success(refined)))
        val sketch = listOf(via0, via1)

        val result = BuildRouteUseCase(fake).fromSketch(sketch, TravelMode.WALKING)

        assertEquals(listOf(via0, via1), result.polyline)
        assertEquals(205.0, result.distanceMeters, 1.0)
        // Chặng tinh chỉnh thành công không được coi là "lỗ hổng dữ liệu".
        assertTrue(result.gapPolylines.isEmpty())
    }

    @Test
    fun `fromSketch keeps the original leg when refining doesn't improve it`() = runTest {
        val via0 = geo(0.0)
        val via1 = geo(200.0)
        val moderateLeg = RouteLeg(
            polyline = listOf(via0, GeoPoint(0.001, via0.longitude), via1),
            distanceMeters = 240.0,
        )
        val initialRoute = PlannedRoute(
            polyline = moderateLeg.polyline,
            distanceMeters = moderateLeg.distanceMeters,
            steps = emptyList(),
            snappedToRoads = true,
            legs = listOf(moderateLeg),
        )
        // Tinh chỉnh không tìm được gì tốt hơn (dài bằng hoặc hơn) -> giữ nguyên chặng gốc.
        val noImprovement = PlannedRoute(
            polyline = listOf(via0, GeoPoint(0.001, via0.longitude), via1),
            distanceMeters = 240.0,
            steps = emptyList(),
            snappedToRoads = true,
        )
        val fake = SequencedFakeDirections(listOf(Result.success(initialRoute), Result.success(noImprovement)))
        val sketch = listOf(via0, via1)

        val result = BuildRouteUseCase(fake).fromSketch(sketch, TravelMode.WALKING)

        assertEquals(moderateLeg.polyline, result.polyline)
        assertEquals(240.0, result.distanceMeters, 1.0)
    }

    @Test
    fun `fromSketch keeps a route as-is when leg count doesn't match via points`() = runTest {
        // legs rỗng (khác số via point) -> fillUnmappedGaps bỏ qua, dùng nguyên route.
        val route = PlannedRoute(listOf(geo(0.0), geo(400.0)), 390.0, emptyList(), snappedToRoads = true)
        val fake = FakeDirections(Result.success(route))

        val result = BuildRouteUseCase(fake).fromSketch(listOf(geo(0.0), geo(200.0), geo(400.0)), TravelMode.WALKING)

        assertEquals(route, result)
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

    /** Trả lần lượt từng kết quả trong [results] theo đúng thứ tự gọi (giữ nguyên kết quả cuối
     *  nếu bị gọi nhiều hơn số phần tử). Dùng cho test cần phân biệt cuộc gọi đầu (route chính)
     *  với cuộc gọi tinh chỉnh chặng ([BuildRouteUseCase.refineLeg]). */
    private class SequencedFakeDirections(
        private val results: List<Result<PlannedRoute>>,
    ) : DirectionsRepository {
        private var index = 0

        override suspend fun route(
            waypoints: List<GeoPoint>,
            mode: TravelMode,
            allowUTurns: Boolean,
            radiusMeters: Double?,
            bearingsDegrees: List<Double>?,
            bearingRangeDegrees: Double,
        ): Result<PlannedRoute> {
            val result = results[index.coerceAtMost(results.size - 1)]
            index++
            return result
        }
    }
}
