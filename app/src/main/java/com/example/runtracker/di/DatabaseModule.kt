package com.example.runtracker.di

import android.content.Context
import androidx.room.Room
import com.example.runtracker.data.local.ALL_MIGRATIONS
import com.example.runtracker.data.local.RunTrackerDatabase
import com.example.runtracker.data.local.dao.ActivityDao
import com.example.runtracker.data.local.dao.ActivityRecordDao
import com.example.runtracker.data.local.dao.BadgeDao
import com.example.runtracker.data.local.dao.BestEffortDao
import com.example.runtracker.data.local.dao.ChallengeDao
import com.example.runtracker.data.local.dao.DeviceConnectionDao
import com.example.runtracker.data.local.dao.PerformancePredictionDao
import com.example.runtracker.data.local.dao.RouteDao
import com.example.runtracker.data.local.dao.SegmentDao
import com.example.runtracker.data.local.dao.TrainingLoadDao
import com.example.runtracker.data.local.dao.UserDao
import com.example.runtracker.data.local.dao.UserZoneSettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RunTrackerDatabase =
        Room.databaseBuilder(context, RunTrackerDatabase::class.java, RunTrackerDatabase.DB_NAME)
            // Room tự bật PRAGMA foreign_keys = ON, nên CASCADE delete (RoutePoint/HeartRateSample
            // -> Activity, RouteWaypoint -> Route) hoạt động sẵn.
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides fun provideUserDao(db: RunTrackerDatabase): UserDao = db.userDao()

    @Provides fun provideActivityDao(db: RunTrackerDatabase): ActivityDao = db.activityDao()

    @Provides fun provideTrainingLoadDao(db: RunTrackerDatabase): TrainingLoadDao = db.trainingLoadDao()

    @Provides fun provideUserZoneSettingsDao(db: RunTrackerDatabase): UserZoneSettingsDao =
        db.userZoneSettingsDao()

    @Provides fun provideSegmentDao(db: RunTrackerDatabase): SegmentDao = db.segmentDao()

    @Provides fun provideRouteDao(db: RunTrackerDatabase): RouteDao = db.routeDao()

    @Provides fun providePerformancePredictionDao(db: RunTrackerDatabase): PerformancePredictionDao =
        db.performancePredictionDao()

    @Provides fun provideDeviceConnectionDao(db: RunTrackerDatabase): DeviceConnectionDao =
        db.deviceConnectionDao()

    @Provides fun provideChallengeDao(db: RunTrackerDatabase): ChallengeDao = db.challengeDao()

    @Provides fun provideBestEffortDao(db: RunTrackerDatabase): BestEffortDao = db.bestEffortDao()

    @Provides fun provideActivityRecordDao(db: RunTrackerDatabase): ActivityRecordDao = db.activityRecordDao()

    @Provides fun provideBadgeDao(db: RunTrackerDatabase): BadgeDao = db.badgeDao()
}
