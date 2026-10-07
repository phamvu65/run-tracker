package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.PlannedRoute
import com.example.runtracker.domain.model.TravelMode
import com.example.runtracker.domain.navigation.RouteGeometry
import com.example.runtracker.domain.repository.DirectionsRepository
import com.example.runtracker.domain.tracking.GeoMath
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/** Only returns road geometry. Failure never becomes a straight-line route. */
class BuildRouteUseCase @Inject constructor(private val directionsRepository: DirectionsRepository) {
    suspend operator fun invoke(waypoints: List<GeoPoint>, mode: TravelMode): PlannedRoute {
        if (!validPoints(waypoints) || waypoints.size > MAX_ANCHORS) return unavailable()
        val result = directionsRepository.route(waypoints, mode, radiusMeters = 50.0).getOrNull()
        currentCoroutineContext().ensureActive()
        return result?.takeIf { validRoad(it) && nearEndpoints(it, waypoints, 55.0) } ?: unavailable()
    }

    /** Preserve corners and relax drawing noise, without relaxing the geometry check. */
    suspend fun fromSketch(sketch: List<GeoPoint>, mode: TravelMode): PlannedRoute {
        if (!validPoints(sketch)) return unavailable()
        val reference = RouteGeometry(sketch)
        if (reference.length < 20.0) return unavailable()
        var best: PlannedRoute? = null
        var bestScore = Double.POSITIVE_INFINITY
        for ((tolerance, radius) in listOf(12.0 to 35.0, 25.0 to 60.0, 40.0 to 45.0)) {
            currentCoroutineContext().ensureActive()
            val anchors = anchors(sketch, tolerance)
            if (anchors.size !in 2..MAX_ANCHORS) continue
            // A drawn corner is not a reliable local road bearing.
            val candidate = directionsRepository.route(
                anchors, mode, allowUTurns = false, radiusMeters = radius,
            ).getOrNull() ?: continue
            currentCoroutineContext().ensureActive()
            val score = score(candidate, reference)
            if (score < bestScore) { best = candidate; bestScore = score }
            if (score <= 0.25) break
        }
        return best ?: unavailable()
    }

    private fun score(route: PlannedRoute, reference: RouteGeometry): Double {
        if (!validRoad(route) || !nearEndpoints(route, reference.points, 65.0)) return Double.POSITIVE_INFINITY
        val actual = RouteGeometry(route.polyline)
        val ratio = actual.length / reference.length
        if (ratio !in 0.7..1.5) return Double.POSITIVE_INFINITY
        val deviation = max(orderedDeviation(actual, reference), orderedDeviation(reference, actual))
        return if (deviation > 65.0) Double.POSITIVE_INFINITY else abs(ratio - 1.0) + deviation / 65.0
    }

    /** Check traversal order as well as proximity, rejecting nearby alley backtracking. */
    private fun orderedDeviation(from: RouteGeometry, to: RouteGeometry): Double {
        var progress = 0.0
        var worst = 0.0
        val samples = from.samples()
        val step = from.length / (samples.size - 1).coerceAtLeast(1)
        for ((index, point) in samples.withIndex()) {
            val p = to.project(point, max(0.0, progress - 30.0), minOf(to.length, progress + step * 2 + 65.0))
            worst = max(worst, p.distance)
            if (worst > 65.0) return worst
            progress = max(progress, p.along)
            if (index == samples.lastIndex && to.length - progress > 65.0) return Double.POSITIVE_INFINITY
        }
        return worst
    }

    /** RDP retains bends; fill long spans without flattening their corners. */
    internal fun anchors(points: List<GeoPoint>, tolerance: Double): List<GeoPoint> {
        val keep = sortedSetOf(0, points.lastIndex)
        val pending = ArrayDeque<Pair<Int, Int>>()
        pending.add(0 to points.lastIndex)
        while (pending.isNotEmpty()) {
            val (first, last) = pending.removeLast()
            if (last <= first + 1) continue
            val segment = RouteGeometry(listOf(points[first], points[last]))
            var farthest = -1
            var maxDistance = tolerance
            for (i in first + 1 until last) {
                val distance = if (segment.length == 0.0) GeoMath.distanceMeters(points[first], points[i])
                    else segment.project(points[i]).distance
                if (distance > maxDistance) { maxDistance = distance; farthest = i }
            }
            if (farthest >= 0) {
                keep += farthest
                if (keep.size > MAX_ANCHORS) return emptyList()
                pending.add(first to farthest)
                pending.add(farthest to last)
            }
        }
        val simplified = keep.map { points[it] }
        val length = GeoMath.pathDistanceMeters(simplified)
        val spacing = max(180.0, length / max(1, MAX_ANCHORS - simplified.size))
        val out = mutableListOf(simplified.first())
        for (i in 1 until simplified.size) {
            val count = ceil(GeoMath.distanceMeters(simplified[i - 1], simplified[i]) / spacing).toInt().coerceAtLeast(1)
            for (j in 1..count) out += GeoMath.interpolate(simplified[i - 1], simplified[i], j.toDouble() / count)
        }
        return if (out.size <= MAX_ANCHORS) out else simplified
    }

    private fun validPoints(points: List<GeoPoint>) = points.size >= 2 && points.all {
        it.latitude.isFinite() && it.longitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0
    } && GeoMath.pathDistanceMeters(points) > 0.0

    private fun validRoad(route: PlannedRoute) = route.snappedToRoads && route.gapPolylines.isEmpty() &&
        validPoints(route.polyline) && route.distanceMeters.isFinite() && route.distanceMeters > 0.0

    private fun nearEndpoints(route: PlannedRoute, input: List<GeoPoint>, radius: Double) =
        GeoMath.distanceMeters(route.polyline.first(), input.first()) <= radius &&
            GeoMath.distanceMeters(route.polyline.last(), input.last()) <= radius

    private fun unavailable() = PlannedRoute(emptyList(), 0.0, emptyList(), snappedToRoads = false)

    private companion object { const val MAX_ANCHORS = 40 }
}
