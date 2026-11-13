package ir.marbledu.datausage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.UsagePlanner
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.ConnectionType
import ir.marbledu.datausage.data.UiMode
import ir.marbledu.datausage.data.DaySummary
import ir.marbledu.datausage.ui.DashboardUi
import ir.marbledu.datausage.ui.HistoryUi
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.IspResolver
import ir.marbledu.datausage.data.JalaliDate
import ir.marbledu.datausage.data.LiveMonitor
import ir.marbledu.datausage.data.NetworkSnapshot
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.service.NotificationFormatter
import ir.marbledu.datausage.ui.AppIcon
import ir.marbledu.datausage.ui.ArcGauge
import ir.marbledu.datausage.ui.BarEntry
import ir.marbledu.datausage.ui.CardHeader
import ir.marbledu.datausage.ui.EmptyState
import ir.marbledu.datausage.ui.FloatingBarClearance
import ir.marbledu.datausage.ui.HeroCard
import ir.marbledu.datausage.ui.InfoChip
import ir.marbledu.datausage.ui.LegendDot
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.MarbleCard
import ir.marbledu.datausage.ui.Metric
import ir.marbledu.datausage.ui.OperatorBadge
import ir.marbledu.datausage.ui.SectionTitle
import ir.marbledu.datausage.ui.SplitBar
import ir.marbledu.datausage.ui.StackedBarChart
import ir.marbledu.datausage.ui.StatCard
import ir.marbledu.datausage.ui.UsageBar
import ir.marbledu.datausage.ui.theme.LocalMarbleColors
import ir.marbledu.datausage.util.UsageAccess

@Composable
fun DashboardScreen(vm: MainViewModel, settings: AppSettings, persian: Boolean) {
    val context = LocalContext.current
    val dashboard by vm.dashboard.collectAsState()
    val usage by vm.usage.collectAsState()
    val history by vm.history.collectAsState()
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = FloatingBarClearance),
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
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = JalaliDate.weekday(System.currentTimeMillis(), persian) + "، " +
                        JalaliDate.fromMillis(System.currentTimeMillis()).long(persian) + "  •  " +
                        JalaliDate.gregorianLong(System.currentTimeMillis(), persian),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary,
                )
            }
        }

        item {
            ConnectionCard(
                vm = vm,
                settings = settings,
                persian = persian,
                onRetry = { vm.refreshWifiOperator() },
            )
        }

        if (!dashboard.hasUsageAccess) {
            item {
                Card(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.errorContainer),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            context.getString(R.string.grant_usage_access),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onErrorContainer,
                        )
                        Text(
                            context.getString(R.string.grant_usage_access_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onErrorContainer,
                        )
                        TextButton(onClick = { UsageAccess.openSettings(context) }) {
                            Text(context.getString(R.string.open))
                        }
                    }
                }
            }
        }

        item {
            ActiveMobileSessionCard(vm, persian)
        }

        if (settings.uiMode == UiMode.MINIMAL) {
            item {
                MinimalDashboardCard(
                    dashboard = dashboard,
                    history = history,
                    settings = settings,
                    persian = persian,
                )
            }
        } else {
            item {
                val mobile = dashboard.todayMobile
                val wifi = dashboard.todayWifi
                val total = mobile.total + wifi.total
                HeroCard {
                    Text(
                        context.getString(R.string.today_usage),
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalContentColor.current.copy(alpha = 0.85f),
                    )
                    Text(
                        Formats.bytes(total, persian),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = when {
                            dashboard.todayVariationPercent > 0 ->
                                "▲ ${Formats.integer(dashboard.todayVariationPercent.toLong(), persian)}" +
                                    (if (persian) "٪ داده‌ی موبایل بیشتر از دیروز" else "% more mobile data than yesterday")
                            dashboard.todayVariationPercent < 0 ->
                                "▼ ${Formats.integer((-dashboard.todayVariationPercent).toLong(), persian)}" +
                                    (if (persian) "٪ داده‌ی موبایل کمتر از دیروز" else "% less mobile data than yesterday")
                            else -> if (persian) "از نیمه‌شب تا الان" else "since midnight"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalContentColor.current.copy(alpha = 0.9f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric(
                            context.getString(R.string.download),
                            Formats.bytes(mobile.rx + wifi.rx, persian),
                            LocalContentColor.current,
                            Modifier.weight(1f),
                            MIcons.Download,
                        )
                        Metric(
                            context.getString(R.string.upload),
                            Formats.bytes(mobile.tx + wifi.tx, persian),
                            LocalContentColor.current,
                            Modifier.weight(1f),
                            MIcons.Upload,
                        )
                    }
                    SplitBar(
                        listOf(
                            mobile.total.toFloat() to Color.White,
                            wifi.total.toFloat() to Color.White.copy(alpha = 0.45f),
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric(
                            context.getString(R.string.cellular),
                            Formats.bytes(mobile.total, persian),
                            LocalContentColor.current,
                            Modifier.weight(1f),
                            MIcons.Signal,
                        )
                        Metric(
                            context.getString(R.string.wifi),
                            Formats.bytes(wifi.total, persian),
                            LocalContentColor.current,
                            Modifier.weight(1f),
                            MIcons.Wifi,
                        )
                    }
                }
            }
            item {
                CompactMetricsCard(dashboard = dashboard, vm = vm, persian = persian)
            }

            if (history.days.takeLast(7).any { it.total > 0 }) {
                item {
                    WeeklyTrendCard(history.days.takeLast(7), persian)
                }
            }

            if (settings.dataCapBytes > 0) {
                item {
                    val used = dashboard.monthMobileBytes
                    val fraction = Formats.percentOf(used, settings.dataCapBytes)
                    val danger = fraction > 0.9f
                    MarbleCard {
                        CardHeader(
                            title = context.getString(R.string.data_cap),
                            icon = MIcons.Shield,
                            tint = if (danger) scheme.error else scheme.primary,
                            trailing = Formats.bytes(settings.dataCapBytes, persian),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ArcGauge(
                                fraction = fraction,
                                startColor = if (danger) scheme.error else marble.heroStart,
                                endColor = if (danger) scheme.error else marble.heroEnd,
                                diameter = 108.dp,
                                stroke = 12.dp,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        Formats.number(fraction * 100.0, persian, 0) + (if (persian) "٪" else "%"),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        context.getString(R.string.used),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Metric(
                                    context.getString(R.string.used),
                                    Formats.bytes(used, persian),
                                    if (danger) scheme.error else scheme.primary,
                                )
                                Metric(
                                    context.getString(R.string.remaining),
                                    Formats.bytes((settings.dataCapBytes - used).coerceAtLeast(0), persian),
                                    scheme.secondary,
                                )
                                if (danger) {
                                    Text(
                                        context.getString(R.string.cap_reached),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = scheme.error,
                                    )
                                }
                            }
                        }
                        // پیش‌بینی بسته: با روند ۷ روز اخیر تا کی دوام می‌آورد؟
                        val forecast = remember(settings.dataCapBytes, used, dashboard.week, settings.billingCycleDay) {
                            UsagePlanner.forecast(
                                capBytes = settings.dataCapBytes,
                                usedBytes = used,
                                recentDailyBytes = dashboard.week.map { it.bytes },
                                nowMillis = System.currentTimeMillis(),
                                billingDay = settings.billingCycleDay,
                            )
                        }
                        if (forecast != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Metric(
                                    if (persian) "دوام بسته" else "Lasts for",
                                    when {
                                        forecast.remainingBytes <= 0L -> if (persian) "تمام شده" else "Used up"
                                        forecast.daysUntilEmpty < 0 -> "—"
                                        else -> Formats.integer(forecast.daysUntilEmpty.toLong(), persian) +
                                            (if (persian) " روز" else " days")
                                    },
                                    if (forecast.willRunOut) scheme.error else scheme.primary,
                                    Modifier.weight(1f),
                                )
                                Metric(
                                    if (persian) "میانگین روزانه" else "Daily average",
                                    Formats.bytes(forecast.averageDailyBytes, persian),
                                    scheme.tertiary,
                                    Modifier.weight(1f),
                                )
                                Metric(
                                    if (persian) "سهم هر روز" else "Daily budget",
                                    Formats.bytes(forecast.dailyBudgetBytes, persian),
                                    scheme.secondary,
                                    Modifier.weight(1f),
                                )
                            }
                            Text(
                                text = when {
                                    forecast.remainingBytes <= 0L ->
                                        if (persian) "بسته تمام شده است." else "Your plan is used up."
                                    forecast.willRunOut -> if (persian) {
                                        "با این روند، بسته " + Formats.integer(
                                            (forecast.daysLeftInCycle - forecast.daysUntilEmpty).toLong(), true,
                                        ) + " روز زودتر از پایان دوره تمام می‌شود."
                                    } else {
                                        "At this pace the plan runs out " +
                                            (forecast.daysLeftInCycle - forecast.daysUntilEmpty) + " days before the cycle ends."
                                    }
                                    else -> if (persian) {
                                        "با این روند، بسته تا پایان دوره (" +
                                            Formats.integer(forecast.daysLeftInCycle.toLong(), true) + " روز دیگر) کافی است."
                                    } else {
                                        "At this pace the plan lasts until the cycle ends (" + forecast.daysLeftInCycle + " days)."
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (forecast.willRunOut) scheme.error else scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (dashboard.hasUsageAccess) {
                item {
                    SectionTitle(
                        text = context.getString(R.string.per_app),
                        action = {
                            TextButton(onClick = { vm.refreshAll() }) {
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
                        val label = OperatorCatalog.specialUidLabel(app.uid, persian) ?: app.label
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(app.packageName.substringBefore(':'), label, size = 34.dp)
                            Spacer(Modifier.width(10.dp))
                            UsageBar(
                                label = label,
                                bytes = app.bytes,
                                fraction = app.bytes.toFloat() / max,
                                persian = persian,
                                trailing = LiveMonitor.formatHourMinute(app.lastTimestamp, persian),
                                color = marble.chart[top.indexOf(app).coerceAtLeast(0) % marble.chart.size],
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }        }


/**
 * کارت «اینترنت فعلی»: نشان و نام اپراتوری که همین الان اینترنت از آن می‌آید.
 * روی وای‌فای/هات‌اسپات هم اپراتور مودم یا گوشی میزبان و هم نوع منبع (هات‌اسپات/مودم)
 * نمایش داده می‌شود و در صورت مسدود شدن توسط VPN یا خطا، دلیل صریح نشان داده می‌شود.
 */
@Composable
private fun ConnectionCard(
    vm: MainViewModel,
    settings: AppSettings,
    persian: Boolean,
    onRetry: () -> Unit,
) {
    val tick by vm.tick.collectAsState()
    val network = tick?.network
    val scheme = MaterialTheme.colorScheme
    val type = network?.type ?: ConnectionType.OFFLINE
    val onWifi = type == ConnectionType.WIFI || type == ConnectionType.ETHERNET
    val override = settings.operatorOverride
    val sim = if (override.isNotBlank()) OperatorCatalog.resolveName(override) else network?.simOperator
    val operator = when {
        network == null -> OperatorCatalog.byId(OperatorCatalog.ID_UNKNOWN)
        type == ConnectionType.CELLULAR && override.isNotBlank() -> OperatorCatalog.resolveName(override)
        else -> network.operator
    }
    val resolved = network?.wifiOperatorResolved == true
    val status = network?.wifiLookupStatus ?: IspResolver.LookupStatus.IDLE
    val sourceKind = network?.wifiSourceKind ?: IspResolver.WifiSourceKind.UNKNOWN
    val sourceLabel = NotificationFormatter.wifiSourceLabel(sourceKind, persian)
    val simLabel = network?.simIdentity?.let { identity ->
        val slot = identity.slotIndex?.let { index ->
            if (persian) "سیم‌کارت ${Formats.integer((index + 1).toLong(), true)}" else "SIM ${index + 1}"
        }
        when {
            slot != null && identity.displayName.isNotBlank() -> "$slot · ${identity.displayName}"
            slot != null -> slot
            identity.displayName.isNotBlank() -> identity.displayName
            else -> null
        }
    }

    MarbleCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OperatorBadge(
                operator,
                size = 42.dp,
                transportIcon = when (type) {
                    ConnectionType.WIFI, ConnectionType.ETHERNET -> MIcons.Wifi
                    ConnectionType.CELLULAR -> MIcons.Signal
                    else -> null
                },
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = if (persian) "اینترنت فعلی" else "Current internet",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = when {
                        type == ConnectionType.OFFLINE -> if (persian) "بدون اتصال" else "Offline"
                        onWifi && !resolved -> when (sourceKind) {
                            IspResolver.WifiSourceKind.HOTSPOT_IPHONE,
                            IspResolver.WifiSourceKind.HOTSPOT_ANDROID,
                            IspResolver.WifiSourceKind.HOTSPOT_WINDOWS,
                            IspResolver.WifiSourceKind.HOTSPOT_METERED,
                            -> if (persian) "وای‌فای ($sourceLabel)" else "Wi-Fi ($sourceLabel)"
                            IspResolver.WifiSourceKind.ETHERNET -> if (persian) "اترنت" else "Ethernet"
                            else -> if (persian) "وای‌فای" else "Wi-Fi"
                        }
                        else -> operator.name(persian)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when {
                        onWifi && resolved -> buildString {
                            append(
                                if (sourceLabel.isNotBlank()) {
                                    if (persian) "از طریق $sourceLabel (وای‌فای)" else "over $sourceLabel (Wi-Fi)"
                                } else {
                                    if (persian) "از طریق وای‌فای" else "over Wi-Fi"
                                },
                            )
                            val asn = network?.ispAsn ?: 0
                            if (asn > 0) {
                                append("  •  ")
                                append(if (persian) "شبکه ${Formats.toPersianDigits(asn.toString())}" else "AS$asn")
                            }
                        }
                        onWifi -> when (status) {
                            IspResolver.LookupStatus.SEARCHING, IspResolver.LookupStatus.IDLE ->
                                if (persian) "در حال شناسایی اپراتور وای‌فای…" else "Identifying Wi-Fi operator…"
                            IspResolver.LookupStatus.CAPTIVE_PORTAL ->
                                if (persian) "نیاز به ورود به صفحه‌ی وای‌فای" else "Wi-Fi sign-in required"
                            IspResolver.LookupStatus.VPN_BLOCKED ->
                                if (persian) "VPN مانع بررسی مستقیم شد (دوباره امتحان کن)" else "VPN blocked direct check (tap re-check)"
                            IspResolver.LookupStatus.FAILED ->
                                if (persian) "اپراتور وای‌فای پیدا نشد — دوباره بزن" else "Couldn't detect Wi-Fi operator — tap re-check"
                            else -> if (persian) "وای‌فای" else "Wi-Fi"
                        }
                        type == ConnectionType.CELLULAR && !network?.mobileTech.isNullOrBlank() ->
                            (if (persian) "داده موبایل" else "Mobile data") + " (${network?.mobileTech})"
                        else -> networkLabel(type.name, persian)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (onWifi && !resolved && (status == IspResolver.LookupStatus.FAILED || status == IspResolver.LookupStatus.VPN_BLOCKED)) {
                        scheme.error
                    } else scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onWifi) {
                TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Icon(MIcons.Refresh, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (persian) "بررسی" else "Re-check", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (settings.uiMode == UiMode.ADVANCED && onWifi && resolved) {
            val detailParts = buildList {
                val ispName = network?.ispName.orEmpty()
                if (ispName.isNotBlank()) {
                    add(if (persian) "سرویس‌دهنده: $ispName" else "ISP: $ispName")
                }
                val methodLabel = detectionMethodLabel(network?.wifiDetectionMethod, persian)
                if (methodLabel.isNotBlank()) add(methodLabel)
            }
            if (detailParts.isNotEmpty()) {
                Text(
                    text = detailParts.joinToString("  •  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (settings.uiMode == UiMode.ADVANCED || type == ConnectionType.CELLULAR) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (settings.uiMode == UiMode.ADVANCED && network?.isValidated == true) {
                    InfoChip(text = if (persian) "آنلاین" else "Online", color = scheme.secondary)
                }
                if (settings.uiMode == UiMode.ADVANCED && network?.vpnActive == true) {
                    InfoChip(text = if (persian) "VPN روشن" else "VPN on", color = scheme.tertiary)
                }
                if (settings.uiMode == UiMode.ADVANCED && onWifi && sourceLabel.isNotBlank()) {
                    InfoChip(text = sourceLabel, color = scheme.primary)
                }
                if (settings.uiMode == UiMode.ADVANCED && onWifi && !network?.wifiSsid.isNullOrBlank()) {
                    InfoChip(text = network?.wifiSsid.orEmpty(), color = scheme.secondary)
                }
                if (type == ConnectionType.CELLULAR && simLabel != null) {
                    InfoChip(text = simLabel, color = scheme.primary)
                } else if (settings.uiMode == UiMode.ADVANCED && onWifi && sim != null) {
                    InfoChip(
                        text = (if (persian) "سیم‌کارت داده: " else "Data SIM: ") + sim.name(persian),
                        color = Color(sim.brandColor).let { c -> if (LocalMarbleColors.current.dark) scheme.primary else c },
                    )
                }
            }
        }
    }
}

private fun detectionMethodLabel(method: IspResolver.DetectionMethod?, persian: Boolean): String = when (method) {
    IspResolver.DetectionMethod.PUBLIC_IP_ASN -> if (persian) "تایید از روی IP عمومی" else "Verified via public IP"
    IspResolver.DetectionMethod.DNS_SERVER_ISP -> if (persian) "تخمین از روی DNS اپراتور" else "Detected via ISP DNS"
    IspResolver.DetectionMethod.IPV6_PREFIX -> if (persian) "تشخیص از روی IPv6 شبکه" else "Detected via IPv6"
    IspResolver.DetectionMethod.DHCP_DOMAIN -> if (persian) "تشخیص از دامنه‌ی شبکه" else "Detected via DHCP domain"
    IspResolver.DetectionMethod.WIFI_SSID -> if (persian) "تخمین از روی نام وای‌فای" else "Matched via Wi-Fi name"
    else -> ""
}

/** نمودار ۷ روز اخیر: ستون‌های دورنگ (موبایل/وای‌فای)، قابل لمس. */
@Composable
private fun WeeklyTrendCard(week: List<DaySummary>, persian: Boolean) {
    val context = LocalContext.current
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme
    var selection by remember(week.size) { mutableStateOf<Int?>(null) }
    val total = week.fold(0L) { acc, d -> acc + d.total }
    MarbleCard {
        CardHeader(
            title = if (persian) "روند ۷ روز اخیر" else "Last 7 days",
            icon = MIcons.Chart,
            tint = scheme.tertiary,
            trailing = Formats.bytes(total, persian),
        )
        StackedBarChart(
            entries = week.map { d ->
                BarEntry(
                    JalaliDate.weekdayShort(d.startMillis, persian) + " " + JalaliDate.axisDay(d.startMillis, persian),
                    listOf(d.mobile.total.toFloat(), d.wifi.total.toFloat()),
                )
            },
            colors = listOf(marble.mobile, marble.wifi),
            height = 144.dp,
            selected = selection,
            onSelect = { selection = if (selection == it) null else it },
            maxLabel = Formats.bytes(week.maxOf { it.total }, persian),
            averageLabel = (if (persian) "میانگین " else "avg ") + Formats.bytes(total / week.size.coerceAtLeast(1), persian),
        )
        val picked = selection?.let { week.getOrNull(it) }
        if (picked != null) {
            Text(
                text = JalaliDate.weekday(picked.startMillis, persian) + "، " +
                    JalaliDate.fromMillis(picked.startMillis).long(persian) + "  •  " +
                    JalaliDate.gregorianNumeric(picked.startMillis, persian),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(context.getString(R.string.total), Formats.bytes(picked.total, persian), scheme.tertiary, Modifier.weight(1f))
                Metric(context.getString(R.string.download), Formats.bytes(picked.all.rx, persian), marble.download, Modifier.weight(1f), MIcons.Download)
                Metric(context.getString(R.string.upload), Formats.bytes(picked.all.tx, persian), marble.upload, Modifier.weight(1f), MIcons.Upload)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(context.getString(R.string.cellular), marble.mobile)
            LegendDot(context.getString(R.string.wifi), marble.wifi)
            if (picked == null) {
                LegendDot(if (persian) "روی ستون‌ها بزن" else "Tap a bar", scheme.outline)
            }
        }
    }
}

@Composable
private fun CompactMetricsCard(dashboard: DashboardUi, vm: MainViewModel, persian: Boolean) {
    val context = LocalContext.current
    val tick by vm.tick.collectAsState()
    val scheme = MaterialTheme.colorScheme
    val mobile = tick?.downBytesPerSecond ?: 0.0
    val upload = tick?.upBytesPerSecond ?: 0.0
    MarbleCard(padding = 14.dp) {
        CardHeader(
            title = if (persian) "آمار این ماه و لحظه‌ای" else "This month & live",
            icon = MIcons.Chart,
            subtitle = if (persian) "مجموع دستگاه در ۳۰ روز گذشته" else "Device totals for the last 30 days",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric(context.getString(R.string.cellular), Formats.bytes(dashboard.monthMobileBytes, persian),
                LocalMarbleColors.current.mobile, Modifier.weight(1f), MIcons.Signal)
            Metric(context.getString(R.string.wifi), Formats.bytes(dashboard.monthWifiBytes, persian),
                LocalMarbleColors.current.wifi, Modifier.weight(1f), MIcons.Wifi)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric(
                if (persian) "سرعت فعلی" else "Current speed",
                Formats.speed(mobile + upload, persian),
                scheme.secondary,
                Modifier.weight(1f),
                MIcons.Speed,
            )
            Metric(
                context.getString(R.string.average_ping),
                if (dashboard.averagePingMs > 0) {
                    "${Formats.number(dashboard.averagePingMs, persian, 0)} ${context.getString(R.string.ms)}"
                } else "—",
                scheme.tertiary,
                Modifier.weight(1f),
                MIcons.Timer,
            )
        }
    }
}

/** جلسه‌ی فعالِ مسیر موبایل؛ بر خلاف uptime اپ، به سیم‌کارت و شمارنده‌ی موبایل متصل است. */
@Composable
private fun ActiveMobileSessionCard(vm: MainViewModel, persian: Boolean) {
    val tick by vm.tick.collectAsState()
    val session = tick?.mobileSession ?: return
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val operator = OperatorCatalog.byId(session.operatorId)
    val slotLabel = session.sim?.slotIndex?.let { slot ->
        if (persian) "سیم‌کارت ${Formats.integer((slot + 1).toLong(), true)}" else "SIM ${slot + 1}"
    }
    val simLabel = session.sim?.displayName?.takeIf { it.isNotBlank() }?.let { name ->
        if (slotLabel != null) "$slotLabel · $name" else name
    } ?: slotLabel ?: if (persian) "سیم‌کارت نامشخص" else "SIM not identified"
    val from = LiveMonitor.formatHourMinute(session.startMillis, persian)
    val to = if (session.isOpen) {
        if (persian) "اکنون" else "now"
    } else {
        LiveMonitor.formatHourMinute(session.endMillis ?: session.lastSampleMillis, persian)
    }

    MarbleCard(padding = 14.dp) {
        CardHeader(
            title = if (persian) "جلسه‌ی فعال داده‌ی موبایل" else "Active mobile data session",
            icon = MIcons.Signal,
            tint = scheme.primary,
            trailing = Formats.bytes(session.totalBytes, persian),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OperatorBadge(operator, size = 34.dp, transportIcon = MIcons.Signal)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "$simLabel · ${operator.name(persian)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "$from – $to",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "↓ ${Formats.bytes(session.rxBytes, persian)}   ↑ ${Formats.bytes(session.txBytes, persian)}" +
                if (session.samplingGaps > 0) {
                    if (persian) "  ·  وقفه‌ی پایش ${Formats.integer(session.samplingGaps.toLong(), true)} بار"
                    else "  ·  ${session.samplingGaps} monitoring gap(s)"
                } else "",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

/** کارت حالت «ساده»: چیده، آرام و فقط برای کاربران عادی و مبتدیان. */
@Composable
private fun MinimalDashboardCard(
    dashboard: DashboardUi,
    history: HistoryUi,
    settings: AppSettings,
    persian: Boolean,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val marble = LocalMarbleColors.current
    val week = history.days.takeLast(7)
    val max = week.maxOfOrNull { it.total } ?: 1L

    MarbleCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    context.getString(R.string.today_usage),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    Formats.bytes(dashboard.todayMobileBytes + dashboard.todayWifiBytes, persian),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            InfoChip(
                text = dashboard.operatorName.ifEmpty { context.getString(R.string.network_unknown) },
                color = scheme.primary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric(context.getString(R.string.cellular), Formats.bytes(dashboard.todayMobileBytes, persian),
                marble.mobile, Modifier.weight(1f), MIcons.Signal)
            Metric(context.getString(R.string.wifi), Formats.bytes(dashboard.todayWifiBytes, persian),
                marble.wifi, Modifier.weight(1f), MIcons.Wifi)
        }
        if (settings.dataCapBytes > 0L) {
            Text(
                text = (if (persian) "باقی‌مانده‌ی بسته: " else "Plan remaining: ") +
                    Formats.bytes((settings.dataCapBytes - dashboard.monthMobileBytes).coerceAtLeast(0), persian),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
        }
        if (week.any { it.total > 0 }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { day ->
                    val fraction = (day.total.toFloat() / max).coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(marble.mobile.copy(alpha = 0.12f + 0.58f * fraction)),
                    )
                }
            }
            Text(
                text = if (persian) "مصرف ۷ روز اخیر" else "Last 7 days",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
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
