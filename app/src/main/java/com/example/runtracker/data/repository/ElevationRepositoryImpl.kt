package com.example.runtracker.data.repository

import com.example.runtracker.data.remote.ElevationApi
import com.example.runtracker.di.IoDispatcher
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.repository.ElevationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ElevationRepositoryImpl @Inject constructor(
    private val api: ElevationApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ElevationRepository {

    override suspend fun elevationsFor(points: List<GeoPoint>): Result<List<Double>> = withContext(io) {
        try {
            // Open-Meteo giới hạn ~100 toạ độ/request — chia theo lô dù CorrectElevationUseCase
            // đã tự cắt còn tối đa ElevationCorrection.MAX_SAMPLE_POINTS, để repo tự đứng vững
            // nếu sau này có caller khác gửi nhiều hơn.
            val elevations = points.chunked(CHUNK_SIZE).flatMap { chunk ->
                val response = api.elevation(
                    latitude = chunk.joinToString(",") { it.latitude.toString() },
                    longitude = chunk.joinToString(",") { it.longitude.toString() },
                )
                check(response.elevation.size == chunk.size) { "elevation response size mismatch" }
                check(response.elevation.all { it.isFinite() }) { "invalid elevation value" }
                response.elevation
            }
            Result.success(elevations)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private companion object {
        const val CHUNK_SIZE = 100
    }
}
