package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.repository.ActivityRecordRepository
import com.example.runtracker.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Tính lại kỷ lục pace/quãng đường/độ cao cho TOÀN BỘ lịch sử của user, theo đúng thứ tự thời
 * gian — hồi cứu cho buổi tập có TRƯỚC khi tính năng ra đời, cùng cách [RecomputeAllBestEffortsUseCase] làm.
 */
class RecomputeAllActivityRecordsUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val activityRecordRepository: ActivityRecordRepository,
) {
    suspend operator fun invoke(userId: String) {
        val activities = activityRepository.observeActivities(userId).first().sortedBy { it.startTime }
        activityRecordRepository.recomputeAllForUser(userId, activities)
    }
}
