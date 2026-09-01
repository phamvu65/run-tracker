package com.example.runtracker.domain.geo

import com.example.runtracker.domain.model.GeoPoint

/**
 * Encoded Polyline Algorithm Format của Google (độ chính xác 1e-5). Thuần JVM.
 * Dùng để lưu đường đi của segment/route gọn trong một cột String.
 */
object PolylineCodec {

    fun encode(points: List<GeoPoint>): String {
        val sb = StringBuilder()
        var lastLat = 0L
        var lastLng = 0L
        for (p in points) {
            val lat = Math.round(p.latitude * 1e5)
            val lng = Math.round(p.longitude * 1e5)
            encodeSignedValue(lat - lastLat, sb)
            encodeSignedValue(lng - lastLng, sb)
            lastLat = lat
            lastLng = lng
        }
        return sb.toString()
    }

    fun decode(encoded: String): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        var index = 0
        var lat = 0
        var lng = 0
        while (index < encoded.length) {
            var shift = 0
            var result = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            points.add(GeoPoint(lat / 1e5, lng / 1e5))
        }
        return points
    }

    private fun encodeSignedValue(value: Long, sb: StringBuilder) {
        var v = if (value < 0) (value shl 1).inv() else value shl 1
        while (v >= 0x20) {
            sb.append((((0x20 or (v and 0x1f).toInt())) + 63).toChar())
            v = v shr 5
        }
        sb.append((v.toInt() + 63).toChar())
    }
}
