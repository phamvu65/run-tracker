package com.example.runtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.data.settings.ThemeMode
import com.example.runtracker.ui.RunTrackerNavHost
import com.example.runtracker.ui.settings.SettingsViewModel
import com.example.runtracker.ui.theme.RunTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // SettingsViewModel không gắn 1 màn cụ thể — dùng chung ở gốc để đọc theme đã lưu.
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            RunTrackerTheme(darkTheme = darkTheme) {
                RunTrackerNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
