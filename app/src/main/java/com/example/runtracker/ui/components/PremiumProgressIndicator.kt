package com.example.runtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.runtracker.ui.theme.LocalPremiumPalette
import com.example.runtracker.ui.theme.LocalReducedMotion
import com.example.runtracker.ui.theme.animatedMetricFraction

@Composable
fun PremiumProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val target = progress().let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0f }
    val animated by animatedMetricFraction(target)
    val visible = if (LocalReducedMotion.current) target else animated
    val brush = LocalPremiumPalette.current.progressGradient
    Canvas(modifier.height(6.dp).semantics { progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) }) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(trackColor, cornerRadius = radius)
        val width = size.width * visible
        if (width > 0f) drawRoundRect(brush,
            topLeft = Offset(if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f, 0f),
            size = Size(width, size.height), cornerRadius = radius)
    }
}
