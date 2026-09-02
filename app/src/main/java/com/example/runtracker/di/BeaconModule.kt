package com.example.runtracker.di

import com.example.runtracker.data.beacon.LoopbackLiveLocationTransport
import com.example.runtracker.domain.beacon.LiveLocationTransport
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BeaconModule {

    /** Đổi sang impl mạng (Firebase/Ktor) khi chốt backend. */
    @Binds
    @Singleton
    abstract fun bindLiveLocationTransport(
        impl: LoopbackLiveLocationTransport,
    ): LiveLocationTransport
}
