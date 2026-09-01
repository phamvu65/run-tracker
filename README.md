# run-tracker

App Android chạy bộ kiểu Strava (Kotlin, Compose). Xem `CLAUDE.md` cho ngữ cảnh dự án
và `docs/database_design.md` cho schema.

## Chạy local

1. Mở bằng Android Studio, để nó Gradle sync.
2. Thêm Google Maps API key vào `local.properties` (file này đã gitignore):

   ```properties
   MAPS_API_KEY=your_key_here
   ```

   Lấy key tại Google Cloud Console → APIs & Services → Credentials, bật
   **Maps SDK for Android**. Thiếu key thì app vẫn chạy, chỉ có bản đồ ở màn
   chi tiết buổi tập hiện trống.
3. Cắm thiết bị thật (khuyến khích, cần GPS) và Run.
