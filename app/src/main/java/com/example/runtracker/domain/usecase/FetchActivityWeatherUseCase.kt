package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.WeatherRepository
import javax.inject.Inject

/**
 * Lấy thời tiết cho một buổi tập (điểm xuất phát + thời điểm bắt đầu) và lưu vào activity.
 * Không ném lỗi — trả [Outcome] để UI hiển thị.
 */
class FetchActivityWeatherUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val weatherRepository: WeatherRepository,
) {
    enum class Outcome { FETCHED, ALREADY_PRESENT, NO_LOCATION, FAILED }

    suspend operator fun invoke(activityId: String, force: Boolean = false): Outcome {
        val activity = activityRepository.getActivity(activityId) ?: return Outcome.FAILED
        if (!force && activity.weather != null) return Outcome.ALREADY_PRESENT

        val start = activityRepository.getRoutePoints(activityId).firstOrNull()
            ?: return Outcome.NO_LOCATION

        val weather = weatherRepository
            .fetch(start.latitude, start.longitude, activity.startTime)
            .getOrElse { return Outcome.FAILED }

        activityRepository.upsertActivity(activity.copy(weather = weather))
        return Outcome.FETCHED
    }
}
