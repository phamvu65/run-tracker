package com.example.runtracker.ui.routes

import androidx.lifecycle.SavedStateHandle
import com.example.runtracker.domain.model.*
import com.example.runtracker.domain.repository.DirectionsRepository
import com.example.runtracker.domain.repository.RouteRepository
import com.example.runtracker.domain.usecase.BuildRouteUseCase
import com.example.runtracker.domain.usecase.SaveRouteUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteBuilderViewModelTest {
    private val points = listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.003))
    private val planned = PlannedRoute(points, 333.0, emptyList(), true)

    private class Routes(var stored: Route? = null) : RouteRepository {
        var writes = 0
        override fun observeRoutes(userId: String) = flowOf(listOfNotNull(stored))
        override suspend fun getRoute(routeId: String) = stored
        override suspend fun saveRoute(route: Route) { writes++; stored = route }
        override suspend fun deleteRoute(routeId: String) { stored = null }
    }

    private class Directions(val response: suspend () -> PlannedRoute) : DirectionsRepository {
        override suspend fun route(waypoints: List<GeoPoint>, mode: TravelMode, allowUTurns: Boolean,
            radiusMeters: Double?, bearingsDegrees: List<Double>?, bearingRangeDegrees: Double) = Result.success(response())
    }

    @Test fun `clearing while request runs does not resurrect the route`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val response = CompletableDeferred<PlannedRoute>()
            // Deliberately uncooperative transport: cancellation alone is insufficient.
            val directions = Directions { withContext(NonCancellable) { response.await() } }
            val routes = Routes()
            val vm = RouteBuilderViewModel(BuildRouteUseCase(directions), SaveRouteUseCase(routes), routes, SavedStateHandle(), dispatcher)
            points.forEach(vm::addPoint)
            vm.computeRoute()
            runCurrent()
            assertTrue(vm.loading)
            vm.clear()
            response.complete(planned)
            advanceUntilIdle()
            assertNull(vm.planned)
            assertTrue(vm.tappedPoints.isEmpty())
            assertFalse(vm.loading)
            assertFalse(vm.canSave)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `changing mode invalidates outstanding calculation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val response = CompletableDeferred<PlannedRoute>()
            val routes = Routes()
            val vm = RouteBuilderViewModel(BuildRouteUseCase(Directions { response.await() }), SaveRouteUseCase(routes), routes, SavedStateHandle(), dispatcher)
            points.forEach(vm::addPoint)
            vm.computeRoute()
            runCurrent()
            vm.selectMode(TravelMode.CYCLING)
            response.complete(planned)
            advanceUntilIdle()
            assertNull(vm.planned)
            assertEquals(TravelMode.CYCLING, vm.mode)
            assertFalse(vm.loading)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `sketch never closes a large gap by a straight chord`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val routes = Routes()
            val vm = RouteBuilderViewModel(BuildRouteUseCase(Directions { planned }), SaveRouteUseCase(routes), routes, SavedStateHandle(), dispatcher)
            val sketch = listOf(GeoPoint(0.0, 0.0), GeoPoint(0.01, 0.0), GeoPoint(0.01, 0.01), GeoPoint(0.0, 0.001))
            vm.applySketch(sketch)
            assertEquals(sketch.last(), vm.lastSketch.last())
            assertNotEquals(vm.lastSketch.first(), vm.lastSketch.last())
            vm.clear()
            advanceUntilIdle()
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `reopening preserves original inputs and overwrites same route only after rebuild`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val existing = Route("id", "local-user", "old", 333.0, 0.0, points, false, 1234L,
                emptyList(), TravelMode.CYCLING, points, false, true)
            val routes = Routes(existing)
            val vm = RouteBuilderViewModel(BuildRouteUseCase(Directions { planned }), SaveRouteUseCase(routes), routes,
                SavedStateHandle(mapOf("editRouteId" to "id")), dispatcher)
            advanceUntilIdle()
            assertEquals(points, vm.tappedPoints)
            assertEquals(TravelMode.CYCLING, vm.mode)
            assertFalse(vm.canSave)
            vm.computeRoute()
            advanceUntilIdle()
            assertTrue(vm.canSave)
            vm.save("new")
            vm.save("duplicate")
            advanceUntilIdle()
            assertEquals(1, routes.writes)
            assertEquals("id", routes.stored!!.id)
            assertEquals(1234L, routes.stored!!.createdAt)
            assertEquals(points, routes.stored!!.sourcePoints)
            assertEquals("new", routes.stored!!.name)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `drawing extends the previous stroke and undo removes the whole extension`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val routes = Routes()
            val vm = RouteBuilderViewModel(BuildRouteUseCase(Directions { planned }), SaveRouteUseCase(routes), routes, SavedStateHandle(), dispatcher)
            vm.applySketch(points)
            val first = vm.lastSketch.toList()
            val end = GeoPoint(0.003, 0.003)
            vm.applySketch(listOf(points.last(), end))
            assertEquals(first, vm.lastSketch.take(first.size))
            assertEquals(end, vm.lastSketch.last())
            vm.undo()
            assertEquals(first, vm.lastSketch)
            assertEquals(first, vm.tappedPoints)
            vm.undo()
            assertTrue(vm.lastSketch.isEmpty())
            assertFalse(vm.canSave)
            advanceUntilIdle()
            assertNull(vm.planned)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `disconnected new stroke cannot silently replace or bridge the old drawing`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val routes = Routes()
            val vm = RouteBuilderViewModel(BuildRouteUseCase(Directions { planned }), SaveRouteUseCase(routes), routes, SavedStateHandle(), dispatcher)
            vm.applySketch(points)
            val previous = vm.lastSketch
            vm.applySketch(listOf(GeoPoint(1.0, 1.0), GeoPoint(1.0, 1.01)))
            assertEquals(previous, vm.lastSketch)
            assertNotNull(vm.notice)
            vm.clear()
            advanceUntilIdle()
        } finally { Dispatchers.resetMain() }
    }
}
