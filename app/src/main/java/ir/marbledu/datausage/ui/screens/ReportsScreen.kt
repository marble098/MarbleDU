package ir.marbledu.datausage.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.SectionTitle
import ir.marbledu.datausage.ui.StatCard
import ir.marbledu.datausage.util.ReportExporter
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(vm: MainViewModel, settings: AppSettings, persian: Boolean) {
    val context = LocalContext.current
    val usage by vm.usage.collectAsState()
    val dashboard by vm.dashboard.collectAsState()
    val hourly by vm.peakHours.collectAsState()
    val scope = rememberCoroutineScope()
    val iran = settings.iran

    // ساعت‌های پرمصرف با تغییر بازه دوباره محاسبه می‌شوند (فقط اگر روشن باشد).
    LaunchedEffect(usage.range) {
        if (iran.showPeakHours) vm.loadPeakHours(usage.range)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
    ) {
        item { SectionTitle(text = context.getString(R.string.reports_title)) }

        item {
            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (persian) "خلاصه‌ی دوره" else "Cycle summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = (if (persian) "بازه: " else "Range: ") +
                            (if (persian) usage.range.labelFa else usage.range.labelEn),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimeRange.entries.forEach { range ->
                            FilledTonalButton(
                                onClick = { vm.setRange(range) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = if (persian) range.labelFa else range.labelEn,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (iran.showDataUsageReport) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = context.getString(R.string.cellular),
                        value = Formats.bytes(usage.mobile.sumOf { it.bytes }, persian),
                        icon = MIcons.Signal,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = context.getString(R.string.wifi),
                        value = Formats.bytes(usage.wifi.sumOf { it.bytes }, persian),
                        icon = MIcons.Wifi,
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (iran.showBillingCycle) {
            item {
                StatCard(
                    title = context.getString(R.string.billing_cycle),
                    value = (if (persian) "روز " else "Day ") + settings.billingCycleDay,
                    subtitle = if (persian) "شروع دوره‌ی صورتحساب شما" else "Your cycle start day",
                    icon = MIcons.Chart,
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val uri = ReportExporter.exportCsv(
                                context = context,
                                range = usage.range,
                                mobile = usage.mobile,
                                wifi = usage.wifi,
                                persian = persian,
                                operatorName = dashboard.operatorName,
                            )
                            val share = ReportExporter.shareIntent(context, uri)
                            context.startActivity(Intent.createChooser(share, context.getString(R.string.share_csv_title)))
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(MIcons.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(context.getString(R.string.export_csv))
                }
                FilledTonalButton(
                    onClick = {
                        scope.launch { ReportExporter.shareSummary(context, usage, dashboard, persian) }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(MIcons.Reports, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(context.getString(R.string.share_report))
                }
            }
        }

        if (iran.showPeakHours) {
            item { SectionTitle(text = context.getString(R.string.peak_hours)) }
            // داده‌ی ساعتی از ViewModel می‌آید (مصرف واقعی هر ساعت).
            if (hourly.all { it.bytes == 0L }) {
                item {
                    Text(
                        text = context.getString(R.string.no_data_yet),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                val max = hourly.maxOf { it.bytes }
                items(hourly.filter { it.bytes > 0 }) { bucket ->
                    ir.marbledu.datausage.ui.UsageBar(
                        label = "${Formats.integer(bucket.hour.toLong(), persian)}:00",
                        bytes = bucket.bytes,
                        fraction = bucket.bytes.toFloat() / max,
                        persian = persian,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        if (usage.mobile.isNotEmpty()) {
            item { SectionTitle(text = context.getString(R.string.per_app)) }
            val top = usage.mobile.take(10)
            val max = top.maxOf { it.bytes }
            items(top) { app ->
                ir.marbledu.datausage.ui.UsageBar(
                    label = app.label,
                    bytes = app.bytes,
                    fraction = app.bytes.toFloat() / max,
                    persian = persian,
                    trailing = LiveMonitor.formatHourMinute(app.lastTimestamp),
                )
            }
        }

        item { Spacer(Modifier.height(6.dp)) }
    }
}

