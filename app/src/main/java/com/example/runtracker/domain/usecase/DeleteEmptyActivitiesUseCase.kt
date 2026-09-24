package com.example.runtracker.domain.usecase

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Dưới mức này coi như buổi tập không có di chuyển thật — chỉ nhiễu GPS lúc đứng yên (haversine
 * giữa các fix nhiễu vẫn cộng dồn vài mét). Dùng quãng đường thay vì `movingTimeSeconds == 0`
 * (điều kiện chặn "Kết thúc" lúc đang ghi — xem `TrackingScreen`) vì ở đây ta xét activity ĐÃ
 * CHỐT xong: một buổi đi bộ thật nhưng rất chậm (dưới ngưỡng `MOVING_SPEED_MPS`) vẫn có thể có
 * `movingTimeSeconds == 0` dù quãng đường thật — quãng đường là tín hiệu an toàn hơn.
 */
const val MIN_REAL_ACTIVITY_DISTANCE_M = 10.0

fun Activity.isMeaningful(): Boolean = distanceMeters >= MIN_REAL_ACTIVITY_DISTANCE_M

/**
 * Dọn các activity không có di chuyển thật của user — chủ yếu là buổi "gián đoạn" (OS kill giữa
 * chừng) được chốt qua `TrackingViewModel.finalizeInterrupted` trước khi có đủ dữ liệu, hoặc buổi
 * cũ tạo ra trước khi có guard "chưa di chuyển" lúc bấm Kết thúc. An toàn gọi lại nhiều lần.
 */
class DeleteEmptyActivitiesUseCase @Inject constructor(
    private val activityRepository: ActivityRepository,
) {
    suspend operator fun invoke(userId: String) {
        val activities = activityRepository.observeActivities(userId).first()
        for (activity in activities) {
            if (!activity.isMeaningful()) activityRepository.deleteActivity(activity.id)
        }
    }
}
