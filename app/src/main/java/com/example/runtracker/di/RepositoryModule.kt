package com.example.runtracker.di

import com.example.runtracker.data.repository.ActivityRepositoryImpl
import com.example.runtracker.data.repository.ChallengeRepositoryImpl
import com.example.runtracker.data.repository.DirectionsRepositoryImpl
import com.example.runtracker.data.repository.PerformancePredictionRepositoryImpl
import com.example.runtracker.data.repository.RouteRepositoryImpl
import com.example.runtracker.data.repository.SegmentRepositoryImpl
import com.example.runtracker.data.repository.TrainingLoadRepositoryImpl
import com.example.runtracker.data.repository.UserRepositoryImpl
import com.example.runtracker.data.repository.ZoneSettingsRepositoryImpl
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.repository.ChallengeRepository
import com.example.runtracker.domain.repository.DirectionsRepository
import com.example.runtracker.domain.repository.PerformancePredictionRepository
import com.example.runtracker.domain.repository.RouteRepository
import com.example.runtracker.domain.repository.SegmentRepository
import com.example.runtracker.domain.repository.TrainingLoadRepository
import com.example.runtracker.domain.repository.UserRepository
import com.example.runtracker.domain.repository.ZoneSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindActivityRepository(impl: ActivityRepositoryImpl): ActivityRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindTrainingLoadRepository(impl: TrainingLoadRepositoryImpl): TrainingLoadRepository

    @Binds
    @Singleton
    abstract fun bindPerformancePredictionRepository(
        impl: PerformancePredictionRepositoryImpl,
    ): PerformancePredictionRepository

    @Binds
    @Singleton
    abstract fun bindZoneSettingsRepository(impl: ZoneSettingsRepositoryImpl): ZoneSettingsRepository

    @Binds
    @Singleton
    abstract fun bindSegmentRepository(impl: SegmentRepositoryImpl): SegmentRepository

    @Binds
    @Singleton
    abstract fun bindRouteRepository(impl: RouteRepositoryImpl): RouteRepository

    @Binds
    @Singleton
    abstract fun bindDirectionsRepository(impl: DirectionsRepositoryImpl): DirectionsRepository

    @Binds
    @Singleton
    abstract fun bindChallengeRepository(impl: ChallengeRepositoryImpl): ChallengeRepository
}
