package com.example.runtracker.di

import com.example.runtracker.data.sensor.AndroidBarometerSource
import com.example.runtracker.domain.health.BarometerSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SensorModule {

    @Binds
    @Singleton
    abstract fun bindBarometerSource(impl: AndroidBarometerSource): BarometerSource
}
