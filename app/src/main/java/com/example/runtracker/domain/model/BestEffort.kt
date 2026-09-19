package com.example.runtracker.domain.model

import com.example.runtracker.domain.training.EffortDistance

/**
 * Một "best effort" đã xếp hạng so với lịch sử: [rank] 1 = nhanh nhất mọi thời đại của user cho
 * [distance] này. Chốt tại thời điểm buổi tập kết thúc — xem [com.example.runtracker.data.local.entity.BestEffortEntity].
 */
data class BestEffort(
    val distance: EffortDistance,
    val elapsedSeconds: Long,
    val rank: Int,
    /** Nhanh hơn buổi từng giữ đúng hạng này trước khi buổi này chen vào — null nếu chưa từng có. */
    val improvedBySeconds: Long?,
)
