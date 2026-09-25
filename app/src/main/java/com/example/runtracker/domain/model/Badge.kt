package com.example.runtracker.domain.model

import java.time.Instant

/** Bộ huy hiệu khởi điểm — mỗi loại chỉ mở khoá 1 lần, không đổi/nâng cấp sau đó. */
enum class BadgeType(val title: String, val description: String) {
    FIRST_ACTIVITY("Buổi đầu tiên", "Hoàn thành buổi tập đầu tiên"),
    DISTANCE_10_KM("10 km", "Tổng quãng đường đạt 10 km"),
    DISTANCE_50_KM("50 km", "Tổng quãng đường đạt 50 km"),
    DISTANCE_100_KM("100 km", "Tổng quãng đường đạt 100 km"),
    DISTANCE_500_KM("500 km", "Tổng quãng đường đạt 500 km"),
    DISTANCE_1000_KM("1.000 km", "Tổng quãng đường đạt 1.000 km"),
    STREAK_7("Chuỗi 7 ngày", "Tập luyện 7 ngày liên tiếp"),
    STREAK_30("Chuỗi 30 ngày", "Tập luyện 30 ngày liên tiếp"),
    FIRST_MARATHON("Marathon đầu tiên", "Hoàn thành 1 buổi từ 42,195 km trở lên"),
    EARLY_BIRD("Chim sớm", "Bắt đầu buổi tập trước 6 giờ sáng"),
}

/** 1 huy hiệu đã mở khoá — gắn với buổi tập đã kích hoạt nó, không đổi sau khi mở khoá. */
data class UnlockedBadge(
    val userId: String,
    val type: BadgeType,
    val activityId: String,
    val unlockedAt: Instant,
)
