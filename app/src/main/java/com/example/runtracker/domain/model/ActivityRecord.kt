package com.example.runtracker.domain.model

import com.example.runtracker.domain.training.ActivityRecordType
import java.time.Instant

/**
 * Một kỷ lục toàn-buổi-tập (pace/quãng đường/độ cao) đã xếp hạng so với lịch sử — cùng quy ước với
 * [BestEffort]: [rank]/[improvedBy] ĐÔNG CỨNG tại [achievedAt], kỷ lục HIỆN TẠI = giá trị tốt nhất
 * (nhỏ nhất hoặc lớn nhất tuỳ [ActivityRecordType.higherIsBetter]) trong toàn bộ lịch sử, không
 * phải dòng có `rank == 1`.
 */
data class ActivityRecord(
    val activityId: String,
    val type: ActivityRecordType,
    val value: Double,
    val achievedAt: Instant,
    val rank: Int,
    val improvedBy: Double?,
)
