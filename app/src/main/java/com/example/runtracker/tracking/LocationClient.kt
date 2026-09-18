package com.example.runtracker.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.runtracker.core.hasLocationPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/** Sự kiện từ nguồn vị trí: điểm fix mới, hoặc thay đổi khả năng cấp tín hiệu (mất/có lại GPS). */
sealed interface LocationEvent {
    data class Fix(val location: Location) : LocationEvent
    data class Availability(val available: Boolean) : LocationEvent
}

/** Nguồn cấp vị trí — tách interface để test / thay thế (mock GPS trong test). */
interface LocationClient {
    /** @throws SecurityException nếu chưa có quyền vị trí. */
    fun locationUpdates(intervalMillis: Long): Flow<LocationEvent>
}

class FusedLocationClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: FusedLocationProviderClient,
) : LocationClient {

    @SuppressLint("MissingPermission") // caller (Service) chỉ start khi đã có quyền
    override fun locationUpdates(intervalMillis: Long): Flow<LocationEvent> = callbackFlow {
        if (!context.hasLocationPermission()) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(LocationEvent.Fix(it)) }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                trySend(LocationEvent.Availability(availability.isLocationAvailable))
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }
}
