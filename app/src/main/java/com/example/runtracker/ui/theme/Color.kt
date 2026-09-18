package com.example.runtracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Bảng màu kiểu GoRun: xanh neon hành động #B9FF8D, nền đen tuyền, thẻ xám than,
 * nhãn xám, đường kẻ mảnh. Tối giản — độ tương phản đến từ nền vs thẻ, không từ màu.
 */

// ---- Light ----
val LightPrimary = Color(0xFF3F7D20)       // xanh đậm hơn Primary-200 GoRun để đủ tương phản trên nền trắng
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD7F4C0)
val LightOnPrimaryContainer = Color(0xFF123404)

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

// ---- Dark (kiểu GoRun: nền đen #0D0D12, thẻ xám than #2E2E2F, xanh neon thương hiệu #B9FF8D) ----
val DarkPrimary = Color(0xFFB9FF8D)        // xanh neon GoRun (Primary-200) — nổi bật trên nền đen
val DarkOnPrimary = Color(0xFF0D0D12)
val DarkPrimaryContainer = Color(0xFF1F3316)
val DarkOnPrimaryContainer = Color(0xFFC7FFA4)

val DarkSecondary = Color(0xFFE6E6E9)
val DarkOnSecondary = Color(0xFF1B1B1D)
val DarkSecondaryContainer = Color(0xFF2A2A2D)
val DarkOnSecondaryContainer = Color(0xFFEDEDF0)

val DarkTertiary = Color(0xFF9EE1D4)       // teal (Success) — tách biệt khỏi primary cho badge thành tích/PR
val DarkOnTertiary = Color(0xFF00332C)
val DarkTertiaryContainer = Color(0xFF12332E)
val DarkOnTertiaryContainer = Color(0xFF9EE1D4)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackground = Color(0xFF0D0D12)
val DarkOnBackground = Color(0xFFF8FAFB)
val DarkSurface = Color(0xFF17161C)
val DarkOnSurface = Color(0xFFF8FAFB)
val DarkSurfaceVariant = Color(0xFF221F29)
val DarkOnSurfaceVariant = Color(0xFFA4ACB9)
val DarkOutline = Color(0xFF3A3A3E)
val DarkOutlineVariant = Color(0xFF2C2C2F)
val DarkSurfaceContainerLowest = Color(0xFF0D0D12)
val DarkSurfaceContainerLow = Color(0xFF1C1B22)
val DarkSurfaceContainer = Color(0xFF221F29)
val DarkSurfaceContainerHigh = Color(0xFF2E2E2F)
val DarkSurfaceContainerHighest = Color(0xFF36394A)

// ---- Accent phụ cho biểu đồ / trạng thái ----
val AccentPositive = Color(0xFFB9FF8D)
val AccentWarning = Color(0xFFFFBE4C)
val AccentInfo = Color(0xFF0277BD)
