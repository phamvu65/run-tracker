package com.example.runtracker.domain.segment

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.tracking.GeoMath
import java.time.Instant

/**
 * Khớp một trace GPS với một segment.
 *  1. bám điểm đầu gần segment start, điểm cuối gần segment end (sau điểm đầu),
 *  2. tổng quãng đường đi qua nằm trong ±25% độ dài segment,
 *  3. hình dạng: đa số điểm mẫu dọc polyline segment đều có điểm route ở gần.
 * Thuần JVM.
 */
object SegmentMatcher {

    const val ENDPOINT_TOLERANCE_METERS = 30.0
    const val DISTANCE_TOLERANCE_FRACTION = 0.25

    const val SHAPE_SAMPLE_COUNT = 20
    const val SHAPE_TOLERANCE_METERS = 35.0
    const val MAX_SHAPE_MISS_FRACTION = 0.2

    data class Match(val elapsedSeconds: Double, val startTime: Instant)

    fun match(
        route: List<RoutePoint>,
        segmentStart: GeoPoint,
        segmentEnd: GeoPoint,
        segmentDistanceMeters: Double,
        segmentPolyline: List<GeoPoint> = emptyList(),
    ): Match? {
        if (route.size < 2 || segmentDistanceMeters <= 0.0) return null

        val startIdx = closestIndex(route, segmentStart, from = 0) ?: return null
        val endIdx = closestIndex(route, segmentEnd, from = startIdx + 1) ?: return null
        if (endIdx <= startIdx) return null

        val traversed = pathDistance(route, startIdx, endIdx)
        val low = segmentDistanceMeters * (1 - DISTANCE_TOLERANCE_FRACTION)
        val high = segmentDistanceMeters * (1 + DISTANCE_TOLERANCE_FRACTION)
        if (traversed < low || traversed > high) return null

        if (segmentPolyline.size >= 2 && !shapeMatches(route, startIdx, endIdx, segmentPolyline)) {
            return null
        }

        val elapsed = (route[endIdx].timestamp.toEpochMilli() - route[startIdx].timestamp.toEpochMilli()) / 1000.0
        if (elapsed <= 0.0) return null

        return Match(elapsed, route[startIdx].timestamp)
    }

    private fun shapeMatches(
        route: List<RoutePoint>,
        fromIdx: Int,
        toIdx: Int,
        segmentPolyline: List<GeoPoint>,
    ): Boolean {
        val samples = GeoMath.resample(segmentPolyline, SHAPE_SAMPLE_COUNT)
        val window = route.subList(fromIdx, toIdx + 1)

        val misses = samples.count { sample ->
            window.none { point ->
                GeoMath.distanceMeters(point.latitude, point.longitude, sample.latitude, sample.longitude) <=
                    SHAPE_TOLERANCE_METERS
            }
        }
        return misses <= samples.size * MAX_SHAPE_MISS_FRACTION
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
