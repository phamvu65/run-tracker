package com.example.runtracker.domain.tracking

import com.example.runtracker.domain.model.RoutePoint

/** Thống kê dẫn xuất từ trace GPS. Thuần JVM. */
object RouteStats {

    /** Khoảng cách tích luỹ (m) tại mỗi điểm; phần tử đầu luôn 0. */
    fun cumulativeDistances(points: List<RoutePoint>): List<Double> {
        if (points.isEmpty()) return emptyList()
        val out = ArrayList<Double>(points.size)
        var cumulative = 0.0
        out += 0.0
        for (i in 1 until points.size) {
            cumulative += GeoMath.distanceMeters(
                points[i - 1].latitude, points[i - 1].longitude,
                points[i].latitude, points[i].longitude,
            )
            out += cumulative
        }
        return out
    }
}
