package com.example.runtracker.domain.navigation

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.Route
import com.example.runtracker.domain.model.RouteWaypoint
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.domain.tracking.GeoMath

/** Thread-safe navigation state. Late network responses cannot revive a stopped/changed route. */
class NavigationSession {
    private var generation = 0L
    private var route: RouteNavigator.PreparedRoute? = null
    private var mode = TravelMode.WALKING
    private var previous: GeoPoint? = null
    private var along = 0.0
    private var index = 0

    data class Update(val progress: RouteNavigator.Progress, val polyline: List<GeoPoint>, val stepCount: Int)
    class RerouteRequest internal constructor(
        internal val generation: Long,
        val origin: GeoPoint,
        val destination: GeoPoint,
        val mode: TravelMode,
        internal val tail: List<GeoPoint>,
        internal val remainingSteps: List<RouteWaypoint>,
    )

    @Synchronized fun start(selected: Route?) {
        generation++
        previous = null
        along = 0.0
        index = 0
        route = selected?.takeIf { it.snappedToRoads && it.travelMode != null && it.polyline.size >= 2 }?.let {
            mode = it.travelMode!!
            val steps = it.waypoints.filter { step -> !step.instruction.isNullOrBlank() }
                .ifEmpty { listOf(RouteWaypoint(0, it.polyline.last(), "Về đích")) }
            RouteNavigator.PreparedRoute(steps, it.polyline)
        }
    }

    @Synchronized fun invalidateRequests() { generation++ }

    @Synchronized fun update(location: GeoPoint): Update? {
        val current = route ?: return null
        // A GPS gap can advance past a missed turn, but not teleport to another lap at a crossing.
        val advance = previous?.let { GeoMath.distanceMeters(it, location) * 2.0 + 50.0 } ?: 100.0
        val result = RouteNavigator.progress(location, current, index, along, advance)
        previous = location
        along = result.alongMeters
        index = result.stepIndex
        return Update(result, current.polyline, current.steps.size)
    }

    @Synchronized fun beginReroute(): RerouteRequest? {
        val current = route ?: return null
        val origin = previous ?: return null
        if (index >= current.steps.size) return null
        val projection = current.geometry.project(origin, along, minOf(current.geometry.length, along + 500.0))
        val target = minOf(current.geometry.length, maxOf(along + 80.0, projection.along))
        val vertex = current.geometry.cumulative.indexOfFirst { it >= target }.takeIf { it >= 0 } ?: return null
        val rejoinDistance = current.geometry.cumulative[vertex]
        return RerouteRequest(generation, origin, current.polyline[vertex], mode,
            current.polyline.drop(vertex), current.steps.filterIndexed { i, _ -> current.stepDistances[i] > rejoinDistance + 1.0 })
    }

    @Synchronized fun applyReroute(request: RerouteRequest, result: PlannedRoute): Boolean {
        if (request.generation != generation || route == null || !result.snappedToRoads ||
            result.polyline.size < 2 || result.gapPolylines.isNotEmpty()) return false
        val latest = previous ?: return false
        if (GeoMath.distanceMeters(latest, request.origin) > 100.0) return false
        // Do not manufacture a large connecting chord if the API snapped to a different road.
        if (GeoMath.distanceMeters(result.polyline.last(), request.destination) > 8.0 ||
            GeoMath.distanceMeters(result.polyline.first(), request.origin) > 55.0) return false
        val polyline = result.polyline + request.tail.drop(1)
        val steps = (result.steps.dropLast(1).map { RouteWaypoint(0, it.location, it.instruction) } +
            request.remainingSteps).toMutableList()
        if (steps.isEmpty() || GeoMath.distanceMeters(steps.last().location, polyline.last()) > 1.0) {
            steps += RouteWaypoint(0, polyline.last(), "Về đích")
        }
        route = RouteNavigator.PreparedRoute(steps.mapIndexed { i, step -> step.copy(orderIndex = i) }, polyline)
        generation++
        along = 0.0
        index = 0
        return true
    }
}
