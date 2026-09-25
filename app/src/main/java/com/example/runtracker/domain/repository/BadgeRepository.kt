package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.BadgeType
import com.example.runtracker.domain.model.UnlockedBadge
import kotlinx.coroutines.flow.Flow

interface BadgeRepository {
    /** Xét + lưu huy hiệu MỚI mở khoá tại buổi tập CUỐI CÙNG trong [activitiesUpToNow]. */
    suspend fun detectNew(userId: String, activitiesUpToNow: List<Activity>): List<BadgeType>

    /** Tính lại TOÀN BỘ lịch sử — [activities] BẮT BUỘC theo thứ tự thời gian tăng dần. */
    suspend fun recomputeAllForUser(userId: String, activities: List<Activity>)

    fun observeForUser(userId: String): Flow<List<UnlockedBadge>>
}
