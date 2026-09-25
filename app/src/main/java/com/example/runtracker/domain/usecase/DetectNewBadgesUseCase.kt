package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.BadgeType
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.BadgeRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Sau khi một activity được chốt số liệu: xét huy hiệu MỚI mở khoá tính tới buổi này (mốc quãng
 * đường, chuỗi ngày, marathon đầu tiên, chim sớm...).
 */
class DetectNewBadgesUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val badgeRepository: BadgeRepository,
) {
    suspend operator fun invoke(activityId: String): List<BadgeType> {
        val activity = activityRepository.getActivity(activityId) ?: return emptyList()
        val earlier = activityRepository.observeActivities(activity.userId).first()
            .filter { it.id != activity.id && it.startTime <= activity.startTime }
            .sortedBy { it.startTime }
        return badgeRepository.detectNew(activity.userId, earlier + activity)
    }
}
