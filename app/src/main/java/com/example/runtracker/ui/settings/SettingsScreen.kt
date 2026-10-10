package com.example.runtracker.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.hasNotificationPermission
import com.example.runtracker.data.settings.ThemeMode
import com.example.runtracker.data.settings.UnitSystem
import com.example.runtracker.ui.components.FlatCard
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val autoPauseEnabled by viewModel.autoPauseEnabled.collectAsState()
    val unitSystem by viewModel.unitSystem.collectAsState()
    val trainingReminderEnabled by viewModel.trainingReminderEnabled.collectAsState()

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.setTrainingReminderEnabled(true) }

    fun onToggleReminder(enabled: Boolean) {
        if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.hasNotificationPermission()) {
            viewModel.setTrainingReminderEnabled(enabled)
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            com.example.runtracker.ui.components.CompactTopHeader(
                title = "Cài đặt",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SectionHeader("Giao diện")
            FlatCard {
                Text("Chế độ hiển thị", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(Spacing.xs))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    ThemeChip("Tối", ThemeMode.DARK, themeMode, viewModel::setThemeMode)
                    ThemeChip("Sáng", ThemeMode.LIGHT, themeMode, viewModel::setThemeMode)
                    ThemeChip("Theo hệ thống", ThemeMode.SYSTEM, themeMode, viewModel::setThemeMode)
                }
            }

            SectionHeader("Đơn vị")
            FlatCard {
                Text("Quãng đường & pace", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(Spacing.xs))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    UnitChip("Km", UnitSystem.METRIC, unitSystem, viewModel::setUnitSystem)
                    UnitChip("Dặm (mi)", UnitSystem.IMPERIAL, unitSystem, viewModel::setUnitSystem)
                }
            }

            SectionHeader("Ghi hoạt động")
            FlatCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Tự động tạm dừng", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Tự tạm dừng khi đứng yên, tự tiếp tục khi di chuyển lại",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Switch(checked = autoPauseEnabled, onCheckedChange = viewModel::setAutoPauseEnabled)
                }
            }

            SectionHeader("Kế hoạch tập luyện")
            FlatCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Nhắc lịch tập", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Báo mỗi sáng nếu hôm nay có buổi tập theo kế hoạch chưa hoàn thành",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Switch(checked = trainingReminderEnabled, onCheckedChange = ::onToggleReminder)
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(
    label: String,
    value: ThemeMode,
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
    )
}

@Composable
private fun UnitChip(
    label: String,
    value: UnitSystem,
    current: UnitSystem,
    onSelect: (UnitSystem) -> Unit,
) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
    )
}
