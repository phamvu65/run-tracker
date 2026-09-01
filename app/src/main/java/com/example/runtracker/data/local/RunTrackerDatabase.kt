package com.example.runtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.runtracker.data.local.dao.ActivityDao
import com.example.runtracker.data.local.dao.ChallengeDao
import com.example.runtracker.data.local.dao.DeviceConnectionDao
import com.example.runtracker.data.local.dao.PerformancePredictionDao
import com.example.runtracker.data.local.dao.RouteDao
import com.example.runtracker.data.local.dao.SegmentDao
import com.example.runtracker.data.local.dao.TrainingLoadDao
import com.example.runtracker.data.local.dao.UserDao
import com.example.runtracker.data.local.dao.UserZoneSettingsDao
import com.example.runtracker.data.local.entity.ActivityEntity
import com.example.runtracker.data.local.entity.ActivityLapEntity
import com.example.runtracker.data.local.entity.ChallengeEntity
import com.example.runtracker.data.local.entity.ChallengeParticipantEntity
import com.example.runtracker.data.local.entity.DailyTrainingLoadEntity
import com.example.runtracker.data.local.entity.DeviceConnectionEntity
import com.example.runtracker.data.local.entity.FitnessFreshnessSnapshotEntity
import com.example.runtracker.data.local.entity.HeartRateSampleEntity
import com.example.runtracker.data.local.entity.PerformancePredictionEntity
import com.example.runtracker.data.local.entity.RouteEntity
import com.example.runtracker.data.local.entity.RoutePointEntity
import com.example.runtracker.data.local.entity.RouteWaypointEntity
import com.example.runtracker.data.local.entity.SegmentEffortEntity
import com.example.runtracker.data.local.entity.SegmentEntity
import com.example.runtracker.data.local.entity.UserEntity
import com.example.runtracker.data.local.entity.UserZoneSettingsEntity

/**
 * Room DB local-first. Toàn bộ schema (cả Phase 2/3) tạo sẵn từ đầu để không phải migrate lớn về sau —
 * xem `docs/database_design.md`.
 *
 * Khi đổi bất kỳ entity nào: tăng [DB_VERSION] và thêm Migration (schema JSON được xuất ra app/schemas/).
 */
@Database(
    entities = [
        // Phase 1 — core tracking
        UserEntity::class,
        ActivityEntity::class,
        RoutePointEntity::class,
        HeartRateSampleEntity::class,
        ActivityLapEntity::class,
        // Phase 2 — "premium" làm free
        UserZoneSettingsEntity::class,
        DailyTrainingLoadEntity::class,
        FitnessFreshnessSnapshotEntity::class,
        SegmentEntity::class,
        SegmentEffortEntity::class,
        RouteEntity::class,
        RouteWaypointEntity::class,
        PerformancePredictionEntity::class,
        DeviceConnectionEntity::class,
        // Phase 3 — khung sẵn
        ChallengeEntity::class,
        ChallengeParticipantEntity::class,
    ],
    version = RunTrackerDatabase.DB_VERSION,
    exportSchema = true
)
abstract class RunTrackerDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun activityDao(): ActivityDao
    abstract fun trainingLoadDao(): TrainingLoadDao
    abstract fun userZoneSettingsDao(): UserZoneSettingsDao
    abstract fun segmentDao(): SegmentDao
    abstract fun routeDao(): RouteDao
    abstract fun performancePredictionDao(): PerformancePredictionDao
    abstract fun deviceConnectionDao(): DeviceConnectionDao
    abstract fun challengeDao(): ChallengeDao

    companion object {
        const val DB_VERSION = 1
        const val DB_NAME = "runtracker.db"
    }
}
