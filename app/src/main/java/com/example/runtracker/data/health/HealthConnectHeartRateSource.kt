package com.example.runtracker.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.runtracker.domain.health.HeartRateSource
import com.example.runtracker.domain.model.HeartRateSample
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject

class HealthConnectHeartRateSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : HeartRateSource {

    private val readHeartRate = HealthPermission.getReadPermission(HeartRateRecord::class)

    private fun clientOrNull(): HealthConnectClient? =
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
        } else {
            null
        }

    override suspend fun isAvailable(): Boolean = clientOrNull() != null

    override fun requiredPermissions(): Set<String> = setOf(readHeartRate)

    override suspend fun hasPermission(): Boolean {
        val client = clientOrNull() ?: return false
        return runCatching {
            client.permissionController.getGrantedPermissions().contains(readHeartRate)
        }.getOrDefault(false)
    }

    override suspend fun samplesBetween(start: Instant, end: Instant): List<HeartRateSample> {
        val client = clientOrNull() ?: return emptyList()
        if (!hasPermission()) return emptyList()

        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        )
        return response.records
            .flatMap { record ->
                record.samples.map { HeartRateSample(bpm = it.beatsPerMinute.toInt(), timestamp = it.time) }
            }
            .filter { it.bpm in 20..250 }
            .sortedBy { it.timestamp }
    }
}
