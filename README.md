# RunTracker

App Android chạy bộ kiểu Strava, viết bằng Kotlin và Jetpack Compose.

## Tài liệu dự án

- `AGENTS.md`: hướng dẫn và ngữ cảnh hiện tại cho Codex (file riêng trong máy).
- `CLAUDE.md`: lịch sử triển khai từ quá trình làm việc với Claude (giữ lại để tra cứu).
- `docs/database_design.md`: tài liệu thiết kế database; đối chiếu với entity và migration hiện tại.

## Chạy local

1. Mở project bằng Android Studio và chờ Gradle sync.
2. Bản đồ dùng osmdroid/OpenStreetMap, routing dùng OSRM; không cần Google Maps API key. Cần mạng để tải bản đồ và gọi dịch vụ routing.
3. Kết nối thiết bị thật có GPS, chọn app và Run; cấp các quyền được yêu cầu cho tính năng sử dụng.

## Kiểm tra trên Windows

Chạy từ thư mục gốc project bằng PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Beacon hiện chỉ dùng loopback trong bộ nhớ cùng tiến trình; chia sẻ vị trí giữa các thiết bị cần backend.

Tích hợp nhịp tim Health Connect/BLE đang tạm ẩn bằng
`FeatureFlags.HEART_RATE_INTEGRATION = false`. App không kết nối đai đã lưu hoặc
xin quyền nhập nhịp tim khi tính năng tắt. Dữ liệu cũ được giữ nguyên; người dùng
vẫn có thể nhập mức gắng sức RPE sau buổi tập để tính tải tập luyện.
