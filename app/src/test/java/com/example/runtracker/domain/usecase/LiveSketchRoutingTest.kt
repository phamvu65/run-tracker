package com.example.runtracker.domain.usecase

import com.example.runtracker.data.remote.DirectionsApi
import com.example.runtracker.data.remote.OsrmRouteResponse
import com.example.runtracker.data.repository.DirectionsRepositoryImpl
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.domain.navigation.RouteGeometry
import com.example.runtracker.domain.tracking.GeoMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Opt-in external check; only the public lake fixture, never device location or user history. */
class LiveSketchRoutingTest {
    @Test fun `Thanh Ha sketch resolves to connected roads with separate start and finish`() = runBlocking {
        assumeTrue(System.getenv("RUNTRACKER_ROUTING_SMOKE") == "true")
        val json = Json { ignoreUnknownKeys = true }
        val fixture = json.parseToJsonElement(File("src/androidTest/assets/routing/thanh-ha-sketch.json")
            .readText().removePrefix("\uFEFF")).jsonObject
        fun JsonElement.points() = jsonArray.map { element -> element.jsonObject.let {
            GeoPoint(it.getValue("lat").jsonPrimitive.double, it.getValue("lon").jsonPrimitive.double)
        } }
        val sketch = fixture.getValue("sketch").points()
        val lake = fixture.getValue("lake").points()
        val api = object : DirectionsApi {
            override suspend fun route(url: String): OsrmRouteResponse {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", "RunTracker-development-check/1.0")
                return try {
                    json.decodeFromString<OsrmRouteResponse>(connection.inputStream.bufferedReader().use { it.readText() })
                } finally { connection.disconnect() }
            }
        }
        val route = BuildRouteUseCase(DirectionsRepositoryImpl(api, Dispatchers.IO)).fromSketch(sketch, TravelMode.WALKING)
        assertTrue("No connected route for the screenshot fixture", route.snappedToRoads)
        assertTrue(route.distanceMeters > 1000)
        assertTrue(GeoMath.distanceMeters(route.polyline.first(), sketch.first()) < 65)
        assertTrue(GeoMath.distanceMeters(route.polyline.last(), sketch.last()) < 65)
        assertTrue(GeoMath.distanceMeters(route.polyline.first(), route.polyline.last()) > 70)
        assertFalse("Route crosses lake interior", RouteGeometry(route.polyline).samples(5.0).any { point ->
            var inside = false
            var previous = lake.last()
            for (vertex in lake) {
                if ((vertex.latitude > point.latitude) != (previous.latitude > point.latitude) &&
                    point.longitude < (previous.longitude - vertex.longitude) * (point.latitude - vertex.latitude) /
                    (previous.latitude - vertex.latitude) + vertex.longitude) inside = !inside
                previous = vertex
            }
            inside
        })
        println("Thanh Ha: ${route.distanceMeters} m, ${route.polyline.size} road vertices, no lake crossing")
    }
}
