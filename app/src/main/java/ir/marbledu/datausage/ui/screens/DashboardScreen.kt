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
    val tick by vm.tick.collectAsState()
    val history by vm.history.collectAsState()
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = FloatingBarClearance),
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
                network = tick?.network,
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
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = context.getString(R.string.cellular),
                        value = Formats.bytes(dashboard.monthMobileBytes, persian),
                        subtitle = if (persian) "۳۰ روز گذشته" else "last 30 days",
                        icon = MIcons.Signal,
                        accent = marble.mobile,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = context.getString(R.string.wifi),
                        value = Formats.bytes(dashboard.monthWifiBytes, persian),
                        subtitle = if (persian) "۳۰ روز گذشته" else "last 30 days",
                        icon = MIcons.Wifi,
                        accent = marble.wifi,
                        modifier = Modifier.weight(1f),
                    )
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
                    accent = scheme.secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (history.days.takeLast(7).any { it.total > 0 }) {
                item {
                    WeeklyTrendCard(history.days.takeLast(7), persian)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = context.getString(R.string.average_speed),
                        value = Formats.speed(tick?.averageSpeed ?: 0.0, persian),
                        icon = MIcons.Speed,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = context.getString(R.string.average_ping),
                        value = if (dashboard.averagePingMs > 0) {
                            "${Formats.number(dashboard.averagePingMs, persian, 0)} ${context.getString(R.string.ms)}"
                        } else "—",
                        icon = MIcons.Timer,
                        accent = scheme.tertiary,
                        modifier = Modifier.weight(1f),
                    )
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
                                diameter = 128.dp,
                                stroke = 14.dp,
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

            if (tick != null) {
                item {
                    SessionCard(vm, persian)
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
    network: NetworkSnapshot?,
    settings: AppSettings,
    persian: Boolean,
    onRetry: () -> Unit,
) {
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

    MarbleCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OperatorBadge(
                operator,
                size = 56.dp,
                transportIcon = when (type) {
                    ConnectionType.WIFI, ConnectionType.ETHERNET -> MIcons.Wifi
                    ConnectionType.CELLULAR -> MIcons.Signal
                    else -> null
                },
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                    style = MaterialTheme.typography.titleLarge,
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
        if (onWifi && resolved) {
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
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (network?.isValidated == true) {
                InfoChip(text = if (persian) "آنلاین" else "Online", color = scheme.secondary)
            }
            if (network?.vpnActive == true) {
                InfoChip(text = if (persian) "VPN روشن" else "VPN on", color = scheme.tertiary)
            }
            if (onWifi && sourceLabel.isNotBlank()) {
                InfoChip(text = sourceLabel, color = scheme.primary)
            }
            if (onWifi && !network?.wifiSsid.isNullOrBlank()) {
                InfoChip(text = network?.wifiSsid.orEmpty(), color = scheme.secondary)
            }
            if (onWifi && sim != null) {
                InfoChip(
                    text = (if (persian) "سیم‌کارت: " else "SIM: ") + sim.name(persian),
                    color = Color(sim.brandColor).let { c -> if (LocalMarbleColors.current.dark) scheme.primary else c },
                )
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
            height = 170.dp,
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
private fun SessionCard(vm: MainViewModel, persian: Boolean) {
    val tick by vm.tick.collectAsState()
    val context = LocalContext.current
    val t = tick ?: return
    val start = remember(t.sessionStart, persian) { LiveMonitor.formatHourMinute(t.sessionStart, persian) }
    val now = remember(t.timestamp, persian) { LiveMonitor.formatHourMinute(t.timestamp, persian) }
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

    MarbleCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = context.getString(R.string.this_month),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = dashboard.operatorName.ifEmpty { context.getString(R.string.network_unknown) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = context.getString(R.string.today_usage),
                    value = Formats.bytes(dashboard.todayMobileBytes + dashboard.todayWifiBytes, persian),
                    icon = MIcons.Chart,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = context.getString(R.string.remaining),
                    value = if (settings.dataCapBytes > 0L) {
                        Formats.bytes((settings.dataCapBytes - dashboard.monthMobileBytes).coerceAtLeast(0), persian)
                    } else "—",
                    icon = MIcons.Shield,
                    modifier = Modifier.weight(1f),
                )
            }
            if (week.any { it.total > 0 }) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    week.forEach { day ->
                        val fraction = (day.total.toFloat() / max)
                        Box(
                            Modifier
                                .weight(1f)
                                .height(32.dp)
                                .background(marble.mobile.copy(alpha = 0.15f + 0.55f * fraction)),
                        )
                    }
                }
                Text(
                    text = if (persian) "۷ روز اخیر" else "Last 7 days",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
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
