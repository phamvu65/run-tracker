package com.example.runtracker.ui.hrsensor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.ui.components.AppListCard
import com.example.runtracker.ui.components.SectionHeader
import com.example.runtracker.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HrSensorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HrSensorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val devices by viewModel.devices.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.all { it }) viewModel.startScan()
    }

    fun scanWithPermission() {
        val granted = viewModel.requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (granted) viewModel.startScan() else permissionLauncher.launch(viewModel.requiredPermissions)
    }

    LaunchedEffect(Unit) {
        if (viewModel.savedAddress != null) viewModel.startPreview()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Đai nhịp tim") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        if (!viewModel.supported) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Thiết bị không hỗ trợ Bluetooth LE.", style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            viewModel.savedAddress?.let { address ->
                ElevatedCard(
                    Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Text(
                            "ĐÃ GHÉP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            viewModel.savedName ?: address,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            viewModel.previewBpm?.let { "$it bpm" } ?: "Đang chờ tín hiệu…",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        OutlinedButton(onClick = viewModel::forget) { Text("Bỏ ghép") }
                    }
                }
            }

            Button(
                onClick = ::scanWithPermission,
                enabled = !viewModel.scanning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (viewModel.scanning) "Đang quét…" else "Quét thiết bị")
            }

            viewModel.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            if (devices.isNotEmpty()) {
                SectionHeader("Thiết bị tìm thấy")
            }
            LazyColumn(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(devices, key = { it.address }) { device ->
                    AppListCard(
                        title = device.name ?: "(không tên)",
                        subtitle = device.address,
                        onClick = { viewModel.select(device) },
                    )
                }
            }
        }
    }
}
