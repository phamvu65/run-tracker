package com.example.runtracker.domain.health

import kotlinx.coroutines.flow.Flow

/**
 * Cảm biến khí áp (barometer) của máy — dùng để tính độ cao chính xác hơn GPS altitude
 * (xem [com.example.runtracker.domain.tracking.BarometerAltitude]). Không phải máy nào cũng có
 * ([isSupported] = false thì tracking service giữ nguyên GPS altitude).
 */
interface BarometerSource {

    fun isSupported(): Boolean

    /** Áp suất khí quyển (hPa), phát liên tục cho tới khi flow bị huỷ. */
    fun pressureUpdates(): Flow<Float>
}
