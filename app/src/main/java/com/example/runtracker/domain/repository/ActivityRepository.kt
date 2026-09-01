package com.example.runtracker.domain.repository

import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityDetail
import com.example.runtracker.domain.model.ActivityLap
import com.example.runtracker.domain.model.HeartRateSample
import com.example.runtracker.domain.model.RoutePoint
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Truy cập dữ liệu buổi tập. Phase 1: ghi/đọc local (Room). Đồng bộ server sẽ thêm sau
 * dựa trên cờ isSynced ở tầng entity.
 */
interface ActivityRepository {

    fun observeActivities(userId: String): Flow<List<Activity>>

    fun observeActivity(activityId: String): Flow<Activity?>

    fun observeRoutePoints(activityId: String): Flow<List<RoutePoint>>

    fun observeLaps(activityId: String): Flow<List<ActivityLap>>

    suspend fun getActivity(activityId: String): Activity?

    suspend fun getRoutePoints(activityId: String): List<RoutePoint>

    suspend fun getActivityDetail(activityId: String): ActivityDetail?

    /** Dùng cho tính TRIMP / training load theo khoảng ngày (Phase 2). */
    suspend fun getActivitiesBetween(userId: String, from: Instant, to: Instant): List<Activity>

    suspend fun getUnsyncedActivities(): List<Activity>

    /** Tạo mới hoặc cập nhật; luôn đánh dấu chưa đồng bộ. */
    suspend fun upsertActivity(activity: Activity)

    /**
     * Nối thêm điểm GPS trong lúc tracking. Điểm được lọc nhiễu trước khi lưu.
     * @return các điểm thực sự được lưu (đã qua lọc), theo thứ tự thời gian; rỗng nếu bị loại hết.
     */
    suspend fun appendRoutePoints(activityId: String, points: List<RoutePoint>): List<RoutePoint>

    suspend fun appendHeartRateSamples(activityId: String, samples: List<HeartRateSample>)

    suspend fun replaceLaps(activityId: String, laps: List<ActivityLap>)

    suspend fun deleteActivity(activityId: String)
}
