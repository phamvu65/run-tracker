package com.example.runtracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.runtracker.domain.model.GeoPoint
import com.example.runtracker.ui.theme.Spacing

/** Nhãn nhỏ IN HOA, giãn chữ, màu phụ — dùng cho mọi tiêu đề khối kiểu Strava. */
@Composable
fun StravaLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}

/** Tiêu đề một khối: nhãn IN HOA + hành động phụ tuỳ chọn bên phải. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            StravaLabel(title)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

/** Một ô số liệu: số rất đậm + nhãn IN HOA phía dưới. */
data class StatCell(val label: String, val value: String)

/** Cột số liệu đơn (số đậm trên, nhãn dưới). */
@Composable
fun StatColumn(
    cell: StatCell,
    modifier: Modifier = Modifier,
    big: Boolean = false,
    alignEnd: Boolean = false,
) {
    Column(
        modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(
            cell.value,
            style = if (big) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        StravaLabel(cell.label)
    }
}

/** Hàng 2–4 ô số liệu, ngăn nhau bằng đường kẻ dọc mảnh — chữ ký giao diện Strava. */
@Composable
fun StatStrip(
    cells: List<StatCell>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEachIndexed { index, cell ->
            StatColumn(cell, Modifier.weight(1f).padding(vertical = Spacing.xs))
            if (index < cells.lastIndex) {
                VerticalDivider(
                    Modifier.fillMaxHeight().padding(horizontal = Spacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

/** Hàng "nhãn ⟷ giá trị". */
@Composable
fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs + 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.End,
        )
    }
}

/** Thẻ phẳng (trắng trên nền xám) — không đổ bóng, viền tóc. */
@Composable
fun FlatCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Card(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(Spacing.lg), content = content)
    }
}

/** Thẻ hoạt động kiểu feed Strava: tiêu đề, phụ đề, dải số liệu, (map tuỳ chọn). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityCard(
    title: String,
    subtitle: String,
    stats: List<StatCell>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    map: (@Composable () -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Column(Modifier.fillMaxWidth().padding(Spacing.lg)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                StatStrip(stats)
            }
            if (map != null) {
                Box(Modifier.fillMaxWidth().height(150.dp)) { map() }
            }
        }
    }
}

/** Nhóm mục điều hướng trong một thẻ phẳng, có gạch ngăn. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGroup(
    items: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        items.forEachIndexed { index, (label, onClick) ->
            ListItem(
                headlineContent = { Text(label, style = MaterialTheme.typography.bodyLarge) },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable(onClick = onClick),
            )
            if (index < items.lastIndex) {
                HorizontalDivider(
                    Modifier.padding(start = Spacing.lg),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

/** Dòng danh sách bấm được, kiểu thẻ phẳng. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(Spacing.md))
                trailing()
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Avatar tròn: chữ cái đầu của tên trên nền xám than. */
@Composable
fun AthleteAvatar(name: String, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 40.dp) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initial,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Hình thu nhỏ đường chạy — vẽ polyline bằng Canvas (nhẹ, hợp danh sách cuộn,
 * không dùng MapView). Chuẩn hoá điểm về khung, giữ tỉ lệ.
 */
@Composable
fun RouteThumbnail(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier,
) {
    val line = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(modifier.background(bg)) {
        if (points.size >= 2) {
            Canvas(Modifier.fillMaxWidth().fillMaxHeight().padding(Spacing.lg)) {
                val lats = points.map { it.latitude }
                val lngs = points.map { it.longitude }
                val minLat = lats.min(); val maxLat = lats.max()
                val minLng = lngs.min(); val maxLng = lngs.max()
                val spanLat = (maxLat - minLat).takeIf { it > 1e-9 } ?: 1e-9
                val spanLng = (maxLng - minLng).takeIf { it > 1e-9 } ?: 1e-9
                // giữ tỉ lệ: scale theo trục hẹp hơn
                val scale = minOf(size.width / spanLng, size.height / spanLat).toDouble()
                val drawW = spanLng * scale
                val drawH = spanLat * scale
                val offX = (size.width - drawW) / 2.0
                val offY = (size.height - drawH) / 2.0
                val path = Path()
                points.forEachIndexed { i, p ->
                    val x = (offX + (p.longitude - minLng) * scale).toFloat()
                    val y = (offY + (maxLat - p.latitude) * scale).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path,
                    color = line,
                    style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                points.firstOrNull()?.let {
                    val x = (offX + (it.longitude - minLng) * scale).toFloat()
                    val y = (offY + (maxLat - it.latitude) * scale).toFloat()
                    drawCircle(line, radius = 7f, center = Offset(x, y))
                }
            }
        }
    }
}

/**
 * Thẻ feed một buổi tập kiểu trang chủ Strava: avatar + tên + thời gian, tiêu đề lớn,
 * dải số liệu, (huy hiệu thành tích), bản đồ tràn viền, rồi kẻ ngăn mảnh.
 */
@Composable
fun FeedActivityCard(
    athleteName: String,
    timeText: String,
    title: String,
    stats: List<StatCell>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    achievementText: String? = null,
    routePoints: List<GeoPoint> = emptyList(),
) {
    Column(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = Spacing.lg),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AthleteAvatar(athleteName)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(athleteName, style = MaterialTheme.typography.titleSmall)
                Text(
                    timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen)
                .padding(top = Spacing.md),
        )

        Box(Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.md)) {
            StatStrip(stats)
        }

        if (achievementText != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screen)
                    .padding(bottom = Spacing.md)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🏅", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(Spacing.md))
                Text(
                    achievementText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        if (routePoints.size >= 2) {
            RouteThumbnail(
                points = routePoints,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
        }

        Spacer(Modifier.height(Spacing.lg))
        HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceContainerLow)
    }
}

/** Trạng thái rỗng — canh giữa, một câu hướng dẫn. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (message != null) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp),
            )
        }
        action?.invoke()
    }
}
