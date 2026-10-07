package com.example.runtracker.domain.navigation

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.tracking.GeoMath
import kotlin.math.cos
import kotlin.math.hypot

/** Distances along a route, preserving traversal order at loops and crossings. */
class RouteGeometry(val points: List<GeoPoint>) {
    val cumulative = DoubleArray(points.size).also { distances ->
        for (i in 1 until points.size) {
            distances[i] = distances[i - 1] + GeoMath.distanceMeters(points[i - 1], points[i])
        }
    }
    val length: Double get() = cumulative.lastOrNull() ?: 0.0
    data class Projection(val along: Double, val distance: Double)

    fun project(point: GeoPoint, minAlong: Double = 0.0, maxAlong: Double = length): Projection {
        var best = Projection(minAlong, Double.MAX_VALUE)
        val xScale = 111_320.0 * cos(Math.toRadians(point.latitude))
        for (i in 1 until points.size) {
            val segmentLength = cumulative[i] - cumulative[i - 1]
            if (segmentLength <= 0.0 || cumulative[i] < minAlong || cumulative[i - 1] > maxAlong) continue
            val a = points[i - 1]
            val b = points[i]
            val ax = (a.longitude - point.longitude) * xScale
            val ay = (a.latitude - point.latitude) * 111_320.0
            val dx = (b.longitude - a.longitude) * xScale
            val dy = (b.latitude - a.latitude) * 111_320.0
            val square = dx * dx + dy * dy
            if (square == 0.0) continue
            val low = ((minAlong - cumulative[i - 1]) / segmentLength).coerceIn(0.0, 1.0)
            val high = ((maxAlong - cumulative[i - 1]) / segmentLength).coerceIn(low, 1.0)
            val fraction = (-(ax * dx + ay * dy) / square).coerceIn(low, high)
            val distance = hypot(ax + fraction * dx, ay + fraction * dy)
            if (distance < best.distance - 0.01) {
                best = Projection(cumulative[i - 1] + fraction * segmentLength, distance)
            }
        }
        return best
    }

    fun pointAt(distance: Double): GeoPoint {
        require(points.isNotEmpty())
        val target = distance.coerceIn(0.0, length)
        val end = cumulative.indexOfFirst { it >= target }.coerceAtLeast(0)
        if (end == 0) return points.first()
        val span = cumulative[end] - cumulative[end - 1]
        return GeoMath.interpolate(points[end - 1], points[end], if (span > 0) (target - cumulative[end - 1]) / span else 0.0)
    }

    fun samples(spacing: Double = 20.0): List<GeoPoint> {
        if (points.size < 2 || length == 0.0) return points
        val count = kotlin.math.ceil(length / spacing).toInt().coerceIn(1, 2000)
        return (0..count).map { pointAt(length * it / count) }
    }
}
