# RunTracker 🏃‍♂️💨

**RunTracker** là ứng dụng theo dõi và phân tích chạy bộ miễn phí dành cho Android, được thiết kế theo phong cách Strava / Nike Run Club. Ứng dụng được viết 100% bằng **Kotlin**, **Jetpack Compose** và kiến trúc **Clean Architecture + MVVM**, mở khóa **miễn phí** toàn bộ các tính năng cao cấp (Premium) vốn bị tính phí trên các nền tảng khác.

---

## 🌟 Điểm nổi bật & Tính năng chính

### 1. Tracking & Ghi nhận hoạt động GPS (`Core Tracking`)
- **Theo dõi thời gian thực**: Ghi nhận quãng đường, nhịp độ (pace), thời gian di chuyển, độ cao lên/xuống, lượng calo tiêu thụ và bước chân (`Sensor.TYPE_STEP_COUNTER`).
- **Xử lý GPS thông minh**: Bộ lọc nhiễu GPS (`GpsTrackFilter`) loại bỏ các điểm sai số lớn (>20m) hoặc tốc độ bất thường. Tự động tạm dừng (auto-pause) khi dừng lại.
- **Xem lại hành trình (Route Playback)**: Mô phỏng người chạy di chuyển mượt mà trên bản đồ theo đúng tỷ lệ thời gian thực.
- **Đọc chỉ đường bằng giọng nói (Voice Guidance)**: Phát âm báo rẽ/chệch tuyến khi chạy theo lộ trình có sẵn.
- **Xuất file GPX**: Cho phép xuất dữ liệu buổi chạy ra file `.gpx` chuẩn để chia sẻ hoặc đồng bộ sang Strava / Garmin.

### 2. Phân tích Thể lực & Phục hồi (`Fitness & Freshness`)
- **Mô hình Impulse-Response (Banister)**:
  - **Fitness ($CTL$)**: Đo lường thể lực tích lũy 42 ngày.
  - **Fatigue ($ATL$)**: Đo lường sự mệt mỏi tích lũy 7 ngày.
  - **Form ($TSB$)**: Chỉ số điểm rơi phong độ ($TSB = CTL - ATL$).
- **Điểm tải TRIMP sinh học**: Tính toán độ nặng buổi tập dựa trên dải nhịp tim thực tế có phân biệt giới tính Nam/Nữ, hoặc tự động fallback sang điểm cảm giác gắng sức (RPE 1–10).
- **Gợi ý tập luyện hàng ngày (Daily Suggestion)**: Tự động phân tích chỉ số Form ($TSB$) để đưa ra lời khuyên hôm nay nên chạy nhẹ Z2, chạy bài chất lượng hay nghỉ ngơi hoàn toàn.

### 3. Thành tích & Kỷ lục cá nhân (`Personal Records / Best Efforts`)
- **Thuật toán Cửa sổ trượt ($O(n)$ Sliding Window)**: Quét vệt GPS để tìm đoạn đường liên tục nhanh nhất cho các mốc cự ly chuẩn: **1 km, 5 km, 10 km, Bán Marathon (21.1 km), Marathon (42.2 km)**.
- **Kỷ lục toàn buổi (Whole Activity PRs)**: Tự động ghi nhận Pace nhanh nhất, Quãng đường dài nhất, Độ cao leo nhiều nhất.
- **Hệ thống Huy hiệu (Badges) & Bảng xếp hạng Top-3**: Lưu giữ mốc lịch sử thành tích cá nhân.

### 4. Lên lịch tập thông minh (`Training Plan Generator`)
- **Cá nhân hóa tự động**: Sinh giáo án chạy nhiều tuần chuẩn bị cho giải đấu dựa trên ngày thi đấu, cự ly ($5\text{K}, 10\text{K}, 21\text{K}, 42\text{K}$) và thể lực thực tế ($CTL$).
- **Áp dụng Quy tắc 10% (Progressive Overload)**: Khối lượng km tuần tăng tối đa $8–10\%$/tuần, chèn tuần phục hồi (giảm $20\%$ km) mỗi 4 tuần.
- **Tùy biến lịch trình**: Lựa chọn 3, 4 hoặc 5 buổi tập/tuần, chọn ngày chạy dài (Thứ 7 hoặc Chủ Nhật).
- **Tính Target Pace**: Tự động quy đổi thời gian thi đấu mục tiêu thành Pace chuẩn cho từng bài `EASY`, `TEMPO`, `INTERVAL`.

### 5. Dựng đường chạy vẽ tay (`Smart Route Builder`)
- **Chuyển nét vẽ tay thành đường chạy**: Tự động chọn các mốc từ nét vẽ tay và dùng OSRM để định tuyến theo đường đi bộ trên bản đồ OpenStreetMap.
- **Nối phân đoạn mềm dẻo (`gapPolylines`)**: Nếu dữ liệu bản đồ bị thiếu ở một đoạn, hệ thống giữ nguyên các đoạn bám đường hợp lệ và nối tạm bằng nét đứt, cho phép người dùng kéo/chỉnh mốc dễ dàng.

### 6. Thử thách & Segments (`Challenges & Leaderboards`)
- **Segments**: Tạo đoạn đường thử thách ven hồ/công viên, tự động so sánh vệt chạy và hiển thị bảng xếp hạng không giới hạn top 10.
- **Group Challenges**: Các thử thách phong trào theo tích lũy quãng đường, số buổi hoặc độ cao.

---

## 🛠 Tech Stack & Architecture

- **Ngôn ngữ**: 100% Kotlin (Coroutines + Flow).
- **UI Framework**: Jetpack Compose + Material 3 Theme.
- **Bảng màu chủ đạo**:
  - Background: Trắng ngà (`#FAF9F6`).
  - Primary Accent: Xanh Teal (`#4EB09B`).
  - Secondary Accent: Coral (`#F28076`), Hồng Pastel (`#FFB6AF`), Kem (`#FAE0C7`), Cam Đào (`#FBC193`).
  - Surface & Text: Card Trắng (`#FFFFFF`), Chữ Xám Đậm (`#1E293B`).
- **Kiến trúc**: MVVM + Clean Architecture (`data` / `domain` / `ui`).
- **Database**: Room Database (SQLite) v7 với 18 bảng dữ liệu chuẩn hóa, hỗ trợ Auto Migration.
- **Bản đồ & GPS**: OpenStreetMap qua **osmdroid** (100% miễn phí, không cần API Key hay thẻ tín dụng Google Cloud) + `FusedLocationProviderClient` trong Foreground Service.
- **Routing**: OSRM API (`routing.openstreetmap.de`).
- **Dependency Injection**: Hilt (Dagger Hilt).
- **Tác vụ ngầm**: WorkManager (job tính CTL/ATL/TSB đệ quy hàng đêm).

---

## 📁 Cấu trúc thư mục dự án

```text
app/src/main/java/com/example/runtracker/
├── core/               # Helper formats, FeatureFlags, Permissions, BatteryOptimization
├── data/               # Room DB (Entities, DAOs, Migrations), Repositories, APIs, Mappers
├── di/                 # Hilt Modules (DatabaseModule, NetworkModule, LocationModule...)
├── domain/             # Core Business Logic & Algorithms (JVM-pure):
│   ├── model/          # Activity, Route, TrainingPlan, FitnessFreshnessSnapshot...
│   ├── repository/     # Domain interfaces
│   ├── tracking/       # GeoMath, GpsTrackFilter, BestEffortCalculator, TrimpCalculator...
│   ├── training/       # TrainingPlanGenerator, FitnessFreshnessCalculator...
│   └── usecase/        # UseCases (BuildRouteUseCase, DetectPersonalRecordsUseCase...)
├── tracking/           # LocationTrackingService, TrackingSession, BeaconController
├── ui/                 # Jetpack Compose UI Screens & ViewModels:
│   ├── activities/     # Danh sách hoạt động (History)
│   ├── challenges/     # Thử thách phong trào
│   ├── detail/         # Chi tiết buổi tập & Replay Map
│   ├── fitness/        # Biểu đồ Fitness & Freshness
│   ├── plan/           # Kế hoạch tập luyện (Training Plan)
│   ├── profile/        # Hồ sơ cá nhân & Thống kê
│   ├── records/        # Thành tích & Kỷ kỷ lục cá nhân (PRs)
│   ├── routes/         # Dựng lộ trình (Route Builder)
│   ├── theme/          # Color (#4EB09B Teal), Type, Shape, Theme (Material 3)
│   └── tracking/       # Màn hình Ghi GPS chính (Active Tracking)
└── work/               # WorkManager workers (CalculateFitnessFreshnessWorker)
```

---

## 🚀 Hướng dẫn Chạy ứng dụng (Local Setup)

### Yêu cầu môi trường
- **Android Studio**: Ladybug / 2024.2.1 trở lên.
- **JDK**: Java 17 (yêu cầu bởi Android Gradle Plugin hiện tại).
- **Hệ điều hành**: Windows / macOS / Linux.
- **Thiết bị thử nghiệm**: Máy thật Android 8.0 (API 26) trở lên có bật GPS.

### Các bước thực hiện
1. Clone dự án và mở bằng Android Studio:
   ```bash
   git clone https://github.com/phamvu65/run-tracker.git
   ```
2. Chờ Android Studio thực hiện **Gradle Sync** tải các phụ thuộc.
3. Bản đồ sử dụng `osmdroid` và dịch vụ routing `OSRM` hoàn toàn miễn phí, **không cần cấu hình Google Maps API Key**.
4. Kết nối thiết bị Android thật, bật **Tùy chọn cho nhà phát triển** $\rightarrow$ **Sửa lỗi USB (USB Debugging)**.
5. Nhấn **Run (Shift + F10)** để cài đặt ứng dụng.

---

## 🧪 Lệnh Kiểm tra trên Windows (Powershell)

Trước khi đóng gói hoặc commit code, chạy bộ lệnh kiểm tra tự động từ thư mục gốc project:

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

> **Ghi chú về kiểm thử:**
> - Lần kiểm tra gần nhất: **215 unit tests passed; 1 test phụ thuộc mạng được bỏ qua**.
> - Cấu hình Android: `compileSdk = 35`, `targetSdk = 35` (Android 15).

---

## 📄 Giấy phép & Tài liệu tham khảo

- **Giấy phép bản đồ**: © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors, Tile OSM.de / Esri World Imagery.
- **Mô hình thể lực**: Banister EWMA Impulse-Response Model ($CTL / ATL / TSB$).
- **Công thức dự đoán**: Riegel Performance Prediction Formula ($T_2 = T_1 \times (D_2 / D_1)^{1.06}$).
