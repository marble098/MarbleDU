package ir.marbledu.datausage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppLanguage
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.LiveMonitor
import ir.marbledu.datausage.data.TimeRange
import ir.marbledu.datausage.ui.EmptyState
import ir.marbledu.datausage.ui.InfoChip
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.SectionTitle
import ir.marbledu.datausage.ui.StatCard
import ir.marbledu.datausage.ui.UsageBar
import ir.marbledu.datausage.util.UsageAccess

@Composable
fun DashboardScreen(vm: MainViewModel, settings: AppSettings, persian: Boolean) {
    val context = LocalContext.current
    val dashboard by vm.dashboard.collectAsState()
    val usage by vm.usage.collectAsState()
    val tick by vm.tick.collectAsState()
    val iran = settings.iran

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 12.dp),
    ) {
        item {
            Column {
                Text(
                    text = context.getString(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = context.getString(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (iran.detectOperator) {
                        InfoChip(
                            text = dashboard.operatorName.ifBlank {
                                context.getString(R.string.network_unknown)
                            },
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    InfoChip(
                        text = networkLabel(tick?.network?.type?.name, persian),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    if (tick?.network?.isValidated == true) {
                        InfoChip(text = if (persian) "آنلاین" else "Online")
                    }
                }
            }
        }

        if (!dashboard.hasUsageAccess) {
            item {
                Card(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            context.getString(R.string.grant_usage_access),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            context.getString(R.string.grant_usage_access_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        TextButton(onClick = { UsageAccess.openSettings(context) }) {
                            Text(context.getString(R.string.open))
                        }
                    }
                }
            }
        }

        if (iran.showDataUsageReport) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = context.getString(R.string.today_usage),
                        value = Formats.bytes(dashboard.todayMobileBytes, persian),
                        subtitle = when {
                            dashboard.todayVariationPercent > 0 ->
                                "▲ ${Formats.integer(dashboard.todayVariationPercent.toLong(), persian)}٪ " +
                                    (if (persian) "بیشتر از دیروز" else "vs yesterday")
                            dashboard.todayVariationPercent < 0 ->
                                "▼ ${Formats.integer((-dashboard.todayVariationPercent).toLong(), persian)}٪ " +
                                    (if (persian) "کمتر از دیروز" else "vs yesterday")
                            else -> if (persian) "داده موبایل" else "mobile data"
                        },
                        icon = MIcons.Download,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = context.getString(R.string.this_month),
                        value = Formats.bytes(dashboard.monthMobileBytes, persian),
                        subtitle = if (persian) "۳۰ روز گذشته" else "last 30 days",
                        icon = MIcons.Chart,
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            StatCard(
                title = if (persian) "سرعت همین حالا" else "Current speed",
                value = Formats.speed(
                    (tick?.downBytesPerSecond ?: 0.0) + (tick?.upBytesPerSecond ?: 0.0),
                    persian,
                ),
                subtitle = "↓ ${Formats.speed(tick?.downBytesPerSecond ?: 0.0, persian)}   " +
                    "↑ ${Formats.speed(tick?.upBytesPerSecond ?: 0.0, persian)}",
                icon = MIcons.Speed,
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (iran.showDataUsageReport && dashboard.week.any { it.bytes > 0 }) {
            item {
                WeeklyTrendCard(dashboard.week, persian)
            }
        }

        if (iran.showAverageSpeed || iran.showAveragePing) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (iran.showAverageSpeed) {
                        StatCard(
                            title = context.getString(R.string.average_speed),
                            value = Formats.speed(tick?.averageSpeed ?: 0.0, persian),
                            icon = MIcons.Speed,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (iran.showAveragePing) {
                        StatCard(
                            title = context.getString(R.string.average_ping),
                            value = if (dashboard.averagePingMs > 0) {
                                "${Formats.number(dashboard.averagePingMs, persian, 0)} ${context.getString(R.string.ms)}"
                            } else "--",
                            icon = MIcons.Timer,
                            accent = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (settings.dataCapBytes > 0) {
            item {
                val used = dashboard.monthMobileBytes
                val fraction = Formats.percentOf(used, settings.dataCapBytes)
                StatCard(
                    title = context.getString(R.string.data_cap),
                    value = "${Formats.bytes(used, persian)} / ${Formats.bytes(settings.dataCapBytes, persian)}",
                    subtitle = if (fraction > 0.9f) context.getString(R.string.cap_reached)
                    else "${context.getString(R.string.remaining)}: ${
                        Formats.bytes((settings.dataCapBytes - used).coerceAtLeast(0), persian)
                    }",
                    icon = MIcons.Shield,
                    accent = if (fraction > 0.9f) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    progress = fraction,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (iran.showSessionTimeline && tick != null) {
            item {
                SessionCard(vm, persian)
            }
        }

        if (iran.showPerAppBreakdown && dashboard.hasUsageAccess) {
            item {
                SectionTitle(
                    text = context.getString(R.string.per_app),
                    action = {
                        TextButton(onClick = { vm.setRange(TimeRange.TODAY); vm.loadUsage(TimeRange.TODAY) }) {
                            Text(context.getString(R.string.refresh_label))
                        }
                    },
                )
            }
            if (usage.mobile.isEmpty()) {
                item { EmptyState(context.getString(R.string.no_data_yet)) }
            } else {
                val top = usage.mobile.take(5)
                val max = top.maxOfOrNull { it.bytes } ?: 1L
                items(top) { app ->
                    UsageBar(
                        label = app.label,
                        bytes = app.bytes,
                        fraction = app.bytes.toFloat() / max,
                        persian = persian,
                        trailing = LiveMonitor.formatHourMinute(app.lastTimestamp),
                    )
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

/** نمودار میله‌ای ساده‌ی ۷ روز اخیر – بدون کتابخانه‌ی اضافی. */
@Composable
private fun WeeklyTrendCard(week: List<ir.marbledu.datausage.data.DayUsage>, persian: Boolean) {
    val context = LocalContext.current
    val max = week.maxOfOrNull { it.bytes }?.coerceAtLeast(1L) ?: 1L
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(MIcons.Chart, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (persian) "روند ۷ روز اخیر" else "Last 7 days",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = Formats.bytes(week.sumOf { it.bytes }, persian),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                Modifier.fillMaxWidth().height(96.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                week.forEach { day ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((8 + 76 * (day.bytes.toFloat() / max)).dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = dayLabel(day.startMillis, persian),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun dayLabel(millis: Long, persian: Boolean): String {
    val names = if (persian) {
        listOf("ی", "د", "س", "چ", "پ", "ج", "ش")
    } else {
        listOf("S", "M", "T", "W", "T", "F", "S")
    }
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    // شنبه = ۰ در تقویم ایرانی
    val index = if (persian) (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    else cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
    return names[index]
}

@Composable
private fun SessionCard(vm: MainViewModel, persian: Boolean) {
    val tick by vm.tick.collectAsState()
    val context = LocalContext.current
    val t = tick ?: return
    val start = remember(t.sessionStart) { LiveMonitor.formatHourMinute(t.sessionStart) }
    val now = remember(t.timestamp) { LiveMonitor.formatHourMinute(t.timestamp) }
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(MIcons.Clock, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Text(
                    context.getString(R.string.session_timeline),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = context.getString(R.string.from_to, start, now),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(
                        context.getString(R.string.download),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Formats.bytes(t.sessionRxBytes, persian),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column {
                    Text(
                        context.getString(R.string.upload),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Formats.bytes(t.sessionTxBytes, persian),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Column {
                    Text(
                        context.getString(R.string.peak_speed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Formats.speed(t.peakSpeed, persian),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

fun networkLabel(type: String?, persian: Boolean): String = when (type) {
    "WIFI" -> if (persian) "وای‌فای" else "Wi-Fi"
    "CELLULAR" -> if (persian) "داده موبایل" else "Mobile data"
    "ETHERNET" -> if (persian) "اترنت" else "Ethernet"
    "VPN" -> "VPN"
    "OFFLINE" -> if (persian) "آفلاین" else "Offline"
    else -> if (persian) "نامشخص" else "Unknown"
}
