package ir.marbledu.datausage.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.AppTraffic
import ir.marbledu.datausage.data.DayReport
import ir.marbledu.datausage.data.DaySummary
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.HourTraffic
import ir.marbledu.datausage.data.JalaliDate
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.OperatorTraffic
import ir.marbledu.datausage.data.TimeMath
import ir.marbledu.datausage.data.UsageLedger
import ir.marbledu.datausage.ui.AppIcon
import ir.marbledu.datausage.ui.BarEntry
import ir.marbledu.datausage.ui.CardHeader
import ir.marbledu.datausage.ui.DonutChart
import ir.marbledu.datausage.ui.DonutLegend
import ir.marbledu.datausage.ui.DonutSlice
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
import ir.marbledu.datausage.ui.theme.LocalMarbleColors
import ir.marbledu.datausage.util.ReportExporter
import ir.marbledu.datausage.util.UsageAccess
import kotlinx.coroutines.launch

/**
 * تب «۳۰ روز»: گزارش کامل هر روز از سی روز اخیر.
 *
 * بالا نمودار ۳۰ روز است؛ روی هر ستون که بزنی، گزارش همان روز زیرش می‌آید:
 * دانلود/آپلود/مجموع، سهم هر اپراتور، نمودار ۲۴ ساعت، مصرف هر اپ و مقایسه
 * با بقیه‌ی روزها. تاریخ‌ها هم شمسی و هم میلادی نوشته می‌شوند.
 */
@Composable
fun ReportsScreen(vm: MainViewModel, settings: AppSettings, persian: Boolean) {
    val context = LocalContext.current
    val history by vm.history.collectAsState()
    val dayUi by vm.dayReport.collectAsState()
    val dashboard by vm.dashboard.collectAsState()
    val usage by vm.usage.collectAsState()
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val iran = settings.iran

    val days = history.days
    val selectedIndex = days.indexOfFirst { it.startMillis == dayUi.dayStart }.takeIf { it >= 0 }
    val selectedDay = selectedIndex?.let { days[it] }
    val report = dayUi.report?.takeIf { it.summary.startMillis == dayUi.dayStart }

    var hourSelection by remember(dayUi.dayStart) { mutableStateOf<Int?>(null) }
    var hourMode by rememberSaveable { mutableStateOf(0) }
    var showAllApps by remember(dayUi.dayStart) { mutableStateOf(false) }
    var appSelection by remember(dayUi.dayStart) { mutableStateOf<Int?>(null) }

    val monthMobile = days.fold(0L) { acc, d -> acc + d.mobile.total }
    val monthWifi = days.fold(0L) { acc, d -> acc + d.wifi.total }
    val monthRx = days.fold(0L) { acc, d -> acc + d.all.rx }
    val monthTx = days.fold(0L) { acc, d -> acc + d.all.tx }
    val monthTotal = monthRx + monthTx
    val activeDays = days.count { it.total > 0 }
    val dailyAverage = if (activeDays > 0) monthTotal / activeDays else 0L
    val busiest = days.maxByOrNull { it.total }?.takeIf { it.total > 0 }
    val todayStart = remember(history.loadedAt) { TimeMath.dayStart(System.currentTimeMillis()) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = FloatingBarClearance),
    ) {
        item {
            SectionTitle(
                text = if (persian) "گزارش ۳۰ روز اخیر" else "Last 30 days",
                action = {
                    TextButton(onClick = { vm.loadHistory() }) {
                        Icon(MIcons.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(context.getString(R.string.refresh_label))
                    }
                },
            )
        }

        if (!dashboard.hasUsageAccess) {
            item {
                MarbleCard(container = scheme.errorContainer) {
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

        if (days.isEmpty()) {
            item {
                if (history.loading) LoadingBox() else EmptyState(context.getString(R.string.no_data_yet))
            }
        } else {
            // ------------------------------------------------ خلاصه‌ی ۳۰ روز
            item {
                HeroCard {
                    Text(
                        text = if (persian) "مجموع ۳۰ روز" else "30-day total",
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalContentColor.current.copy(alpha = 0.85f),
                    )
                    Text(
                        text = Formats.bytes(monthTotal, persian),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = rangeText(days.first().startMillis, days.last().startMillis, persian),
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalContentColor.current.copy(alpha = 0.85f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Metric(
                            context.getString(R.string.download),
                            Formats.bytes(monthRx, persian),
                            LocalContentColor.current,
                            Modifier.weight(1f),
                            MIcons.Download,
                        )
                        Metric(
                            context.getString(R.string.upload),
                            Formats.bytes(monthTx, persian),
                            LocalContentColor.current,
                            Modifier.weight(1f),
                            MIcons.Upload,
                        )
                        Metric(
                            context.getString(R.string.daily_average),
                            Formats.bytes(dailyAverage, persian),
                            LocalContentColor.current,
                            Modifier.weight(1f),
                            MIcons.Chart,
                        )
                    }
                    SplitBar(
                        listOf(
                            monthMobile.toFloat() to Color.White,
                            monthWifi.toFloat() to Color.White.copy(alpha = 0.45f),
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            "${context.getString(R.string.cellular)}: ${Formats.bytes(monthMobile, persian)}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            "${context.getString(R.string.wifi)}: ${Formats.bytes(monthWifi, persian)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalContentColor.current.copy(alpha = 0.8f),
                        )
                    }
                    if (busiest != null) {
                        Text(
                            text = (if (persian) "پرمصرف‌ترین روز: " else "Busiest day: ") +
                                primaryDate(busiest.startMillis, persian) + " • " +
                                Formats.bytes(busiest.total, persian),
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalContentColor.current.copy(alpha = 0.9f),
                        )
                    }
                }
            }

            // ------------------------------------------------ نمودار ۳۰ روز
            item {
                MarbleCard {
                    CardHeader(
                        title = if (persian) "روند روزانه" else "Daily trend",
                        icon = MIcons.Chart,
                        tint = scheme.primary,
                        subtitle = if (persian) "روی هر ستون بزن یا انگشتت را بکش" else "Tap or drag across the bars",
                    )
                    StackedBarChart(
                        entries = days.map { d ->
                            BarEntry(
                                JalaliDate.axisDay(d.startMillis, persian),
                                listOf(d.mobile.total.toFloat(), d.wifi.total.toFloat()),
                            )
                        },
                        colors = listOf(marble.mobile, marble.wifi),
                        height = 210.dp,
                        selected = selectedIndex,
                        onSelect = { index -> days.getOrNull(index)?.let { vm.selectDay(it.startMillis) } },
                        labelEvery = 5,
                        maxLabel = Formats.bytes(days.maxOf { it.total }, persian),
                        averageLabel = (if (persian) "میانگین " else "avg ") +
                            Formats.bytes(monthTotal / days.size.coerceAtLeast(1), persian),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(context.getString(R.string.cellular), marble.mobile)
                        LegendDot(context.getString(R.string.wifi), marble.wifi)
                        LegendDot(if (persian) "میانگین" else "Average", scheme.tertiary)
                    }
                }
            }

            // ------------------------------------------------ روز انتخاب‌شده
            if (selectedDay != null && selectedIndex != null) {
                item {
                    DayHeaderCard(
                        day = selectedDay,
                        persian = persian,
                        isToday = selectedDay.startMillis == todayStart,
                        canOlder = selectedIndex > 0,
                        canNewer = selectedIndex < days.lastIndex,
                        onOlder = { vm.selectDay(days[selectedIndex - 1].startMillis) },
                        onNewer = { vm.selectDay(days[selectedIndex + 1].startMillis) },
                        rank = days.count { it.total > selectedDay.total } + 1,
                        dayCount = days.size,
                        average = dailyAverage,
                        monthTotal = monthTotal,
                        loading = dayUi.loading,
                    )
                }

                if (report == null) {
                    item { LoadingBox() }
                } else {
                    if (iran.detectOperator) {
                        item { OperatorsCard(report, persian) }
                    }
                    if (iran.showPeakHours) {
                        item {
                            HoursCard(
                                report = report,
                                persian = persian,
                                selection = hourSelection,
                                onSelect = { hourSelection = if (hourSelection == it) null else it },
                                mode = hourMode,
                                onMode = { hourMode = it },
                            )
                        }
                    }
                    if (iran.showPerAppBreakdown) {
                        item {
                            AppsCard(
                                report = report,
                                persian = persian,
                                showAll = showAllApps,
                                onToggleAll = { showAllApps = !showAllApps },
                                selection = appSelection,
                                onSelect = { appSelection = it },
                            )
                        }
                    }
                }
            }

            // ------------------------------------------------------- خروجی
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                val uri = ReportExporter.exportDailyCsv(context, days)
                                context.startActivity(
                                    Intent.createChooser(
                                        ReportExporter.shareIntent(context, uri),
                                        context.getString(R.string.share_csv_title),
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Icon(MIcons.Share, null, Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (persian) "CSV ۳۰ روز" else "30-day CSV", maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = {
                            val r = report
                            if (r != null) {
                                scope.launch {
                                    val uri = ReportExporter.exportDayCsv(context, r)
                                    context.startActivity(
                                        Intent.createChooser(
                                            ReportExporter.shareIntent(context, uri),
                                            context.getString(R.string.share_csv_title),
                                        ),
                                    )
                                }
                            }
                        },
                        enabled = report != null,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Icon(MIcons.Reports, null, Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (persian) "CSV این روز" else "This day CSV", maxLines = 1)
                    }
                }
            }

            item {
                TextButton(
                    onClick = { ReportExporter.shareSummary(context, usage, dashboard, persian) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(MIcons.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(context.getString(R.string.share_report))
                }
            }

            if (iran.showBillingCycle) {
                item {
                    StatCard(
                        title = context.getString(R.string.billing_cycle),
                        value = (if (persian) "روز " else "Day ") +
                            Formats.integer(settings.billingCycleDay.toLong(), persian),
                        subtitle = if (persian) "شروع دوره‌ی صورتحساب شما" else "Your cycle start day",
                        icon = MIcons.Calendar,
                        accent = scheme.tertiary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // -------------------------------------------------- همه‌ی روزها
            item { SectionTitle(text = if (persian) "همه‌ی روزها" else "All days") }
            val maxDay = days.maxOf { it.total }.coerceAtLeast(1L)
            items(days.reversed(), key = { it.startMillis }) { day ->
                DayRow(
                    day = day,
                    persian = persian,
                    max = maxDay,
                    selected = day.startMillis == dayUi.dayStart,
                    isToday = day.startMillis == todayStart,
                    onClick = {
                        vm.selectDay(day.startMillis)
                        scope.launch { listState.animateScrollToItem(2) }
                    },
                )
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** تاریخ اصلی: فارسی → شمسی، انگلیسی → میلادی. */
private fun primaryDate(millis: Long, persian: Boolean): String =
    if (persian) JalaliDate.fromMillis(millis).long(true) else JalaliDate.gregorianLong(millis, false)

/** تاریخ دوم: فارسی → میلادی، انگلیسی → شمسی. */
private fun secondaryDate(millis: Long, persian: Boolean): String =
    if (persian) JalaliDate.gregorianLong(millis, true) else JalaliDate.fromMillis(millis).long(false)

private fun rangeText(from: Long, to: Long, persian: Boolean): String {
    val jalali = JalaliDate.fromMillis(from).short(persian) + " – " + JalaliDate.fromMillis(to).long(persian)
    val gregorian = JalaliDate.gregorianNumeric(from, persian) + " – " + JalaliDate.gregorianNumeric(to, persian)
    return "$jalali  •  $gregorian"
}

private fun percentText(value: Double, persian: Boolean): String =
    Formats.number(value, persian, if (value < 10 && value > 0) 1 else 0) + (if (persian) "٪" else "%")

private fun hourText(hour: Int, persian: Boolean): String {
    val text = "%02d:00".format(java.util.Locale.US, hour)
    return if (persian) Formats.toPersianDigits(text) else text
}

@Composable
private fun DayHeaderCard(
    day: DaySummary,
    persian: Boolean,
    isToday: Boolean,
    canOlder: Boolean,
    canNewer: Boolean,
    onOlder: () -> Unit,
    onNewer: () -> Unit,
    rank: Int,
    dayCount: Int,
    average: Long,
    monthTotal: Long,
    loading: Boolean,
) {
    val context = LocalContext.current
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme
    MarbleCard(container = scheme.primaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // دکمه‌ی اول همیشه «روز قبل» است؛ جهت فلش با جهت چیدمان عوض می‌شود.
            FilledTonalIconButton(onClick = onOlder, enabled = canOlder) {
                Icon(
                    if (persian) MIcons.ChevronEnd else MIcons.ChevronStart,
                    contentDescription = if (persian) "روز قبل" else "Previous day",
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                Modifier.weight(1f).padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = JalaliDate.weekday(day.startMillis, persian),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onPrimaryContainer,
                    )
                    if (isToday) {
                        Spacer(Modifier.width(6.dp))
                        InfoChip(if (persian) "امروز" else "Today", color = scheme.primary)
                    }
                    if (loading) {
                        Spacer(Modifier.width(8.dp))
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    }
                }
                Text(
                    text = primaryDate(day.startMillis, persian),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimaryContainer,
                    maxLines = 1,
                )
                Text(
                    text = secondaryDate(day.startMillis, persian) + "  •  " +
                        JalaliDate.fromMillis(day.startMillis).numeric(persian),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                )
            }
            FilledTonalIconButton(onClick = onNewer, enabled = canNewer) {
                Icon(
                    if (persian) MIcons.ChevronStart else MIcons.ChevronEnd,
                    contentDescription = if (persian) "روز بعد" else "Next day",
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Text(
            text = Formats.bytes(day.total, persian),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onPrimaryContainer,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric(
                context.getString(R.string.download),
                Formats.bytes(day.all.rx, persian),
                marble.download,
                Modifier.weight(1f),
                MIcons.Download,
            )
            Metric(
                context.getString(R.string.upload),
                Formats.bytes(day.all.tx, persian),
                marble.upload,
                Modifier.weight(1f),
                MIcons.Upload,
            )
            Metric(
                context.getString(R.string.total),
                Formats.bytes(day.total, persian),
                scheme.onPrimaryContainer,
                Modifier.weight(1f),
                MIcons.Chart,
            )
        }
        SplitBar(
            listOf(day.mobile.total.toFloat() to marble.mobile, day.wifi.total.toFloat() to marble.wifi),
            height = 10.dp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric(
                context.getString(R.string.cellular),
                Formats.bytes(day.mobile.total, persian),
                marble.mobile,
                Modifier.weight(1f),
            )
            Metric(
                context.getString(R.string.wifi),
                Formats.bytes(day.wifi.total, persian),
                marble.wifi,
                Modifier.weight(1f),
            )
            Metric(
                if (persian) "نسبت آپلود" else "Upload share",
                percentText(if (day.total > 0) day.all.tx * 100.0 / day.total else 0.0, persian),
                marble.upload,
                Modifier.weight(1f),
            )
        }
        HorizontalDivider(color = scheme.onPrimaryContainer.copy(alpha = 0.15f))
        // مقایسه با بقیه‌ی روزها
        val diff = if (average > 0) (day.total - average) * 100.0 / average else 0.0
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip(
                text = when {
                    average <= 0L || day.total == 0L -> if (persian) "بدون مصرف" else "No usage"
                    diff >= 0 -> "▲ " + percentText(diff, persian) + (if (persian) " بالاتر از میانگین" else " above avg")
                    else -> "▼ " + percentText(-diff, persian) + (if (persian) " کمتر از میانگین" else " below avg")
                },
                color = if (diff > 0) scheme.error else scheme.secondary,
            )
            InfoChip(
                text = (if (persian) "رتبه " else "Rank ") + Formats.integer(rank.toLong(), persian) +
                    (if (persian) " از " else " of ") + Formats.integer(dayCount.toLong(), persian),
                color = scheme.tertiary,
            )
        }
        Text(
            text = (if (persian) "سهم از کل ۳۰ روز: " else "Share of 30 days: ") +
                percentText(if (monthTotal > 0) day.total * 100.0 / monthTotal else 0.0, persian),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onPrimaryContainer.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun OperatorsCard(report: DayReport, persian: Boolean) {
    val context = LocalContext.current
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme
    val rows = report.operators
    val total = rows.fold(0L) { acc, r -> acc + r.traffic.total }.coerceAtLeast(1L)
    MarbleCard {
        CardHeader(
            title = if (persian) "مصرف هر اپراتور" else "Usage by operator",
            icon = MIcons.Carrier,
            tint = scheme.tertiary,
            trailing = Formats.bytes(report.summary.total, persian),
        )
        if (rows.isEmpty()) {
            Text(
                context.getString(R.string.no_data_yet),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        rows.forEach { row -> OperatorRow(row, total, persian) }
        if (rows.any { it.estimated && it.transport == UsageLedger.WIFI }) {
            Text(
                text = if (persian) {
                    "«وای‌فای – اپراتور نامشخص» یعنی آن موقع MarbleDU در حال پایش نبوده یا هنوز اپراتور مودم را پیدا نکرده بود. " +
                        "با روشن ماندن «پایش در پس‌زمینه» روزهای بعد دقیق ثبت می‌شوند."
                } else {
                    "“Wi-Fi – unknown operator” means MarbleDU was not monitoring at that time or had not identified the router's ISP yet. " +
                        "Keep background monitoring on for exact attribution."
                },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(context.getString(R.string.download), marble.download)
            LegendDot(context.getString(R.string.upload), marble.upload)
        }
    }
}

@Composable
private fun OperatorRow(row: OperatorTraffic, total: Long, persian: Boolean) {
    val context = LocalContext.current
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme
    val operator = OperatorCatalog.byId(row.operatorId)
    val wifi = row.transport == UsageLedger.WIFI
    val name = when {
        wifi && row.operatorId == OperatorCatalog.ID_WIFI ->
            if (persian) "وای‌فای – اپراتور نامشخص" else "Wi-Fi – unknown operator"
        row.operatorId == OperatorCatalog.ID_MOBILE -> if (persian) "سیم‌کارت نامشخص" else "Unknown SIM"
        else -> operator.name(persian)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OperatorBadge(operator, size = 46.dp, transportIcon = if (wifi) MIcons.Wifi else MIcons.Signal)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    Formats.bytes(row.traffic.total, persian),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                    maxLines = 1,
                )
            }
            Text(
                text = (if (wifi) context.getString(R.string.wifi) else context.getString(R.string.cellular)) +
                    "  •  " + percentText(row.traffic.total * 100.0 / total, persian) +
                    (if (row.estimated && !wifi) (if (persian) "  •  تخمینی" else "  •  estimated") else ""),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            SplitBar(
                listOf(row.traffic.rx.toFloat() to marble.download, row.traffic.tx.toFloat() to marble.upload),
                modifier = Modifier.fillMaxWidth((row.traffic.total.toFloat() / total).coerceIn(0.06f, 1f)),
                height = 7.dp,
            )
            Text(
                text = "↓ ${Formats.bytes(row.traffic.rx, persian)}    ↑ ${Formats.bytes(row.traffic.tx, persian)}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HoursCard(
    report: DayReport,
    persian: Boolean,
    selection: Int?,
    onSelect: (Int) -> Unit,
    mode: Int,
    onMode: (Int) -> Unit,
) {
    val context = LocalContext.current
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme
    val hours = report.hours
    val peak = hours.maxByOrNull { it.total }?.takeIf { it.total > 0 }
    val active = hours.filter { it.total > 0 }
    val quiet = active.minByOrNull { it.total }
    val dayTotal = hours.fold(0L) { acc, h -> acc + h.total }

    MarbleCard {
        CardHeader(
            title = if (persian) "نمودار ۲۴ ساعت" else "24-hour chart",
            icon = MIcons.Clock,
            tint = scheme.secondary,
            subtitle = if (persian) "هر ستون یک ساعت است؛ لمس کن تا جزئیات بیاید" else "One bar per hour – tap for details",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == 0,
                onClick = { onMode(0) },
                label = { Text(if (persian) "موبایل / وای‌فای" else "Mobile / Wi-Fi") },
            )
            FilterChip(
                selected = mode == 1,
                onClick = { onMode(1) },
                label = { Text(if (persian) "دانلود / آپلود" else "Down / Up") },
            )
        }
        if (peak == null) {
            Text(
                context.getString(R.string.no_data_yet),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        } else {
            StackedBarChart(
                entries = hours.map { h ->
                    BarEntry(
                        Formats.integer(h.hour.toLong(), persian),
                        if (mode == 0) listOf(h.mobile.total.toFloat(), h.wifi.total.toFloat())
                        else listOf(h.rx.toFloat(), h.tx.toFloat()),
                    )
                },
                colors = if (mode == 0) listOf(marble.mobile, marble.wifi) else listOf(marble.download, marble.upload),
                height = 200.dp,
                selected = selection,
                onSelect = onSelect,
                labelEvery = 3,
                maxLabel = Formats.bytes(peak.total, persian),
                averageLabel = if (persian) "میانگین" else "avg",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (mode == 0) {
                    LegendDot(context.getString(R.string.cellular), marble.mobile)
                    LegendDot(context.getString(R.string.wifi), marble.wifi)
                } else {
                    LegendDot(context.getString(R.string.download), marble.download)
                    LegendDot(context.getString(R.string.upload), marble.upload)
                }
            }

            // جزئیات ساعتِ انتخاب‌شده (یا ساعت اوج، اگر چیزی انتخاب نشده)
            val shown: HourTraffic = selection?.let { hours.getOrNull(it) } ?: peak
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(scheme.secondary.copy(alpha = 0.10f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = hourText(shown.hour, persian) + " – " + hourText((shown.hour + 1) % 24, persian),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    if (selection == null) InfoChip(if (persian) "ساعت اوج" else "Peak hour", color = scheme.secondary)
                    Spacer(Modifier.weight(1f))
                    Text(
                        Formats.bytes(shown.total, persian),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.secondary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric(context.getString(R.string.download), Formats.bytes(shown.rx, persian), marble.download, Modifier.weight(1f), MIcons.Download)
                    Metric(context.getString(R.string.upload), Formats.bytes(shown.tx, persian), marble.upload, Modifier.weight(1f), MIcons.Upload)
                    Metric(context.getString(R.string.cellular), Formats.bytes(shown.mobile.total, persian), marble.mobile, Modifier.weight(1f))
                    Metric(context.getString(R.string.wifi), Formats.bytes(shown.wifi.total, persian), marble.wifi, Modifier.weight(1f))
                }
                Text(
                    text = (if (persian) "سهم از کل روز: " else "Share of the day: ") +
                        percentText(if (dayTotal > 0) shown.total * 100.0 / dayTotal else 0.0, persian),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(
                    if (persian) "ساعت اوج" else "Peak hour",
                    hourText(peak.hour, persian),
                    scheme.error,
                    Modifier.weight(1f),
                )
                Metric(
                    if (persian) "کم‌مصرف‌ترین" else "Quietest",
                    quiet?.let { hourText(it.hour, persian) } ?: "—",
                    scheme.secondary,
                    Modifier.weight(1f),
                )
                Metric(
                    if (persian) "ساعت‌های فعال" else "Active hours",
                    Formats.integer(active.size.toLong(), persian) + (if (persian) " از ۲۴" else " of 24"),
                    scheme.primary,
                    Modifier.weight(1f),
                )
                Metric(
                    if (persian) "میانگین ساعتی" else "Hourly avg",
                    Formats.bytes(if (active.isNotEmpty()) dayTotal / active.size else 0L, persian),
                    scheme.tertiary,
                    Modifier.weight(1f),
                )
            }

            // چهار بخش شبانه‌روز
            val parts = listOf(
                (if (persian) "بامداد ۰–۶" else "Night 0–6") to (0..5),
                (if (persian) "صبح ۶–۱۲" else "Morning 6–12") to (6..11),
                (if (persian) "عصر ۱۲–۱۸" else "Afternoon 12–18") to (12..17),
                (if (persian) "شب ۱۸–۲۴" else "Evening 18–24") to (18..23),
            )
            val partColors = listOf(marble.chart[4 % marble.chart.size], marble.chart[5 % marble.chart.size], marble.chart[3 % marble.chart.size], marble.chart[1])
            val partTotals = parts.map { (_, range) -> hours.filter { it.hour in range }.fold(0L) { acc, h -> acc + h.total } }
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f))
            Text(
                if (persian) "بخش‌های شبانه‌روز" else "Parts of the day",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            SplitBar(partTotals.mapIndexed { i, v -> v.toFloat() to partColors[i] }, height = 12.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                parts.forEachIndexed { i, (label, _) ->
                    Metric(label, Formats.bytes(partTotals[i], persian), partColors[i], Modifier.weight(1f))
                }
            }
        }
    }
}

/** نام بومیِ ردیف‌های ویژه (سیستم، هات‌اسپات، اپ‌های حذف‌شده). */
fun appDisplayName(app: AppTraffic, persian: Boolean): String = when (app.uid) {
    android.app.usage.NetworkStats.Bucket.UID_REMOVED -> if (persian) "اپ‌های حذف‌شده" else "Removed apps"
    android.app.usage.NetworkStats.Bucket.UID_TETHERING -> if (persian) "هات‌اسپات" else "Hotspot / tethering"
    0 -> if (persian) "هسته‌ی سیستم" else "System core"
    1000 -> if (persian) "سیستم اندروید" else "Android system"
    else -> app.label
}

@Composable
private fun AppsCard(
    report: DayReport,
    persian: Boolean,
    showAll: Boolean,
    onToggleAll: () -> Unit,
    selection: Int?,
    onSelect: (Int?) -> Unit,
) {
    val context = LocalContext.current
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme
    val apps = report.apps
    val total = apps.fold(0L) { acc, a -> acc + a.total.total }.coerceAtLeast(1L)
    MarbleCard {
        CardHeader(
            title = if (persian) "مصرف هر اپ" else "Usage by app",
            icon = MIcons.Apps,
            tint = scheme.primary,
            trailing = Formats.integer(apps.size.toLong(), persian) + (if (persian) " اپ" else " apps"),
        )
        if (apps.isEmpty()) {
            Text(
                context.getString(R.string.no_data_yet),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            return@MarbleCard
        }
        val top = apps.take(5)
        val rest = apps.drop(5).fold(0L) { acc, a -> acc + a.total.total }
        val slices = top.mapIndexed { i, a ->
            DonutSlice(appDisplayName(a, persian), a.total.total.toFloat(), marble.chart[i % marble.chart.size], Formats.bytes(a.total.total, persian))
        } + if (rest > 0) listOf(
            DonutSlice(if (persian) "بقیه" else "Others", rest.toFloat(), scheme.outline, Formats.bytes(rest, persian)),
        ) else emptyList()
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(slices, diameter = 128.dp, stroke = 17.dp, selected = selection) {
                val picked = selection?.let { slices.getOrNull(it) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (picked != null) picked.detail else Formats.bytes(report.summary.total, persian),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Text(
                        text = if (picked != null) picked.label else context.getString(R.string.total),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(78.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            DonutLegend(slices, persian, Modifier.weight(1f), selection, onSelect)
        }
        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f))
        val visible = if (showAll) apps else apps.take(8)
        val maxApp = apps.first().total.total.coerceAtLeast(1L)
        visible.forEachIndexed { i, app ->
            AppTrafficRow(app, persian, maxApp, total, marble.chart[i % marble.chart.size])
        }
        if (apps.size > 8) {
            TextButton(onClick = onToggleAll, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(
                    if (showAll) (if (persian) "نمایش کمتر" else "Show less")
                    else (if (persian) "نمایش همه‌ی ${Formats.integer(apps.size.toLong(), true)} اپ" else "Show all ${apps.size} apps"),
                )
            }
        }
    }
}

/** ردیف یک اپ: آیکون، نام، مجموع، دانلود/آپلود و نوار موبایل/وای‌فای. */
@Composable
fun AppTrafficRow(app: AppTraffic, persian: Boolean, max: Long, total: Long, accent: Color) {
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme
    val name = appDisplayName(app, persian)
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(app.packageName, name, fallbackColor = accent)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    Formats.bytes(app.total.total, persian),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                    maxLines = 1,
                )
            }
            SplitBar(
                listOf(app.mobile.total.toFloat() to marble.mobile, app.wifi.total.toFloat() to marble.wifi),
                modifier = Modifier.fillMaxWidth((app.total.total.toFloat() / max).coerceIn(0.05f, 1f)),
                height = 6.dp,
            )
            Text(
                text = "↓ ${Formats.bytes(app.total.rx, persian)}   ↑ ${Formats.bytes(app.total.tx, persian)}   •   " +
                    percentText(app.total.total * 100.0 / total.coerceAtLeast(1L), persian),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DayRow(
    day: DaySummary,
    persian: Boolean,
    max: Long,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme
    val jalali = JalaliDate.fromMillis(day.startMillis)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) scheme.primaryContainer else scheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // روزِ ماه در یک دایره‌ی رنگی
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (selected) scheme.primary else scheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = JalaliDate.axisDay(day.startMillis, persian),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) scheme.onPrimary else scheme.primary,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = JalaliDate.weekday(day.startMillis, persian) + "، " +
                        (if (persian) jalali.short(true) else JalaliDate.gregorianLong(day.startMillis, false)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isToday) {
                    Spacer(Modifier.width(6.dp))
                    InfoChip(if (persian) "امروز" else "Today", color = scheme.primary)
                }
            }
            Text(
                text = jalali.numeric(persian) + "  •  " + JalaliDate.gregorianNumeric(day.startMillis, persian),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
            )
            SplitBar(
                listOf(day.mobile.total.toFloat() to marble.mobile, day.wifi.total.toFloat() to marble.wifi),
                modifier = Modifier.fillMaxWidth((day.total.toFloat() / max).coerceIn(0.04f, 1f)),
                height = 6.dp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Formats.bytes(day.total, persian),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                "↓ ${Formats.bytes(day.all.rx, persian)}",
                style = MaterialTheme.typography.labelSmall,
                color = marble.download,
                maxLines = 1,
            )
            Text(
                "↑ ${Formats.bytes(day.all.tx, persian)}",
                style = MaterialTheme.typography.labelSmall,
                color = marble.upload,
                maxLines = 1,
            )
        }
    }
}
