# CLAUDE.md — RunTracker App

Đây là ngữ cảnh dự án. Đọc file này trước khi thực hiện bất kỳ task nào. Nếu cần chi tiết schema đầy đủ, đọc thêm `docs/database_design.md`.

## Mục tiêu dự án

Xây app Android chạy bộ kiểu Strava, viết bằng Kotlin. Điểm khác biệt cạnh tranh: mở khóa **miễn phí** các tính năng mà Strava hiện đang khóa sau subscription (heart rate/training zones, training load, fitness & freshness, full segment leaderboard, route builder, performance prediction). **Chưa làm mạng xã hội (social feed)** ở giai đoạn đầu — tập trung vào tracking + phân tích dữ liệu cá nhân trước.

## Tech stack

- Ngôn ngữ: Kotlin 100%
- UI: Jetpack Compose
- Kiến trúc: MVVM + Clean Architecture (data / domain / presentation)
- Async: Kotlin Coroutines + Flow
- DI: Hilt
- Local DB: Room
- Network: Retrofit + OkHttp
- Map: Google Maps SDK
- Location: FusedLocationProviderClient trong Foreground Service
- Wearable data: Health Connect API (ưu tiên hơn tích hợp riêng từng hãng)
- Backend: chưa chốt (Firebase cho MVP, hoặc Ktor/Spring Boot + Postgres nếu scale) — chưa triển khai ở giai đoạn hiện tại, đang tập trung local-first

## Roadmap chức năng

**Phase 1 — Core tracking (làm trước tiên)**
- Ghi hoạt động qua GPS: distance, pace, duration, elevation
- Bản đồ route (polyline), có offline map
- Lịch sử hoạt động, xem chi tiết từng buổi
- Tính calo

**Phase 2 — Tính năng "premium" của Strava nhưng làm free (ưu tiên ngay sau Phase 1, đây là giá trị cạnh tranh chính)**
- Heart rate & training zones analysis không giới hạn
- Relative Effort & Training Load (dựa trên TRIMP — công thức bên dưới)
- Fitness & Freshness: CTL (Fitness) / ATL (Fatigue) / TSB (Form) — mô hình Banister, công thức bên dưới
- Full Segment + Leaderboard (không giới hạn top 10)
- Route Builder + turn-by-turn navigation
- Performance Predictions (Riegel formula)

**Phase 3 — Mở rộng (làm sau, đã chừa sẵn cột/bảng trong schema)**
- Group Challenges (quy mô nhỏ, chưa cần social feed đầy đủ)
- Live tracking / Beacon (an toàn, chia sẻ vị trí real-time)
- Weather overlay trên activity
- Training plan gợi ý (có thể tích hợp AI sau)

**Chưa làm ở giai đoạn này:** mạng xã hội đầy đủ (feed công khai, follow/unfollow, comment công khai).

## Công thức tính toán quan trọng

**TRIMP (Training Impulse) mỗi activity:**
```
ΔHR_ratio = (avgHR_hoạt_động − restingHR) / (maxHR − restingHR)
TRIMP = duration_phút × ΔHR_ratio × 0.64 × e^(1.92 × ΔHR_ratio)   [nam]
TRIMP = duration_phút × ΔHR_ratio × 0.86 × e^(1.67 × ΔHR_ratio)   [nữ]
```
Nếu không có dữ liệu nhịp tim, fallback: `TRIMP = duration_phút × RPE` (RPE người dùng tự nhập, thang 1-10).

**CTL / ATL / TSB (EWMA, tính đệ quy mỗi ngày — KHÔNG tính lại từ đầu mỗi lần):**
```
CTL_hôm_nay = CTL_hôm_qua + (TRIMP_hôm_nay − CTL_hôm_qua) / 42
ATL_hôm_nay = ATL_hôm_qua + (TRIMP_hôm_nay − ATL_hôm_qua) / 7
TSB_hôm_nay = CTL_hôm_qua − ATL_hôm_qua
```
Nên chạy bằng WorkManager job mỗi đêm hoặc ngay sau khi có activity mới.

**Performance Prediction (Riegel formula):**
```
T2 = T1 × (D2 / D1) ^ 1.06
```
T1/D1 = thời gian/quãng đường activity tốt nhất gần đây; D2 = cự ly muốn dự đoán.

## Lưu ý kỹ thuật quan trọng

- Lọc nhiễu GPS trước khi lưu route point: bỏ điểm có accuracy > 20-30m hoặc tốc độ tức thời bất thường (>~7 m/s cho chạy bộ).
- Foreground Service bắt buộc cho location tracking nền trên Android 10+; xin quyền `ACCESS_BACKGROUND_LOCATION` cẩn thận, có notification liên tục khi đang ghi hoạt động.
- Một số thiết bị (Xiaomi, Oppo...) tự kill service nền — cần hướng dẫn người dùng whitelist app khỏi battery optimization.
- maxHeartRate: ưu tiên giá trị đo thực tế (`UserEntity.maxHeartRate`), fallback công thức `220 - tuổi` nếu null.
- Database schema đầy đủ (toàn bộ Entity Kotlin cho Room) đã thiết kế sẵn ở `docs/database_design.md` — bao gồm User, Activity, RoutePoint, HeartRateSample, ActivityLap, UserZoneSettings, DailyTrainingLoad, FitnessFreshnessSnapshot, Segment, SegmentEffort, Route, RouteWaypoint, PerformancePrediction, DeviceConnection, Challenge, ChallengeParticipant. Dùng đúng schema này, không tự ý đổi cấu trúc bảng nếu chưa thảo luận.

## Trạng thái hiện tại

- Đã tạo project mới trong Android Studio (Empty Activity, Compose, Kotlin, min SDK 26), build và chạy thành công trên thiết bị thật.
- **Đã thêm dependencies** vào version catalog (`gradle/libs.versions.toml`) + `app/build.gradle.kts`: Room (+KSP), Hilt (+KSP), Coroutines, WorkManager, lifecycle-viewmodel-compose, hilt-navigation-compose, play-services-location, maps-compose, Health Connect client. Retrofit/OkHttp **chưa thêm** (local-first, backend chưa chốt). Hilt WorkerFactory **chưa wire** — để tới khi làm job Phase 2.
- **Đã tạo toàn bộ Room layer** theo `docs/database_design.md` (build pass, schema xuất ra `app/schemas/`):
  - 16 Entity trong `app/src/main/java/com/example/runtracker/data/local/entity/`
  - 9 DAO trong `.../data/local/dao/` (nhóm theo aggregate: `ActivityDao` gộp activity/route_points/heart_rate_samples/activity_laps; `TrainingLoadDao` gộp daily_training_load/fitness_freshness_snapshots; `SegmentDao`, `RouteDao`, `ChallengeDao` gộp cha-con tương ứng)
  - `RunTrackerDatabase` (version 1, `exportSchema = true`)
  - `di/DatabaseModule` (Hilt, provides DB + tất cả DAO), `RunTrackerApp` (`@HiltAndroidApp`), `MainActivity` gắn `@AndroidEntryPoint`
- **Đã có Activity repository layer** (nhánh `feat/activity-repository`, build + unit test pass):
  - `domain/model/`: `Activity` (+ `ActivityType`), `RoutePoint`, `HeartRateSample`, `ActivityLap`, `ActivityDetail` — dùng `java.time.Instant` + `kotlin.time.Duration`, không mang `isSynced`/`updatedAt`.
  - `domain/tracking/`: `GpsTrackFilter` (lọc nhiễu: accuracy > 25m, tốc độ > 7 m/s giữa 2 điểm, timestamp trùng/lệch) + `GeoMath` (haversine) — thuần JVM, có test.
  - `domain/repository/ActivityRepository` + `data/repository/ActivityRepositoryImpl` (dùng `ActivityDao`, `@IoDispatcher`). `appendRoutePoints` tự lọc nhiễu dựa trên điểm cuối đã lưu (`ActivityDao.getLastRoutePoint`).
  - `data/mapper/ActivityMappers.kt`: entity <-> domain.
  - DI mới: `di/RepositoryModule` (`@Binds ActivityRepository`), `di/DispatcherModule` + `di/IoDispatcher` qualifier.
  - Test: `GpsTrackFilterTest`, `ActivityMappersTest`.
  - `appendRoutePoints` trả về `List<RoutePoint>` (các điểm thực lưu sau lọc) thay vì `Int` — service dùng để cộng dồn quãng đường.
- **Đã có Foreground GPS tracking service** (nhánh `feat/gps-tracking-service`, build + lint + test pass, CHƯA chạy trên device):
  - `tracking/LocationTrackingService` (`@AndroidEntryPoint Service`): action START/PAUSE/RESUME/STOP; notification liên tục (channel `tracking`, IMPORTANCE_LOW) có nút Tạm dừng/Tiếp tục/Kết thúc; partial wake lock khi đang ghi; `START_NOT_STICKY`; `foregroundServiceType=location`.
  - Tạo `ActivityEntity` (userId cố định `"local-user"`, type `RUNNING`) lúc START, ghi đè aggregate lúc STOP (`finalizeActivity` trong `NonCancellable`).
  - `tracking/TrackingSession` (`@Singleton`, `StateFlow<TrackingState>`) — cầu nối state Service ↔ UI, sống độc lập vòng đời Service.
  - `tracking/LocationClient` (interface) + `FusedLocationClient` (callbackFlow quanh `FusedLocationProviderClient`, HIGH_ACCURACY, interval 3s).
  - `di/LocationModule` + `LocationBindModule`. `core/PermissionExt.kt`, `core/Format.kt`.
  - Aggregate tính tăng dần từ điểm đã qua `GpsTrackFilter`: distance (haversine), movingTime (speed ≥ 0.6 m/s), elevation gain/loss (ngưỡng 1m), pace theo moving time.
  - UI: `ui/tracking/TrackingScreen` + `TrackingViewModel` — xin quyền (fine/coarse + POST_NOTIFICATIONS), nút Start/Pause/Resume/Stop, số liệu live, lịch sử activity. **Thay hẳn debug harness cũ** (đã xoá `debug/`).
  - Test: `TrackingStateTest` (pace, formatClock).
- **Đã có màn chi tiết activity + map polyline** (nhánh `feat/activity-detail-map`, build + lint + test pass; map cần API key mới hiện):
  - `androidx.navigation:navigation-compose` — `ui/RunTrackerNavHost` với route `tracking` và `detail/{activityId}`. `MainActivity` giờ render NavHost.
  - `ui/detail/ActivityDetailViewModel` (`SavedStateHandle` lấy `activityId`, `combine(observeActivity, observeRoutePoints)`), `ActivityDetailScreen` (TopAppBar + back, map chiếm nửa trên, panel số liệu cuộn được), `RouteMap` (maps-compose `GoogleMap` + `Polyline` + marker đầu/cuối, camera fit bounds sau `onMapLoaded`).
  - `TrackingScreen`: item lịch sử bấm được → điều hướng sang chi tiết.
  - **Google Maps API key**: đọc `MAPS_API_KEY` từ `local.properties` trong `app/build.gradle.kts` → `manifestPlaceholders` → `<meta-data com.google.android.geo.API_KEY>`. Thiếu key vẫn build, chỉ map trống. Thêm `INTERNET` + `ACCESS_NETWORK_STATE` vào manifest.
  - `core/Format.kt` thêm `formatDistanceKm`.
  - **Chưa làm**: lap tự động theo km, biểu đồ pace/elevation, khôi phục sau khi OS kill service, `ACCESS_BACKGROUND_LOCATION`, whitelist battery Xiaomi/Oppo, HR từ Health Connect.
- **Chưa có** repository cho các entity khác, chưa có domain use-case.
- Sai lệch nhỏ so với bản thiết kế gốc (có chủ đích, đã cập nhật lại `docs/database_design.md`):
  - Thêm `Index` cho `route_waypoints.routeId` (tránh warning FK của Room).
  - Thêm cột `sex: String?` ("MALE"/"FEMALE", null -> dùng nhánh công thức nam) vào bảng `users` để phục vụ hệ số TRIMP nam/nữ.

## Việc cần làm tiếp theo

1. ~~Thêm dependencies vào Gradle~~ ✅
2. ~~Tạo các Room Entity + DAO theo schema~~ ✅
3. ~~Activity repository layer (domain model + mapper + GPS filter)~~ ✅ (`feat/activity-repository`)
4. ~~Foreground Service ghi GPS + notification liên tục~~ ✅ (`feat/gps-tracking-service`) — cần test trên device thật
5. ~~Màn chi tiết activity + map polyline~~ ✅ (`feat/activity-detail-map`) — cần `MAPS_API_KEY` trong `local.properties` để map hiện
6. Lap tự động theo km + biểu đồ pace/elevation trong màn chi tiết
7. Khôi phục tracking sau khi service bị kill; hướng dẫn whitelist battery (Xiaomi/Oppo)
8. Repository cho `UserEntity` (cần cho tính TRIMP/zone) + các entity còn lại khi tới việc dùng
9. Sau khi Phase 1 chạy ổn: module TRIMP + job WorkManager tính CTL/ATL/TSB (Phase 2) — nhớ wire Hilt WorkerFactory