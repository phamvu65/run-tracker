package com.example.runtracker.domain.navigation

import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.RouteWaypoint
import com.example.runtracker.domain.tracking.GeoMath
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Điều hướng turn-by-turn: từ vị trí hiện tại + danh sách bước rẽ + polyline route,
 * cho biết bước kế tiếp, còn bao xa, đã lệch route chưa. Thuần JVM (stateless — service
 * giữ `currentStepIndex`).
 */
object RouteNavigator {

    /** Trong bán kính này coi như đã tới bước rẽ, chuyển sang bước sau. */
    const val ADVANCE_RADIUS_METERS = 25.0

    /** Xa polyline hơn mức này coi là đã đi chệch route. */
    const val OFF_ROUTE_METERS = 50.0

    data class Progress(
        val stepIndex: Int,
        val nextInstruction: String?,
        val distanceToNextMeters: Double?,
        val offRoute: Boolean,
        val arrived: Boolean,
    )

    fun progress(
        location: GeoPoint,
        steps: List<RouteWaypoint>,
        polyline: List<GeoPoint>,
        currentStepIndex: Int,
    ): Progress {
        val offRoute = polyline.size >= 2 &&
            distanceToPolylineMeters(location, polyline) > OFF_ROUTE_METERS

        if (steps.isEmpty()) {
            return Progress(currentStepIndex, null, null, offRoute, arrived = false)
        }

        var index = currentStepIndex.coerceIn(0, steps.size)
        while (index < steps.size &&
            GeoMath.distanceMeters(location, steps[index].location) <= ADVANCE_RADIUS_METERS
        ) {
            index++
        }

        if (index >= steps.size) {
            return Progress(steps.size, null, null, offRoute, arrived = true)
        }

        return Progress(
            stepIndex = index,
            nextInstruction = steps[index].instruction,
            distanceToNextMeters = GeoMath.distanceMeters(location, steps[index].location),
            offRoute = offRoute,
            arrived = false,
        )
    }

    /** Khoảng cách ngắn nhất từ điểm tới đường gấp khúc, mét. */
    fun distanceToPolylineMeters(point: GeoPoint, polyline: List<GeoPoint>): Double {
        if (polyline.isEmpty()) return Double.MAX_VALUE
        if (polyline.size == 1) return GeoMath.distanceMeters(point, polyline[0])

        var minDistance = Double.MAX_VALUE
        for (i in 1 until polyline.size) {
            minDistance = min(minDistance, distanceToSegmentMeters(point, polyline[i - 1], polyline[i]))
        }
        return minDistance
    }

    /**
     * Khoảng cách điểm–đoạn thẳng bằng phép chiếu equirectangular quanh điểm (đủ chính xác < ~1 km).
     */
    private fun distanceToSegmentMeters(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val metersPerDegLat = 111_320.0
        val metersPerDegLon = 111_320.0 * cos(Math.toRadians(p.latitude))

        val px = 0.0
        val py = 0.0
        val ax = (a.longitude - p.longitude) * metersPerDegLon
        val ay = (a.latitude - p.latitude) * metersPerDegLat
        val bx = (b.longitude - p.longitude) * metersPerDegLon
        val by = (b.latitude - p.latitude) * metersPerDegLat

        val dx = bx - ax
        val dy = by - ay
        val lengthSq = dx * dx + dy * dy
        if (lengthSq == 0.0) return hypot(ax - px, ay - py)

        val t = (((px - ax) * dx + (py - ay) * dy) / lengthSq).let { max(0.0, min(1.0, it)) }
        val projX = ax + t * dx
        val projY = ay + t * dy
        return hypot(projX - px, projY - py)
    }
}
