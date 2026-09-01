package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.HeartRateSample
import kotlin.math.roundToInt

data class HeartRateSummary(val averageBpm: Int, val maxBpm: Int)

/** Trung bình + đỉnh nhịp tim từ danh sách mẫu; null nếu rỗng. */
fun List<HeartRateSample>.summary(): HeartRateSummary? {
    if (isEmpty()) return null
    val bpms = map { it.bpm }
    return HeartRateSummary(
        averageBpm = bpms.average().roundToInt(),
        maxBpm = bpms.max(),
    )
}
