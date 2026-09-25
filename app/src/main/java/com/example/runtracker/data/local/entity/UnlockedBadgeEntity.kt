package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 1 huy hiệu đã mở khoá — chỉ 1 dòng / (userId, badgeType), không đổi sau khi tạo. Unique index
 * làm lưới an toàn phụ (logic chính chống mở khoá trùng đã nằm ở `BadgeEvaluator`/use-case).
 */
@Entity(
    tableName = "unlocked_badges",
    indices = [Index("userId", "badgeType", unique = true), Index("activityId")],
)
data class UnlockedBadgeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val badgeType: String, // BadgeType.name
    val activityId: String,
    val unlockedAt: Long,
)
