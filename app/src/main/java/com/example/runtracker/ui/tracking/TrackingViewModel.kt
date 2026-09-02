package com.example.runtracker.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.Route
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.RouteRepository
import com.example.runtracker.domain.usecase.FinalizeActivityUseCase
import com.example.runtracker.tracking.BeaconController
import com.example.runtracker.tracking.BeaconShareState
import com.example.runtracker.tracking.TrackingSession
import com.example.runtracker.tracking.TrackingState
import com.example.runtracker.tracking.TrackingStateStore
import com.example.runtracker.tracking.TrackingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val session: TrackingSession,
    private val repository: ActivityRepository,
    private val routeRepository: RouteRepository,
    private val stateStore: TrackingStateStore,
    private val finalizeActivityUseCase: FinalizeActivityUseCase,
    private val beaconController: BeaconController,
) : ViewModel() {

    val tracking: StateFlow<TrackingState> = session.state

    val beacon: StateFlow<BeaconShareState> = beaconController.state

    fun setBeaconSharing(enabled: Boolean) {
        if (enabled) beaconController.enable() else beaconController.disable()
    }

    val activities: StateFlow<List<Activity>> = repository.observeActivities(LOCAL_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val routes: StateFlow<List<Route>> = routeRepository.observeRoutes(LOCAL_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedRoute: StateFlow<Route?> = session.selectedRoute

    /** Trace GPS đã ghi của buổi đang chạy, để vẽ polyline live trên map. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val liveTrace: StateFlow<List<GeoPoint>> = session.state
        .map { it.activityId }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(emptyList())
            } else {
                repository.observeRoutePoints(id)
                    .map { points -> points.map { GeoPoint(it.latitude, it.longitude) } }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectRoute(routeId: String?) {
        viewModelScope.launch {
            session.selectRoute(routeId?.let { routeRepository.getRoute(it) })
        }
    }

    /** Id buổi tập còn cờ "đang chạy" nhưng service không còn tracking — tức bị OS kill. */
    private val _interruptedActivityId = MutableStateFlow<String?>(null)
    val interruptedActivityId: StateFlow<String?> = _interruptedActivityId.asStateFlow()

    init {
        viewModelScope.launch {
            session.state.collect { state ->
                _interruptedActivityId.value =
                    if (state.status == TrackingStatus.IDLE) stateStore.activeActivityId() else null
            }
        }
    }

    /** Người dùng chọn "tiếp tục ghi": service sẽ tự khôi phục, chỉ cần ẩn hộp thoại. */
    fun onInterruptedResumed() {
        _interruptedActivityId.value = null
    }

    /** Chốt số liệu buổi bị gián đoạn từ trace đã lưu (endTime = điểm GPS cuối). */
    fun finalizeInterrupted() {
        val id = _interruptedActivityId.value ?: return
        _interruptedActivityId.value = null
        viewModelScope.launch {
            finalizeActivityUseCase(id)
            stateStore.clear()
        }
    }

    fun discardInterrupted() {
        val id = _interruptedActivityId.value ?: return
        _interruptedActivityId.value = null
        viewModelScope.launch {
            repository.deleteActivity(id)
            stateStore.clear()
        }
    }
}
