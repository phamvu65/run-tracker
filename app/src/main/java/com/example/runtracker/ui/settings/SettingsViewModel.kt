package com.example.runtracker.ui.settings

import androidx.lifecycle.ViewModel
import com.example.runtracker.data.settings.AppSettingsStore
import com.example.runtracker.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: AppSettingsStore,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsStore.themeMode

    fun setThemeMode(mode: ThemeMode) = settingsStore.setThemeMode(mode)
}
