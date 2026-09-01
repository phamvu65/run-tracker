package com.example.runtracker.domain.segment

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.tracking.GeoMath
import java.time.Instant

/**
 * Khớp một trace GPS với một segment (điểm đầu / cuối + độ dài).
 * v1: bám điểm đầu gần segment start, điểm cuối gần segment end (sau điểm đầu),
 * và tổng quãng đường đi qua nằm trong ±25% độ dài segment. Chưa so khớp toàn bộ hình dạng.
 * Thuần JVM.
 */
object SegmentMatcher {

    const val ENDPOINT_TOLERANCE_METERS = 30.0
    const val DISTANCE_TOLERANCE_FRACTION = 0.25

    data class Match(val elapsedSeconds: Double, val startTime: Instant)

    fun match(
        route: List<RoutePoint>,
        segmentStart: GeoPoint,
        segmentEnd: GeoPoint,
        segmentDistanceMeters: Double,
    ): Match? {
        if (route.size < 2 || segmentDistanceMeters <= 0.0) return null

        val startIdx = closestIndex(route, segmentStart, from = 0) ?: return null
        val endIdx = closestIndex(route, segmentEnd, from = startIdx + 1) ?: return null
        if (endIdx <= startIdx) return null

        val traversed = pathDistance(route, startIdx, endIdx)
        val low = segmentDistanceMeters * (1 - DISTANCE_TOLERANCE_FRACTION)
        val high = segmentDistanceMeters * (1 + DISTANCE_TOLERANCE_FRACTION)
        if (traversed < low || traversed > high) return null

        val elapsed = (route[endIdx].timestamp.toEpochMilli() - route[startIdx].timestamp.toEpochMilli()) / 1000.0
        if (elapsed <= 0.0) return null

        return Match(elapsed, route[startIdx].timestamp)
    }

    private fun closestIndex(route: List<RoutePoint>, target: GeoPoint, from: Int): Int? {
        var bestIdx = -1
        var bestDist = Double.MAX_VALUE
        for (i in from until route.size) {
            val d = GeoMath.distanceMeters(
                route[i].latitude, route[i].longitude, target.latitude, target.longitude,
            )
            if (d < bestDist) {
                bestDist = d
                bestIdx = i
            }
        }
        return if (bestIdx >= 0 && bestDist <= ENDPOINT_TOLERANCE_METERS) bestIdx else null
    }

    private fun pathDistance(route: List<RoutePoint>, fromIdx: Int, toIdx: Int): Double {
        var sum = 0.0
        for (i in fromIdx + 1..toIdx) {
            sum += GeoMath.distanceMeters(
                route[i - 1].latitude, route[i - 1].longitude,
                route[i].latitude, route[i].longitude,
            )
        }
        return sum
    }
}
