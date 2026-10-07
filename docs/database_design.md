# Thiết kế Database cho App Chạy Bộ (Kotlin + Room)

Tài liệu này thiết kế đầy đủ schema cho cả 3 phase đã thống nhất, để bạn không phải refactor database về sau. Local DB dùng **Room (SQLite)**, đồng bộ lên backend (Postgres) qua các bảng có cờ `isSynced` / `updatedAt`.

---

## 1. Sơ đồ tổng quan các bảng

```
User ──< Activity ──< RoutePoint
  │           │
  │           ├──< HeartRateSample
  │           ├──< SegmentEffort >── Segment ──< SegmentLeaderboardEntry
  │           └──< ActivityLap
  │
  ├──< DailyTrainingLoad
  ├──< FitnessFreshnessSnapshot
  ├──< PerformancePrediction
  ├──< UserZoneSettings (1-1)
  ├──< Route (route builder) ──< RouteWaypoint
  ├──< DeviceConnection
  └──< ChallengeParticipant >── Challenge
```

---

## 2. Phase 1 — Core Tracking

### 2.1 User

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String?,
    val birthYear: Int?,          // dùng để tính maxHR ước lượng (220 - age)
    val weightKg: Double?,        // dùng để tính calo
    val restingHeartRate: Int?,   // dùng cho công thức TRIMP/Banister
    val maxHeartRate: Int?,       // có thể đo thực tế, override công thức ước lượng
    val sex: String? = null,      // "MALE" / "FEMALE" — chọn hệ số TRIMP nam/nữ; null -> fallback công thức nam
    val createdAt: Long,
    val updatedAt: Long
)
```

> `sex` được thêm sau bản thiết kế gốc: công thức TRIMP (mục 3.2) có hệ số khác nhau cho nam và nữ nên cần biết giới tính sinh học của người dùng. Nếu null (người dùng không khai báo), dùng nhánh công thức nam.

### 2.2 Activity

```kotlin
@Entity(
    tableName = "activities",
    indices = [Index("userId"), Index("startTime")]
)
data class ActivityEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,              // RUNNING, CYCLING, WALKING...
    val startTime: Long,
    val endTime: Long,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val movingTimeSeconds: Long,   // loại trừ thời gian dừng (đèn đỏ, nghỉ)
    val avgPaceSecPerKm: Double,
    val avgSpeedKmh: Double,
    val elevationGainMeters: Double,
    val elevationLossMeters: Double,
    val avgHeartRate: Int?,
    val maxHeartRate: Int?,
    val calories: Int?,
    val avgCadence: Int?,
    val weatherTempC: Double?,          // Phase 3 — weather overlay (Open-Meteo), null nếu chưa lấy
    val weatherApparentTempC: Double? = null,  // thêm ở DB v2 (Migration 1→2)
    val weatherHumidityPct: Int? = null,       // %
    val weatherWindMps: Double? = null,        // m/s
    val weatherWindDirDeg: Int? = null,        // độ
    val weatherCode: Int? = null,              // mã WMO
    val perceivedExertion: Int?,   // RPE 1-10, người dùng tự nhập (dùng cho TRIMP nếu ko có HR)
    val gpxRawPath: String?,       // đường dẫn file gốc nếu cần export/backup
    val isSynced: Boolean = false,
    val updatedAt: Long
)
```

### 2.3 RoutePoint (GPS trace thô)

```kotlin
@Entity(
    tableName = "route_points",
    foreignKeys = [ForeignKey(
        entity = ActivityEntity::class,
        parentColumns = ["id"], childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("activityId")]
)
data class RoutePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speedMps: Float?,
    val accuracyMeters: Float?,   // dùng để lọc nhiễu GPS (bỏ điểm accuracy quá kém)
    val timestamp: Long
)
```

### 2.4 HeartRateSample (tách riêng RoutePoint để lấy mẫu nhịp tim dày hơn/thưa hơn GPS)

```kotlin
@Entity(
    tableName = "heart_rate_samples",
    foreignKeys = [ForeignKey(
        entity = ActivityEntity::class,
        parentColumns = ["id"], childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("activityId")]
)
data class HeartRateSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: String,
    val bpm: Int,
    val timestamp: Long
)
```

### 2.5 ActivityLap (chia activity thành các lap tự động theo km hoặc thủ công)

```kotlin
@Entity(tableName = "activity_laps")
data class ActivityLapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: String,
    val lapIndex: Int,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val avgPaceSecPerKm: Double,
    val avgHeartRate: Int?
)
```

---

## 3. Phase 2 — Các tính năng "premium" làm free

### 3.1 UserZoneSettings — cấu hình vùng nhịp tim / pace (nền tảng cho mọi phân tích)

```kotlin
@Entity(tableName = "user_zone_settings")
data class UserZoneSettingsEntity(
    @PrimaryKey val userId: String,
    // 5 vùng HR theo % của maxHR, lưu dạng JSON hoặc 5 cột min-max
    val zone1Min: Int, val zone1Max: Int,
    val zone2Min: Int, val zone2Max: Int,
    val zone3Min: Int, val zone3Max: Int,
    val zone4Min: Int, val zone4Max: Int,
    val zone5Min: Int, val zone5Max: Int,
    val thresholdPaceSecPerKm: Double?, // pace ngưỡng (FTP-pace), dùng cho Performance Prediction
    val updatedAt: Long
)
```

> Việc tách bảng này ra riêng (thay vì hard-code trong code) là điểm quan trọng: Strava khóa "Training Zones" sau paywall, nghĩa là công thức tính zone phải cấu hình được theo từng người, không fix cứng.

### 3.2 DailyTrainingLoad — mỗi ngày 1 dòng, làm nguyên liệu cho Fitness & Freshness

```kotlin
@Entity(
    tableName = "daily_training_load",
    primaryKeys = ["userId", "date"]
)
data class DailyTrainingLoadEntity(
    val userId: String,
    val date: String,          // "yyyy-MM-dd", 1 user chỉ có 1 dòng/ngày
    val trimpScore: Double,    // tổng TRIMP của tất cả activity trong ngày
    val activityCount: Int,
    val totalDurationSeconds: Long,
    val updatedAt: Long
)
```

**Công thức TRIMP (Training Impulse) — Banister**, tính cho từng activity rồi cộng dồn vào ngày:

```
TRIMP = duration_phut × ΔHR_ratio × 0.64 × e^(1.92 × ΔHR_ratio)   [nam]
TRIMP = duration_phut × ΔHR_ratio × 0.86 × e^(1.67 × ΔHR_ratio)   [nữ]

ΔHR_ratio = (avgHR_hoat_dong − restingHR) / (maxHR − restingHR)
```

Nếu activity không có dữ liệu nhịp tim, fallback dùng RPE (perceivedExertion) theo công thức TRIMP đơn giản: `TRIMP = duration_phut × RPE`.

### 3.3 FitnessFreshnessSnapshot — CTL / ATL / TSB (mô hình Banister, giống "Fitness & Freshness" của Strava)

```kotlin
@Entity(
    tableName = "fitness_freshness_snapshots",
    primaryKeys = ["userId", "date"]
)
data class FitnessFreshnessSnapshotEntity(
    val userId: String,
    val date: String,          // "yyyy-MM-dd"
    val ctl: Double,           // Chronic Training Load = "Fitness", EWMA 42 ngày của TRIMP
    val atl: Double,           // Acute Training Load = "Fatigue", EWMA 7 ngày của TRIMP
    val tsb: Double,           // Training Stress Balance = "Form" = CTL(hôm qua) − ATL(hôm qua)
    val computedAt: Long
)
```

**Công thức đệ quy (EWMA — exponentially weighted moving average):**

```
CTL_hôm_nay = CTL_hôm_qua + (TRIMP_hôm_nay − CTL_hôm_qua) / 42
ATL_hôm_nay = ATL_hôm_qua + (TRIMP_hôm_nay − ATL_hôm_qua) / 7
TSB_hôm_nay = CTL_hôm_qua − ATL_hôm_qua
```

Bảng này nên được tính bằng một **WorkManager job chạy mỗi đêm** (hoặc ngay sau khi sync activity mới), đọc `DailyTrainingLoad` và ghi đè/append snapshot mới — không tính lại từ đầu mỗi lần.

### 3.4 Segment + SegmentEffort + Leaderboard

```kotlin
@Entity(tableName = "segments")
data class SegmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startLat: Double, val startLng: Double,
    val endLat: Double, val endLng: Double,
    val distanceMeters: Double,
    val avgGrade: Double,
    val polyline: String,      // encoded polyline để match GPS trace
    val createdByUserId: String,
    val isPublic: Boolean = true
)

@Entity(
    tableName = "segment_efforts",
    indices = [Index("segmentId"), Index("userId")]
)
data class SegmentEffortEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val segmentId: String,
    val activityId: String,
    val userId: String,
    val elapsedSeconds: Double,
    val startTime: Long,
    val avgHeartRate: Int?,
    val rank: Int? = null       // cache rank tại thời điểm tính, để hiện leaderboard nhanh
)
```

> Không cần bảng leaderboard riêng — `SELECT ... ORDER BY elapsedSeconds ASC` trên `segment_efforts WHERE segmentId = ?` là đủ, vì bạn **không giới hạn top 10** như Strava free.

### 3.5 Route + RouteWaypoint (Route Builder, khác với RoutePoint đã ghi)

```kotlin
@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val distanceMeters: Double,
    val elevationGainMeters: Double,
    val polyline: String,
    val isPublic: Boolean = false,
    val createdAt: Long,
    val travelMode: String? = null,       // WALKING / CYCLING; null với route cũ
    val sourcePolyline: String? = null,   // điểm người dùng chấm hoặc nét vẽ gốc, polyline 1e-5
    @ColumnInfo(defaultValue = "0") val drawnFromSketch: Boolean = false,
    @ColumnInfo(defaultValue = "0") val snappedToRoads: Boolean = false,
)

@Entity(
    tableName = "route_waypoints",
    foreignKeys = [ForeignKey(
        entity = RouteEntity::class,
        parentColumns = ["id"], childColumns = ["routeId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class RouteWaypointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: String,
    val orderIndex: Int,       // thứ tự điểm trong route để turn-by-turn navigation
    val latitude: Double,
    val longitude: Double,
    val instruction: String?   // "Rẽ trái vào Nguyễn Trãi", dùng cho navigation
)
```

> Migration 6→7 bổ sung bốn cột trên, giữ nguyên route/waypoint cũ. Không suy đoán chế độ hoặc độ tin cậy của dữ liệu cũ: người dùng mở Sửa, chọn chế độ và tính đường lại trước khi dẫn đường. `route_waypoints` vẫn lưu chỉ dẫn rẽ; `sourcePolyline` giữ riêng đầu vào để chỉnh sửa. Chỉ route bám đường thành công mới được lưu mới/cập nhật.

### 3.6 PerformancePrediction

```kotlin
@Entity(
    tableName = "performance_predictions",
    primaryKeys = ["userId", "distanceLabel"]
)
data class PerformancePredictionEntity(
    val userId: String,
    val distanceLabel: String,     // "5K", "10K", "HALF", "FULL"
    val predictedSeconds: Double,
    val basedOnActivityId: String, // activity gần nhất/tốt nhất dùng để tính
    val computedAt: Long
)
```

Công thức đơn giản, đáng tin cậy để bắt đầu: **Riegel formula**

```
T2 = T1 × (D2 / D1) ^ 1.06
```

Trong đó T1/D1 là thời gian/quãng đường của activity tốt nhất gần đây, D2 là cự ly muốn dự đoán.

### 3.7 DeviceConnection (đồng hồ/vòng đeo qua Health Connect / Bluetooth)

```kotlin
@Entity(tableName = "device_connections")
data class DeviceConnectionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val deviceType: String,     // "HEALTH_CONNECT", "BLE_HR_STRAP", "GARMIN"...
    val deviceName: String,
    val isActive: Boolean,
    val lastSyncedAt: Long?
)
```

---

## 4. Phase 3 — Mở rộng

> **Group Challenges đã triển khai** (việc 22): local-first, người tạo tự tham gia, tiến độ tính lại từ
> activity của người dùng trong `[startDate, endDate]` theo `goalType` (`TOTAL_DISTANCE` mét /
> `TOTAL_ACTIVITIES` số buổi / `TOTAL_ELEVATION` mét / `TOTAL_DURATION` giây). `ChallengeParticipant`
> có cấu trúc leaderboard nhiều người nhưng chờ backend mới có người tham gia khác. Không đổi schema
> hai bảng dưới.

```kotlin
@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val goalType: String,      // "TOTAL_DISTANCE", "TOTAL_ACTIVITIES"...
    val goalValue: Double,
    val startDate: String,
    val endDate: String,
    val createdByUserId: String
)

@Entity(
    tableName = "challenge_participants",
    primaryKeys = ["challengeId", "userId"]
)
data class ChallengeParticipantEntity(
    val challengeId: String,
    val userId: String,
    val currentProgress: Double,
    val joinedAt: Long
)
```

Weather overlay (việc 23) đã triển khai: thêm 5 cột `weather*` vào `activities` qua **Migration 1→2**
(xem `data/local/Migrations.kt`), lấy dữ liệu từ Open-Meteo (miễn phí, không cần key) khi mở màn chi tiết
buổi tập. DB_VERSION = 2.

Training plan gợi ý (việc 24) **không có bảng DB**: chỉ lưu mục tiêu (cự ly + ngày thi đấu) trong
SharedPreferences (`data/training/TrainingGoalStore`), kế hoạch chi tiết sinh lại mỗi lần từ CTL +
quãng đường gần đây (`TrainingPlanGenerator`).

Live tracking / Beacon (việc 25) **không có bảng DB** ở giai đoạn client: vị trí trực tiếp đi qua
interface `domain/beacon/LiveLocationTransport` (hiện là bản loopback trong bộ nhớ). Khi có backend,
thêm 1 impl mạng — không đụng schema local.

---

## 5. Lưu ý khi triển khai

1. **Không tính CTL/ATL/TSB theo kiểu query trực tiếp mỗi lần mở app** — chi phí tính lại toàn bộ lịch sử sẽ tăng dần theo thời gian. Luôn tính tăng dần (incremental) và lưu snapshot mỗi ngày.
2. **Lọc nhiễu GPS trước khi lưu RoutePoint**: bỏ điểm có `accuracyMeters` quá lớn (>20-30m) hoặc tốc độ tức thời bất thường (>~7 m/s cho chạy bộ), tránh làm sai `distanceMeters` và ảnh hưởng dây chuyền tới TRIMP.
3. **Đồng bộ hai chiều**: các bảng cần sync lên server (Activity, Segment, Route, Challenge...) nên có `isSynced`/`updatedAt`; các bảng tính toán thuần local (FitnessFreshnessSnapshot, DailyTrainingLoad) có thể tính lại từ Activity nếu cần, nên rủi ro mất dữ liệu thấp hơn.
4. **maxHeartRate**: ưu tiên giá trị đo thực tế trong `UserEntity.maxHeartRate`; nếu null thì fallback công thức `220 - tuổi` khi tính TRIMP và zone.
5. **Index** đã thêm cho các cột dùng để JOIN/WHERE thường xuyên (`userId`, `activityId`, `segmentId`, `date`) — tránh full table scan khi dữ liệu lớn dần.
