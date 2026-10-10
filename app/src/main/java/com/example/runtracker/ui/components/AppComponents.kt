package com.example.runtracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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

/**
 * Thanh tiêu đề cố định thu gọn (Compact Fixed Header):
 * Chiều cao 48dp, tiêu đề chữ to đậm (`titleLarge` + ExtraBold), khoảng trống phía trên thu gọn,
 * giữ cố định khi nội dung cuộn bên dưới trong Scaffold.
 */
@Composable
fun CompactTopHeader(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .padding(horizontal = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationIcon != null) {
                navigationIcon()
                Spacer(Modifier.width(Spacing.xs))
            } else {
                Spacer(Modifier.width(Spacing.md))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (actions != null) {
                Row(verticalAlignment = Alignment.CenterVertically, content = actions)
            } else {
                Spacer(Modifier.width(Spacing.md))
            }
        }
    }
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

/** Dữ liệu cho một ô [IconStatTile]. */
data class IconStatTileData(val icon: ImageVector, val label: String, val value: String, val unit: String? = null)

/** Ô số liệu có icon phía trên — kiểu "Button" tile của GoRun (RunRecap). */
@Composable
fun IconStatTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        StravaLabel(label)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (unit != null) {
                Spacer(Modifier.width(2.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Lưới các [IconStatTile], tự xuống dòng — thay cho [StatStrip] ở các thẻ kiểu GoRun. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconStatGrid(cells: List<IconStatTileData>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        cells.forEach { cell ->
            IconStatTile(
                cell.icon,
                cell.label,
                cell.value,
                unit = cell.unit,
                modifier = Modifier.weight(1f, fill = true).widthIn(min = 96.dp),
            )
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

/**
 * Thẻ biểu đồ kiểu GoRun "Diagram Card": tiêu đề + hành động phụ, chú giải tuỳ chọn,
 * slot biểu đồ, rồi lưới số liệu ([IconStatGrid]) phía dưới.
 */
@Composable
fun DiagramCard(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    legend: (@Composable () -> Unit)? = null,
    footerGrid: (@Composable () -> Unit)? = null,
    chart: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            trailing?.invoke()
        }
        legend?.invoke()
        chart?.invoke()
        footerGrid?.invoke()
    }
}

/**
 * Thẻ thử thách kiểu GoRun "Run Card = Challenge": icon huy hiệu, tiêu đề/mô tả,
 * thanh tiến độ, hàng ngày tháng.
 */
@Composable
fun ChallengeRunCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    dateRangeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    statusLabel: String? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (statusLabel != null) {
                    Text(
                        statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(dateRangeText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Toggle nhỏ kiểu "Switch" GoRun: viên nổi bật màu primary ở lựa chọn đang chọn, trượt trên
 * nền viên thuốc xám — dùng để đổi kỳ xem (tuần/tháng...) trong [DiagramCard].
 */
@Composable
fun PeriodSwitch(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Thẻ mẹo dạng carousel kiểu GoRun "Tips": icon lớn giữa nền gradient, chú thích mờ phía dưới
 * (thay ảnh minh hoạ bằng gradient token màu — nội dung tĩnh trong app, không tải ảnh ngoài).
 */
@Composable
fun TipCard(
    icon: ImageVector,
    text: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .width(148.dp)
            .height(168.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.verticalGradient(gradientColors)),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.align(Alignment.Center).size(40.dp),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.38f))
                .padding(Spacing.sm),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Icon nhỏ + text — dùng thay một ô [StatCell] trong hàng số liệu compact (kiểu Run Card = History). */
@Composable
fun IconStatChip(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
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

/** Nền "bản đồ" hình thu nhỏ — nền teal nhạt tươi mát, hài hoà với bảng màu trắng ngà. */
private val RouteThumbnailBackground = Color(0xFFEAF5F2)

/**
 * Hình thu nhỏ đường chạy — vẽ polyline bằng Canvas (nhẹ, hợp danh sách cuộn, không dùng MapView
 * thật — xem CLAUDE.md, quyết định có chủ đích để tránh giật khi cuộn danh sách dài). Chuẩn hoá
 * điểm về khung, giữ tỉ lệ. Nền tối cố định + mốc đầu/cuối riêng biệt để gợi cảm giác bản đồ thật
 * dù không có tile đường phố.
 */
@Composable
fun RouteThumbnail(
    points: List<GeoPoint>,
    modifier: Modifier = Modifier,
) {
    val line = MaterialTheme.colorScheme.primary
    Box(modifier.background(RouteThumbnailBackground)) {
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
                fun toOffset(p: GeoPoint) = Offset(
                    (offX + (p.longitude - minLng) * scale).toFloat(),
                    (offY + (maxLat - p.latitude) * scale).toFloat(),
                )
                val path = Path()
                points.forEachIndexed { i, p ->
                    val o = toOffset(p)
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                drawPath(
                    path,
                    color = line,
                    style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                val start = toOffset(points.first())
                val end = toOffset(points.last())
                drawCircle(Color.White, radius = 10f, center = start)
                drawCircle(line, radius = 6.5f, center = start)
                drawCircle(Color.White, radius = 10f, center = end)
                drawCircle(Color.Black, radius = 6.5f, center = end)
            }
        }
    }
}

/** Huy chương thành tích cho một thẻ feed — xem `ActivityListViewModel.toFeedAchievement`. */
data class FeedAchievement(
    val medalCount: Int,
    val bestMedalEmoji: String,
    val bannerText: String,
    val improvedText: String?,
)

/**
 * Thẻ feed một buổi tập kiểu trang chủ Strava: avatar + tên + giờ + vị trí, tiêu đề lớn, dải số
 * liệu (nhãn trên/giá trị đậm dưới, kiểu Strava — khác [StatColumn] value-trước dùng ở màn khác)
 * cùng huy chương thành tích ở cuối hàng, banner thành tích (nếu có), bản đồ tràn viền. Nền thẻ
 * dùng `surfaceContainer` (sáng hơn nền đen của màn) thay vì chỉ 1 đường kẻ mảnh — cùng với
 * khoảng cách giữa các thẻ do `LazyColumn` chừa ra (xem `ActivityListScreen`), tạo ranh giới rõ
 * ràng giữa các buổi tập thay vì các thẻ dính liền nhau (user phản hồi feed cũ "chia chưa rõ ràng").
 * KHÔNG có hàng nút Thích/Bình luận/Chia sẻ — app chưa làm mạng xã hội (xem CLAUDE.md), chỉ có 1
 * người dùng local nên các nút đó sẽ không có ý nghĩa gì.
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
    locationText: String? = null,
    achievement: FeedAchievement? = null,
    routePoints: List<GeoPoint> = emptyList(),
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(top = Spacing.lg),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
            verticalAlignment = Alignment.Top,
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
                if (locationText != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            locationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen, vertical = Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                Modifier.weight(1f).padding(end = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                stats.forEach { cell -> FeedStatColumn(cell) }
            }
            if (achievement != null) {
                Column(horizontalAlignment = Alignment.End) {
                    StravaLabel("Thành tích")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${achievement.bestMedalEmoji} ${achievement.medalCount}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        if (achievement != null) {
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
                Text(achievement.bestMedalEmoji, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        achievement.bannerText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    if (achievement.improvedText != null) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.tertiary)
                                .padding(horizontal = Spacing.sm, vertical = 2.dp),
                        ) {
                            Text(
                                achievement.improvedText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiary,
                            )
                        }
                    }
                }
            }
        }

        if (routePoints.size >= 2) {
            RouteThumbnail(
                points = routePoints,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
        }

        Spacer(Modifier.height(Spacing.lg))
    }
}

/**
 * Cột số liệu kiểu Strava cho [FeedActivityCard]: nhãn nhỏ TRÊN, giá trị đậm DƯỚI, 1 dòng — không
 * bao giờ wrap xuống dòng 2. Dùng `TextOverflow.Ellipsis` (không phải clip trần) khi quá hẹp: một
 * con số bị CẮT CỤT giữa chừng ("36:1" thay vì "36:10") dễ đọc nhầm thành giá trị khác — "…" báo rõ
 * là bị rút gọn thay vì trông như số liệu thật.
 */
@Composable
private fun FeedStatColumn(cell: StatCell, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            cell.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            cell.value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
