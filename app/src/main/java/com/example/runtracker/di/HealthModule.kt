package com.example.runtracker.di

import com.example.runtracker.data.health.HealthConnectHeartRateSource
import com.example.runtracker.domain.health.HeartRateSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthModule {

    @Binds
    @Singleton
    abstract fun bindHeartRateSource(impl: HealthConnectHeartRateSource): HeartRateSource
}
