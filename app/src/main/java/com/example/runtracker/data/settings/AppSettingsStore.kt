package com.example.runtracker.data.settings

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Chế độ giao diện. `SYSTEM` theo `isSystemInDarkTheme()`; mặc định `DARK` — giữ đúng hành vi cũ. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Lưu bền các cài đặt chung của app (không gắn với 1 activity/dữ liệu cụ thể) — cùng khuôn với
 * `TrainingGoalStore`/`BleHeartRateStore`: `SharedPreferences` riêng, expose `StateFlow` cho UI.
 */
@Singleton
class AppSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        _themeMode.value = mode
    }

    private fun readThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.DARK
        return runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.DARK)
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
    }
}
