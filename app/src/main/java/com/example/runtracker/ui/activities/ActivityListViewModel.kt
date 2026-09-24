package com.example.runtracker.ui.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.core.formatMinutesSeconds
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.BestEffort
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.BestEffortRepository
import com.example.runtracker.domain.repository.UserRepository
import com.example.runtracker.domain.usecase.DeleteEmptyActivitiesUseCase
import com.example.runtracker.domain.usecase.FetchActivityLocationUseCase
import com.example.runtracker.domain.usecase.RecomputeAllActivityRecordsUseCase
import com.example.runtracker.domain.usecase.RecomputeAllBestEffortsUseCase
import com.example.runtracker.ui.components.FeedAchievement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Một bài trong feed: buổi tập + polyline đã rút gọn để vẽ thumbnail + huy chương (nếu có). */
data class FeedItem(
    val activity: Activity,
    val routePoints: List<GeoPoint>,
    val achievement: FeedAchievement? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityListViewModel @Inject constructor(
    private val repository: ActivityRepository,
    private val bestEffortRepository: BestEffortRepository,
    private val fetchActivityLocation: FetchActivityLocationUseCase,
    private val recomputeAllBestEfforts: RecomputeAllBestEffortsUseCase,
    private val recomputeAllActivityRecords: RecomputeAllActivityRecordsUseCase,
    private val deleteEmptyActivities: DeleteEmptyActivitiesUseCase,
    userRepository: UserRepository,
) : ViewModel() {

    val athleteName: StateFlow<String> = userRepository.observeCurrentUser()
        .map { it?.displayName?.takeIf { name -> name.isNotBlank() } ?: "Bạn" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Bạn")

    private val rawFeed: StateFlow<List<FeedItem>> = repository.observeActivities(LOCAL_USER_ID)
        .flatMapLatest { activities ->
            flow {
                emit(activities.map { FeedItem(it, emptyList()) })
                emit(
                    activities.map { activity ->
                        val pts = repository.getRoutePoints(activity.id)
                            .map { GeoPoint(it.latitude, it.longitude) }
                        FeedItem(activity, decimate(pts, MAX_THUMB_POINTS))
                    },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val achievementsByActivity: StateFlow<Map<String, List<BestEffort>>> =
        bestEffortRepository.observeGroupedByActivity(LOCAL_USER_ID)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val feed: StateFlow<List<FeedItem>> = combine(rawFeed, achievementsByActivity) { items, achievements ->
        items.map { item -> item.copy(achievement = achievements[item.activity.id]?.toFeedAchievement()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Bổ sung dần tên khu vực cho các buổi còn thiếu — TUẦN TỰ có giãn cách để tôn trọng giới
        // hạn 1 request/giây của Nominatim (xem GeocodingApi), không gọi hàng loạt cùng lúc.
        val attempted = mutableSetOf<String>()
        repository.observeActivities(LOCAL_USER_ID)
            .onEach { activities ->
                for (activity in activities) {
                    if (activity.locationName != null || activity.id in attempted) continue
                    attempted += activity.id
                    fetchActivityLocation(activity.id)
                    delay(GEOCODE_THROTTLE_MS)
                }
            }
            .launchIn(viewModelScope)

        // Dọn activity không có di chuyển thật TRƯỚC, rồi mới hồi cứu huy chương/kỷ lục — tuần tự
        // trong CÙNG coroutine để tránh việc hồi cứu xử lý nhầm 1 activity vừa bị xoá (race nếu
        // chạy song song). Rẻ ở quy mô cá nhân nên chạy lại mỗi lần mở tab, không cần cờ "đã chạy".
        viewModelScope.launch {
            deleteEmptyActivities(LOCAL_USER_ID)
            recomputeAllBestEfforts(LOCAL_USER_ID)
            recomputeAllActivityRecords(LOCAL_USER_ID)
        }
    }

    private companion object {
        const val MAX_THUMB_POINTS = 60
        const val GEOCODE_THROTTLE_MS = 1_100L

        fun decimate(points: List<GeoPoint>, max: Int): List<GeoPoint> {
            if (points.size <= max) return points
            val step = points.size.toDouble() / max
            return (0 until max).map { points[(it * step).toInt()] } + points.last()
        }
    }
}

/**
 * Chọn thành tích ấn tượng nhất (hạng thấp nhất = tốt nhất) trong số các cự ly đạt top-3 mọi thời
 * đại; đếm tổng số cự ly đạt top-3 làm số huy chương. `null` nếu buổi này không có cự ly nào lọt top-3.
 */
private fun List<BestEffort>.toFeedAchievement(): FeedAchievement? {
    val medalWorthy = filter { it.rank <= 3 }
    if (medalWorthy.isEmpty()) return null
    val best = medalWorthy.minBy { it.rank }
    val emoji = when (best.rank) {
        1 -> "🥇"
        2 -> "🥈"
        else -> "🥉"
    }
    // Chữ rõ nghĩa thay vì chỉ 1 mũi tên + số giây trần trụi (user phản hồi không hiểu "▼ 20giây" là gì) —
    // đọc tiếp câu banner phía trên ("...nhanh thứ N của bạn!") nên hiểu là nhanh hơn buổi từng giữ đúng hạng N đó.
    val improvedText = best.improvedBySeconds
        ?.takeIf { it > 0 }
        ?.let { "Nhanh hơn ${formatMinutesSeconds(it)}" }
    return FeedAchievement(
        medalCount = medalWorthy.size,
        bestMedalEmoji = emoji,
        bannerText = "Thành tích chạy ${best.distance.label} nhanh thứ ${best.rank} của bạn!",
        improvedText = improvedText,
    )
}
