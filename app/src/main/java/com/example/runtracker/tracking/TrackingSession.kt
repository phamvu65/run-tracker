package com.example.runtracker.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cầu nối trạng thái giữa [LocationTrackingService] (ghi) và UI (đọc).
 * Singleton nên sống độc lập với vòng đời của Service — UI vẫn đọc được state cuối
 * ngay cả khi Service vừa bị dừng.
 */
@Singleton
class TrackingSession @Inject constructor() {

    private val _state = MutableStateFlow(TrackingState())
    val state: StateFlow<TrackingState> = _state.asStateFlow()

    fun update(transform: (TrackingState) -> TrackingState) = _state.update(transform)

    fun reset() {
        _state.value = TrackingState()
    }
}
