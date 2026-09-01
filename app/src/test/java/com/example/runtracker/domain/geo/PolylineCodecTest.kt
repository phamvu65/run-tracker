package com.example.runtracker.domain.geo

import com.example.runtracker.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class PolylineCodecTest {

    @Test
    fun `decodes the canonical Google example`() {
        val points = PolylineCodec.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@")

        assertEquals(3, points.size)
        assertEquals(38.5, points[0].latitude, 1e-5)
        assertEquals(-120.2, points[0].longitude, 1e-5)
        assertEquals(40.7, points[1].latitude, 1e-5)
        assertEquals(-120.95, points[1].longitude, 1e-5)
        assertEquals(43.252, points[2].latitude, 1e-5)
        assertEquals(-126.453, points[2].longitude, 1e-5)
    }

    @Test
    fun `round trips arbitrary points at 1e-5 precision`() {
        val original = listOf(
            GeoPoint(10.762622, 106.660172),
            GeoPoint(10.763100, 106.661000),
            GeoPoint(10.761900, 106.659500),
        )

        val restored = PolylineCodec.decode(PolylineCodec.encode(original))

        assertEquals(original.size, restored.size)
        original.zip(restored).forEach { (a, b) ->
            assertEquals(a.latitude, b.latitude, 1e-5)
            assertEquals(a.longitude, b.longitude, 1e-5)
        }
    }

    @Test
    fun `empty input round trips to empty`() {
        assertEquals(emptyList<GeoPoint>(), PolylineCodec.decode(PolylineCodec.encode(emptyList())))
    }
}
