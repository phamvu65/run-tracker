package com.example.runtracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Một "best effort" (thành tích chạy nhanh nhất liên tục đúng 1 cự ly chuẩn) của MỘT buổi tập, cho
 * MỘT cự ly ("ONE_K"/"FIVE_K"/"TEN_K"/"HALF_MARATHON" — xem `EffortDistance`). Một buổi tập có thể
 * có tới 4 dòng (một cho mỗi cự ly đạt được). [rankAtAchievement]/[improvedBySecondsAtAchievement]
 * chốt tại thời điểm chạy xong — KHÔNG tính lại nếu sau này có buổi khác vượt qua (giống cách
 * calories/TRIMP đã "đông cứng" tại thời điểm chốt buổi trong app này).
 */
@Entity(
    tableName = "best_efforts",
    indices = [Index("activityId"), Index("userId", "distance")],
)
data class BestEffortEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val activityId: String,
    val distance: String,          // EffortDistance.name: "ONE_K"/"FIVE_K"/"TEN_K"/"HALF_MARATHON"
    val elapsedSeconds: Long,
    val achievedAt: Long,          // = activity.startTime, dùng hiển thị/sắp xếp phụ khi bằng thời gian
    /** Thứ hạng (1 = nhanh nhất mọi thời đại) trong số TOÀN BỘ buổi tập đã có tại thời điểm chốt. */
    val rankAtAchievement: Int,
    /** Nhanh hơn buổi từng giữ đúng hạng này trước khi buổi này chen vào — null nếu là buổi đầu tiên đạt cự ly này. */
    val improvedBySecondsAtAchievement: Long?,
)
