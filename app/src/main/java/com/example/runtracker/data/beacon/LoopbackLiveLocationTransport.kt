package com.example.runtracker.data.beacon

import com.example.runtracker.domain.beacon.LiveLocationTransport
import com.example.runtracker.domain.model.BeaconSnapshot
import com.example.runtracker.domain.model.LiveLocationUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bản truyền chạy hoàn toàn trong bộ nhớ tiến trình — người phát và người theo dõi phải ở
 * cùng một máy (đủ để demo và test end-to-end). Thay bằng impl mạng thật khi có backend.
 */
@Singleton
class LoopbackLiveLocationTransport @Inject constructor() : LiveLocationTransport {

    private val channels = ConcurrentHashMap<String, MutableStateFlow<BeaconSnapshot>>()

    private fun channel(code: String): MutableStateFlow<BeaconSnapshot> =
        channels.getOrPut(code) { MutableStateFlow(BeaconSnapshot.empty(code)) }

    override suspend fun startBroadcast(code: String) {
        channel(code).value = BeaconSnapshot.empty(code)
    }

    override suspend fun publish(code: String, update: LiveLocationUpdate) {
        channel(code).update { snapshot ->
            snapshot.copy(
                latest = update,
                trail = (snapshot.trail + update).takeLast(MAX_TRAIL),
                ended = false,
            )
        }
    }

    override suspend fun endBroadcast(code: String) {
        channel(code).update { it.copy(ended = true) }
    }

    override fun observe(code: String): Flow<BeaconSnapshot> = channel(code).asStateFlow()

    private companion object {
        const val MAX_TRAIL = 500
    }
}
