package ir.marbledu.datausage.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.LiveMonitor
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.UsageLedger
import ir.marbledu.datausage.data.UsageSession
import ir.marbledu.datausage.ui.CardHeader
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MarbleCard

/** Session bytes are observed samples, not a substitute for system billing totals. */
@Composable
fun SessionsCard(sessions: List<UsageSession>, persian: Boolean, showDate: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    MarbleCard {
        CardHeader(
            title = if (persian) "نشست‌های اتصال" else "Connection sessions",
            icon = MIcons.Clock,
            tint = scheme.primary,
            trailing = Formats.integer(sessions.size.toLong(), persian),
        )
        Text(
            if (persian) "فقط مصرف مشاهده‌شده هنگام پایش؛ زمان‌ها تقریبی‌اند و ممکن است بخشی از مصرف ثبت نشده باشد. نشست‌های عبوری از نیمه‌شب جدا می‌شوند."
            else "Observed while monitoring only; times are approximate and some traffic may be missing. Sessions are split at midnight.",
            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
        )
        if (sessions.isEmpty()) {
            Text(if (persian) "هنوز نشستی ثبت نشده است." else "No sessions recorded yet.",
                style = MaterialTheme.typography.bodyMedium)
        }
        sessions.forEachIndexed { index, session ->
            if (index > 0) HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
            val type = when (session.transport) {
                UsageLedger.MOBILE -> if (persian) "داده موبایل" else "Mobile data"
                "ethernet" -> if (persian) "اترنت" else "Ethernet"
                else -> if (persian) "وای‌فای" else "Wi-Fi"
            }
            val operator = OperatorCatalog.byId(session.operatorId).name(persian)
            val end = session.endMillis ?: session.lastSampleMillis
            val minutes = ((end - session.startMillis).coerceAtLeast(0L) / 60_000L)
            Column(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(type, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(Formats.bytes(session.totalBytes, persian), style = MaterialTheme.typography.titleSmall,
                        color = scheme.primary, fontWeight = FontWeight.Bold)
                }
                Text(listOf(operator, session.networkName, session.sim?.displayName.orEmpty(),
                    session.sim?.slotIndex?.let { if (persian) "سیم ${Formats.integer((it + 1).toLong(), true)}" else "SIM ${it + 1}" }.orEmpty())
                    .filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                val date = if (showDate) LiveMonitor.formatDate(session.startMillis, persian) + " · " else ""
                val until = if (session.isOpen) { if (persian) "اکنون" else "Now" } else LiveMonitor.formatHourMinute(end, persian)
                Text(date + LiveMonitor.formatHourMinute(session.startMillis, persian) + " – " + until +
                    "  ·  " + Formats.integer(minutes, persian) + if (persian) " دقیقه" else " min",
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Text("↓ ${Formats.bytes(session.rxBytes, persian)}   ↑ ${Formats.bytes(session.txBytes, persian)}" +
                    if (session.samplingGaps > 0) {
                        if (persian) "  ·  وقفه پایش: ${Formats.integer(session.samplingGaps.toLong(), true)}" else "  ·  Monitoring gaps: ${session.samplingGaps}"
                    } else "", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }
    }
}
