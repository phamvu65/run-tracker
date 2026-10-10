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
import com.example.runtracker.core.FeatureFlags
import com.example.runtracker.MainActivity
import com.example.runtracker.core.LOCAL_USER_ID
import com.example.runtracker.core.formatClock
import com.example.runtracker.data.health.BleHeartRateStore
import com.example.runtracker.data.settings.AppSettingsStore
import com.example.runtracker.domain.beacon.LiveLocationTransport
import com.example.runtracker.domain.health.BarometerSource
import com.example.runtracker.domain.health.LiveHeartRateSource
import com.example.runtracker.domain.health.StepCounterSource
import com.example.runtracker.domain.model.Activity
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.domain.model.HeartRateSample
import com.example.runtracker.domain.model.LiveLocationUpdate
import com.example.runtracker.domain.model.RoutePoint
import com.example.runtracker.domain.navigation.NavigationSession
import com.example.runtracker.domain.repository.ActivityRepository
import com.example.runtracker.domain.tracking.AltitudeEstimator
import com.example.runtracker.domain.tracking.GeoMath
import com.example.runtracker.domain.tracking.RunAggregator
import com.example.runtracker.domain.tracking.ElevationAccumulator
import com.example.runtracker.domain.usecase.BuildRouteUseCase
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
import kotlin.math.roundToInt
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
    @Inject lateinit var liveHeartRateSource: LiveHeartRateSource
    @Inject lateinit var bleHeartRateStore: BleHeartRateStore
    @Inject lateinit var beaconController: BeaconController
    @Inject lateinit var liveLocationTransport: LiveLocationTransport
    @Inject lateinit var barometerSource: BarometerSource
    @Inject lateinit var stepCounterSource: StepCounterSource
    @Inject lateinit var settingsStore: AppSettingsStore
    @Inject lateinit var buildRouteUseCase: BuildRouteUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var locationJob: Job? = null
    private var tickerJob: Job? = null
    private var heartRateJob: Job? = null
    private var beaconJob: Job? = null
    private var barometerJob: Job? = null
    private var stepJob: Job? = null
    private var broadcastingCode: String? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val heartRateBuffer = mutableListOf<HeartRateSample>()

    private var activityId: String? = null
    private var startedAt: Instant? = null
    private var lastAccepted: RoutePoint? = null
    private var elevationAccumulator = ElevationAccumulator()
    private var pausedAccumSeconds: Long = 0
    private var pausedAt: Instant? = null

    // Mốc thời gian nhận fix GPS gần nhất — dùng để phát hiện mất tín hiệu giữa buổi
    // (khác lastAccepted.timestamp vì đó là giờ GPS, có thể lệch giờ hệ thống).
    private var lastFixAt: Instant? = null

    // Mốc lần cuối phát hiện đang di chuyển thật (tốc độ tức thời ≥ ngưỡng) — dùng cho auto-pause.
    private var lastMovingAt: Instant? = null

    private var altitudeEstimator = AltitudeEstimator()
    private var altitudeReading: AltitudeEstimator.Reading? = null
    private var allRecordedAltitudesBarometric = true
    private var previousAltitudeSource: Boolean? = null

    // Số bước tích luỹ từ cảm biến phần cứng — chỉ cộng dồn khi đang TRACKING (không tính lúc tạm dừng).
    private var lastStepCumulative: Int? = null
    private var stepsAccumulated: Int = 0

    // Nhịp bước/phút live — cửa sổ trượt (thời điểm, tổng bước tích luỹ) để tính tốc độ bước gần đây.
    private val cadenceWindow = ArrayDeque<Pair<Instant, Int>>()
    private var cadenceSum: Long = 0
    private var cadenceSampleCount: Int = 0

    // Điều hướng turn-by-turn (rỗng nếu không theo route)
    private val navigation = NavigationSession()
    private val navigationLock = Any()
    private var navigationEnabled = false
    private var rerouteJob: Job? = null

    // Rerouting: mốc bắt đầu lệch tuyến liên tục + lần gọi Directions gần nhất (debounce).
    private var offRouteSince: Instant? = null
    private var lastRerouteAttemptAt: Instant? = null

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
            ACTION_DISCARD -> discard()
            else -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopNavigation()
        locationJob?.cancel()
        tickerJob?.cancel()
        heartRateJob?.cancel()
        beaconJob?.cancel()
        barometerJob?.cancel()
        stepJob?.cancel()
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
        lastFixAt = now
        lastMovingAt = now
        lastStepCumulative = null
        stepsAccumulated = 0
        cadenceWindow.clear()
        cadenceSum = 0
        cadenceSampleCount = 0
        stateStore.markActive(id)

        startAsForeground(TrackingStatus.TRACKING)
        acquireWakeLock()

        val route = session.selectedRoute.value?.takeIf { it.snappedToRoads && it.travelMode != null }
        synchronized(navigationLock) {
            rerouteJob?.cancel()
            rerouteJob = null
            navigation.start(route)
            navigationEnabled = route != null
            offRouteSince = null
            lastRerouteAttemptAt = null
        }

        session.reset()
        session.update {
            it.copy(
                status = TrackingStatus.TRACKING,
                activityId = id,
                startedAt = now,
                navRouteName = route?.name,
                navStepCount = route?.waypoints?.size ?: 0,
                navPolyline = route?.polyline.orEmpty(),
            )
        }

        scope.launch { repository.upsertActivity(initialActivity(id, now)) }
        startLocationCollection()
        startTicker()
        startHeartRateCollection(id)
        startBeaconCollection()
        startBarometerCollection()
        startStepCollection()
    }

    /**
     * Đếm bước chân bằng cảm biến phần cứng (`TYPE_STEP_COUNTER`, tích luỹ từ khi máy khởi động).
     * Job này KHÔNG bị huỷ lúc tạm dừng (khác [locationJob]) — cần giữ chạy để [lastStepCumulative]
     * luôn cập nhật, nếu không bước đi trong lúc tạm dừng sẽ bị cộng nhầm vào lúc tiếp tục ghi.
     * Chỉ cộng dồn vào [stepsAccumulated] khi trạng thái đang TRACKING.
     */
    private fun startStepCollection() {
        stepJob?.cancel()
        if (!stepCounterSource.isSupported() || !stepCounterSource.hasPermission()) return
        stepJob = scope.launch {
            stepCounterSource.stepCountUpdates()
                .catch { e -> Log.w(TAG, "step counter stream error", e) }
                .collect { total ->
                    val prev = lastStepCumulative
                    lastStepCumulative = total
                    if (session.state.value.status != TrackingStatus.TRACKING) {
                        // Đứng/tạm dừng: bỏ cửa sổ cadence cũ (khoảng lặng sẽ làm sai tốc độ bước
                        // tính tiếp) và ẩn số cadence live, KHÔNG cộng dồn bước.
                        cadenceWindow.clear()
                        if (session.state.value.liveCadenceSpm != null) {
                            session.update { it.copy(liveCadenceSpm = null) }
                        }
                        return@collect
                    }
                    if (prev != null) stepsAccumulated += (total - prev).coerceAtLeast(0)
                    updateCadence(total)
                }
        }
    }

    /** Nhịp bước/phút gần đây, từ cửa sổ trượt [CADENCE_WINDOW_SECONDS]. */
    private fun updateCadence(totalSteps: Int) {
        val now = Instant.now()
        cadenceWindow.addLast(now to totalSteps)
        val cutoff = now.minusSeconds(CADENCE_WINDOW_SECONDS)
        while (cadenceWindow.size > 1 && cadenceWindow.first().first.isBefore(cutoff)) {
            cadenceWindow.removeFirst()
        }
        val oldest = cadenceWindow.first()
        val elapsedSeconds = Duration.between(oldest.first, now).seconds
        if (elapsedSeconds < MIN_CADENCE_WINDOW_SECONDS) return

        val stepsInWindow = totalSteps - oldest.second
        val cadence = (stepsInWindow * 60.0 / elapsedSeconds).roundToInt().coerceAtLeast(0)
        cadenceSum += cadence
        cadenceSampleCount++
        session.update { it.copy(liveCadenceSpm = cadence) }
    }

    /**
     * Đọc khí áp liên tục trong suốt buổi tập (kể cả lúc tạm dừng, để hiệu chỉnh không bị gián
     * đoạn). Không phải máy nào cũng có cảm biến này — [onLocation] tự fallback GPS altitude
     * khi chưa hiệu chỉnh được.
     */
    private fun startBarometerCollection() {
        barometerJob?.cancel()
        if (!barometerSource.isSupported()) return
        barometerJob = scope.launch {
            barometerSource.pressureUpdates()
                .catch { e -> Log.w(TAG, "barometer stream error", e) }
                .collect { altitudeEstimator.updatePressure(it, android.os.SystemClock.elapsedRealtime()) }
        }
    }

    /**
     * Kết nối đai BLE và tự nối lại nếu rớt sóng: mỗi lần flow [LiveHeartRateSource.connect]
     * kết thúc (mất kết nối hoặc lỗi), xoá bpm live khỏi state rồi thử lại sau một khoảng chờ,
     * cho tới khi service dừng (job bị huỷ ở [stop]/[pause]/[onDestroy]).
     */
    private fun startHeartRateCollection(id: String) {
        heartRateJob?.cancel()
        if (!FeatureFlags.HEART_RATE_INTEGRATION) return
        val address = bleHeartRateStore.savedAddress() ?: return
        if (!liveHeartRateSource.isSupported() || !liveHeartRateSource.hasPermissions()) return

        heartRateJob = scope.launch {
            while (isActive) {
                liveHeartRateSource.connect(address)
                    .catch { e -> Log.w(TAG, "heart-rate stream error", e) }
                    .collect { bpm -> onHeartRate(id, bpm) }
                session.update { it.copy(liveHeartRateBpm = null) }
                updateNotification()
                if (!isActive) break
                delay(HEART_RATE_RECONNECT_DELAY_MS)
            }
        }
    }

    private suspend fun onHeartRate(id: String, bpm: Int) {
        session.update { it.copy(liveHeartRateBpm = bpm) }
        if (session.state.value.status != TrackingStatus.TRACKING) return

        heartRateBuffer += HeartRateSample(bpm = bpm, timestamp = Instant.now())
        if (heartRateBuffer.size >= HEART_RATE_FLUSH_SIZE) {
            val batch = heartRateBuffer.toList()
            heartRateBuffer.clear()
            repository.appendHeartRateSamples(id, batch)
        }
    }

    private suspend fun flushHeartRate() {
        val id = activityId ?: return
        if (heartRateBuffer.isEmpty()) return
        val batch = heartRateBuffer.toList()
        heartRateBuffer.clear()
        repository.appendHeartRateSamples(id, batch)
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
            // The current schema does not persist the altitude source of recovered points.
            allRecordedAltitudesBarometric = false
            val aggregate = RunAggregator.fromPoints(points)
            elevationAccumulator = ElevationAccumulator().also { accumulator ->
                points.forEach { accumulator.add(it.altitude, it.timestamp) }
            }
            val last = points.lastOrNull()

            startedAt = activity.startTime
            lastAccepted = last
            pausedAccumSeconds = 0
            pausedAt = null
            lastFixAt = Instant.now()
            lastMovingAt = Instant.now()
            // Steps không có nguồn persisted để tính lại như route points -> bắt đầu lại từ 0
            // sau khi Service bị OS kill (cùng giới hạn với nav route/beacon, xem CLAUDE.md).
            lastStepCumulative = null
            stepsAccumulated = 0

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
            startHeartRateCollection(id)
            startBeaconCollection()
            startBarometerCollection()
            startStepCollection()
            updateNotification()
        }
    }

    // ---- Beacon (chia sẻ vị trí trực tiếp) ----

    /** Theo dõi [BeaconController]: mở kênh khi bật, đóng khi tắt. */
    private fun startBeaconCollection() {
        beaconJob?.cancel()
        beaconJob = scope.launch {
            beaconController.state.collect { share ->
                val code = share.code
                when {
                    share.sharing && code != null && broadcastingCode == null -> {
                        broadcastingCode = code
                        liveLocationTransport.startBroadcast(code)
                        publishBeacon()
                    }
                    (!share.sharing || code == null) && broadcastingCode != null -> {
                        val ended = broadcastingCode!!
                        broadcastingCode = null
                        liveLocationTransport.endBroadcast(ended)
                    }
                }
            }
        }
    }

    private fun publishBeacon() {
        val code = broadcastingCode ?: return
        val s = session.state.value
        val lat = s.lastLatitude ?: return
        val lng = s.lastLongitude ?: return
        scope.launch {
            liveLocationTransport.publish(
                code,
                LiveLocationUpdate(
                    latitude = lat,
                    longitude = lng,
                    timestamp = Instant.now(),
                    elapsedSeconds = s.elapsedSeconds,
                    distanceMeters = s.distanceMeters,
                    paused = s.status == TrackingStatus.PAUSED,
                ),
            )
        }
    }

    private suspend fun stopBeacon() {
        broadcastingCode?.let { liveLocationTransport.endBroadcast(it) }
        broadcastingCode = null
        beaconJob?.cancel()
        beaconJob = null
    }

    private fun pause() {
        cancelReroute()
        offRouteSince = null
        if (session.state.value.status != TrackingStatus.TRACKING) return
        pausedAt = Instant.now()
        locationJob?.cancel()
        locationJob = null
        session.update { it.copy(status = TrackingStatus.PAUSED, autoPaused = false) }
        updateNotification()
        publishBeacon()
    }

    private fun resume() {
        if (session.state.value.status != TrackingStatus.PAUSED) return
        pausedAt?.let { pausedAccumSeconds += Duration.between(it, Instant.now()).seconds }
        pausedAt = null
        lastFixAt = Instant.now()
        lastMovingAt = Instant.now()
        session.update { it.copy(status = TrackingStatus.TRACKING, gpsSignalOk = true, autoPaused = false) }
        // Job có thể đã sống sẵn nếu vừa tự động tạm dừng (xem [autoPause]) — khởi động lại vẫn an
        // toàn (huỷ job cũ trước khi mở job mới).
        startLocationCollection()
        updateNotification()
        publishBeacon()
    }

    /**
     * Tự tạm dừng khi đứng yên quá [AUTO_PAUSE_IDLE_SECONDS] (bật trong Cài đặt) — KHÁC [pause] ở
     * chỗ KHÔNG huỷ [locationJob]: vẫn phải nhận GPS để tự phát hiện lúc di chuyển lại ([onLocation]
     * gọi [autoResume] khi đó) — điểm nhận được trong lúc này KHÔNG lưu vào trace (xem [onLocation]),
     * giống hệt cách tạm dừng tay không ghi điểm nào cả.
     */
    private fun autoPause() {
        cancelReroute()
        offRouteSince = null
        pausedAt = Instant.now()
        session.update { it.copy(status = TrackingStatus.PAUSED, autoPaused = true) }
        updateNotification()
        publishBeacon()
    }

    private fun autoResume() {
        pausedAt?.let { pausedAccumSeconds += Duration.between(it, Instant.now()).seconds }
        pausedAt = null
        lastMovingAt = Instant.now()
        session.update { it.copy(status = TrackingStatus.TRACKING, autoPaused = false, gpsSignalOk = true) }
        updateNotification()
        publishBeacon()
    }

    private fun stop() {
        stopCollecting()
        scope.launch {
            withContext(NonCancellable) {
                flushHeartRate()
                stopBeacon()
                finalizeAndReset()
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /**
     * Huỷ buổi đang ghi mà KHÔNG lưu lại — dùng khi người dùng bấm Kết thúc lúc chưa di chuyển gì
     * (xem [com.example.runtracker.ui.tracking.TrackingScreen], dialog "chưa di chuyển"). Khác
     * [stop] ở chỗ xoá thẳng activity đã tạo lúc START thay vì chốt số liệu.
     */
    private fun discard() {
        stopCollecting()
        val id = activityId
        scope.launch {
            withContext(NonCancellable) {
                stopBeacon()
                id?.let { repository.deleteActivity(it) }
                stateStore.clear()
                beaconController.reset()
            }
            resetState()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopCollecting() {
        stopNavigation()
        locationJob?.cancel()
        tickerJob?.cancel()
        heartRateJob?.cancel()
        barometerJob?.cancel()
        stepJob?.cancel()
        releaseWakeLock()
    }

    // ---- Thu thập vị trí ----

    private fun startLocationCollection() {
        locationJob?.cancel()
        locationJob = scope.launch {
            locationClient.locationUpdates(LOCATION_INTERVAL_MS)
                .catch { e ->
                    Log.w(TAG, "location updates stopped", e)
                    session.update { it.copy(gpsSignalOk = false) }
                }
                .collect { event ->
                    when (event) {
                        is LocationEvent.Fix -> onLocation(event.location)
                        // Bỏ qua: `isLocationAvailable` khá nhạy, có thể nhảy true/false liên tục
                        // dù fix vẫn tới đều — dùng trực tiếp làm banner nháy liên tục dù GPS đang
                        // kết nối tốt. Mất tín hiệu thật đã được [startTicker] phát hiện ổn định
                        // hơn qua "quá GPS_STALE_THRESHOLD_SECONDS giây không có fix mới".
                        is LocationEvent.Availability -> Unit
                    }
                }
        }
    }

    private data class MovementDelta(
        val meters: Double,
        val dtSeconds: Double,
        val dAlt: Double,
        val moving: Boolean,
    )

    private fun movementBetween(prev: RoutePoint, next: RoutePoint): MovementDelta {
        val meters = GeoMath.distanceMeters(prev.latitude, prev.longitude, next.latitude, next.longitude)
        val dtSeconds = (next.timestamp.toEpochMilli() - prev.timestamp.toEpochMilli()) / 1000.0
        val dAlt = next.altitude - prev.altitude
        val moving = dtSeconds > 0 && meters / dtSeconds >= RunAggregator.MOVING_SPEED_MPS
        return MovementDelta(meters, dtSeconds, dAlt, moving)
    }

    private suspend fun onLocation(location: Location) {
        val id = activityId ?: return
        val point = location.toRoutePoint()
        lastFixAt = Instant.now()

        // Chỉ auto-pause mới còn nhận fix lúc PAUSED (tạm dừng tay huỷ locationJob) — điểm đứng
        // yên trong lúc này KHÔNG lưu vào trace, chỉ dùng để dò lúc di chuyển lại rồi tự resume.
        if (session.state.value.status == TrackingStatus.PAUSED) {
            val prev = lastAccepted
            if (prev != null && !movementBetween(prev, point).moving) return
            autoResume()
        }

        val stored = repository.appendRoutePoints(id, listOf(point))
        val accepted = stored.lastOrNull() ?: return
        val reading = altitudeReading
        allRecordedAltitudesBarometric = allRecordedAltitudesBarometric && reading?.barometric == true
        if (previousAltitudeSource != reading?.barometric) {
            elevationAccumulator.add(null, accepted.timestamp.minusNanos(1))
        }
        previousAltitudeSource = reading?.barometric
        elevationAccumulator.add(reading?.meters, accepted.timestamp)

        val prev = lastAccepted
        if (prev != null) {
            val delta = movementBetween(prev, accepted)
            if (delta.moving) lastMovingAt = Instant.now()

            session.update { s ->
                s.copy(
                    distanceMeters = s.distanceMeters + delta.meters,
                    movingTimeSeconds = s.movingTimeSeconds +
                        if (delta.moving) delta.dtSeconds.roundToLong() else 0,
                    elevationGainMeters = elevationAccumulator.gainMeters,
                    elevationLossMeters = elevationAccumulator.lossMeters,
                    pointCount = s.pointCount + 1,
                    lastLatitude = accepted.latitude,
                    lastLongitude = accepted.longitude,
                    lastUpdate = accepted.timestamp,
                    gpsSignalOk = true,
                )
            }
        } else {
            lastMovingAt = Instant.now()
            session.update { s ->
                s.copy(
                    pointCount = s.pointCount + 1,
                    lastLatitude = accepted.latitude,
                    lastLongitude = accepted.longitude,
                    lastUpdate = accepted.timestamp,
                    gpsSignalOk = true,
                )
            }
        }
        lastAccepted = accepted
        updateNavigation(accepted)
        updateNotification()
        publishBeacon()
    }

    private fun updateNavigation(point: RoutePoint) = synchronized(navigationLock) {
        if (!navigationEnabled || session.state.value.status != TrackingStatus.TRACKING) return@synchronized
        val update = navigation.update(GeoPoint(point.latitude, point.longitude)) ?: return@synchronized
        publishNavigation(update)
        if (!update.progress.offRoute || update.progress.arrived) {
            offRouteSince = null
            cancelReroute()
            return@synchronized
        }
        val now = Instant.now()
        val since = offRouteSince ?: now.also { offRouteSince = it }
        val last = lastRerouteAttemptAt
        if (rerouteJob?.isActive == true ||
            Duration.between(since, now).seconds < REROUTE_TRIGGER_SECONDS ||
            (last != null && Duration.between(last, now).seconds < REROUTE_DEBOUNCE_SECONDS)) return@synchronized
        val request = navigation.beginReroute() ?: return@synchronized
        lastRerouteAttemptAt = now
        session.update { it.copy(navRerouting = true) }
        val job = scope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            try {
                val result = kotlinx.coroutines.withTimeoutOrNull(25_000) {
                    buildRouteUseCase(listOf(request.origin, request.destination), request.mode)
                }
                synchronized(navigationLock) {
                    if (result != null && navigationEnabled &&
                        session.state.value.status == TrackingStatus.TRACKING &&
                        navigation.applyReroute(request, result)) {
                        val state = session.state.value
                        val location = state.lastLatitude?.let { lat ->
                            state.lastLongitude?.let { lon -> GeoPoint(lat, lon) }
                        } ?: request.origin
                        navigation.update(location)?.let(::publishNavigation)
                        offRouteSince = null
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "Rerouting failed", error)
            } finally {
                val completingJob = kotlinx.coroutines.currentCoroutineContext()[Job]
                synchronized(navigationLock) {
                    // An older job must not clear the loading state of a newer request.
                    if (rerouteJob === completingJob) {
                        rerouteJob = null
                        session.update { it.copy(navRerouting = false) }
                    }
                }
            }
        }
        rerouteJob = job
        job.start()
    }

    private fun publishNavigation(update: NavigationSession.Update) {
        val progress = update.progress
        session.update {
            it.copy(
                navInstruction = if (progress.arrived) "Đã tới đích" else progress.nextInstruction,
                navDistanceMeters = progress.distanceToNextMeters,
                navOffRoute = progress.offRoute,
                navStepIndex = progress.stepIndex,
                navStepCount = update.stepCount,
                navPolyline = update.polyline,
            )
        }
    }

    private fun cancelReroute() = synchronized(navigationLock) {
        navigation.invalidateRequests()
        rerouteJob?.cancel()
        rerouteJob = null
        session.update { it.copy(navRerouting = false) }
    }

    private fun stopNavigation() = synchronized(navigationLock) {
        navigationEnabled = false
        cancelReroute()
        navigation.start(null)
        offRouteSince = null
        lastRerouteAttemptAt = null
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1_000)
                val start = startedAt ?: continue
                if (session.state.value.status != TrackingStatus.TRACKING) continue
                val elapsed = Duration.between(start, Instant.now()).seconds - pausedAccumSeconds
                val stale = lastFixAt?.let {
                    Duration.between(it, Instant.now()).seconds >= GPS_STALE_THRESHOLD_SECONDS
                } ?: false
                session.update {
                    it.copy(
                        elapsedSeconds = elapsed.coerceAtLeast(0),
                        gpsSignalOk = it.gpsSignalOk && !stale,
                    )
                }
                updateNotification()

                if (settingsStore.autoPauseEnabled.value) {
                    val idleSeconds = lastMovingAt?.let {
                        Duration.between(it, Instant.now()).seconds
                    } ?: 0L
                    if (idleSeconds >= AUTO_PAUSE_IDLE_SECONDS) autoPause()
                }
            }
        }
    }

    // ---- Ghi Activity ----

    private fun initialActivity(id: String, now: Instant) = Activity(
        id = id,
        userId = LOCAL_USER_ID,
        type = session.plannedType.value,
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
        steps = null,
        avgCadence = null,
        perceivedExertion = null,
        weather = null,
        gpxRawPath = null,
    )

    private suspend fun finalizeAndReset() {
        val id = activityId ?: return
        val avgCadence = if (cadenceSampleCount > 0) (cadenceSum / cadenceSampleCount).toInt() else null
        // Tính lại aggregate + lap từ trace đã lưu (số liệu chuẩn, không lệ thuộc state bộ nhớ).
        finalizeActivityUseCase(
            id,
            endTime = Instant.now(),
            pausedSeconds = pausedAccumSeconds,
            steps = stepsAccumulated,
            avgCadence = avgCadence,
            reliableBarometricAltitude = allRecordedAltitudesBarometric && lastAccepted != null,
        )
        session.activityFinished(id)
        stateStore.clear()
        beaconController.reset()
        resetState()
    }

    /** Đưa mọi state trong bộ nhớ về mốc IDLE — dùng chung cho cả chốt buổi ([finalizeAndReset]) lẫn huỷ ([discard]). */
    private fun resetState() {
        elevationAccumulator = ElevationAccumulator()
        session.reset()
        session.selectRoute(null)
        activityId = null
        startedAt = null
        lastAccepted = null
        pausedAccumSeconds = 0
        pausedAt = null
        lastFixAt = null
        lastMovingAt = null
        stopNavigation()
        heartRateBuffer.clear()
        altitudeEstimator = AltitudeEstimator()
        altitudeReading = null
        allRecordedAltitudesBarometric = true
        previousAltitudeSource = null
        lastStepCumulative = null
        stepsAccumulated = 0
        cadenceWindow.clear()
        cadenceSum = 0
        cadenceSampleCount = 0
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
        val title = when {
            status == TrackingStatus.PAUSED && s.autoPaused -> "Tự động tạm dừng"
            status == TrackingStatus.PAUSED -> "Đã tạm dừng"
            else -> "Đang ghi hoạt động"
        }
        val text = buildString {
            append("%.2f km · %s".format(km, formatClock(s.elapsedSeconds)))
            s.liveHeartRateBpm?.let { append(" · ").append(it).append(" bpm") }
        }

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

    private fun Location.toRoutePoint(): RoutePoint {
        val gpsAltitude = if (hasAltitude()) altitude else null
        altitudeReading = altitudeEstimator.read(
            gpsAltitude,
            if (hasVerticalAccuracy()) verticalAccuracyMeters else null,
            android.os.SystemClock.elapsedRealtime(),
        )
        return RoutePoint(
            latitude = latitude,
            longitude = longitude,
            // Legacy non-null schema: keep raw GPS for offline recovery, but only validated
            // readings enter live ascent. Finalization tries DEM when coverage is incomplete.
            altitude = altitudeReading?.meters ?: gpsAltitude?.takeIf { it.isFinite() } ?: 0.0,
            speedMps = if (hasSpeed()) speed else null,
            accuracyMeters = if (hasAccuracy()) accuracy else null,
            timestamp = Instant.ofEpochMilli(time),
        )
    }

    companion object {
        const val ACTION_START = "com.example.runtracker.tracking.START"
        const val ACTION_RESTORE = "com.example.runtracker.tracking.RESTORE"
        const val ACTION_PAUSE = "com.example.runtracker.tracking.PAUSE"
        const val ACTION_RESUME = "com.example.runtracker.tracking.RESUME"
        const val ACTION_STOP = "com.example.runtracker.tracking.STOP"
        const val ACTION_DISCARD = "com.example.runtracker.tracking.DISCARD"

        private const val EXTRA_ACTIVITY_ID = "activityId"
        private const val TAG = "LocationTrackingService"
        private const val CHANNEL_ID = "tracking"
        private const val NOTIF_ID = 1001
        private const val LOCATION_INTERVAL_MS = 3_000L
        private const val GPS_STALE_THRESHOLD_SECONDS = 12L // ~4x chu kỳ cập nhật vị trí
        private const val AUTO_PAUSE_IDLE_SECONDS = 15L
        private const val REROUTE_TRIGGER_SECONDS = 20L
        private const val REROUTE_DEBOUNCE_SECONDS = 25L
        private const val CADENCE_WINDOW_SECONDS = 10L
        private const val MIN_CADENCE_WINDOW_SECONDS = 5L
        private const val HEART_RATE_FLUSH_SIZE = 10
        private const val HEART_RATE_RECONNECT_DELAY_MS = 5_000L
        private const val MAX_WAKE_LOCK_MS = 6L * 60 * 60 * 1000 // 6h an toàn

        fun start(context: Context) = send(context, ACTION_START, foreground = true)
        fun pause(context: Context) = send(context, ACTION_PAUSE, foreground = false)
        fun resume(context: Context) = send(context, ACTION_RESUME, foreground = false)
        fun stop(context: Context) = send(context, ACTION_STOP, foreground = false)
        fun discard(context: Context) = send(context, ACTION_DISCARD, foreground = false)

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
