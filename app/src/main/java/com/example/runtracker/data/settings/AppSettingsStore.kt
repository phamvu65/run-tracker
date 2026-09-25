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

/** Đơn vị hiển thị quãng đường/pace — dữ liệu/tính toán trong DB luôn giữ mét, chỉ đổi tầng hiển thị. */
enum class UnitSystem { METRIC, IMPERIAL }

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

    /** Tự tạm dừng khi đứng yên quá lâu lúc đang ghi — mặc định TẮT, không đổi hành vi người dùng cũ. */
    private val _autoPauseEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_PAUSE, false))
    val autoPauseEnabled: StateFlow<Boolean> = _autoPauseEnabled.asStateFlow()

    fun setAutoPauseEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_PAUSE, enabled) }
        _autoPauseEnabled.value = enabled
    }

    private val _unitSystem = MutableStateFlow(readUnitSystem())
    val unitSystem: StateFlow<UnitSystem> = _unitSystem.asStateFlow()

    fun setUnitSystem(unit: UnitSystem) {
        prefs.edit { putString(KEY_UNIT_SYSTEM, unit.name) }
        _unitSystem.value = unit
    }

    private fun readUnitSystem(): UnitSystem {
        val raw = prefs.getString(KEY_UNIT_SYSTEM, null) ?: return UnitSystem.METRIC
        return runCatching { UnitSystem.valueOf(raw) }.getOrDefault(UnitSystem.METRIC)
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_AUTO_PAUSE = "auto_pause_enabled"
        const val KEY_UNIT_SYSTEM = "unit_system"
    }
}
