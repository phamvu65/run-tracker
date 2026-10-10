package com.example.runtracker.data.repository

import com.example.runtracker.data.remote.ElevationApi
import com.example.runtracker.data.remote.ElevationResponse
import com.example.runtracker.domain.model.GeoPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ElevationRepositoryImplTest {
    @Test fun `invalid DEM numbers are not stored as elevations`() = runTest {
        val api = object : ElevationApi {
            override suspend fun elevation(latitude: String, longitude: String) = ElevationResponse(listOf(Double.NaN))
        }
        assertTrue(ElevationRepositoryImpl(api, StandardTestDispatcher(testScheduler))
            .elevationsFor(listOf(GeoPoint(0.0, 0.0))).isFailure)
    }

    @Test fun `DEM cancellation propagates instead of continuing finalization`() = runTest {
        val api = object : ElevationApi {
            override suspend fun elevation(latitude: String, longitude: String): ElevationResponse = throw CancellationException()
        }
        try {
            ElevationRepositoryImpl(api, StandardTestDispatcher(testScheduler))
                .elevationsFor(listOf(GeoPoint(0.0, 0.0)))
            fail("Cancellation swallowed")
        } catch (_: CancellationException) { }
    }

    @Test fun `DEM batching preserves order beyond one hundred locations`() = runTest {
        var calls = 0
        val api = object : ElevationApi {
            override suspend fun elevation(latitude: String, longitude: String): ElevationResponse {
                calls++
                return ElevationResponse(latitude.split(',').map { it.toDouble() })
            }
        }
        val locations = (0..100).map { GeoPoint(it / 100.0, 0.0) }
        val result = ElevationRepositoryImpl(api, StandardTestDispatcher(testScheduler)).elevationsFor(locations).getOrThrow()
        assertEquals(2, calls)
        assertEquals(locations.map { it.latitude }, result)
    }
}
