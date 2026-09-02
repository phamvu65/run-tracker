package com.example.runtracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Bảng màu kiểu Strava: cam hành động #FC5200, nền xám nhạt mát, thẻ trắng, chữ gần đen,
 * nhãn xám, đường kẻ mảnh. Tối giản — độ tương phản đến từ nền vs thẻ, không từ màu.
 */

// ---- Light ----
val LightPrimary = Color(0xFFEE4B00)       // đậm hơn #FC5200 để chữ trắng đủ tương phản
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFFE3D6)
val LightOnPrimaryContainer = Color(0xFF3D1400)

val LightSecondary = Color(0xFF2B2B2E)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE6E6EA)
val LightOnSecondaryContainer = Color(0xFF1B1B1D)

val LightTertiary = Color(0xFF00668B)      // xanh cho link/thông tin
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFC5E7FF)
val LightOnTertiaryContainer = Color(0xFF001E2C)

val LightError = Color(0xFFC0271F)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightBackground = Color(0xFFF2F2F5)
val LightOnBackground = Color(0xFF1B1B1D)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1B1B1D)
val LightSurfaceVariant = Color(0xFFECECF0)
val LightOnSurfaceVariant = Color(0xFF6B6B72)
val LightOutline = Color(0xFFC9C9CF)
val LightOutlineVariant = Color(0xFFE6E6EA)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFAFAFC)
val LightSurfaceContainer = Color(0xFFFFFFFF)
val LightSurfaceContainerHigh = Color(0xFFF6F6F8)
val LightSurfaceContainerHighest = Color(0xFFEFEFF2)

// ---- Dark (kiểu Strava: nền đen tuyền, thẻ xám than, cam thương hiệu #FC5200) ----
val DarkPrimary = Color(0xFFFC5200)        // cam Strava nguyên bản — đủ nổi trên nền đen
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Color(0xFF3A1300)
val DarkOnPrimaryContainer = Color(0xFFFFDCCB)

val DarkSecondary = Color(0xFFE6E6E9)
val DarkOnSecondary = Color(0xFF1B1B1D)
val DarkSecondaryContainer = Color(0xFF2A2A2D)
val DarkOnSecondaryContainer = Color(0xFFEDEDF0)

val DarkTertiary = Color(0xFF7DD0FF)
val DarkOnTertiary = Color(0xFF00344A)
val DarkTertiaryContainer = Color(0xFF15361F)   // xanh rêu đậm cho khối "thành tích PR"
val DarkOnTertiaryContainer = Color(0xFFB6E3B9)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackground = Color(0xFF000000)
val DarkOnBackground = Color(0xFFF4F4F5)
val DarkSurface = Color(0xFF1C1C1E)
val DarkOnSurface = Color(0xFFF4F4F5)
val DarkSurfaceVariant = Color(0xFF262629)
val DarkOnSurfaceVariant = Color(0xFF9A9AA0)
val DarkOutline = Color(0xFF3A3A3E)
val DarkOutlineVariant = Color(0xFF2C2C2F)
val DarkSurfaceContainerLowest = Color(0xFF000000)
val DarkSurfaceContainerLow = Color(0xFF131315)
val DarkSurfaceContainer = Color(0xFF1C1C1E)
val DarkSurfaceContainerHigh = Color(0xFF242427)
val DarkSurfaceContainerHighest = Color(0xFF2E2E31)

// ---- Accent phụ cho biểu đồ / trạng thái ----
val AccentPositive = Color(0xFF2E7D32)
val AccentWarning = Color(0xFFEF6C00)
val AccentInfo = Color(0xFF0277BD)
