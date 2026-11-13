package ir.marbledu.datausage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.LiveMonitor
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.TimeRange
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import ir.marbledu.datausage.ui.AppIcon
import ir.marbledu.datausage.ui.CardHeader
import ir.marbledu.datausage.ui.DonutChart
import ir.marbledu.datausage.ui.DonutLegend
import ir.marbledu.datausage.ui.DonutSlice
import ir.marbledu.datausage.ui.EmptyState
import ir.marbledu.datausage.ui.LegendDot
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MarbleCard
import ir.marbledu.datausage.ui.SplitBar
import ir.marbledu.datausage.ui.theme.LocalMarbleColors
import ir.marbledu.datausage.ui.InfoChip
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.SectionTitle
import ir.marbledu.datausage.ui.UsageBar
import ir.marbledu.datausage.util.UsageAccess

@Composable
fun AppsScreen(vm: MainViewModel, settings: AppSettings, persian: Boolean) {
    val context = LocalContext.current
    val usage by vm.usage.collectAsState()
    val dashboard by vm.dashboard.collectAsState()
    val marble = LocalMarbleColors.current
    var sliceSelection by remember(usage.range) { mutableStateOf<Int?>(null) }

    // فقط یک بار در ورود به صفحه لود می‌شود. نسخه‌ی قبلی usage.range را به‌عنوان
    // کلید استفاده می‌کرد که بعد از هر لود تغییر می‌کرد و حلقه‌ی بی‌نهایت می‌ساخت.
    LaunchedEffect(Unit) {
        vm.loadUsage()
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = ir.marbledu.datausage.ui.FloatingBarClearance),
    ) {
        item { SectionTitle(text = context.getString(R.string.per_app)) }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TimeRange.entries.toList()) { range ->
                    FilterChip(
                        selected = usage.range == range,
                        onClick = { vm.setRange(range) },
                        label = { Text(if (persian) range.labelFa else range.labelEn) },
                    )
                }
            }
        }

        if (!dashboard.hasUsageAccess) {
            item {
                Card(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            context.getString(R.string.no_apps),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        TextButton(onClick = { UsageAccess.openSettings(context) }) {
                            Text(context.getString(R.string.open))
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(
                    text = "${context.getString(R.string.cellular)}: ${
                        Formats.bytes(usage.mobile.sumOf { it.bytes }, persian)
                    }",
                    color = MaterialTheme.colorScheme.primary,
                )
                InfoChip(
                    text = "${context.getString(R.string.wifi)}: ${
                        Formats.bytes(usage.wifi.sumOf { it.bytes }, persian)
                    }",
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        val combined = mergeByPackage(usage.mobile, usage.wifi)
        if (usage.loading) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (combined.isEmpty()) {
            item { EmptyState(context.getString(R.string.no_data_yet)) }
        } else {
            val max = combined.maxOf { it.first }
            item {
                val top = combined.take(5)
                val rest = combined.drop(5).fold(0L) { acc, pair -> acc + pair.first }
                val slices = top.mapIndexed { i, (bytes, app) ->
                    val displayName = OperatorCatalog.specialUidLabel(app.uid, persian) ?: app.label
                    DonutSlice(displayName, bytes.toFloat(), marble.chart[i % marble.chart.size], Formats.bytes(bytes, persian))
                } + if (rest > 0) listOf(
                    DonutSlice(
                        if (persian) "بقیه" else "Others",
                        rest.toFloat(),
                        MaterialTheme.colorScheme.outline,
                        Formats.bytes(rest, persian),
                    ),
                ) else emptyList()
                val grandTotal = combined.fold(0L) { acc, pair -> acc + pair.first }
                MarbleCard {
                    CardHeader(
                        title = if (persian) "سهم اپ‌ها" else "App share",
                        icon = MIcons.Apps,
                        trailing = Formats.integer(combined.size.toLong(), persian) + (if (persian) " اپ" else " apps"),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(slices, diameter = 128.dp, stroke = 17.dp, selected = sliceSelection) {
                            val picked = sliceSelection?.let { slices.getOrNull(it) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = picked?.detail ?: Formats.bytes(grandTotal, persian),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                                Text(
                                    text = picked?.label ?: context.getString(R.string.total),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(78.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        DonutLegend(slices, persian, Modifier.weight(1f), sliceSelection) { sliceSelection = it }
                    }
                    SplitBar(
                        listOf(
                            usage.mobile.sumOf { it.bytes }.toFloat() to marble.mobile,
                            usage.wifi.sumOf { it.bytes }.toFloat() to marble.wifi,
                        ),
                        height = 10.dp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(context.getString(R.string.cellular), marble.mobile)
                        LegendDot(context.getString(R.string.wifi), marble.wifi)
                    }
                }
            }
            items(combined) { (bytes, app) ->
                val displayName = OperatorCatalog.specialUidLabel(app.uid, persian) ?: app.label
                val subLabel = if (app.packageName.startsWith("uid:")) {
                    if (persian) "سرویس سیستم" else "System service"
                } else app.packageName
                Card(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(app.packageName.substringBefore(':'), displayName, size = 34.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = Formats.bytes(bytes, persian),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = "${LiveMonitor.formatDate(app.firstTimestamp, persian)} • " +
                                context.getString(
                                    R.string.from_to,
                                    LiveMonitor.formatHourMinute(app.firstTimestamp, persian),
                                    LiveMonitor.formatHourMinute(app.lastTimestamp, persian),
                                ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        UsageBar(
                            label = subLabel,
                            bytes = bytes,
                            fraction = bytes.toFloat() / max,
                            persian = persian,
                            color = marble.chart[(combined.indexOfFirst { it.second.packageName == app.packageName }
                                .coerceAtLeast(0)) % marble.chart.size],
                        )
                    }
                }
            }
        }
    }
}

/** ادغام مصرف موبایل و وای‌فای بر اساس پکیج. */
private fun mergeByPackage(
    mobile: List<ir.marbledu.datausage.data.AppUsage>,
    wifi: List<ir.marbledu.datausage.data.AppUsage>,
): List<Pair<Long, ir.marbledu.datausage.data.AppUsage>> {
    val map = LinkedHashMap<String, Pair<Long, ir.marbledu.datausage.data.AppUsage>>()
    (mobile + wifi).forEach { usage ->
        val existing = map[usage.packageName]
        map[usage.packageName] = if (existing == null) {
            usage.bytes to usage
        } else {
            (existing.first + usage.bytes) to existing.second.copy(
                firstTimestamp = minOf(existing.second.firstTimestamp, usage.firstTimestamp),
                lastTimestamp = maxOf(existing.second.lastTimestamp, usage.lastTimestamp),
            )
        }
    }
    return map.values.sortedByDescending { it.first }
}
