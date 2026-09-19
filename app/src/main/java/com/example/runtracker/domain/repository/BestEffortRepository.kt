package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.BestEffort
import com.example.runtracker.domain.model.RoutePoint
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface BestEffortRepository {
    /**
     * Tính best effort từ trace GPS, xoá bản ghi cũ của activity này (idempotent khi chốt lại),
     * xếp hạng so với lịch sử rồi lưu. Trả về các mốc đạt được trong buổi này (rỗng nếu buổi quá
     * ngắn để đạt bất kỳ cự ly chuẩn nào).
     */
    suspend fun recomputeForActivity(
        userId: String,
        activityId: String,
        points: List<RoutePoint>,
        achievedAt: Instant,
    ): List<BestEffort>

    /**
     * Tính lại TOÀN BỘ lịch sử best effort của user từ đầu — xoá sạch rồi xử lý từng buổi theo
     * đúng [activities] (BẮT BUỘC theo thứ tự thời gian tăng dần để hạng/mức cải thiện phản ánh
     * đúng diễn biến lịch sử). Dùng để hồi cứu cho buổi tập đã có TRƯỚC khi tính năng này ra đời —
     * xem nơi gọi ([com.example.runtracker.domain.usecase.RecomputeAllBestEffortsUseCase]).
     */
    suspend fun recomputeAllForUser(userId: String, activities: List<Pair<String, List<RoutePoint>>>, achievedAtByActivity: Map<String, Instant>)

    /** Mọi best effort của user, gộp theo activityId — dùng hiển thị huy chương trên feed hoạt động. */
    fun observeGroupedByActivity(userId: String): Flow<Map<String, List<BestEffort>>>

    /** Mọi best effort của user, KHÔNG gộp — dùng cho màn Thành tích (kỷ lục hiện tại + lịch sử). */
    fun observeAllForUser(userId: String): Flow<List<BestEffort>>
}
