package com.example.runtracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class PremiumPalette(
    val summaryStart: Color,
    val summaryEnd: Color,
    val summaryContent: Color,
    val summarySecondary: Color,
    val progressStart: Color = BrandPink,
    val progressEnd: Color = BrandHighlight,
    val accentStart: Color = BrandPink,
    val accentMiddle: Color = Color(0xFFFF67B7),
    val accentEnd: Color = Color(0xFFED79D0),
    val onAccent: Color = BrandAccentInk,
    val glow: Color = BrandPink,
) {
    val accentGradient: Brush get() = Brush.linearGradient(listOf(accentStart, accentMiddle, accentEnd))
    val summaryGradient: Brush get() = Brush.linearGradient(listOf(summaryStart, summaryEnd))
    val progressGradient: Brush get() = Brush.linearGradient(listOf(progressStart, progressEnd))
}

val DarkPremiumPalette = PremiumPalette(
    summaryStart = Color(0xFF392130), summaryEnd = Color(0xFF211D2C),
    summaryContent = DarkOnSurface, summarySecondary = Color(0xFFD3BFCF),
)
val LightPremiumPalette = PremiumPalette(
    summaryStart = Color(0xFFFFE7F2), summaryEnd = Color(0xFFF0E7F7),
    summaryContent = Color(0xFF35152B), summarySecondary = Color(0xFF745069),
    progressStart = LightPrimary, progressEnd = LightTertiary,
)
val LocalPremiumPalette = staticCompositionLocalOf { DarkPremiumPalette }

/** Soft radial lighting, drawn with a cached brush; no bitmap blur or API 31 dependency. */
fun Modifier.ambientGlow(color: Color, strength: Float = 0.10f): Modifier = drawWithCache {
    val center = Offset(size.width * 0.5f, size.height * 0.5f)
    val radius = maxOf(size.width, size.height) * 0.62f
    val brush = Brush.radialGradient(listOf(color.copy(alpha = strength), Color.Transparent), center, radius.coerceAtLeast(1f))
    onDrawBehind { drawCircle(brush, radius, center) }
}
