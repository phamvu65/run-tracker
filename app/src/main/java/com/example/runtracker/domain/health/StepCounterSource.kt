package com.example.runtracker.domain.health

import kotlinx.coroutines.flow.Flow

/**
 * Cảm biến đếm bước chân phần cứng (`Sensor.TYPE_STEP_COUNTER`) — không phải máy nào cũng có
 * ([isSupported]), và từ Android 10 (API 29) cần quyền `ACTIVITY_RECOGNITION` ([hasPermission]).
 */
interface StepCounterSource {

    fun isSupported(): Boolean

    fun hasPermission(): Boolean

    /** Tổng số bước tích luỹ từ khi thiết bị khởi động — caller tự tính delta giữa 2 lần phát. */
    fun stepCountUpdates(): Flow<Int>
}
