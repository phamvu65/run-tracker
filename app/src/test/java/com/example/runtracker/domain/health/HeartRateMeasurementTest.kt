package com.example.runtracker.domain.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeartRateMeasurementTest {

    @Test
    fun `uint8 format`() {
        // flags 0x00 -> uint8; 0x4B = 75 bpm
        assertEquals(75, HeartRateMeasurement.parseBpm(byteArrayOf(0x00, 0x4B)))
    }

    @Test
    fun `uint16 format little-endian`() {
        // flags 0x01 -> uint16; 0x2C 0x01 -> 300... vô lý -> null; dùng 0x90 0x00 = 144
        assertEquals(144, HeartRateMeasurement.parseBpm(byteArrayOf(0x01, 0x90.toByte(), 0x00)))
    }

    @Test
    fun `rejects implausible values`() {
        assertNull(HeartRateMeasurement.parseBpm(byteArrayOf(0x00, 0x05))) // 5 bpm
        assertNull(HeartRateMeasurement.parseBpm(byteArrayOf(0x01, 0xFF.toByte(), 0x00))) // 255
    }

    @Test
    fun `rejects truncated packets`() {
        assertNull(HeartRateMeasurement.parseBpm(byteArrayOf()))
        assertNull(HeartRateMeasurement.parseBpm(byteArrayOf(0x00)))
        assertNull(HeartRateMeasurement.parseBpm(byteArrayOf(0x01, 0x4B)))
    }
}
