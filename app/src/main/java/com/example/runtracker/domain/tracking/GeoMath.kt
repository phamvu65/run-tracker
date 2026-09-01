package com.example.runtracker.domain.tracking

import com.example.runtracker.domain.model.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Tiện ích hình học địa lý thuần (không phụ thuộc Android) để test được trên JVM.
 */
object GeoMath {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Khoảng cách great-circle giữa 2 toạ độ, mét (haversine). */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(a)))
    }

    /** Tổng độ dài đường gấp khúc qua các điểm, mét. */
    fun pathDistanceMeters(points: List<GeoPoint>): Double {
        var sum = 0.0
        for (i in 1 until points.size) {
            sum += distanceMeters(
                points[i - 1].latitude, points[i - 1].longitude,
                points[i].latitude, points[i].longitude,
            )
        }
        return sum
    }
}
