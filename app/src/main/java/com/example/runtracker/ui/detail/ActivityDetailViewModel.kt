package com.example.runtracker.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.health.HeartRateSource
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityLap
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.SegmentRepository
import com.example.runtracker.domain.repository.UserRepository
import com.example.runtracker.domain.repository.ZoneSettingsRepository
import com.example.runtracker.domain.training.TrimpCalculator
import com.example.runtracker.domain.training.ZoneDistribution
import com.example.runtracker.domain.training.ZoneTime
import com.example.runtracker.domain.usecase.ExportActivityGpxUseCase
import com.example.runtracker.domain.usecase.FetchActivityWeatherUseCase
import com.example.runtracker.domain.usecase.ImportHeartRateUseCase
import com.example.runtracker.domain.usecase.RefreshTrainingMetricsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.Year
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

const val ARG_ACTIVITY_ID = "activityId"

@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ActivityRepository,
    userRepository: UserRepository,
    zoneSettingsRepository: ZoneSettingsRepository,
    segmentRepository: SegmentRepository,
    private val refreshTrainingMetrics: RefreshTrainingMetricsUseCase,
    private val heartRateSource: HeartRateSource,
    private val importHeartRate: ImportHeartRateUseCase,
    private val fetchActivityWeather: FetchActivityWeatherUseCase,
    private val exportActivityGpx: ExportActivityGpxUseCase,
) : ViewModel() {

    private val activityId: String = checkNotNull(savedStateHandle[ARG_ACTIVITY_ID])

    val heartRatePermissions: Set<String> = heartRateSource.requiredPermissions()

    private val _heartRateAvailable = MutableStateFlow(false)
    val heartRateAvailable: StateFlow<Boolean> = _heartRateAvailable

    var importMessage by mutableStateOf<String?>(null)
        private set

    var weatherMessage by mutableStateOf<String?>(null)
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

    val zoneDistribution: StateFlow<List<ZoneTime>> = combine(
        repository.observeHeartRateSamples(activityId),
        zoneSettingsRepository.observeZones(LOCAL_USER_ID),
    ) { samples, zones ->
        if (zones == null) emptyList() else ZoneDistribution.compute(samples, zones.zones)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val segmentEfforts: StateFlow<List<SegmentEffortRow>> = combine(
        segmentRepository.observeEffortsForActivity(activityId),
        segmentRepository.observeSegments(),
    ) { efforts, segments ->
        val byId = segments.associateBy { it.id }
        efforts.mapNotNull { effort ->
            byId[effort.segmentId]?.let { SegmentEffortRow(it.name, effort.elapsedSeconds) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { _heartRateAvailable.value = heartRateSource.isAvailable() }
        viewModelScope.launch { autoFetchWeather() }
    }

    /** Tự lấy thời tiết khi mở chi tiết nếu chưa có + buổi tập trong ~90 ngày (im lặng khi thất bại). */
    private suspend fun autoFetchWeather() {
        val activity = repository.getActivity(activityId) ?: return
        if (activity.weather != null) return
        if (activity.startTime.isBefore(Instant.now().minus(90, ChronoUnit.DAYS))) return
        fetchActivityWeather(activityId)
    }

    fun refreshWeather() {
        viewModelScope.launch {
            weatherMessage = "Đang lấy thời tiết…"
            weatherMessage = when (fetchActivityWeather(activityId, force = true)) {
                FetchActivityWeatherUseCase.Outcome.FETCHED -> "Đã cập nhật thời tiết."
                FetchActivityWeatherUseCase.Outcome.ALREADY_PRESENT -> null
                FetchActivityWeatherUseCase.Outcome.NO_LOCATION ->
                    "Buổi tập không có dữ liệu GPS để tra thời tiết."
                FetchActivityWeatherUseCase.Outcome.FAILED ->
                    "Không lấy được thời tiết (chỉ hỗ trợ khoảng 90 ngày gần đây)."
            }
        }
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

    suspend fun exportGpx(): String? = exportActivityGpx(activityId)

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

data class SegmentEffortRow(val segmentName: String, val elapsedSeconds: Double)

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
