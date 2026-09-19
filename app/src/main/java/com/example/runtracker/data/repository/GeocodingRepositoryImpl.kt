package com.example.runtracker.data.repository

import com.example.runtracker.data.remote.GeocodingApi
import com.example.runtracker.data.remote.toLocationName
import com.example.runtracker.di.IoDispatcher
import com.example.runtracker.domain.repository.GeocodingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocodingRepositoryImpl @Inject constructor(
    private val api: GeocodingApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) : GeocodingRepository {

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<String> =
        withContext(io) {
            runCatching {
                api.reverse(latitude, longitude).toLocationName()
                    ?: error("Không xác định được khu vực")
            }
        }
}
