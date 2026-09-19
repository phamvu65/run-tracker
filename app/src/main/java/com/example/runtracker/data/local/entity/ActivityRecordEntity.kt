package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Một kỷ lục toàn-buổi-tập (pace nhanh nhất / quãng đường dài nhất / độ cao lên nhiều nhất) của
 * MỘT buổi tập — cùng quy ước đông cứng rank tại thời điểm đạt được như [BestEffortEntity], khác
 * ở chỗ giá trị lấy trực tiếp từ field đã tổng hợp của activity, không cần tính cửa sổ trượt.
 */
@Entity(
    tableName = "activity_records",
    indices = [Index("activityId"), Index("userId", "recordType")],
)
data class ActivityRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val activityId: String,
    val recordType: String, // ActivityRecordType.name
    val value: Double,
    val achievedAt: Long,
    val rankAtAchievement: Int,
    val improvedByAtAchievement: Double?,
)
