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
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.SectionTitle
import ir.marbledu.datausage.ui.StatCard

@Composable
fun LiveScreen(vm: MainViewModel, settings: AppSettings, persian: Boolean) {
    val context = LocalContext.current
    val tick by vm.tick.collectAsState()
    val pings by vm.ping.collectAsState()
    val t = tick

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
    ) {
        item {
            SectionTitle(text = context.getString(R.string.live_now))
        }

        item {
            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(MIcons.Download, null, Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            context.getString(R.string.download),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            Formats.speed(t?.downBytesPerSecond ?: 0.0, persian),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    LinearWavyProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(MIcons.Upload, null, Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            context.getString(R.string.upload),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            Formats.speed(t?.upBytesPerSecond ?: 0.0, persian),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
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
