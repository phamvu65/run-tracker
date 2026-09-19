package com.example.runtracker.domain.model

import com.example.runtracker.domain.training.EffortDistance
import java.time.Instant

/**
 * Một "best effort" đã xếp hạng so với lịch sử: [rank] 1 = nhanh nhất mọi thời đại của user cho
 * [distance] này TẠI THỜI ĐIỂM [achievedAt] (chốt lúc buổi tập kết thúc, không tính lại về sau —
 * xem [com.example.runtracker.data.local.entity.BestEffortEntity]). Để biết kỷ lục HIỆN TẠI của
 * một cự ly, lấy `elapsedSeconds` nhỏ nhất trong số mọi [BestEffort] cùng [distance], KHÔNG dựa
 * vào `rank == 1` (rank đã đông cứng, không tự cập nhật khi có buổi khác vượt qua sau này).
 */
data class BestEffort(
    val activityId: String,
    val distance: EffortDistance,
    val elapsedSeconds: Long,
    val achievedAt: Instant,
    val rank: Int,
    /** Nhanh hơn buổi từng giữ đúng hạng này trước khi buổi này chen vào — null nếu chưa từng có. */
    val improvedBySeconds: Long?,
)
