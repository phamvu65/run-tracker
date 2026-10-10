package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.ElevationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.time.Instant

class CorrectElevationUseCaseTest {
    private val points = (0..4).map {
        RoutePoint(latitude = 21.0 + it * 0.0001, longitude = 105.0, altitude = 0.0,
            speedMps = null, accuracyMeters = 5f, timestamp = Instant.EPOCH.plusSeconds(it * 5L))
    }

    private class Storage {
        var saved: List<RoutePoint>? = null
        // This use case must only update route points, never rewrite an activity or other data.
        val repository = Proxy.newProxyInstance(
            ActivityRepository::class.java.classLoader, arrayOf(ActivityRepository::class.java),
        ) { _, method, args ->
            check(method.name == "updateRoutePoints") { method.name }
            @Suppress("UNCHECKED_CAST")
            saved = args[1] as List<RoutePoint>
            Unit
        } as ActivityRepository
    }

    @Test fun `unknown or missing sensor coverage still fetches terrain`() = runTest {
        val storage = Storage()
        var requests = 0
        val elevation = object : ElevationRepository {
            override suspend fun elevationsFor(points: List<GeoPoint>): Result<List<Double>> {
                requests++
                return Result.success(points.indices.map { 100.0 + it * 4 })
            }
        }
        val corrected = CorrectElevationUseCase(storage.repository, elevation)("run", points)
        assertEquals(1, requests)
        assertEquals(100.0, corrected.first().altitude, 0.0)
        assertEquals(116.0, corrected.last().altitude, 0.0)
        assertEquals(corrected, storage.saved)
    }

    @Test fun `verified barometric trace is preserved without terrain request`() = runTest {
        val storage = Storage()
        val elevation = object : ElevationRepository {
            override suspend fun elevationsFor(points: List<GeoPoint>): Result<List<Double>> =
                error("Must retain verified sensor elevations")
        }
        assertSame(points, CorrectElevationUseCase(storage.repository, elevation)("run", points, true))
        assertNull(storage.saved)
    }

    @Test fun `invalid terrain response cannot replace recorded elevations`() = runTest {
        val storage = Storage()
        val elevation = object : ElevationRepository {
            override suspend fun elevationsFor(points: List<GeoPoint>) = Result.success(listOf(Double.NaN))
        }
        assertSame(points, CorrectElevationUseCase(storage.repository, elevation)("run", points))
        assertNull(storage.saved)
    }
}
