package com.example.runtracker.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Harness tạm để xác nhận Room + Hilt + ActivityRepository chạy được trên máy thật.
 * Xoá khi có UI tracking thật (Phase 1, việc 5).
 */
@HiltViewModel
class DebugViewModel @Inject constructor(
    private val repository: ActivityRepository,
) : ViewModel() {

    private val userId = "debug-user"

    val activities: StateFlow<List<Activity>> = repository.observeActivities(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var status by mutableStateOf<String?>(null)
        private set

    fun addFakeActivity() {
        viewModelScope.launch {
            val end = Instant.now()
            val start = end.minusSeconds(600)
            val id = UUID.randomUUID().toString()

            repository.upsertActivity(
                Activity(
                    id = id,
                    userId = userId,
                    type = ActivityType.RUNNING,
                    startTime = start,
                    endTime = end,
                    distanceMeters = 2000.0,
                    duration = 600.seconds,
                    movingTime = 570.seconds,
                    avgPaceSecPerKm = 300.0,
                    avgSpeedKmh = 12.0,
                    elevationGainMeters = 15.0,
                    elevationLossMeters = 12.0,
                    avgHeartRate = 150,
                    maxHeartRate = 172,
                    calories = 140,
                    avgCadence = 170,
                    perceivedExertion = 5,
                    weatherTempC = null,
                    gpxRawPath = null,
                ),
            )

            val points = buildList {
                var lat = 10.76260
                repeat(10) { i ->
                    add(
                        RoutePoint(
                            latitude = lat,
                            longitude = 106.66020,
                            altitude = 5.0,
                            speedMps = 3.0f,
                            accuracyMeters = 5f,
                            timestamp = start.plusSeconds(i.toLong()),
                        ),
                    )
                    lat += 0.00003 // ~3.3 m/s giữa 2 điểm
                }
                // 1 điểm nhiễu: nhảy ~15 km + accuracy 80m -> GpsTrackFilter phải loại
                add(
                    RoutePoint(
                        latitude = 10.90000,
                        longitude = 106.66020,
                        altitude = 5.0,
                        speedMps = null,
                        accuracyMeters = 80f,
                        timestamp = start.plusSeconds(11),
                    ),
                )
            }

            val kept = repository.appendRoutePoints(id, points)
            val detail = repository.getActivityDetail(id)
            status = "appendRoutePoints giữ $kept/${points.size} điểm · " +
                "getActivityDetail đọc lại ${detail?.routePoints?.size} điểm, " +
                "distance=${detail?.activity?.distanceMeters}"
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            activities.value.forEach { repository.deleteActivity(it.id) }
            status = "đã xoá ${activities.value.size} activity (route_points cascade)"
        }
    }
}
