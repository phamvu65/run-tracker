package com.example.runtracker.ui.hrsensor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

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
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            viewModel.savedAddress?.let { address ->
                Column {
                    Text("Đã ghép", style = MaterialTheme.typography.labelMedium)
                    Text(
                        viewModel.savedName ?: address,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        viewModel.previewBpm?.let { "$it bpm" } ?: "Đang chờ tín hiệu…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                OutlinedButton(onClick = viewModel::forget) { Text("Bỏ ghép") }
                HorizontalDivider()
            }

            Button(onClick = ::scanWithPermission, enabled = !viewModel.scanning) {
                Text(if (viewModel.scanning) "Đang quét…" else "Quét thiết bị")
            }

            viewModel.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            LazyColumn(Modifier.fillMaxWidth()) {
                items(devices, key = { it.address }) { device ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.select(device) }
                            .padding(vertical = 12.dp),
                    ) {
                        Text(device.name ?: "(không tên)", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            device.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
