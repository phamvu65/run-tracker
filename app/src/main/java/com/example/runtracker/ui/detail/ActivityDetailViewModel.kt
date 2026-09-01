package com.example.runtracker.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.health.HeartRateSource
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityLap
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.UserRepository
import com.example.runtracker.domain.training.TrimpCalculator
import com.example.runtracker.domain.usecase.ImportHeartRateUseCase
import com.example.runtracker.domain.usecase.RefreshTrainingMetricsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Year
import java.time.ZoneId
import javax.inject.Inject

const val ARG_ACTIVITY_ID = "activityId"

@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ActivityRepository,
    userRepository: UserRepository,
    private val refreshTrainingMetrics: RefreshTrainingMetricsUseCase,
    private val heartRateSource: HeartRateSource,
    private val importHeartRate: ImportHeartRateUseCase,
) : ViewModel() {

    private val activityId: String = checkNotNull(savedStateHandle[ARG_ACTIVITY_ID])

    val heartRatePermissions: Set<String> = heartRateSource.requiredPermissions()

    private val _heartRateAvailable = MutableStateFlow(false)
    val heartRateAvailable: StateFlow<Boolean> = _heartRateAvailable

    var importMessage by mutableStateOf<String?>(null)
        private set

    val state: StateFlow<ActivityDetailUiState> = combine(
        repository.observeActivity(activityId),
        repository.observeRoutePoints(activityId),
        repository.observeLaps(activityId),
        userRepository.observeCurrentUser(),
    ) { activity, points, laps, user ->
        if (activity == null) {
            ActivityDetailUiState.NotFound
        } else {
            ActivityDetailUiState.Loaded(
                activity = activity,
                routePoints = points,
                laps = laps,
                trimp = TrimpCalculator.forActivity(activity, user, Year.now().value),
                canEnterRpe = activity.avgHeartRate == null,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ActivityDetailUiState.Loading,
    )

    init {
        viewModelScope.launch { _heartRateAvailable.value = heartRateSource.isAvailable() }
    }

    fun setPerceivedExertion(rpe: Int) {
        viewModelScope.launch {
            val activity = repository.getActivity(activityId) ?: return@launch
            repository.upsertActivity(activity.copy(perceivedExertion = rpe.coerceIn(1, 10)))
            refreshTrainingMetrics(activity.startTime.atZone(ZoneId.systemDefault()).toLocalDate())
        }
    }

    /** Nhập HR nếu đã có quyền; nếu chưa, gọi [onNeedPermission] để màn hình xin quyền. */
    fun importHeartRateOrRequest(onNeedPermission: () -> Unit) {
        viewModelScope.launch {
            if (heartRateSource.hasPermission()) runImport() else onNeedPermission()
        }
    }

    fun onHeartRatePermissionGranted() {
        viewModelScope.launch { runImport() }
    }

    private suspend fun runImport() {
        importMessage = "Đang đồng bộ nhịp tim…"
        val count = importHeartRate(activityId)
        importMessage = if (count > 0) {
            "Đã nhập $count mẫu nhịp tim."
        } else {
            "Không tìm thấy dữ liệu nhịp tim cho buổi tập này."
        }
    }
}

sealed interface ActivityDetailUiState {
    data object Loading : ActivityDetailUiState
    data object NotFound : ActivityDetailUiState
    data class Loaded(
        val activity: Activity,
        val routePoints: List<RoutePoint>,
        val laps: List<ActivityLap>,
        val trimp: Double?,
        val canEnterRpe: Boolean,
    ) : ActivityDetailUiState
}
