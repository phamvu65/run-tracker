package com.example.runtracker.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.ui.components.FlatCard
import com.example.runtracker.ui.components.NavGroup
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.components.StravaLabel
import com.example.runtracker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenInfo: () -> Unit = {},
    onOpenSegments: () -> Unit = {},
    onOpenPlan: () -> Unit = {},
    onOpenZones: () -> Unit = {},
    onOpenRoutes: () -> Unit = {},
    onOpenHrSensor: () -> Unit = {},
    onOpenBeacon: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Hồ sơ") },
                actions = {
                    IconButton(onClick = onOpenInfo) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Thông tin cá nhân")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            FlatCard {
                StravaLabel("Vận động viên")
                Text(
                    user?.displayName?.takeIf { it.isNotBlank() } ?: "Bạn",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "Nhấn để xem và sửa thông tin cá nhân",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }

            SectionHeader("Phân tích")
            NavGroup(
                items = listOf(
                    "Segments" to onOpenSegments,
                    "Kế hoạch tập luyện" to onOpenPlan,
                    "Vùng nhịp tim" to onOpenZones,
                ),
            )

            SectionHeader("Công cụ")
            NavGroup(
                items = listOf(
                    "Routes đã lưu" to onOpenRoutes,
                    "Đai nhịp tim (BLE)" to onOpenHrSensor,
                    "Theo dõi trực tiếp (Beacon)" to onOpenBeacon,
                ),
            )
        }
    }
}
