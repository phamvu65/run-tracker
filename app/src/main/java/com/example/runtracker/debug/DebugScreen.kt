package com.example.runtracker.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DebugScreen(
    modifier: Modifier = Modifier,
    viewModel: DebugViewModel = hiltViewModel(),
) {
    val activities by viewModel.activities.collectAsState()

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("RunTracker · debug harness", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::addFakeActivity) { Text("Add fake run") }
            OutlinedButton(onClick = viewModel::clearAll) { Text("Clear") }
        }

        viewModel.status?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        Text("Activities (${activities.size})", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))

        LazyColumn(Modifier.fillMaxSize()) {
            items(activities, key = { it.id }) { activity ->
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text("${activity.type} · ${activity.distanceMeters.toInt()} m · ${activity.duration}")
                    Text(
                        "start ${activity.startTime}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
