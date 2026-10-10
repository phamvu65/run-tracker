package com.example.runtracker.data.repository

import com.example.runtracker.data.remote.*
import com.example.runtracker.domain.geo.PolylineCodec
import com.example.runtracker.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DirectionsRepositoryImplTest {
    private val points = listOf(GeoPoint(10.0, 106.0), GeoPoint(10.001, 106.001), GeoPoint(10.002, 106.002))
    private fun step(type: String, index: Int, mode: String = "walking") = OsrmStep(
        mode = mode, distance = 100.0,
        maneuver = OsrmManeuver(listOf(points[index].longitude, points[index].latitude), type, "left"),
        geometry = PolylineCodec.encode(points),
    )
    private fun response(steps: List<OsrmStep>) = OsrmRouteResponse("Ok", listOf(
        OsrmRoute(PolylineCodec.encode(points), 300.0, listOf(OsrmLeg(steps, 300.0))),
    ))

    @Test fun `via arrivals are not announced as destinations`() = runTest {
        val api = object : DirectionsApi {
            override suspend fun route(url: String) = response(listOf(step("depart", 0), step("arrive", 1),
                step("depart", 1), step("turn", 1), step("arrive", 2)))
        }
        val result = DirectionsRepositoryImpl(api, StandardTestDispatcher(testScheduler))
            .route(points, TravelMode.WALKING, false, 40.0, null, 60.0).getOrThrow()
        assertEquals(2, result.steps.size)
        assertEquals(points[1], result.steps.first().location)
        assertEquals(points.last(), result.steps.last().location)
    }

    @Test fun `ferry geometry is not accepted as a running path over water`() = runTest {
        val api = object : DirectionsApi {
            override suspend fun route(url: String) = response(listOf(step("depart", 0, "ferry"), step("arrive", 2)))
        }
        assertTrue(DirectionsRepositoryImpl(api, StandardTestDispatcher(testScheduler))
            .route(points, TravelMode.WALKING, false, null, null, 60.0).isFailure)
    }

    @Test fun `cancelled request does not become routing failure`() = runTest {
        val api = object : DirectionsApi {
            override suspend fun route(url: String): OsrmRouteResponse = throw CancellationException()
        }
        try {
            DirectionsRepositoryImpl(api, StandardTestDispatcher(testScheduler))
                .route(points, TravelMode.WALKING, false, null, null, 60.0)
            fail("Cancellation swallowed")
        } catch (_: CancellationException) { }
    }

    @Test fun `trace matcher uses matching geometry and preserves walking profile`() = runTest {
        var requested = ""
        val route = response(listOf(step("depart", 0), step("turn", 1), step("arrive", 2))).routes.single()
        val api = object : DirectionsApi {
            override suspend fun route(url: String): OsrmRouteResponse {
                requested = url
                return OsrmRouteResponse(code = "Ok", matchings = listOf(route),
                    tracepoints = points.map { OsrmTracepoint(listOf(it.longitude, it.latitude), 0) })
            }
        }
        val result = DirectionsRepositoryImpl(api, StandardTestDispatcher(testScheduler))
            .matchSketch(points, TravelMode.WALKING, 20.0).getOrThrow()
        assertTrue(requested.contains("routed-foot/match/v1/foot"))
        assertTrue(requested.contains("tidy=false"))
        assertTrue(result.snappedToRoads)
        assertEquals(points, result.polyline)
    }

    @Test fun `split matches or missing endpoints cannot become a continuous route`() = runTest {
        val route = response(listOf(step("depart", 0), step("arrive", 2))).routes.single()
        for (split in listOf(true, false)) {
            val api = object : DirectionsApi {
                override suspend fun route(url: String) = OsrmRouteResponse(code = "Ok",
                    matchings = if (split) listOf(route, route) else listOf(route),
                    tracepoints = listOf(null, OsrmTracepoint(listOf(0.0, 0.0), 0), OsrmTracepoint(listOf(0.0, 0.0), 0)))
            }
            assertTrue(DirectionsRepositoryImpl(api, StandardTestDispatcher(testScheduler))
                .matchSketch(points, TravelMode.WALKING, 20.0).isFailure)
        }
    }

    @Test fun `cancelled trace matching propagates cancellation`() = runTest {
        val api = object : DirectionsApi {
            override suspend fun route(url: String): OsrmRouteResponse = throw CancellationException()
        }
        try {
            DirectionsRepositoryImpl(api, StandardTestDispatcher(testScheduler))
                .matchSketch(points, TravelMode.WALKING, 20.0)
            fail("Cancellation swallowed")
        } catch (_: CancellationException) { }
    }
}
