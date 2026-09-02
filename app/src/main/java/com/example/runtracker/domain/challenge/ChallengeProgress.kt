package com.example.runtracker.domain.challenge

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ChallengeGoalType

/**
 * Tính tiến độ một thử thách từ danh sách activity đã lọc theo khoảng ngày của thử thách.
 * Thuần JVM, không phụ thuộc Android — để test được.
 */
object ChallengeProgress {

    /** @return tiến độ theo đơn vị gốc của [goalType] (mét / số buổi / giây). */
    fun compute(goalType: ChallengeGoalType, activities: List<Activity>): Double = when (goalType) {
        ChallengeGoalType.TOTAL_DISTANCE -> activities.sumOf { it.distanceMeters }
        ChallengeGoalType.TOTAL_ACTIVITIES -> activities.size.toDouble()
        ChallengeGoalType.TOTAL_ELEVATION -> activities.sumOf { it.elevationGainMeters }
        ChallengeGoalType.TOTAL_DURATION -> activities.sumOf { durationSecondsOf(it) }
    }

    private fun durationSecondsOf(activity: Activity): Double {
        val moving = activity.movingTime.inWholeSeconds
        return (if (moving > 0) moving else activity.duration.inWholeSeconds).toDouble()
    }
}
