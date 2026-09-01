package com.example.runtracker.domain.health

/**
 * Giải mã characteristic Heart Rate Measurement (BLE GATT 0x2A37).
 *
 * Byte 0 = flags:
 *  - bit 0: định dạng giá trị HR (0 = uint8 ở byte 1, 1 = uint16 little-endian ở byte 1-2)
 *
 * Thuần JVM.
 */
object HeartRateMeasurement {

    /** @return nhịp tim (bpm) hoặc null nếu dữ liệu không hợp lệ / vô lý. */
    fun parseBpm(bytes: ByteArray): Int? {
        if (bytes.isEmpty()) return null
        val flags = bytes[0].toInt()
        val is16Bit = flags and 0x01 == 0x01

        val bpm = if (is16Bit) {
            if (bytes.size < 3) return null
            (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
        } else {
            if (bytes.size < 2) return null
            bytes[1].toInt() and 0xFF
        }

        return bpm.takeIf { it in 20..250 }
    }
}
