package com.example.runtracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import com.example.runtracker.ui.theme.LocalPremiumPalette
import com.example.runtracker.ui.theme.LocalReducedMotion
import com.example.runtracker.ui.theme.ambientGlow

/** Shared primary CTA; Material Button retains ripple, keyboard, touch target and semantics. */
@Composable
fun PremiumButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val brand = LocalPremiumPalette.current
    val reduced = LocalReducedMotion.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled && !reduced) 0.98f else 1f,
        tween(if (reduced) 0 else 110), label = "primary-press")
    val buttonColors = colors ?: ButtonDefaults.buttonColors(contentColor = brand.onAccent)
    val stops = listOf(brand.accentStart, brand.accentMiddle, brand.accentEnd)
        .map { if (pressed && enabled) lerp(it, Color.White, 0.07f) else it }
    val brush = if (enabled) Brush.linearGradient(stops)
        else Brush.linearGradient(listOf(buttonColors.disabledContainerColor, buttonColors.disabledContainerColor))
    Button(
        onClick = onClick, enabled = enabled, shape = shape,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            .ambientGlow(brand.glow, if (enabled) 0.12f else 0f)
            .background(brush, shape),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,
            contentColor = buttonColors.contentColor, disabledContainerColor = Color.Transparent,
            disabledContentColor = buttonColors.disabledContentColor),
        elevation = null, contentPadding = contentPadding, interactionSource = source, content = content,
    )
}
