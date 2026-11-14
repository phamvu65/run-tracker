package com.example.runtracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.example.runtracker.ui.theme.LocalReducedMotion

/** One interaction language for toolbar actions; Material retains the 48dp touch target. */
@Composable
fun AthleticIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val reduced = LocalReducedMotion.current
    val duration = if (reduced) 0 else 110
    val scale by animateFloatAsState(if (pressed && enabled && !reduced) 0.96f else 1f,
        tween(duration), label = "icon-press")
    val color by animateColorAsState(
        if (pressed && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        tween(duration), label = "icon-color")
    IconButton(onClick = onClick, enabled = enabled, interactionSource = source,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        colors = IconButtonDefaults.iconButtonColors(contentColor = color,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)),
        content = content)
}
