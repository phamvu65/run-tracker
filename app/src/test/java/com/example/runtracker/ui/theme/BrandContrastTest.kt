package com.example.runtracker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards against unreadable text when either palette or a gradient endpoint is adjusted. */
class BrandContrastTest {
    @Test fun semanticTextMeetsContrastInBothModes() {
        val pairs = listOf(
            DarkOnBackground to DarkBackground, DarkOnSurface to DarkSurface,
            DarkOnSurfaceVariant to DarkSurfaceContainerHigh, DarkPrimary to DarkSurface,
            DarkOnPrimary to DarkPrimary, DarkOnPrimaryContainer to DarkPrimaryContainer,
            DarkOnErrorContainer to DarkErrorContainer,
            LightOnBackground to LightBackground, LightOnSurface to LightSurface,
            LightOnSurfaceVariant to LightSurfaceContainerHigh, LightPrimary to LightSurface,
            LightOnPrimary to LightPrimary, LightOnPrimaryContainer to LightPrimaryContainer,
            LightOnErrorContainer to LightErrorContainer,
        )
        pairs.forEach { (text, background) -> assertContrast(text, background, 4.5f) }
    }

    @Test fun ctaAndSummaryLabelsStayReadableAcrossEntireGradient() {
        for (palette in listOf(DarkPremiumPalette, LightPremiumPalette)) {
            for (step in 0..100) {
                val t = step / 100f
                val cta = if (t <= 0.5f) blend(palette.accentStart, palette.accentMiddle, t * 2)
                    else blend(palette.accentMiddle, palette.accentEnd, (t - 0.5f) * 2)
                assertContrast(palette.onAccent, cta, 4.5f)
                val summary = blend(palette.summaryStart, palette.summaryEnd, t)
                assertContrast(palette.summaryContent, summary, 4.5f)
                assertContrast(palette.summarySecondary, summary, 4.5f)
            }
        }
    }

    @Test fun chartStrokesRemainDistinctFromBothSurfaceLevels() {
        for ((palette, surfaces) in listOf(
            DarkPremiumPalette to listOf(DarkSurface, DarkSurfaceContainerHigh),
            LightPremiumPalette to listOf(LightSurface, LightSurfaceContainerHigh),
        )) {
            for (step in 0..100) {
                val color = blend(palette.progressStart, palette.progressEnd, step / 100f)
                surfaces.forEach { assertContrast(color, it, 3f) }
            }
        }
    }

    private fun blend(a: Color, b: Color, t: Float) = Color(
        a.red + (b.red - a.red) * t,
        a.green + (b.green - a.green) * t,
        a.blue + (b.blue - a.blue) * t,
    )

    private fun assertContrast(foreground: Color, background: Color, minimum: Float) {
        val a = foreground.luminance()
        val b = background.luminance()
        val ratio = (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
        assertTrue("Contrast $ratio < $minimum for $foreground on $background", ratio >= minimum)
    }
}
