package com.example.runtracker.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.example.runtracker.MainActivity
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.core.formatClock
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.ActivityType
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.tracking.GeoMath
import com.example.runtracker.domain.tracking.RunAggregator
import com.example.runtracker.domain.usecase.FinalizeActivityUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Foreground Service ghi GPS (Phase 1). Vòng đời điều khiển qua các action Intent:
 * START / PAUSE / RESUME / STOP.
 *
 * - Điểm GPS đi qua [ActivityRepository.appendRoutePoints] (đã lọc nhiễu) rồi mới cộng vào aggregate.
 * - Trạng thái live đẩy sang [TrackingSession] cho UI.
 * - Notification liên tục bắt buộc khi đang ghi (Android 8+); giữ partial wake lock để CPU
 *   không ngủ giữa các lần cập nhật vị trí.
 * - START_NOT_STICKY. Nếu OS kill: các điểm đã lưu vẫn còn trong DB và [TrackingStateStore]
 *   giữ id buổi đang chạy; lần mở app sau, UI phát hiện và cho chọn tiếp tục / kết thúc / xoá
 *   (action [ACTION_RESTORE]).
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var locationClient: LocationClient
    @Inject lateinit var repository: ActivityRepository
    @Inject lateinit var session: TrackingSession
    @Inject lateinit var stateStore: TrackingStateStore
    @Inject lateinit var finalizeActivityUseCase: FinalizeActivityUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var locationJob: Job? = null
    private var tickerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var activityId: String? = null
    private var startedAt: Instant? = null
    private var lastAccepted: RoutePoint? = null
    private var pausedAccumSeconds: Long = 0
    private var pausedAt: Instant? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_RESTORE -> restore(intent.getStringExtra(EXTRA_ACTIVITY_ID))
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_STOP -> stop()
            else -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        locationJob?.cancel()
        tickerJob?.cancel()
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    // ---- Điều khiển ----

    private fun start() {
        if (activityId != null) return

        val id = UUID.randomUUID().toString()
        val now = Instant.now()
        activityId = id
        startedAt = now
        lastAccepted = null
        pausedAccumSeconds = 0
        pausedAt = null
        stateStore.markActive(id)

        startAsForeground(TrackingStatus.TRACKING)
        acquireWakeLock()

        session.reset()
        session.update {
            it.copy(status = TrackingStatus.TRACKING, activityId = id, startedAt = now)
        }

        scope.launch { repository.upsertActivity(initialActivity(id, now)) }
        startLocationCollection()
        startTicker()
    }

    /** Khôi phục buổi tập bị gián đoạn: nạp lại aggregate từ trace đã lưu rồi ghi tiếp. */
    private fun restore(id: String?) {
        if (id == null) {
            stopSelf()
            return
        }
        if (activityId != null) return
        activityId = id
        startAsForeground(TrackingStatus.TRACKING)
        acquireWakeLock()

        scope.launch {
            val activity = repository.getActivity(id)
            if (activity == null) {
                stateStore.clear()
                activityId = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }
            val points = repository.getRoutePoints(id)
            val aggregate = RunAggregator.fromPoints(points)
            val last = points.lastOrNull()

            startedAt = activity.startTime
            lastAccepted = last
            pausedAccumSeconds = 0
            pausedAt = null

            session.reset()
            session.update {
                it.copy(
                    status = TrackingStatus.TRACKING,
                    activityId = id,
                    startedAt = activity.startTime,
                    distanceMeters = aggregate.distanceMeters,
                    movingTimeSeconds = aggregate.movingTimeSeconds,
                    elevationGainMeters = aggregate.elevationGainMeters,
                    elevationLossMeters = aggregate.elevationLossMeters,
                    pointCount = points.size,
                    lastLatitude = last?.latitude,
                    lastLongitude = last?.longitude,
                    lastUpdate = last?.timestamp,
                )
            }

            startLocationCollection()
            startTicker()
            updateNotification()
        }
    }

    private fun pause() {
        if (session.state.value.status != TrackingStatus.TRACKING) return
        pausedAt = Instant.now()
        locationJob?.cancel()
        locationJob = null
        session.update { it.copy(status = TrackingStatus.PAUSED) }
        updateNotification()
    }

    private fun resume() {
        if (session.state.value.status != TrackingStatus.PAUSED) return
        pausedAt?.let { pausedAccumSeconds += Duration.between(it, Instant.now()).seconds }
        pausedAt = null
        session.update { it.copy(status = TrackingStatus.TRACKING) }
        startLocationCollection()
        updateNotification()
    }

    private fun stop() {
        locationJob?.cancel()
        tickerJob?.cancel()
        releaseWakeLock()
        scope.launch {
            withContext(NonCancellable) { finalizeAndReset() }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // ---- Thu thập vị trí ----

    private fun startLocationCollection() {
        locationJob?.cancel()
        locationJob = scope.launch {
            locationClient.locationUpdates(LOCATION_INTERVAL_MS)
                .catch { e -> Log.w(TAG, "location updates stopped", e) }
                .collect { onLocation(it) }
        }
    }

    private suspend fun onLocation(location: Location) {
        val id = activityId ?: return
        val stored = repository.appendRoutePoints(id, listOf(location.toRoutePoint()))
        val accepted = stored.lastOrNull() ?: return

        val prev = lastAccepted
        if (prev != null) {
            val meters = GeoMath.distanceMeters(
                prev.latitude, prev.longitude, accepted.latitude, accepted.longitude,
            )
            val dtSeconds = (accepted.timestamp.toEpochMilli() - prev.timestamp.toEpochMilli()) / 1000.0
            val dAlt = accepted.altitude - prev.altitude
            val moving = dtSeconds > 0 && meters / dtSeconds >= RunAggregator.MOVING_SPEED_MPS

            session.update { s ->
                s.copy(
                    distanceMeters = s.distanceMeters + meters,
                    movingTimeSeconds = s.movingTimeSeconds +
                        if (moving) dtSeconds.roundToLong() else 0,
                    elevationGainMeters = s.elevationGainMeters +
                        if (dAlt > RunAggregator.ELEVATION_THRESHOLD_M) dAlt else 0.0,
                    elevationLossMeters = s.elevationLossMeters +
                        if (dAlt < -RunAggregator.ELEVATION_THRESHOLD_M) -dAlt else 0.0,
                    pointCount = s.pointCount + 1,
                    lastLatitude = accepted.latitude,
                    lastLongitude = accepted.longitude,
                    lastUpdate = accepted.timestamp,
                )
            }
        } else {
            session.update { s ->
                s.copy(
                    pointCount = s.pointCount + 1,
                    lastLatitude = accepted.latitude,
                    lastLongitude = accepted.longitude,
                    lastUpdate = accepted.timestamp,
                )
            }
        }
        lastAccepted = accepted
        updateNotification()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1_000)
                val start = startedAt ?: continue
                if (session.state.value.status != TrackingStatus.TRACKING) continue
                val elapsed = Duration.between(start, Instant.now()).seconds - pausedAccumSeconds
                session.update { it.copy(elapsedSeconds = elapsed.coerceAtLeast(0)) }
                updateNotification()
            }
        }
    }

    // ---- Ghi Activity ----

    private fun initialActivity(id: String, now: Instant) = Activity(
        id = id,
        userId = LOCAL_USER_ID,
        type = ActivityType.RUNNING,
        startTime = now,
        endTime = now,
        distanceMeters = 0.0,
        duration = kotlin.time.Duration.ZERO,
        movingTime = kotlin.time.Duration.ZERO,
        avgPaceSecPerKm = 0.0,
        avgSpeedKmh = 0.0,
        elevationGainMeters = 0.0,
        elevationLossMeters = 0.0,
        avgHeartRate = null,
        maxHeartRate = null,
        calories = null,
        avgCadence = null,
        perceivedExertion = null,
        weatherTempC = null,
        gpxRawPath = null,
    )

    private suspend fun finalizeAndReset() {
        val id = activityId ?: return
        // Tính lại aggregate + lap từ trace đã lưu (số liệu chuẩn, không lệ thuộc state bộ nhớ).
        finalizeActivityUseCase(id, endTime = Instant.now(), pausedSeconds = pausedAccumSeconds)
        stateStore.clear()

        session.reset()
        activityId = null
        startedAt = null
        lastAccepted = null
        pausedAccumSeconds = 0
        pausedAt = null
    }

    // ---- Notification ----

    private fun startAsForeground(status: TrackingStatus) {
        val notification = buildNotification(status)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun updateNotification() {
        if (activityId == null) return
        getSystemService<NotificationManager>()
            ?.notify(NOTIF_ID, buildNotification(session.state.value.status))
    }

    private fun buildNotification(status: TrackingStatus): Notification {
        val s = session.state.value
        val km = s.distanceMeters / 1000.0
        val title = when (status) {
            TrackingStatus.PAUSED -> "Đã tạm dừng"
            else -> "Đang ghi hoạt động"
        }
        val text = "%.2f km · %s".format(km, formatClock(s.elapsedSeconds))

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        when (status) {
            TrackingStatus.PAUSED -> builder.addAction(
                0, "Tiếp tục", servicePendingIntent(ACTION_RESUME, 1),
            )
            else -> builder.addAction(
                0, "Tạm dừng", servicePendingIntent(ACTION_PAUSE, 2),
            )
        }
        builder.addAction(0, "Kết thúc", servicePendingIntent(ACTION_STOP, 3))
        return builder.build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, LocationTrackingService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ghi hoạt động",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Thông báo liên tục khi đang ghi GPS" }
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    // ---- Wake lock ----

    private fun acquireWakeLock() {
        if (wakeLock != null) return
        wakeLock = getSystemService<PowerManager>()
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "runtracker:tracking")
            ?.apply { setReferenceCounted(false); acquire(MAX_WAKE_LOCK_MS) }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun Location.toRoutePoint() = RoutePoint(
        latitude = latitude,
        longitude = longitude,
        altitude = if (hasAltitude()) altitude else 0.0,
        speedMps = if (hasSpeed()) speed else null,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        timestamp = Instant.ofEpochMilli(time),
    )

    companion object {
        const val ACTION_START = "com.example.runtracker.tracking.START"
        const val ACTION_RESTORE = "com.example.runtracker.tracking.RESTORE"
        const val ACTION_PAUSE = "com.example.runtracker.tracking.PAUSE"
        const val ACTION_RESUME = "com.example.runtracker.tracking.RESUME"
        const val ACTION_STOP = "com.example.runtracker.tracking.STOP"

        private const val EXTRA_ACTIVITY_ID = "activityId"
        private const val TAG = "LocationTrackingService"
        private const val CHANNEL_ID = "tracking"
        private const val NOTIF_ID = 1001
        private const val LOCATION_INTERVAL_MS = 3_000L
        private const val MAX_WAKE_LOCK_MS = 6L * 60 * 60 * 1000 // 6h an toàn

        fun start(context: Context) = send(context, ACTION_START, foreground = true)
        fun pause(context: Context) = send(context, ACTION_PAUSE, foreground = false)
        fun resume(context: Context) = send(context, ACTION_RESUME, foreground = false)
        fun stop(context: Context) = send(context, ACTION_STOP, foreground = false)

        fun restore(context: Context, activityId: String) {
            val intent = Intent(context, LocationTrackingService::class.java)
                .setAction(ACTION_RESTORE)
                .putExtra(EXTRA_ACTIVITY_ID, activityId)
            context.startForegroundService(intent)
        }

        private fun send(context: Context, action: String, foreground: Boolean) {
            val intent = Intent(context, LocationTrackingService::class.java).setAction(action)
            if (foreground) context.startForegroundService(intent) else context.startService(intent)
        }
    }
}
