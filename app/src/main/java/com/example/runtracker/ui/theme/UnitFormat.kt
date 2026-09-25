package com.example.runtracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.runtracker.data.settings.UnitSystem
import kotlin.math.roundToInt

/**
 * Đơn vị hiển thị hiện tại — provide 1 lần ở gốc (`RunTrackerNavHost`) từ `AppSettingsStore`.
 * Mặc định `METRIC` khi không có provider nào bao ngoài (ví dụ preview).
 */
val LocalUnitSystem = staticCompositionLocalOf { UnitSystem.METRIC }

private const val KM_PER_MILE = 1.609344

/**
 * Mét -> "x.xx km" hoặc "x.xx mi" tuỳ [LocalUnitSystem]. Bản `@Composable` của
 * `core.formatDistanceKm` — dùng ở màn hình cần tôn trọng đơn vị người dùng chọn trong Cài đặt;
 * `core.formatDistanceKm` (luôn km) vẫn giữ nguyên cho code ngoài Compose (domain/use-case).
 */
@Composable
fun formatDistanceUnit(meters: Double): String = when (LocalUnitSystem.current) {
    UnitSystem.METRIC -> "%.2f km".format(meters / 1000.0)
    UnitSystem.IMPERIAL -> "%.2f mi".format(meters / 1000.0 / KM_PER_MILE)
}

/** Pace giây/km -> "m:ss /km" hoặc "m:ss /mi" tuỳ [LocalUnitSystem]; "--:--" khi chưa có dữ liệu. */
@Composable
fun formatPaceUnit(secPerKm: Double): String {
    val unit = LocalUnitSystem.current
    val label = if (unit == UnitSystem.METRIC) "/km" else "/mi"
    if (secPerKm <= 0.0) return "--:-- $label"
    val secPerUnit = if (unit == UnitSystem.METRIC) secPerKm else secPerKm * KM_PER_MILE
    val total = secPerUnit.roundToInt()
    return "%d:%02d %s".format(total / 60, total % 60, label)
}

/** Km/h -> "x.x km/h" hoặc "x.x mph" tuỳ [LocalUnitSystem]. */
@Composable
fun formatSpeedUnit(kmh: Double): String = when (LocalUnitSystem.current) {
    UnitSystem.METRIC -> "%.1f km/h".format(kmh)
    UnitSystem.IMPERIAL -> "%.1f mph".format(kmh / KM_PER_MILE)
}
