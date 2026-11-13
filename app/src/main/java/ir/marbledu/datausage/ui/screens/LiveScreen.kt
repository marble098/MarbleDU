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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.R
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
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.SectionTitle
import ir.marbledu.datausage.ui.StatCard

@Composable
fun LiveScreen(vm: MainViewModel, settings: AppSettings, persian: Boolean) {
    val context = LocalContext.current
    val tick by vm.tick.collectAsState()
    val pings by vm.ping.collectAsState()
    val t = tick
    val speedHistory by vm.speedHistory.collectAsState()
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = FloatingBarClearance),
    ) {
        item {
            SectionTitle(text = context.getString(R.string.live_now))
        }

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
                        diameter = 158.dp,
                        stroke = 15.dp,
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
                        if (network != null && settings.iran.detectOperator) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OperatorBadge(network.operator, size = 26.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    network.operator.name(persian),
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
                    height = 160.dp,
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
                value = if (t == null) "--" else context.getString(
                    R.string.from_to,
                    LiveMonitor.formatHourMinute(t.sessionStart),
                    LiveMonitor.formatHourMinute(t.timestamp),
                ),
                subtitle = if (t == null) null else
                    "${Formats.bytes(t.sessionRxBytes + t.sessionTxBytes, persian)} " +
                        if (persian) "در این نشست" else "this session",
                icon = MIcons.Clock,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { vm.runPingTest() }, modifier = Modifier.weight(1f)) {
                    Icon(MIcons.Timer, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(context.getString(R.string.run_ping_test))
                }
            }
        }

        if (pings.isNotEmpty()) {
            item { SectionTitle(text = context.getString(R.string.average_ping)) }
            items(pings) { result ->
                Card(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(MIcons.Globe, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(result.host, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${context.getString(R.string.jitter)}: ${
                                    Formats.integer(result.jitterMs, persian)
                                } ${context.getString(R.string.ms)}   •   ${
                                    context.getString(R.string.packet_loss)
                                }: ${Formats.integer(result.lossPercent.toLong(), persian)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = if (result.averageMs > 0) {
                                "${Formats.number(result.averageMs, persian, 0)} ${context.getString(R.string.ms)}"
                            } else "--",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (result.averageMs in 1.0..120.0) {
                                MaterialTheme.colorScheme.secondary
                            } else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}
