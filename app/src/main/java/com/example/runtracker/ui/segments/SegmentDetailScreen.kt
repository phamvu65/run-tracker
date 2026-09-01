package com.example.runtracker.ui.segments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.runtracker.core.formatClock
import com.example.runtracker.core.formatDistanceKm
import com.example.runtracker.domain.model.SegmentEffort
import com.example.runtracker.ui.common.PathMap
import com.google.android.gms.maps.model.LatLng
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SegmentDetailViewModel = hiltViewModel(),
) {
    val segment by viewModel.segment.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(segment?.name ?: "Segment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        val s = segment
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (s != null && s.points.size >= 2) {
                val latLngs = remember(s) { s.points.map { LatLng(it.latitude, it.longitude) } }
                PathMap(latLngs = latLngs, modifier = Modifier.fillMaxWidth().height(240.dp))
            }

            if (s != null) {
                Text(
                    "${formatDistanceKm(s.distanceMeters)} · độ dốc TB ${"%.1f".format(s.avgGrade)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Text(
                "Bảng xếp hạng (${leaderboard.size})",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            if (leaderboard.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Chưa có lượt nào. Chạy qua đoạn này để ghi thành tích.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(leaderboard, key = { it.id }) { effort ->
                        LeaderboardRow(effort)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
private fun LeaderboardRow(effort: SegmentEffort) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("${effort.rank ?: "-"}", Modifier.width(24.dp), style = MaterialTheme.typography.bodyMedium)
        Text(
            effort.startTime.atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMAT),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            formatClock(effort.elapsedSeconds.toLong()),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}
