package ir.marbledu.datausage.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.HourOperatorTraffic
import ir.marbledu.datausage.data.JalaliDate
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.SimIdentity
import ir.marbledu.datausage.ui.BarEntry
import ir.marbledu.datausage.ui.CardHeader
import ir.marbledu.datausage.ui.FloatingBarClearance
import ir.marbledu.datausage.ui.GlassBar
import ir.marbledu.datausage.ui.HeroCard
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.MarbleCard
import ir.marbledu.datausage.ui.Metric
import ir.marbledu.datausage.ui.OperatorBadge
import ir.marbledu.datausage.ui.StackedBarChart
import ir.marbledu.datausage.ui.SplitBar
import ir.marbledu.datausage.ui.LegendDot
import ir.marbledu.datausage.ui.theme.LocalMarbleColors
import androidx.compose.material3.LocalContentColor

private fun simLabel(sim: SimIdentity?, persian: Boolean): String {
    val slot = sim?.slotIndex?.let { if (persian) "سیم‌کارت ${it + 1}" else "SIM ${it + 1}" }
    val name = sim?.displayName?.takeIf { it.isNotBlank() }
    return listOfNotNull(slot, name).joinToString(" · ")
}

private fun hourText(hour: Int, persian: Boolean): String {
    val text = "%02d:00".format(java.util.Locale.US, hour)
    return if (persian) Formats.toPersianDigits(text) else text
}

/**
 * صفحه‌ی کامل جزئیات یک اپراتور در ۳۰ روز اخیر:
 * مجموع، روند روزانه، هیستوگرام ساعتی، روزهای پیک و تفکیک وای‌فای/موبایل.
 * روی هر ستون روز در نمودار روزانه بزن تا ۲۴ ساعت همان روز باز شود.
 */
@Composable
fun OperatorDetailScreen(
    vm: MainViewModel,
    operatorId: String,
    sim: SimIdentity?,
    persian: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val context = androidx.compose.ui.platform.LocalContext.current
    val detail by vm.operatorDetail.collectAsState()
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme
    val operator = OperatorCatalog.byId(operatorId)

    LaunchedEffect(operatorId, sim) {
        vm.openOperatorDetail(operatorId, sim)
    }

    val days = detail.days
    val totalMobile = days.sumOf { it.mobile.total }
    val totalWifi = days.sumOf { it.wifi.total }
    val totalRx = days.sumOf { it.mobile.rx + it.wifi.rx }
    val totalTx = days.sumOf { it.mobile.tx + it.wifi.tx }
    val total = totalMobile + totalWifi
    val activeDays = days.count { it.total > 0L }
    val dailyAverage = if (activeDays > 0) total / activeDays else 0L
    val peakDay = days.maxByOrNull { it.total }?.takeIf { it.total > 0L }
    val peakHour = detail.hours.maxByOrNull { it.total }?.takeIf { it.total > 0L }?.hour ?: -1
    val maxDayTotal = days.maxOfOrNull { it.total } ?: 1L
    val selectedIndex = days.indexOfFirst { it.dayStart == detail.selectedDayStart }.takeIf { it >= 0 }

    val density = LocalDensity.current
    val statusTopDp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }

    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = statusTopDp + 72.dp,
                bottom = FloatingBarClearance,
            ),
        ) {
            item {
                HeroCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OperatorBadge(operator, size = 62.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = operator.name(persian),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val simText = simLabel(sim, persian)
                            if (simText.isNotBlank()) {
                                Text(
                                    text = simText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LocalContentColor.current.copy(alpha = 0.85f),
                                )
                            }
                            Text(
                                text = operator.mark,
                                style = MaterialTheme.typography.labelSmall,
                                color = LocalContentColor.current.copy(alpha = 0.75f),
                                maxLines = 1,
                            )
                        }
                    }
                    Text(
                        text = if (persian) "مجموع مصرف این اپراتور" else "Total usage on this operator",
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalContentColor.current.copy(alpha = 0.85f),
                    )
                    Text(
                        text = Formats.bytes(total, persian),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Metric(
                            context.getString(R.string.download),
                            Formats.bytes(totalRx, persian),
                            LocalContentColor.current,
                            Modifier.weight(1f),
                            MIcons.Download,
                        )
                        Metric(
                            context.getString(R.string.upload),
                            Formats.bytes(totalTx, persian),
                            LocalContentColor.current,
                            Modifier.weight(1f),
                            MIcons.Upload,
                        )
                    }
                    SplitBar(
                        listOf(
                            totalMobile.toFloat() to androidx.compose.ui.graphics.Color.White,
                            totalWifi.toFloat() to androidx.compose.ui.graphics.Color.White.copy(alpha = 0.45f),
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            "${context.getString(R.string.cellular)}: ${Formats.bytes(totalMobile, persian)}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            "${context.getString(R.string.wifi)}: ${Formats.bytes(totalWifi, persian)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalContentColor.current.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            // ردیف آمارهای کلیدی
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyStat(
                        label = if (persian) "میانگین روزانه" else "Daily average",
                        value = Formats.bytes(dailyAverage, persian),
                        modifier = Modifier.weight(1f),
                    )
                    KeyStat(
                        label = if (persian) "روزهای مصرف" else "Active days",
                        value = Formats.integer(activeDays.toLong(), persian),
                        modifier = Modifier.weight(1f),
                    )
                    KeyStat(
                        label = if (persian) "ساعت اوج" else "Peak hour",
                        value = if (peakHour >= 0) hourText(peakHour, persian) else "—",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // نمودار مصرف روزانه در ۳۰ روز
            item {
                MarbleCard {
                    CardHeader(
                        title = if (persian) "مصرف روزانه" else "Daily usage",
                        icon = MIcons.Chart,
                        tint = scheme.primary,
                        subtitle = if (persian) "روی هر ستون بزن تا ۲۴ ساعت همان روز باز شود"
                        else "Tap a bar to see that day's 24 hours",
                    )
                    if (detail.loading) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 3.dp)
                        }
                    } else if (days.none { it.total > 0L }) {
                        Text(
                            text = if (persian) {
                                "هنوز مصرفی برای این اپراتور ثبت نشده. پایش پس‌زمینه را روشن نگه دار."
                            } else {
                                "No usage recorded for this operator yet. Keep background monitoring on."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    } else {
                        StackedBarChart(
                            entries = days.map { day ->
                                BarEntry(
                                    JalaliDate.axisDay(day.dayStart, persian),
                                    listOf(day.mobile.total.toFloat(), day.wifi.total.toFloat()),
                                )
                            },
                            colors = listOf(marble.mobile, marble.wifi),
                            height = 150.dp,
                            selected = selectedIndex,
                            onSelect = { index -> days.getOrNull(index)?.let { vm.selectOperatorDetailDay(it.dayStart) } },
                            labelEvery = 5,
                            maxLabel = Formats.bytes(maxDayTotal, persian),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendDot(context.getString(R.string.cellular), marble.mobile)
                            LegendDot(context.getString(R.string.wifi), marble.wifi)
                        }
                        if (peakDay != null) {
                            Text(
                                text = (if (persian) "پرمصرف‌ترین روز: " else "Busiest day: ") +
                                    JalaliDate.fromMillis(peakDay.dayStart).long(persian) + " • " +
                                    Formats.bytes(peakDay.total, persian),
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ۲۴ ساعته‌ی روز انتخاب‌شده
            if (detail.selectedDayStart > 0L) {
                item {
                    MarbleCard {
                        CardHeader(
                            title = (if (persian) "۲۴ ساعت: " else "24 hours: ") +
                                JalaliDate.fromMillis(detail.selectedDayStart).long(persian),
                            icon = MIcons.Clock,
                            tint = scheme.tertiary,
                        )
                        val hours = detail.selectedDayHours
                        if (detail.selectedDayLoading) {
                            Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(strokeWidth = 3.dp)
                            }
                        } else if (hours.none { it.total > 0L }) {
                            Text(
                                text = if (persian) "داده‌ی ساعتی برای این روز ثبت نشده"
                                else "No hourly data recorded for this day",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        } else {
                            HourlyBars(hours = hours, persian = persian)
                        }
                    }
                }
            }

            // هیستوگرام ساعتی کل ۳۰ روز
            item {
                MarbleCard {
                    CardHeader(
                        title = if (persian) "ساعت‌های پرمصرف این اپراتور (۳۰ روز)"
                        else "Peak hours for this operator (30 days)",
                        icon = MIcons.Timer,
                        tint = scheme.secondary,
                        subtitle = if (persian) "جمع همه‌ی روزها، ساعت به ساعت" else "All days added up, hour by hour",
                    )
                    val agg = detail.hours
                    if (agg.none { it.total > 0L }) {
                        Text(
                            text = if (persian) "داده‌ی ساعتی ثبت نشده" else "No hourly data recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    } else {
                        HourlyBars(hours = agg, persian = persian)
                        val top = agg.filter { it.total > 0L }.sortedByDescending { it.total }.take(3)
                        if (top.isNotEmpty()) {
                            Text(
                                text = (if (persian) "ساعت‌های اوج: " else "Peak hours: ") +
                                    top.joinToString(" · ") { hourText(it.hour, persian) },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.primary,
                            )
                        }
                    }
                }
            }
        }

        // نوار شیشه‌ای شناور بالای صفحه با دکمه‌ی بازگشت؛ محتوا از زیر آن عبور می‌کند.
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            GlassBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(24.dp),
                tint = Color(operator.brandColor),
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(MIcons.ChevronStart, contentDescription = if (persian) "بازگشت" else "Back")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (persian) "جزئیات اپراتور" else "Operator details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            text = "${operator.name(persian)} · ${if (persian) "۳۰ روز اخیر" else "Last 30 days"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OperatorBadge(operator, size = 32.dp)
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
private fun KeyStat(label: String, value: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surfaceContainerLow)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** نمودار میله‌ای افقیِ ۲۴ ساعت با مقادیر زیر هر ستون. */
@Composable
private fun HourlyBars(hours: List<HourOperatorTraffic>, persian: Boolean) {
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        val max = (hours.maxOfOrNull { it.total } ?: 1L).coerceAtLeast(1L)
        hours.forEach { hour ->
            Column(
                modifier = Modifier.width(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (hour.total > 0L) Formats.bytes(hour.total, persian) else "·",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    maxLines = 1,
                    color = scheme.onSurfaceVariant,
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.66f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(scheme.surfaceVariant),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(
                                if (hour.total > 0L) (hour.total.toFloat() / max).coerceIn(0.10f, 1f) else 0f,
                            )
                            .clip(RoundedCornerShape(7.dp))
                            .background(marble.mobile),
                    )
                }
                Text(
                    text = hourText(hour.hour, persian),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
