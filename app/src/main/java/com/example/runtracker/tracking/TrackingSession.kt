package com.example.runtracker.tracking

import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.model.Route
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

    /** Route người dùng chọn để điều hướng turn-by-turn cho buổi tập sắp tới. */
    private val _selectedRoute = MutableStateFlow<Route?>(null)
    val selectedRoute: StateFlow<Route?> = _selectedRoute.asStateFlow()

    /** Loại hoạt động cho buổi tập sắp tới (Service đọc lúc START). */
    private val _plannedType = MutableStateFlow(ActivityType.RUNNING)
    val plannedType: StateFlow<ActivityType> = _plannedType.asStateFlow()

    fun setPlannedType(type: ActivityType) {
        _plannedType.value = type
    }

    /**
     * Id buổi tập vừa chốt số liệu xong (STOP bình thường hoặc "kết thúc buổi gián đoạn") — tín
     * hiệu một lần để UI điều hướng sang màn kết quả. Chỉ mang id, không mang kỷ lục: màn kết
     * quả tự tính lại qua [com.example.runtracker.domain.usecase.DetectPersonalRecordsUseCase]
     * khi mở, tránh phải truyền dữ liệu phức tạp qua Compose Navigation.
     */
    private val _justFinishedActivityId = MutableStateFlow<String?>(null)
    val justFinishedActivityId: StateFlow<String?> = _justFinishedActivityId.asStateFlow()

    fun activityFinished(activityId: String) {
        _justFinishedActivityId.value = activityId
    }

    fun consumeJustFinishedActivity() {
        _justFinishedActivityId.value = null
    }

    fun update(transform: (TrackingState) -> TrackingState) = _state.update(transform)

    fun reset() {
        _state.value = TrackingState()
    }

    fun selectRoute(route: Route?) {
        _selectedRoute.value = route
    }
}
