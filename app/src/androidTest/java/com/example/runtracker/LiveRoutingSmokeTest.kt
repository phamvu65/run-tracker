package com.example.runtracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.runtracker.data.repository.DirectionsRepositoryImpl
import com.example.runtracker.di.NetworkModule
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.domain.usecase.BuildRouteUseCase
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
