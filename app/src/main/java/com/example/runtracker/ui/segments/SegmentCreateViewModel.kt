package com.example.runtracker.ui.segments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.tracking.RouteStats
import com.example.runtracker.domain.usecase.CreateSegmentUseCase
import com.example.runtracker.ui.detail.ARG_ACTIVITY_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SegmentCreateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val activityRepository: ActivityRepository,
    private val createSegment: CreateSegmentUseCase,
) : ViewModel() {

    private val activityId: String = checkNotNull(savedStateHandle[ARG_ACTIVITY_ID])

    var routePoints by mutableStateOf<List<GeoPoint>>(emptyList())
        private set
    var totalDistanceMeters by mutableStateOf(0f)
        private set
    var savedSegmentId by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

    private var cumulative: List<Double> = emptyList()

    init {
        viewModelScope.launch {
            val points = activityRepository.getRoutePoints(activityId)
            routePoints = points.map { GeoPoint(it.latitude, it.longitude) }
            cumulative = RouteStats.cumulativeDistances(points)
            totalDistanceMeters = cumulative.lastOrNull()?.toFloat() ?: 0f
        }
    }

    /** Đoạn polyline nằm trong khoảng quãng đường đã chọn (để tô màu trên map). */
    fun subRange(fromMeters: Float, toMeters: Float): List<GeoPoint> {
        if (cumulative.isEmpty()) return emptyList()
        return routePoints.filterIndexed { i, _ ->
            cumulative[i] in fromMeters.toDouble()..toMeters.toDouble()
        }
    }

    fun save(name: String, fromMeters: Float, toMeters: Float) {
        if (saving) return
        saving = true
        viewModelScope.launch {
            savedSegmentId = createSegment(
                activityId = activityId,
                name = name,
                fromDistanceMeters = fromMeters.toDouble(),
                toDistanceMeters = toMeters.toDouble(),
            )
            saving = false
        }
    }

    fun consumeSaved() {
        savedSegmentId = null
    }
}
