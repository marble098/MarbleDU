package ir.marbledu.datausage.ui.screens

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.ConnectionQuality
import ir.marbledu.datausage.data.PingResult
import ir.marbledu.datausage.data.PingRoute
import ir.marbledu.datausage.data.PingScope
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.LiveMonitor
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import ir.marbledu.datausage.ui.ArcGauge
import ir.marbledu.datausage.ui.CardHeader
import ir.marbledu.datausage.ui.FloatingBarClearance
import ir.marbledu.datausage.ui.LegendDot
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MarbleCard
import ir.marbledu.datausage.ui.Metric
import ir.marbledu.datausage.ui.OperatorBadge
import ir.marbledu.datausage.ui.SpeedLineChart
import ir.marbledu.datausage.ui.theme.LocalMarbleColors
import ir.marbledu.datausage.ui.LargeTitleScaffold
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.SectionTitle
import ir.marbledu.datausage.ui.StatCard

@Composable
fun LiveScreen(vm: MainViewModel, settings: AppSettings, persian: Boolean) {
    val context = LocalContext.current
    val tick by vm.tick.collectAsState()
    val pings by vm.ping.collectAsState()
    val pingRunning by vm.pingRunning.collectAsState()
    val t = tick
    val speedHistory by vm.speedHistory.collectAsState()
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val liveTabs = if (persian) listOf("سرعت", "کیفیت اتصال") else listOf("Speed", "Connection")
    val listState = rememberLazyListState()
    LargeTitleScaffold(
        title = if (persian) "همین حالا" else "Live",
        listState = listState,
        tabs = liveTabs,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
    ) {
        if (selectedTab == 0) {

        item {
            val down = t?.downBytesPerSecond ?: 0.0
            val up = t?.upBytesPerSecond ?: 0.0
            val peak = (t?.peakSpeed ?: 0.0).coerceAtLeast(64.0 * 1024)
            MarbleCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArcGauge(
                        fraction = ((down + up) / peak).toFloat(),
                        startColor = marble.heroStart,
                        endColor = marble.heroEnd,
                        diameter = 126.dp,
                        stroke = 12.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(MIcons.Speed, null, Modifier.size(18.dp), tint = scheme.primary)
                            Text(
                                Formats.bytes((down + up).toLong(), persian),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                            Text(
                                if (persian) "بر ثانیه" else "per second",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric(
                            context.getString(R.string.download),
                            Formats.speed(down, persian),
                            marble.download,
                            icon = MIcons.Download,
                        )
                        Metric(
                            context.getString(R.string.upload),
                            Formats.speed(up, persian),
                            marble.upload,
                            icon = MIcons.Upload,
                        )
                        val network = t?.network
                        if (network != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OperatorBadge(network.operator, size = 26.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (network.type == ir.marbledu.datausage.data.ConnectionType.WIFI &&
                                        !network.wifiOperatorResolved &&
                                        network.wifiLookupStatus == ir.marbledu.datausage.data.IspResolver.LookupStatus.SEARCHING
                                    ) {
                                        if (persian) "در حال شناسایی اپراتور…" else "Detecting ISP…"
                                    } else {
                                        network.operator.name(persian)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            MarbleCard {
                CardHeader(
                    title = if (persian) "نمودار زنده‌ی سرعت" else "Live speed chart",
                    icon = MIcons.Live,
                    tint = scheme.primary,
                    subtitle = if (persian) "۶۰ ثانیه‌ی اخیر" else "Last 60 seconds",
                )
                SpeedLineChart(
                    down = speedHistory.map { it.down.toFloat() },
                    up = speedHistory.map { it.up.toFloat() },
                    downColor = marble.download,
                    upColor = marble.upload,
                    height = 136.dp,
                    maxLabel = Formats.speed(
                        (speedHistory.maxOfOrNull { maxOf(it.down, it.up) } ?: 0.0).coerceAtLeast(1024.0) * 1.2,
                        persian,
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(context.getString(R.string.download), marble.download)
                    LegendDot(context.getString(R.string.upload), marble.upload)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric(
                        if (persian) "بیشترین دانلود" else "Max download",
                        Formats.speed(speedHistory.maxOfOrNull { it.down } ?: 0.0, persian),
                        marble.download,
                        Modifier.weight(1f),
                    )
                    Metric(
                        if (persian) "بیشترین آپلود" else "Max upload",
                        Formats.speed(speedHistory.maxOfOrNull { it.up } ?: 0.0, persian),
                        marble.upload,
                        Modifier.weight(1f),
                    )
                    Metric(
                        if (persian) "حجم این ۶۰ ثانیه" else "Volume (60 s)",
                        Formats.bytes(speedHistory.sumOf { it.down + it.up }.toLong(), persian),
                        scheme.tertiary,
                        Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = context.getString(R.string.average_speed),
                    value = Formats.speed(t?.averageSpeed ?: 0.0, persian),
                    icon = MIcons.Speed,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = context.getString(R.string.peak_speed),
                    value = Formats.speed(t?.peakSpeed ?: 0.0, persian),
                    icon = MIcons.Chart,
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            StatCard(
                title = context.getString(R.string.session_timeline),
                value = if (t?.mobileSession == null) "—" else context.getString(
                    R.string.from_to,
                    LiveMonitor.formatHourMinute(t.sessionStart, persian),
                    LiveMonitor.formatHourMinute(t.timestamp, persian),
                ),
                subtitle = if (t?.mobileSession == null) null else
                    "${Formats.bytes(t.sessionRxBytes + t.sessionTxBytes, persian)} " +
                        if (persian) "در این نشست" else "this session",
                icon = MIcons.Clock,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        } else {

        item {
            MarbleCard {
                CardHeader(
                    title = if (persian) "تست کیفیت اتصال" else "Connection quality test",
                    icon = MIcons.Timer,
                    tint = scheme.primary,
                    subtitle = if (persian) "تأخیر واقعی تا سرورهای داخلی و بین‌المللی" else
                        "Real round-trip delay to Iranian and global servers",
                )
                Text(
                    text = if (persian) {
                        "زمان رفت‌وبرگشتِ یک درخواست واقعی تا خودِ سرور اندازه‌گیری می‌شود؛ " +
                            "پاسخ محلیِ VPN (همان ۳–۴ میلی‌ثانیه‌ی اشتباه) دیگر حساب نمی‌شود."
                    } else {
                        "Measures the round trip of a real request to the server itself, so a VPN " +
                            "answering locally can no longer fake a 3–4 ms ping."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Button(
                    onClick = { vm.runPingTest() },
                    enabled = !pingRunning,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(MIcons.Timer, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        if (pingRunning) {
                            if (persian) "در حال اندازه‌گیری…" else "Measuring…"
                        } else context.getString(R.string.run_ping_test),
                    )
                }
            }
        }

        if (pings.isNotEmpty()) {
            val defaultRoute = pings.first().route
            val primary = pings.filter { it.route == defaultRoute }
            val direct = if (defaultRoute == PingRoute.VPN) pings.filter { it.route == PingRoute.DIRECT } else emptyList()
            val verdict = ConnectionQuality.evaluate(primary)

            item {
                MarbleCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (persian) "نمره‌ی کیفیت" else "Quality score",
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (verdict == null) "—" else
                                    Formats.integer(verdict.score.toLong(), persian) + (if (persian) " از ۱۰۰" else " / 100"),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = gradeColor(verdict?.overall),
                            )
                            Text(
                                text = if (verdict == null) {
                                    if (persian) "هیچ سروری پاسخ نداد" else "No server answered"
                                } else gradeLabel(verdict.overall, persian) +
                                    (if (defaultRoute == PingRoute.VPN) (if (persian) " • از مسیر VPN" else " • via VPN") else ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Metric(
                                if (persian) "داخلی" else "Iran",
                                scopeAverage(primary, PingScope.DOMESTIC, persian, context.getString(R.string.ms)),
                                marble.download,
                            )
                            Metric(
                                if (persian) "بین‌الملل" else "Global",
                                scopeAverage(primary, PingScope.GLOBAL, persian, context.getString(R.string.ms)),
                                marble.upload,
                            )
                        }
                    }
                    if (verdict != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UseCaseChip(if (persian) "بازی" else "Gaming", verdict.gaming, persian, Modifier.weight(1f))
                            UseCaseChip(if (persian) "تماس تصویری" else "Video call", verdict.videoCall, persian, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UseCaseChip(if (persian) "ویدیو" else "Streaming", verdict.streaming, persian, Modifier.weight(1f))
                            UseCaseChip(if (persian) "وب‌گردی" else "Browsing", verdict.browsing, persian, Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                SectionTitle(
                    text = if (defaultRoute == PingRoute.VPN) {
                        if (persian) "تأخیر از مسیر VPN" else "Delay through VPN"
                    } else context.getString(R.string.average_ping),
                )
            }
            items(primary, key = { "p-" + it.target.id }) { result -> PingRow(result, persian) }

            if (direct.isNotEmpty()) {
                item {
                    SectionTitle(text = if (persian) "تأخیر مستقیم (بدون VPN)" else "Direct delay (without VPN)")
                }
                items(direct, key = { "d-" + it.target.id }) { result -> PingRow(result, persian) }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
        }
    }
}

@Composable
private fun gradeColor(grade: ConnectionQuality.Grade?): Color = when (grade) {
    ConnectionQuality.Grade.EXCELLENT -> Color(0xFF16A34A)
    ConnectionQuality.Grade.GOOD -> Color(0xFF65A30D)
    ConnectionQuality.Grade.FAIR -> Color(0xFFD97706)
    ConnectionQuality.Grade.POOR -> MaterialTheme.colorScheme.error
    null -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun gradeLabel(grade: ConnectionQuality.Grade, persian: Boolean): String = when (grade) {
    ConnectionQuality.Grade.EXCELLENT -> if (persian) "عالی" else "Excellent"
    ConnectionQuality.Grade.GOOD -> if (persian) "خوب" else "Good"
    ConnectionQuality.Grade.FAIR -> if (persian) "متوسط" else "Fair"
    ConnectionQuality.Grade.POOR -> if (persian) "ضعیف" else "Poor"
}

private fun scopeAverage(results: List<PingResult>, scope: PingScope, persian: Boolean, unit: String): String {
    val ok = results.filter { it.target.scope == scope && it.ok }
    if (ok.isEmpty()) return "—"
    return Formats.number(ok.map { it.averageMs }.average(), persian, 0) + " " + unit
}

@Composable
private fun UseCaseChip(label: String, grade: ConnectionQuality.Grade, persian: Boolean, modifier: Modifier = Modifier) {
    val color = gradeColor(grade)
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            gradeLabel(grade, persian),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun PingRow(result: PingResult, persian: Boolean) {
    val context = LocalContext.current
    val ms = context.getString(R.string.ms)
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (result.target.scope == PingScope.DOMESTIC) MIcons.Home else MIcons.Globe,
                null,
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    result.target.name(persian),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (result.ok) {
                        "${context.getString(R.string.jitter)}: ${Formats.number(result.jitterMs, persian, 0)} $ms   •   " +
                            "${context.getString(R.string.packet_loss)}: " +
                            "${Formats.integer(result.lossPercent.toLong(), persian)}${if (persian) "٪" else "%"}   •   " +
                            "${if (persian) "کمترین" else "min"} ${Formats.number(result.minMs, persian, 0)}"
                    } else {
                        if (persian) "پاسخی دریافت نشد (مسدود یا قطع)" else "No reply (blocked or offline)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (result.ok) "${Formats.number(result.averageMs, persian, 0)} $ms" else "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    !result.ok -> MaterialTheme.colorScheme.error
                    result.averageMs <= 120.0 -> gradeColor(ConnectionQuality.Grade.EXCELLENT)
                    result.averageMs <= 250.0 -> gradeColor(ConnectionQuality.Grade.FAIR)
                    else -> MaterialTheme.colorScheme.error
                },
            )
        }
    }
}
