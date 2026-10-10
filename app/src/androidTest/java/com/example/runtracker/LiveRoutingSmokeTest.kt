package com.example.runtracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.runtracker.data.repository.DirectionsRepositoryImpl
import com.example.runtracker.di.NetworkModule
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.domain.usecase.BuildRouteUseCase
import com.example.runtracker.domain.navigation.RouteGeometry
import com.example.runtracker.domain.tracking.GeoMath
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Explicit opt-in: public example coordinates, never the device's location or saved activities. */
@RunWith(AndroidJUnit4::class)
class LiveRoutingSmokeTest {
    @Test fun thanhHaSketchFollowsConnectedRoadsAroundTheLake() = runBlocking<Unit> {
        assumeTrue(InstrumentationRegistry.getArguments().getString("routingNetworkSmoke") == "true")
        val json = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("routing/thanh-ha-sketch.json").bufferedReader().use { it.readText().removePrefix("\uFEFF") })
        fun JSONArray.points() = (0 until length()).map { getJSONObject(it).let { p ->
            GeoPoint(p.getDouble("lat"), p.getDouble("lon"))
        } }
        val sketch = json.getJSONArray("sketch").points()
        val lake = json.getJSONArray("lake").points()
        val retrofit = NetworkModule.provideRetrofit(NetworkModule.provideOkHttpClient(), NetworkModule.provideJson())
        val repository = DirectionsRepositoryImpl(NetworkModule.provideDirectionsApi(retrofit), Dispatchers.IO)
        val result = withTimeout(35_000) { BuildRouteUseCase(repository).fromSketch(sketch, TravelMode.WALKING) }
        assertTrue("Thanh Ha sketch did not produce a connected route", result.snappedToRoads)
        assertTrue(result.distanceMeters > 1000)
        assertTrue(GeoMath.distanceMeters(result.polyline.first(), sketch.first()) < 65)
        assertTrue(GeoMath.distanceMeters(result.polyline.last(), sketch.last()) < 65)
        assertTrue(GeoMath.distanceMeters(result.polyline.first(), result.polyline.last()) > 70)
        assertFalse("Route crosses the lake interior", RouteGeometry(result.polyline).samples(5.0).any { point ->
            var inside = false
            var previous = lake.last()
            for (vertex in lake) {
                if ((vertex.latitude > point.latitude) != (previous.latitude > point.latitude) &&
                    point.longitude < (previous.longitude - vertex.longitude) *
                    (point.latitude - vertex.latitude) / (previous.latitude - vertex.latitude) + vertex.longitude) {
                    inside = !inside
                }
                previous = vertex
            }
            inside
        })
    }

    @Test fun liveFootRouteAndSketchFollowTheRoad() = runBlocking<Unit> {
        assumeTrue(InstrumentationRegistry.getArguments().getString("routingNetworkSmoke") == "true")
        val client = NetworkModule.provideOkHttpClient()
        val retrofit = NetworkModule.provideRetrofit(client, NetworkModule.provideJson())
        val repository = DirectionsRepositoryImpl(NetworkModule.provideDirectionsApi(retrofit), Dispatchers.IO)
        val builder = BuildRouteUseCase(repository)
        withTimeout(60_000) {
            // Public coordinates from OSRM API documentation (Berlin).
            val points = listOf(GeoPoint(52.517037, 13.388860), GeoPoint(52.529407, 13.397634))
            val route = builder(points, TravelMode.WALKING)
            assertTrue("Live foot routing failed", route.snappedToRoads)
            assertTrue(route.polyline.size > 2)
            assertTrue(route.steps.isNotEmpty())
            val sketch = builder.fromSketch(route.polyline, TravelMode.WALKING)
            assertTrue("Rebuilding a sketch of a real road failed", sketch.snappedToRoads)
            assertTrue(sketch.gapPolylines.isEmpty())
        }
    }
}
