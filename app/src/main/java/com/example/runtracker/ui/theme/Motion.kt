package com.example.runtracker.ui.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/** Mirrors Android's Remove animations setting. Also overridable for previews and UI tests. */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
internal fun rememberSystemReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun read() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    val reduced = remember(resolver) { mutableStateOf(read()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { reduced.value = read() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced.value
}

@Composable
fun animatedMetricFraction(target: Float): State<Float> {
    val reduced = LocalReducedMotion.current
    val value = remember { Animatable(if (reduced) target else 0f) }
    LaunchedEffect(target, reduced) {
        if (reduced) value.snapTo(target) else value.animateTo(target, tween(360))
    }
    return value.asState()
}

@Composable
fun Modifier.metricReveal(key: Any): Modifier {
    val reduced = LocalReducedMotion.current
    val alpha = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(key, reduced) {
        if (reduced) alpha.snapTo(1f) else alpha.animateTo(1f, tween(280))
    }
    return graphicsLayer { this.alpha = if (reduced) 1f else alpha.value }
}

@Composable
fun runningPulseAlpha(running: Boolean): Float {
    if (!running || LocalReducedMotion.current) return 1f
    val transition = rememberInfiniteTransition(label = "running-state")
    val alpha by transition.animateFloat(0.55f, 1f,
        infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "running-indicator")
    return alpha
}
