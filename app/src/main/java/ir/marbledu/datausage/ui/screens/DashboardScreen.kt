package ir.marbledu.datausage.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.AppSurgeDetector
import ir.marbledu.datausage.data.ConnectionType
import ir.marbledu.datausage.data.DaySummary
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.IspResolver
import ir.marbledu.datausage.data.JalaliDate
import ir.marbledu.datausage.data.LiveMonitor
import ir.marbledu.datausage.data.NetworkSnapshot
import ir.marbledu.datausage.data.OffPeakWindow
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.SimIdentity
import ir.marbledu.datausage.data.UiMode
import ir.marbledu.datausage.data.UsageLedger
import ir.marbledu.datausage.data.UsagePlanner
import ir.marbledu.datausage.service.NotificationFormatter
import ir.marbledu.datausage.ui.AppIcon
import ir.marbledu.datausage.ui.ArcGauge
import ir.marbledu.datausage.ui.BarEntry
import ir.marbledu.datausage.ui.CardHeader
import ir.marbledu.datausage.ui.DashboardUi
import ir.marbledu.datausage.ui.EmptyState
import ir.marbledu.datausage.ui.FloatingBarClearance
import ir.marbledu.datausage.ui.GlassPanel
import ir.marbledu.datausage.ui.HeroCard
import ir.marbledu.datausage.ui.HistoryUi
import ir.marbledu.datausage.ui.InfoChip
import ir.marbledu.datausage.ui.InsightsUi
import ir.marbledu.datausage.ui.LargeTitleScaffold
import ir.marbledu.datausage.ui.LegendDot
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.MarbleCard
import ir.marbledu.datausage.ui.Metric
import ir.marbledu.datausage.ui.OffPeakUi
import ir.marbledu.datausage.ui.OperatorBadge
import ir.marbledu.datausage.ui.theme.deepBrandColor
import ir.marbledu.datausage.ui.SectionTitle
import ir.marbledu.datausage.ui.SplitBar
import ir.marbledu.datausage.ui.StackedBarChart
import ir.marbledu.datausage.ui.StatCard
import ir.marbledu.datausage.ui.UsageBar
import ir.marbledu.datausage.ui.theme.LocalMarbleColors
import ir.marbledu.datausage.ui.theme.accessibleColor
import ir.marbledu.datausage.util.UsageAccess

@Composable
fun DashboardScreen(
    vm: MainViewModel,
    settings: AppSettings,
    persian: Boolean,
    onOpenOperatorDetail: (operatorId: String, sim: SimIdentity?) -> Unit,
) {
    val context = LocalContext.current
    val dashboard by vm.dashboard.collectAsState()
    val usage by vm.usage.collectAsState()
    val history by vm.history.collectAsState()
    val sessionTick by vm.tick.collectAsState()
    val appSurges by vm.appSurges.collectAsState()
    val offPeak by vm.offPeak.collectAsState()
    val appBudget by vm.appBudget.collectAsState()
    val surgeWatch by vm.surgeWatch.collectAsState()
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showOffPeakDialog by remember { mutableStateOf(false) }
    LaunchedEffect(selectedTab, settings.offPeakEnabled) {
        if (selectedTab == 1 && settings.offPeakEnabled) vm.loadOffPeak()
        if (selectedTab == 1) vm.loadAppBudget()
        if (selectedTab == 1) vm.loadSurgeWatch()
    }
    val homeTabs = if (persian) listOf("امروز", "بسته و اپراتورها") else listOf("Today", "Plan & operators")
    val listState = rememberLazyListState()
    LargeTitleScaffold(
        title = context.getString(R.string.app_name),
        listState = listState,
        subtitle = JalaliDate.weekday(System.currentTimeMillis(), persian) + "، " +
            JalaliDate.fromMillis(System.currentTimeMillis()).long(persian) + "  •  " +
            JalaliDate.gregorianLong(System.currentTimeMillis(), persian),
        tabs = homeTabs,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
    ) {
        if (selectedTab == 0) {
        item {
            ConnectionCard(
                vm = vm,
                settings = settings,
                persian = persian,
                onRetry = { vm.refreshWifiOperator() },
            )
        }

        if (!dashboard.hasUsageAccess) {
            item { UsageAccessBanner(persian = persian) }
        }

        // «هشدار زودهنگام جهش مصرف»: فقط وقتی واقعاً خبری هست، بالای صفحه می‌آید
        // تا صفحه‌ی خانه شلوغ نشود؛ نسخه‌ی همیشه‌رو در تب «بسته و اپراتورها» است.
        if (surgeWatch.result?.isAlert == true) {
            item {
                SurgeWatchCard(
                    watch = surgeWatch,
                    persian = persian,
                    onRefresh = { vm.loadSurgeWatch(force = true) },
                )
            }
        }

        if (settings.sessionsCardEnabled) {
            item {
                val since = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DAY_OF_YEAR, -29)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                // فقط چند نشست اول خوانده می‌شود؛ بقیه با اسکرول داخل خود کارت دیده می‌شوند.
                val recent = remember(sessionTick?.timestamp, since, settings.sessionsVisibleCount) {
                    UsageLedger.recentSessions(context, since).take(40)
                }
                SessionsCard(
                    sessions = recent,
                    persian = persian,
                    showDate = true,
                    tabbed = settings.sessionsTabsEnabled,
                    maxVisible = settings.sessionsVisibleCount,
                )
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
                CompactMetricsCard(dashboard = dashboard, vm = vm, persian = persian)
            }

            // بینش مصرف: تحلیل الگوی ۳۰ روز اخیر و بودجه‌ی امروز.
            item {
                val insightState: InsightsUi by vm.insights.collectAsState()
                InsightsCard(insights = insightState, persian = persian)
            }

            if (appSurges.items.isNotEmpty()) {
                item {
                    DashboardSurgeAlertCard(
                        surges = appSurges.items.take(3),
                        persian = persian,
                        onOpenAppSettings = { pkg -> UsageAccess.openAppDataUsageSettings(context, pkg) },
                    )
                }
            }

            if (history.days.takeLast(7).any { it.total > 0 }) {
                item {
                    WeeklyTrendCard(history.days.takeLast(7), persian)
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
        } else {
            item {
                OperatorRangeCard(
                    vm = vm,
                    persian = persian,
                    onOpenOperatorDetail = onOpenOperatorDetail,
                )
            }

            if (settings.dataCapBytes > 0) {
                item {
                    val excludedOffPeak = if (settings.offPeakEnabled && settings.offPeakExcludeFromMainCap) {
                        offPeak.monthMobileOffPeak
                    } else 0L
                    val used = (dashboard.monthMobileBytes - excludedOffPeak).coerceAtLeast(0L)
                    val fraction = Formats.percentOf(used, settings.dataCapBytes)
                    val danger = fraction > 0.9f
                    MarbleCard {
                        CardHeader(
                            title = context.getString(R.string.data_cap),
                            icon = MIcons.Shield,
                            tint = if (danger) scheme.error else scheme.primary,
                            trailing = Formats.bytes(settings.dataCapBytes, persian),
                        )
                        if (excludedOffPeak > 0L) {
                            InfoChip(
                                text = if (persian) {
                                    "بدون ${Formats.bytes(excludedOffPeak, true)} مصرف ساعات رایگان"
                                } else {
                                    "Excludes ${Formats.bytes(excludedOffPeak, false)} off-peak"
                                },
                                color = scheme.tertiary,
                            )
                        }
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
            } else {
                item {
                    MarbleCard {
                        CardHeader(
                            title = if (persian) "سقف مصرف" else "Data cap",
                            icon = MIcons.Shield,
                            tint = scheme.primary,
                        )
                        Text(
                            text = if (persian) {
                                "برای دیدن پیشرفت بسته، پیش‌بینی روزهای باقی‌مانده و هزینه‌ی آن، سقف مصرف را در تنظیمات تعیین کن."
                            } else {
                                "Set a data cap in Settings to see plan progress, a runway forecast and cost."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                OffPeakPlanCard(
                    settings = settings,
                    offPeak = offPeak,
                    persian = persian,
                    onConfigure = { showOffPeakDialog = true },
                )
            }

            // قابلیت تازه‌ی ۱.۶.۴: سهم هر اپ از باقی‌مانده‌ی بسته + پایش جهش مصرف.
            item {
                AppBudgetCard(budget = appBudget, persian = persian)
            }
            item {
                SurgeWatchCard(
                    watch = surgeWatch,
                    persian = persian,
                    onRefresh = { vm.loadSurgeWatch(force = true) },
                )
            }
        }
    }

    if (showOffPeakDialog) {
        OffPeakConfigDialog(
            settings = settings,
            persian = persian,
            onDismiss = { showOffPeakDialog = false },
            onSave = { enabled, startH, endH, capBytes, excludeMain ->
                vm.setOffPeakEnabled(enabled)
                vm.setOffPeakWindow(startH, endH)
                vm.setOffPeakCapBytes(capBytes)
                vm.setOffPeakExcludeFromMainCap(excludeMain)
                showOffPeakDialog = false
            },
        )
    }
}


/**
 * کارت «اپراتور و اینترنت فعلی» — کارت شاخص صفحه‌ی خانه.
 *
 * زبان طراحی این کارت عمداً «کارت اعتباری/شناسنامه» است، نه فهرست ساده:
 *
 *  ┌────────────────────────────────────────────────────────┐
 *  │  ◉ لوگو    اینترنت فعلی · زنده        [ در حال پایش ]  │
 *  │            Irancell · LTE                              │
 *  │  5G · سیم‌کارت ۱ · MTN                      (نشان آبی)  │
 *  │  ───────────────────────────────────────────────────   │
 *  │  ↓ ۲۴٫۱ ک.ب/ث      ↑ ۳٫۴ ک.ب/ث      پینگ ۴۲ ms       │
 *  └────────────────────────────────────────────────────────┘
 *
 *  - پس‌زمینه‌ی گرادیانی از رنگ خودِ اپراتور (امضای رنگی هر اپراتور)؛
 *  - لوگو در کاشی سفید با حلقه‌ی نازک، اندازه‌ی بزرگ‌تر و وسط‌چین؛
 *  - نام اپراتور درشت و نام لاتین/فنی آن ریز و کم‌رنگ زیرش (سلسله‌مراتب روشن)؛
 *  - چیپ‌های فناوری شبکه، شماره‌ی سیم‌کارت و منبع وای‌فای/هات‌اسپات؛
 *  - سرعت زنده و پینگ در یک ردیف پایانی با خط جداکننده.
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
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val marble = LocalMarbleColors.current
    val type = network?.type ?: ConnectionType.OFFLINE
    val onWifi = type == ConnectionType.WIFI || type == ConnectionType.ETHERNET
    val override = settings.operatorOverride
    val operator = when {
        network == null -> OperatorCatalog.byId(OperatorCatalog.ID_UNKNOWN)
        type == ConnectionType.CELLULAR && override.isNotBlank() -> OperatorCatalog.resolveName(override)
        else -> network.operator
    }
    val resolved = network?.wifiOperatorResolved == true
    val status = network?.wifiLookupStatus ?: IspResolver.LookupStatus.IDLE
    val sourceKind = network?.wifiSourceKind ?: IspResolver.WifiSourceKind.UNKNOWN
    val sourceLabel = NotificationFormatter.wifiSourceLabel(sourceKind, persian)
    val brand = Color(operator.brandColor)
    val accent = Color(operator.accentColor)
    val online = network?.isValidated == true && type != ConnectionType.OFFLINE

    val slotLabel = network?.simIdentity?.let { identity ->
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

    // نام نمایشی و زیرنویس فنی: نام فارسی درشت، نام لاتین/فنی ریز زیرش.
    val displayName: String
    val technicalName: String
    when {
        type == ConnectionType.OFFLINE -> {
            displayName = if (persian) "بدون اتصال" else "Offline"
            technicalName = if (persian) "اینترنت قطع است" else "No internet right now"
        }
        onWifi && !resolved -> {
            displayName = when (sourceKind) {
                IspResolver.WifiSourceKind.HOTSPOT_IPHONE,
                IspResolver.WifiSourceKind.HOTSPOT_ANDROID,
                IspResolver.WifiSourceKind.HOTSPOT_WINDOWS,
                IspResolver.WifiSourceKind.HOTSPOT_METERED,
                -> if (persian) "هات‌اسپات" else "Hotspot"
                IspResolver.WifiSourceKind.ETHERNET -> if (persian) "اترنت" else "Ethernet"
                else -> if (persian) "وای‌فای" else "Wi-Fi"
            }
            technicalName = when (status) {
                IspResolver.LookupStatus.SEARCHING, IspResolver.LookupStatus.IDLE ->
                    if (persian) "در حال شناسایی اپراتور…" else "Identifying operator…"
                IspResolver.LookupStatus.CAPTIVE_PORTAL ->
                    if (persian) "نیاز به ورود به صفحه‌ی وای‌فای" else "Sign-in required"
                IspResolver.LookupStatus.VPN_BLOCKED ->
                    if (persian) "VPN مانع بررسی شد" else "Blocked by VPN"
                IspResolver.LookupStatus.FAILED ->
                    if (persian) "اپراتور پیدا نشد" else "Operator not detected"
                else -> if (persian) "شبکه‌ی محلی" else "Local network"
            }
        }
        else -> {
            displayName = operator.name(persian)
            technicalName = buildList {
                if (!persian) operator.fa.takeIf { it.isNotBlank() }?.let { add(it) }
                val asn = network?.ispAsn ?: 0
                if (onWifi && sourceLabel.isNotBlank()) add(sourceLabel)
                if (onWifi && asn > 0) add(if (persian) "شبکه ${Formats.toPersianDigits(asn.toString())}" else "AS$asn")
                if (type == ConnectionType.CELLULAR && !network?.mobileTech.isNullOrBlank()) {
                    add(network?.mobileTech.orEmpty())
                }
            }.distinct().joinToString("  ·  ")
        }
    }
    val fallbackTechnical = when {
        type == ConnectionType.CELLULAR -> operator.en
        onWifi -> if (persian) "اینترنت ثابت" else "Fixed internet"
        else -> ""
    }

    // پالت کارت: رنگ برند اپراتور با کنتراست تضمین‌شده برای متن روی گرادیان.
    val dark = marble.dark
    // گرادیان کارت از رنگ خودِ اپراتور ساخته می‌شود، ولی تا حدی که متن سفید
    // رویش خوانا بماند؛ هر اپراتور «امضای رنگی» خودش را می‌گیرد.
    val offline = type == ConnectionType.OFFLINE
    val heroStart = if (offline) scheme.surfaceContainerHigh else deepBrandColor(brand, 5.6f)
    // هر دو سرِ گرادیان باید با متن سفید کنتراست کافی داشته باشند؛ اگر فقط سرِ
    // اول تیره شود، رنگ مکمل برخی اپراتورها (زرد/نارنجی) انتهای کارت را روشن
    // می‌کند و متن آن‌جا کم‌خوان می‌شود.
    val heroEnd = if (offline) {
        scheme.surfaceContainer
    } else {
        deepBrandColor(verticalPartner(heroStart, accent), 5.6f)
    }
    val ink = if (offline) scheme.onSurface else Color.White
    val inkSoft = ink.copy(alpha = 0.88f)
    val shape = RoundedCornerShape(28.dp)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(heroStart, heroEnd))),
    ) {
        // درخشش نرم و حلقه‌های تزئینی: حس «هولوگرام» بدون شلوغی.
        Canvas(Modifier.matchParentSize().clip(shape)) {
            drawCircle(
                brush = Brush.radialGradient(
                    // روی کارت رنگی، درخشش کم است تا متن سفید کم‌خوان نشود.
                    listOf(Color.White.copy(alpha = if (!offline) 0.05f else if (dark) 0.16f else 0.30f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.02f),
                    radius = size.width * 0.55f,
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.92f, size.height * 0.02f),
            )
            drawCircle(
                color = ink.copy(alpha = 0.06f),
                radius = size.width * 0.34f,
                center = Offset(size.width * 0.04f, size.height * 1.06f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(
                            alpha = if (!offline) 0.04f else if (dark) 0.08f else 0.18f,
                        ),
                        Color.Transparent,
                    ),
                    endY = size.height * 0.40f,
                ),
                size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.40f),
            )
        }
        Box(
            Modifier.matchParentSize().border(
                1.dp,
                ink.copy(alpha = if (dark) 0.14f else 0.22f),
                shape,
            ),
        )

        CompositionLocalProvider(LocalContentColor provides ink) {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ── سربرگ: لوگوی اپراتور + نام + وضعیت
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OperatorBadge(
                        operator,
                        size = 54.dp,
                        transportIcon = when (type) {
                            ConnectionType.WIFI, ConnectionType.ETHERNET -> MIcons.Wifi
                            ConnectionType.CELLULAR -> MIcons.Signal
                            else -> null
                        },
                    )
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (online) Color(0xFF4ADE80) else ink.copy(alpha = 0.6f)),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    type == ConnectionType.OFFLINE ->
                                        if (persian) "آفلاین" else "Offline"
                                    type == ConnectionType.CELLULAR ->
                                        if (persian) "داده موبایل" else "Mobile data"
                                    type == ConnectionType.ETHERNET -> if (persian) "اترنت" else "Ethernet"
                                    else -> if (persian) "وای‌فای" else "Wi-Fi"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = inkSoft,
                                fontWeight = FontWeight.Medium,
                            )
                            if (network?.vpnActive == true) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "VPN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ink.copy(alpha = 0.55f),
                                )
                            }
                        }
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val sub = technicalName.ifBlank { fallbackTechnical }
                        if (sub.isNotBlank()) {
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.labelMedium,
                                color = inkSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (onWifi) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(ink.copy(alpha = 0.14f))
                                .clickable(onClick = onRetry)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(MIcons.Refresh, null, Modifier.size(13.dp), tint = ink)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (persian) "بررسی" else "Re-check",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ink,
                                )
                            }
                        }
                    }
                }

                // ── چیپ‌های هویت: فناوری شبکه، سیم‌کارت، اپراتور، منبع
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (type == ConnectionType.CELLULAR && !network?.mobileTech.isNullOrBlank()) {
                        HeroChip(network?.mobileTech.orEmpty(), ink, emphasize = true)
                    }
                    if (slotLabel != null) HeroChip(slotLabel, ink)
                    HeroChip(operator.en, ink)
                    if (onWifi && sourceLabel.isNotBlank()) HeroChip(sourceLabel, ink)
                    if (network?.isValidated == true) {
                        HeroChip(if (persian) "اینترنت سالم" else "Online", ink)
                    }
                }

                // ── ردیف پایانی: سرعت زنده + پینگ، با خط جداکننده‌ی مویی
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ink.copy(alpha = 0.16f)),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeroStat(
                        icon = MIcons.Download,
                        label = context.getString(R.string.download),
                        value = Formats.compactSpeed(tick?.downBytesPerSecond ?: 0.0, persian),
                        ink = ink,
                        modifier = Modifier.weight(1f),
                    )
                    HeroStat(
                        icon = MIcons.Upload,
                        label = context.getString(R.string.upload),
                        value = Formats.compactSpeed(tick?.upBytesPerSecond ?: 0.0, persian),
                        ink = ink,
                        modifier = Modifier.weight(1f),
                    )
                    HeroStat(
                        icon = MIcons.Timer,
                        label = if (persian) "پینگ" else "Ping",
                        value = if ((vm.dashboard.value.averagePingMs) > 0) {
                            Formats.integer(vm.dashboard.value.averagePingMs.toLong(), persian) +
                                (if (persian) " م.ث" else " ms")
                        } else "—",
                        ink = ink,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** چیپ شیشه‌ای روی کارت اپراتور؛ با یک نسخه‌ی تأکیدی برای فناوری شبکه. */
@Composable
private fun HeroChip(text: String, ink: Color, emphasize: Boolean = false) {
    // چیپ باید «سطح» را از پس‌زمینه جدا کند، نه روشن‌ترش کند: روی گرادیان تیره‌ی
    // برند یک لایه‌ی مشکی محو می‌نشیند تا متن سفید کنتراست کافی داشته باشد
    // (لایه‌ی سفید، متن سفید را روی پس‌زمینه‌ی روشن‌تر می‌شست).
    val shade = if (ink.luminance() > 0.5f) Color.Black else ink
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(shade.copy(alpha = if (emphasize) 0.26f else 0.16f))
            .border(
                width = 1.dp,
                color = ink.copy(alpha = if (emphasize) 0.42f else 0.22f),
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            color = ink,
            maxLines = 1,
        )
    }
}

/**
 * رنگ دوم گرادیان: رنگ تأکیدی اپراتور، هم‌روشنایی‌شده با رنگ اول تا گرادیان
 * یکدست دیده شود و رنگ‌های ناهمگون (مثل زرد + بنفش) کنار هم «کثیف» نشوند.
 */
private fun verticalPartner(start: Color, accent: Color): Color {
    val blended = lerp(accent, start, 0.42f)
    return lerp(blended, Color.Black, 0.16f)
}

/** یک عدد کلیدی (دانلود/آپلود/پینگ) روی کارت اپراتور. */
@Composable
private fun HeroStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    ink: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(12.dp), tint = ink.copy(alpha = 0.75f))
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.75f),
                maxLines = 1,
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

/**
 * بنر «اجازه‌ی دسترسی به مصرف اپ‌ها» — یکدست با بقیه‌ی کارت‌ها.
 *
 * قبلاً این بنر کارت قرمزِ پررنگ با دکمه‌ی متنی بی‌قاعده بود و کنارش آیکون
 * کوچکی می‌نشست که ناهماهنگ دیده می‌شد. حالا آیکون داخل نشان رنگی است و
 * دکمه‌ی اقدام، دکمه‌ی پرِ هم‌خانواده‌ی بقیه‌ی صفحه‌هاست.
 */
@Composable
private fun UsageAccessBanner(persian: Boolean) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    MarbleCard(padding = 14.dp) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                    .background(scheme.error.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MIcons.Shield, null, Modifier.size(19.dp), tint = scheme.error)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    context.getString(R.string.grant_usage_access),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    context.getString(R.string.grant_usage_access_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        androidx.compose.material3.FilledTonalButton(
            onClick = { UsageAccess.openSettings(context) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(MIcons.OpenInNew, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (persian) "فعال کردن دسترسی" else "Enable access")
        }
    }
}

/**
 * کارت هشدار جهش غیرعادی اپ‌ها در تب خانه؛ تنها زمانی نمایش داده می‌شود که
 * امروز یک یا چند برنامه جهش معنادار نسبت به میانگین ۷ روزه داشته باشند.
 */
@Composable
private fun DashboardSurgeAlertCard(
    surges: List<AppSurgeDetector.SurgeItem>,
    persian: Boolean,
    onOpenAppSettings: (String) -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val warnTint = accessibleColor(Color(0xFFF59E0B), scheme.surfaceContainerLow)

    MarbleCard(padding = 14.dp) {
        CardHeader(
            title = context.getString(R.string.surge_title),
            icon = MIcons.Speed,
            tint = warnTint,
            subtitle = context.getString(R.string.surge_subtitle),
            trailing = Formats.integer(surges.size.toLong(), persian) + (if (persian) " اپ" else " apps"),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            surges.forEach { item ->
                val displayName = OperatorCatalog.specialUidLabel(item.uid, persian) ?: item.label
                val badge = if (item.isNewSpike) {
                    context.getString(R.string.surge_new_spike)
                } else {
                    "${Formats.number(item.multiplier, persian, 1)}×"
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.surfaceContainer)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(item.packageName.substringBefore(':'), displayName, size = 30.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (item.baselineDailyBytes > 0L) {
                                "${Formats.bytes(item.todayTotalBytes, persian)} (${if (persian) "میانگین" else "avg"} ${Formats.bytes(item.baselineDailyBytes, persian)})"
                            } else {
                                Formats.bytes(item.todayTotalBytes, persian)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    InfoChip(text = badge, color = warnTint)
                    if (item.isRestrictableApp) {
                        TextButton(
                            onClick = { onOpenAppSettings(item.packageName) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        ) {
                            Text(if (persian) "تنظیم" else "Manage", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * کارت «ساعات رایگان / شبانه» در تب بسته و اپراتورها.
 */
@Composable
private fun OffPeakPlanCard(
    settings: AppSettings,
    offPeak: OffPeakUi,
    persian: Boolean,
    onConfigure: () -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val marble = LocalMarbleColors.current

    MarbleCard(padding = 14.dp) {
        CardHeader(
            title = context.getString(R.string.off_peak_title),
            icon = MIcons.Clock,
            tint = scheme.tertiary,
            subtitle = if (settings.offPeakEnabled) {
                OffPeakWindow.windowLabel(settings.offPeakStartHour, settings.offPeakEndHour, persian)
            } else {
                context.getString(R.string.off_peak_desc)
            },
            trailing = if (settings.offPeakEnabled) {
                if (persian) "فعال" else "Active"
            } else null,
        )

        if (!settings.offPeakEnabled) {
            androidx.compose.material3.FilledTonalButton(
                onClick = onConfigure,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (persian) "تنظیم ساعات رایگان / شبانه" else "Set up off-peak hours")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(
                    label = if (persian) "موبایل شبانه امروز" else "Today mobile",
                    value = Formats.bytes(offPeak.todayMobileOffPeak, persian),
                    color = marble.mobile,
                    modifier = Modifier.weight(1f),
                    icon = MIcons.Signal,
                )
                Metric(
                    label = if (persian) "موبایل شبانه ۳۰ روز" else "30d mobile",
                    value = Formats.bytes(offPeak.monthMobileOffPeak, persian),
                    color = scheme.tertiary,
                    modifier = Modifier.weight(1f),
                    icon = MIcons.Clock,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (offPeak.todayWifiOffPeak > 0L || offPeak.monthWifiOffPeak > 0L) {
                    InfoChip(
                        text = "${context.getString(R.string.wifi)} (${if (persian) "۳۰ روز" else "30d"}): ${
                            Formats.bytes(offPeak.monthWifiOffPeak, persian)
                        }",
                        color = marble.wifi,
                    )
                }
                if (settings.offPeakExcludeFromMainCap && settings.dataCapBytes > 0L) {
                    InfoChip(
                        text = if (persian) "کسر از بسته‌ی اصلی" else "Excluded from main cap",
                        color = scheme.primary,
                    )
                }
            }

            if (settings.offPeakCapBytes > 0L) {
                val fraction = Formats.percentOf(offPeak.monthMobileOffPeak, settings.offPeakCapBytes)
                val danger = fraction > 0.9f
                val remaining = (settings.offPeakCapBytes - offPeak.monthMobileOffPeak).coerceAtLeast(0L)
                UsageBar(
                    label = context.getString(R.string.off_peak_cap),
                    bytes = offPeak.monthMobileOffPeak,
                    fraction = fraction,
                    persian = persian,
                    trailing = "${context.getString(R.string.remaining)}: ${Formats.bytes(remaining, persian)}",
                    color = if (danger) scheme.error else scheme.tertiary,
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onConfigure) {
                    Text(if (persian) "تغییر بازه و سقف" else "Edit window & cap")
                }
            }
        }
    }
}

@Composable
internal fun OffPeakConfigDialog(
    settings: AppSettings,
    persian: Boolean,
    onDismiss: () -> Unit,
    onSave: (enabled: Boolean, startHour: Int, endHour: Int, capBytes: Long, excludeFromMainCap: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var enabled by remember { mutableStateOf(true) }
    var startHour by remember { mutableStateOf(settings.offPeakStartHour) }
    var endHour by remember { mutableStateOf(settings.offPeakEndHour) }
    var excludeMain by remember { mutableStateOf(settings.offPeakExcludeFromMainCap) }
    var capGbText by remember {
        mutableStateOf(
            if (settings.offPeakCapBytes > 0L) {
                val gb = settings.offPeakCapBytes / (1024.0 * 1024 * 1024)
                if (gb % 1.0 == 0.0) gb.toLong().toString() else String.format(java.util.Locale.US, "%.1f", gb)
            } else "",
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.off_peak_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        if (persian) "فعال بودن ردیاب ساعات رایگان" else "Enable off-peak tracking",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                Text(
                    context.getString(R.string.off_peak_window_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OffPeakWindow.presets.forEach { preset ->
                        val active = startHour == preset.startHour && endHour == preset.endHour
                        FilterChip(
                            selected = active,
                            onClick = {
                                startHour = preset.startHour
                                endHour = preset.endHour
                            },
                            label = { Text(if (persian) preset.fa else preset.en) },
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            if (persian) "ساعت شروع" else "Start hour",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { startHour = (startHour + 23) % 24 }) { Text("−") }
                            Text(
                                OffPeakWindow.hourText(startHour, persian),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            TextButton(onClick = { startHour = (startHour + 1) % 24 }) { Text("+") }
                        }
                    }
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            if (persian) "ساعت پایان" else "End hour",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { endHour = (endHour + 23) % 24 }) { Text("−") }
                            Text(
                                OffPeakWindow.hourText(endHour, persian),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            TextButton(onClick = { endHour = (endHour + 1) % 24 }) { Text("+") }
                        }
                    }
                }

                if (!OffPeakWindow.isValidWindow(startHour, endHour)) {
                    Text(
                        if (persian) "ساعت شروع و پایان نباید یکسان باشند." else "Start and end hours must differ.",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.error,
                    )
                }

                OutlinedTextField(
                    value = capGbText,
                    onValueChange = { capGbText = it },
                    label = {
                        Text(
                            if (persian) "سقف بسته‌ی شبانه (GB، اختیاری)" else "Off-peak cap (GB, optional)",
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        context.getString(R.string.off_peak_exclude),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = excludeMain, onCheckedChange = { excludeMain = it })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = OffPeakWindow.isValidWindow(startHour, endHour),
                onClick = {
                    val gb = capGbText.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                    val capBytes = (gb * 1024.0 * 1024 * 1024).toLong().coerceAtLeast(0L)
                    onSave(enabled, startHour, endHour, capBytes, excludeMain)
                },
            ) {
                Text(context.getString(R.string.save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        },
    )
}

