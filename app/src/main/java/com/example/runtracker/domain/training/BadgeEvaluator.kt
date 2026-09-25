package com.example.runtracker.domain.training

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.BadgeType
import java.time.ZoneId

/**
 * Xét huy hiệu MỚI mở khoá tại buổi tập CUỐI CÙNG trong [activitiesUpToNow] — dùng cho cả (a) dò
 * ngay sau khi chốt 1 buổi mới (truyền toàn bộ lịch sử kết thúc đúng buổi đó) và (b) hồi cứu (gọi
 * lặp lại cho từng tiền tố lịch sử theo thứ tự thời gian tăng dần, xem `RecomputeAllBadgesUseCase`
 * — cách này tự nhiên tránh mở khoá lại huy hiệu cũ vì [alreadyUnlocked] được caller cập nhật dần).
 * Thuần JVM, có test.
 */
object BadgeEvaluator {

    private const val MARATHON_METERS = 42_195.0
    private const val EARLY_BIRD_HOUR = 6
    private val DISTANCE_MILESTONES = listOf(
        10_000.0 to BadgeType.DISTANCE_10_KM,
        50_000.0 to BadgeType.DISTANCE_50_KM,
        100_000.0 to BadgeType.DISTANCE_100_KM,
        500_000.0 to BadgeType.DISTANCE_500_KM,
        1_000_000.0 to BadgeType.DISTANCE_1000_KM,
    )

    fun evaluateForLatest(
        activitiesUpToNow: List<Activity>,
        alreadyUnlocked: Set<BadgeType>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<BadgeType> {
        val latest = activitiesUpToNow.lastOrNull() ?: return emptyList()
        val newly = mutableListOf<BadgeType>()
        fun unlock(type: BadgeType) {
            if (type !in alreadyUnlocked) newly += type
        }

        if (activitiesUpToNow.size == 1) unlock(BadgeType.FIRST_ACTIVITY)

        val totalDistance = activitiesUpToNow.sumOf { it.distanceMeters }
        DISTANCE_MILESTONES.forEach { (threshold, type) -> if (totalDistance >= threshold) unlock(type) }

        if (latest.distanceMeters >= MARATHON_METERS) unlock(BadgeType.FIRST_MARATHON)

        if (latest.startTime.atZone(zoneId).hour < EARLY_BIRD_HOUR) unlock(BadgeType.EARLY_BIRD)

        val streak = currentStreakDays(activitiesUpToNow, zoneId)
        if (streak >= 7) unlock(BadgeType.STREAK_7)
        if (streak >= 30) unlock(BadgeType.STREAK_30)

        return newly
    }

    /** Số ngày liên tiếp CÓ buổi tập, tính lùi từ ngày của activity cuối cùng trong danh sách. */
    private fun currentStreakDays(activities: List<Activity>, zoneId: ZoneId): Int {
        val days = activities.map { it.startTime.atZone(zoneId).toLocalDate() }.toSortedSet()
        if (days.isEmpty()) return 0
        var streak = 1
        var cursor = days.last()
        while (days.contains(cursor.minusDays(1))) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
