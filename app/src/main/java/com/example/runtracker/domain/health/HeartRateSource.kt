package com.example.runtracker.domain.health

import com.example.runtracker.domain.model.HeartRateSample
import java.time.Instant

/**
 * Nguồn dữ liệu nhịp tim. Hiện có bản Health Connect; sau này thêm bản BLE strap
 * mà không phải sửa use-case.
 */
interface HeartRateSource {

    /** Nền tảng có hỗ trợ nguồn này không (VD: máy đã cài Health Connect). */
    suspend fun isAvailable(): Boolean

    /** Các quyền cần xin — chuỗi do implementation quy định (khớp contract xin quyền của nó). */
    fun requiredPermissions(): Set<String>

    suspend fun hasPermission(): Boolean

    /** Mẫu nhịp tim trong khoảng thời gian, theo thứ tự thời gian tăng dần. */
    suspend fun samplesBetween(start: Instant, end: Instant): List<HeartRateSample>
}
