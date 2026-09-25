package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.BadgeRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Tính lại huy hiệu cho TOÀN BỘ lịch sử của user, theo đúng thứ tự thời gian — hồi cứu cho các
 * buổi tập đã có TRƯỚC khi tính năng huy hiệu ra đời (khác [DetectNewBadgesUseCase], chỉ chạy cho
 * 1 buổi mới chốt). Cùng cách [RecomputeAllBestEffortsUseCase] đã làm.
 */
class RecomputeAllBadgesUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val badgeRepository: BadgeRepository,
) {
    suspend operator fun invoke(userId: String) {
        val activities = activityRepository.observeActivities(userId).first().sortedBy { it.startTime }
        badgeRepository.recomputeAllForUser(userId, activities)
    }
}
