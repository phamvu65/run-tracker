package com.example.runtracker.ui.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Một bài trong feed: buổi tập + polyline đã rút gọn để vẽ thumbnail. */
data class FeedItem(
    val activity: Activity,
    val routePoints: List<GeoPoint>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActivityListViewModel @Inject constructor(
    private val repository: ActivityRepository,
    userRepository: UserRepository,
) : ViewModel() {

    val athleteName: StateFlow<String> = userRepository.observeCurrentUser()
        .map { it?.displayName?.takeIf { name -> name.isNotBlank() } ?: "Bạn" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Bạn")

    val feed: StateFlow<List<FeedItem>> = repository.observeActivities(LOCAL_USER_ID)
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

    private companion object {
        const val MAX_THUMB_POINTS = 60

        fun decimate(points: List<GeoPoint>, max: Int): List<GeoPoint> {
            if (points.size <= max) return points
            val step = points.size.toDouble() / max
            return (0 until max).map { points[(it * step).toInt()] } + points.last()
        }
    }
}
