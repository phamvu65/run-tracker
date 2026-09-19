package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityRecord
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ActivityRecordRepository {
    /**
     * Tính kỷ lục toàn-buổi của [activity] cho cả 3 [com.example.runtracker.domain.training.ActivityRecordType],
     * xoá dòng cũ của activity này (idempotent khi chốt lại), xếp hạng so với lịch sử rồi lưu.
     */
    suspend fun recomputeForActivity(
        userId: String,
        activityId: String,
        activity: Activity,
        achievedAt: Instant,
    ): List<ActivityRecord>

    /** Tính lại TOÀN BỘ lịch sử — [activities] BẮT BUỘC theo thứ tự thời gian tăng dần. */
    suspend fun recomputeAllForUser(userId: String, activities: List<Activity>)

    /** Mọi kỷ lục toàn-buổi của user — dùng cho màn Thành tích. */
    fun observeAllForUser(userId: String): Flow<List<ActivityRecord>>
}
