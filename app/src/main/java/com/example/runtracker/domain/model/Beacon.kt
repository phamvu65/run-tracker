package com.example.runtracker.domain.model

import java.time.Instant

/** Một lần cập nhật vị trí trực tiếp mà người chạy phát đi (beacon). */
data class LiveLocationUpdate(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Instant,
    val elapsedSeconds: Long,
    val distanceMeters: Double,
    val paused: Boolean,
)

/** Trạng thái hiện tại của một beacon mà người theo dõi nhìn thấy. */
data class BeaconSnapshot(
    val code: String,
    val latest: LiveLocationUpdate?,
    /** Vệt đường đã đi (giới hạn số điểm gần nhất). */
    val trail: List<LiveLocationUpdate>,
    val ended: Boolean,
) {
    companion object {
        fun empty(code: String) = BeaconSnapshot(code, latest = null, trail = emptyList(), ended = false)
    }
}

enum class BeaconStatus { WAITING, LIVE, STALE, ENDED }
