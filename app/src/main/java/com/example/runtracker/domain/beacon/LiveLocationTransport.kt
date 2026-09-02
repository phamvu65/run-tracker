package com.example.runtracker.domain.beacon

import com.example.runtracker.domain.model.BeaconSnapshot
import com.example.runtracker.domain.model.LiveLocationUpdate
import kotlinx.coroutines.flow.Flow

/**
 * Kênh truyền vị trí trực tiếp giữa người chạy (phát) và người theo dõi (nhận).
 *
 * Hiện có bản [com.example.runtracker.data.beacon.LoopbackLiveLocationTransport] chạy trong bộ nhớ
 * (demo/test trên cùng một máy). Khi chốt backend, thêm một impl mới (Firebase Realtime Database,
 * Ktor + WebSocket...) và đổi binding trong `di/BeaconModule` — phần còn lại không phải sửa.
 */
interface LiveLocationTransport {

    /** Người chạy: mở kênh cho [code] (xoá dữ liệu cũ nếu có). */
    suspend fun startBroadcast(code: String)

    /** Người chạy: đẩy một cập nhật vị trí. */
    suspend fun publish(code: String, update: LiveLocationUpdate)

    /** Người chạy: kết thúc buổi phát. */
    suspend fun endBroadcast(code: String)

    /** Người theo dõi: dòng trạng thái beacon theo [code]. */
    fun observe(code: String): Flow<BeaconSnapshot>
}
