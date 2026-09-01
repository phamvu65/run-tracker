package com.example.runtracker.domain.model

/**
 * Toàn bộ dữ liệu của một buổi tập: bản ghi tổng hợp + trace GPS + mẫu HR + lap.
 * Dùng cho màn hình chi tiết activity.
 */
data class ActivityDetail(
    val activity: Activity,
    val routePoints: List<RoutePoint>,
    val heartRateSamples: List<HeartRateSample>,
    val laps: List<ActivityLap>,
)
