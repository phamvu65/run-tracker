package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.GeocodingRepository
import javax.inject.Inject

/**
 * Lấy tên khu vực (reverse-geocode điểm xuất phát) cho một buổi tập và lưu vào activity.
 * Không ném lỗi — trả [Outcome] để nơi gọi quyết định (ví dụ bỏ qua và thử buổi khác).
 */
class FetchActivityLocationUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val geocodingRepository: GeocodingRepository,
) {
    enum class Outcome { FETCHED, ALREADY_PRESENT, NO_LOCATION, FAILED }

    suspend operator fun invoke(activityId: String): Outcome {
        val activity = activityRepository.getActivity(activityId) ?: return Outcome.FAILED
        if (activity.locationName != null) return Outcome.ALREADY_PRESENT

        val start = activityRepository.getRoutePoints(activityId).firstOrNull()
            ?: return Outcome.NO_LOCATION

        val name = geocodingRepository.reverseGeocode(start.latitude, start.longitude)
            .getOrElse { return Outcome.FAILED }

        activityRepository.upsertActivity(activity.copy(locationName = name))
        return Outcome.FETCHED
    }
}
