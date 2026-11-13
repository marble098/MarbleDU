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
import ir.marbledu.datausage.data.TimeRange
import ir.marbledu.datausage.ui.EmptyState
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

    // فقط یک بار در ورود به صفحه لود می‌شود. نسخه‌ی قبلی usage.range را به‌عنوان
    // کلید استفاده می‌کرد که بعد از هر لود تغییر می‌کرد و حلقه‌ی بی‌نهایت می‌ساخت.
    LaunchedEffect(Unit) {
        vm.loadUsage()
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
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
            items(combined) { (bytes, app) ->
                Card(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = app.label,
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
                            text = "${LiveMonitor.formatDate(app.firstTimestamp)} • ${
                                LiveMonitor.formatHourMinute(app.firstTimestamp)
                            } → ${LiveMonitor.formatHourMinute(app.lastTimestamp)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        UsageBar(
                            label = app.packageName,
                            bytes = bytes,
                            fraction = bytes.toFloat() / max,
                            persian = persian,
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
