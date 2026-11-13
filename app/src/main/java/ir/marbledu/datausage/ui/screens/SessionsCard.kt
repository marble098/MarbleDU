package ir.marbledu.datausage.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.LiveMonitor
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.UsageLedger
import ir.marbledu.datausage.data.UsageSession
import ir.marbledu.datausage.ui.CardHeader
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MarbleCard
import ir.marbledu.datausage.ui.OperatorBadge
import ir.marbledu.datausage.ui.theme.LocalMarbleColors

/** یک تبِ داخل کارت نشست‌ها: عنوان + فیلتر + نشان رنگی. */
private data class SessionTab(
    val label: String,
    val color: Color,
    val filter: (UsageSession) -> Boolean,
)

private fun isMobile(session: UsageSession) = session.transport == UsageLedger.MOBILE

/**
 * کارت «نشست‌های اتصال».
 *
 * - اگر [tabbed] روشن باشد، داخل کارت تب‌بندی می‌شود: «همه»، «داده موبایل»،
 *   «وای‌فای» و بعد یک تب برای هر اپراتوری که در بازه نشست دارد (داده‌ای و
 *   وای‌فای هر اپراتور جدا).
 * - فقط [maxVisible] نشست اول دیده می‌شود؛ اگر تعداد بیشتر باشد، فهرست
 *   با ارتفاع ثابت داخل خودش اسکرول می‌شود تا صفحه‌ی خانه بی‌نهایت بلند نشود.
 * - هر ردیف: نشان اپراتور، نوع اتصال، زمان شروع تا پایان، مدت و مصرف.
 *
 * Session bytes are observed samples, not a substitute for system billing totals.
 */
@Composable
fun SessionsCard(
    sessions: List<UsageSession>,
    persian: Boolean,
    showDate: Boolean = false,
    tabbed: Boolean = true,
    maxVisible: Int = 4,
    tall: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val marble = LocalMarbleColors.current

    val tabs = remember(sessions, persian, tabbed, marble.mobile, marble.wifi) {
        if (!tabbed) emptyList() else buildSessionTabs(sessions, persian, marble.mobile, marble.wifi)
    }
    var selectedTab by rememberSaveable(sessions.size, tabbed) { mutableIntStateOf(0) }
    val safeTab = selectedTab.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
    val current = tabs.getOrNull(safeTab)
    val visible = remember(sessions, safeTab, tabs, tabbed) {
        if (current == null) sessions else sessions.filter(current.filter)
    }
    val shown = if (maxVisible <= 0) visible else visible.take(maxVisible)
    val scrollable = visible.size > shown.size
    val rowHeight = if (tall) 92.dp else 84.dp
    val listHeight = rowHeight * (maxVisible.coerceAtLeast(1)) + (if (scrollable) 18.dp else 0.dp)

    MarbleCard(padding = 14.dp) {
        CardHeader(
            title = if (persian) "نشست‌های اتصال" else "Connection sessions",
            icon = MIcons.Clock,
            tint = scheme.primary,
            subtitle = if (persian) {
                "فقط مصرفِ دیده‌شده هنگام پایش؛ زمان‌ها تقریبی‌اند."
            } else {
                "Only traffic seen while monitoring; times are approximate."
            },
            trailing = Formats.integer(visible.size.toLong(), persian),
        )

        if (tabbed && tabs.size > 1) {
            SessionTabs(
                tabs = tabs,
                selected = safeTab,
                persian = persian,
                onSelect = { selectedTab = it },
            )
        }

        if (visible.isEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape)
                        .background(scheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(MIcons.Clock, null, Modifier.size(18.dp), tint = scheme.primary)
                }
                Text(
                    text = if (persian) "هنوز نشستی در این بخش ثبت نشده است." else "No sessions here yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        } else {
            Box(Modifier.fillMaxWidth().heightIn(max = if (scrollable) listHeight else rowHeight * maxVisible)) {
                LazyColumn(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(shown) { session ->
                        SessionRow(
                            session = session,
                            persian = persian,
                            showDate = showDate,
                            accent = current?.color ?: scheme.primary,
                        )
                    }
                }
            }
            if (scrollable) {
                val remaining = visible.size - shown.size
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        MIcons.ChevronEnd,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (persian) {
                            "${Formats.integer(remaining.toLong(), true)} نشست دیگر — برای دیدن همه اسکرول کن"
                        } else {
                            "${Formats.integer(remaining.toLong(), false)} more — scroll inside the list"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** تب‌های داخل کارت: همه / موبایل / وای‌فای / هر اپراتور (جدا و رنگی). */
@Composable
private fun SessionTabs(
    tabs: List<SessionTab>,
    selected: Int,
    persian: Boolean,
    onSelect: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainerHigh.copy(alpha = 0.7f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selected
            val container by animateColorAsState(
                targetValue = if (isSelected) tab.color.copy(alpha = 0.20f) else Color.Transparent,
                label = "session-tab-bg",
            )
            val tint by animateColorAsState(
                targetValue = if (isSelected) tab.color else scheme.onSurfaceVariant,
                label = "session-tab-tint",
            )
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.97f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "session-tab-scale",
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .background(container)
                    .semantics { this.selected = isSelected }
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    Modifier
                        .size(if (isSelected) 8.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) tab.color else tab.color.copy(alpha = 0.55f)),
                )
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = tint,
                    maxLines = 1,
                    modifier = Modifier.scale(scale),
                )
            }
        }
    }
}

/** یک ردیف نشست؛ چیدمان تازه، خوانا و یک‌دست. */
@Composable
private fun SessionRow(
    session: UsageSession,
    persian: Boolean,
    showDate: Boolean,
    accent: Color,
) {
    val scheme = MaterialTheme.colorScheme
    val marble = LocalMarbleColors.current
    val mobile = isMobile(session)
    val operator = OperatorCatalog.byId(session.operatorId)
    val end = session.endMillis ?: session.lastSampleMillis
    val minutes = ((end - session.startMillis).coerceAtLeast(0L) / 60_000L)
    val typeLabel = when {
        mobile -> if (persian) "داده موبایل" else "Mobile data"
        session.transport == "ethernet" -> if (persian) "اترنت" else "Ethernet"
        else -> if (persian) "وای‌فای" else "Wi-Fi"
    }
    val tint = if (mobile) marble.mobile else marble.wifi
    val simLabel = session.sim?.slotIndex?.let { slot ->
        if (persian) "سیم ${Formats.integer((slot + 1).toLong(), true)}" else "SIM ${slot + 1}"
    }
    val subParts = listOfNotNull(
        typeLabel,
        operator.name(persian).takeIf { it.isNotBlank() && !operator.id.startsWith("unknown") },
        session.networkName.takeIf { it.isNotBlank() },
        session.sim?.displayName?.takeIf { it.isNotBlank() },
        simLabel,
    ).distinct()

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainer.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box {
            OperatorBadge(
                operator = operator,
                size = 36.dp,
                transportIcon = if (mobile) MIcons.Signal else MIcons.Wifi,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (session.isOpen) {
                        if (persian) "در جریان" else "Live"
                    } else {
                        typeLabel
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (session.isOpen) accent else scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (session.isOpen) {
                    Spacer(Modifier.width(5.dp))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = Formats.bytes(session.totalBytes, persian),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                )
            }
            Text(
                text = subParts.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val date = if (showDate) LiveMonitor.formatDate(session.startMillis, persian) + " · " else ""
            val until = if (session.isOpen) {
                if (persian) "اکنون" else "now"
            } else {
                LiveMonitor.formatHourMinute(end, persian)
            }
            Text(
                text = buildString {
                    append(date)
                    append(LiveMonitor.formatHourMinute(session.startMillis, persian))
                    append(" – ")
                    append(until)
                    append("  ·  ")
                    append(Formats.integer(minutes, persian))
                    append(if (persian) " دقیقه" else " min")
                    if (session.samplingGaps > 0) {
                        append("  ·  ")
                        append(
                            if (persian) "وقفه ${Formats.integer(session.samplingGaps.toLong(), true)}"
                            else "${session.samplingGaps} gaps",
                        )
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "↓ ${Formats.bytes(session.rxBytes, persian)}   ↑ ${Formats.bytes(session.txBytes, persian)}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 1,
            )
        }
    }
}

/**
 * ساخت تب‌ها: «همه»، «داده موبایل»، «وای‌فای» و بعد هر اپراتوری که در بازه
 * نشست دارد — برای اپراتورهای دارای هر دو نوع داده، دو تب جدا ساخته می‌شود
 * (مثلاً «ایرانسل موبایل» و «ایرانسل وای‌فای») دقیقاً همان‌طور که خواسته شده.
 */
private fun buildSessionTabs(
    sessions: List<UsageSession>,
    persian: Boolean,
    mobileColor: Color,
    wifiColor: Color,
): List<SessionTab> {
    val mobile = sessions.filter(::isMobile)
    val wifi = sessions.filter { !isMobile(it) }
    val mobileOpIds = mobile.map { it.operatorId }.distinct()
    val wifiOpIds = wifi.map { it.operatorId }.distinct()

    val list = ArrayList<SessionTab>()
    list.add(
        SessionTab(
            label = if (persian) "همه" else "All",
            color = mobileColor,
            filter = { true },
        ),
    )
    if (mobile.isNotEmpty()) {
        list.add(
            SessionTab(
                label = if (persian) "داده موبایل" else "Mobile",
                color = mobileColor,
                filter = ::isMobile,
            ),
        )
    }
    if (wifi.isNotEmpty()) {
        list.add(
            SessionTab(
                label = if (persian) "وای‌فای" else "Wi-Fi",
                color = wifiColor,
                filter = { !isMobile(it) },
            ),
        )
    }
    mobileOpIds.forEach { id ->
        val operator = OperatorCatalog.byId(id)
        val name = operator.name(persian)
        list.add(
            SessionTab(
                label = if (persian) "$name · موبایل" else "$name · mobile",
                color = Color(operator.brandColor),
                filter = { isMobile(it) && it.operatorId == id },
            ),
        )
    }
    wifiOpIds.forEach { id ->
        val operator = OperatorCatalog.byId(id)
        val name = operator.name(persian)
        list.add(
            SessionTab(
                label = if (persian) "$name · وای‌فای" else "$name · Wi-Fi",
                color = Color(operator.accentColor),
                filter = { !isMobile(it) && it.operatorId == id },
            ),
        )
    }
    return list
}
