package com.example.runtracker.tracking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.example.runtracker.core.hasLocationPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/** Điểm vị trí mới từ nguồn GPS. */
sealed interface LocationEvent {
    data class Fix(val location: Location) : LocationEvent
}

/** Nguồn cấp vị trí — tách interface để test / thay thế (mock GPS trong test). */
interface LocationClient {
    /** Completing [stopInput] closes the source while allowing buffered fixes to drain. */
    fun locationUpdates(intervalMillis: Long, stopInput: Job): Flow<LocationEvent>
}

class FusedLocationClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: FusedLocationProviderClient,
) : LocationClient {

    @SuppressLint("MissingPermission") // caller (Service) chỉ start khi đã có quyền
    override fun locationUpdates(intervalMillis: Long, stopInput: Job): Flow<LocationEvent> = callbackFlow {
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
                for (location in result.locations) {
                    val sent = trySend(LocationEvent.Fix(location))
                    if (sent.isFailure && !sent.isClosed) Log.w("FusedLocationClient", "GPS buffer full")
                }
            }
        }

        val stopHandle = stopInput.invokeOnCompletion { close() }
        try {
            if (!stopInput.isCompleted) {
                client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                    .addOnFailureListener { close(it) }
            }
            awaitClose()
        } finally {
            stopHandle.dispose()
            client.removeLocationUpdates(callback)
        }
    }
}
