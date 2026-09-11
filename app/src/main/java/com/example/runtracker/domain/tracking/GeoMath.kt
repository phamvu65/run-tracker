package com.example.runtracker.domain.tracking

import com.example.runtracker.domain.model.GeoPoint
import kotlin.math.asin
import kotlin.math.atan2
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

    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double =
        distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)

    /** Hướng ban đầu (độ, 0-360, 0 = Bắc) đi từ [a] tới [b] theo great circle. */
    fun bearingDegrees(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Nội suy tuyến tính giữa 2 điểm; `fraction` 0..1. */
    fun interpolate(a: GeoPoint, b: GeoPoint, fraction: Double): GeoPoint = GeoPoint(
        latitude = a.latitude + (b.latitude - a.latitude) * fraction,
        longitude = a.longitude + (b.longitude - a.longitude) * fraction,
    )

    /**
     * Lấy `count` điểm cách đều nhau theo chiều dài dọc đường gấp khúc.
     * Trả về nguyên list nếu đã ít hơn `count` điểm.
     */
    fun resample(points: List<GeoPoint>, count: Int): List<GeoPoint> {
        if (count < 2 || points.size <= count) return points
        val total = pathDistanceMeters(points)
        if (total == 0.0) return listOf(points.first())

        val step = total / (count - 1)
        val out = ArrayList<GeoPoint>(count)
        out += points.first()
        var accumulated = 0.0
        var target = step

        for (i in 1 until points.size) {
            val segLength = distanceMeters(points[i - 1], points[i])
            while (segLength > 0.0 && accumulated + segLength >= target && out.size < count - 1) {
                val fraction = (target - accumulated) / segLength
                out += interpolate(points[i - 1], points[i], fraction)
                target += step
            }
            accumulated += segLength
        }
        out += points.last()
        return out
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
