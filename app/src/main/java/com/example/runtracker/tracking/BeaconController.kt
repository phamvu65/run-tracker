package com.example.runtracker.tracking

import com.example.runtracker.domain.beacon.BeaconCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cầu nối bật/tắt chia sẻ vị trí trực tiếp giữa UI và [LocationTrackingService].
 * Singleton — [LocationTrackingService] quan sát [state] để mở/đóng kênh phát.
 */
@Singleton
class BeaconController @Inject constructor() {

    private val _state = MutableStateFlow(BeaconShareState())
    val state: StateFlow<BeaconShareState> = _state.asStateFlow()

    /** Bật chia sẻ; sinh mã mới nếu chưa có. */
    fun enable() {
        _state.update {
            if (it.sharing) it else it.copy(sharing = true, code = it.code ?: BeaconCode.random())
        }
    }

    fun disable() {
        _state.update { it.copy(sharing = false) }
    }

    /** Xoá hẳn (gọi khi buổi tập kết thúc). */
    fun reset() {
        _state.value = BeaconShareState()
    }
}

data class BeaconShareState(
    val sharing: Boolean = false,
    val code: String? = null,
)
