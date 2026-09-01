package com.example.runtracker.domain.health

import kotlinx.coroutines.flow.Flow

/** Thiết bị nhịp tim BLE tìm thấy khi quét. */
data class BleDevice(val name: String?, val address: String)

/**
 * Nguồn nhịp tim thời gian thực (đai đeo ngực BLE — GATT Heart Rate Service).
 * Khác [HeartRateSource] (đọc lịch sử): đây là luồng bpm live trong lúc chạy.
 */
interface LiveHeartRateSource {

    fun isSupported(): Boolean

    fun requiredPermissions(): Array<String>

    fun hasPermissions(): Boolean

    /** Quét thiết bị HR gần đó cho tới khi flow bị huỷ. */
    fun scan(): Flow<BleDevice>

    /** Kết nối và phát bpm live; flow kết thúc khi mất kết nối. */
    fun connect(deviceAddress: String): Flow<Int>
}
