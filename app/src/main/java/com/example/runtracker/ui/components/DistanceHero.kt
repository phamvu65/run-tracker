package com.example.runtracker.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.example.runtracker.ui.theme.AthleticType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.runtracker.ui.theme.formatDistanceUnit

/** One distance value, with a separate unit and a size that also fits long runs. */
@Composable
fun DistanceHero(
    meters: Double,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    centered: Boolean = false,
    unitColor: Color = color.copy(alpha = 0.7f),
) {
    val formatted = formatDistanceUnit(meters)
    val number = formatted.substringBeforeLast(' ')
    val unit = formatted.substringAfterLast(' ')
    val alignment = if (centered) TextAlign.Center else TextAlign.Start
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val size = minOf(112f, maxWidth.value / (number.length * 0.53f * LocalDensity.current.fontScale)).sp
        Column {
            Text(
                number,
                modifier = Modifier.fillMaxWidth(),
                style = AthleticType.distance,
                fontSize = size,
                lineHeight = size * 1.1f,
                color = color,
                textAlign = alignment,
            )
            Text(
                unit.uppercase(),
                modifier = Modifier.fillMaxWidth(),
                style = AthleticType.unit,
                color = unitColor,
                textAlign = alignment,
            )
        }
    }
}
