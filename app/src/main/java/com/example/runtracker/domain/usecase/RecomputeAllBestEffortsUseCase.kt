package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.BestEffortRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Tính lại "best effort" cho TOÀN BỘ lịch sử của user, theo đúng thứ tự thời gian — hồi cứu cho
 * các buổi tập đã có TRƯỚC khi tính năng huy chương ra đời (khác [DetectBestEffortsUseCase], chỉ
 * chạy cho 1 buổi mới chốt). Rẻ ở quy mô cá nhân (local-first, một người dùng — vài chục tới vài
 * trăm buổi) nên gọi lại an toàn mỗi khi mở tab Hoạt động thay vì cần cờ "đã hồi cứu" phức tạp.
 */
class RecomputeAllBestEffortsUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val bestEffortRepository: BestEffortRepository,
) {
    suspend operator fun invoke(userId: String) {
        val activities = activityRepository.observeActivities(userId).first().sortedBy { it.startTime }
        val withPoints = activities.map { it.id to activityRepository.getRoutePoints(it.id) }
        bestEffortRepository.recomputeAllForUser(
            userId = userId,
            activities = withPoints,
            achievedAtByActivity = activities.associate { it.id to it.startTime },
        )
    }
}
