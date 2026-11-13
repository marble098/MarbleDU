package ir.marbledu.datausage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.UsageInsights
import ir.marbledu.datausage.ui.CardHeader
import ir.marbledu.datausage.ui.InfoChip
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MarbleCard
import ir.marbledu.datausage.ui.Metric
import ir.marbledu.datausage.ui.theme.LocalMarbleColors

/**
 * کارت «بینش مصرف» — قابلیت تازه‌ی ۱.۵.
 *
 * به‌جای فقط نشان دادنِ «چقدر مصرف شد»، الگوی مصرف را تحلیل می‌کند:
 *  - امروز نسبت به یک روز عادی چقدر است (سبک / عادی / سنگین / خیلی سنگین)؛
 *  - سنگین‌ترین و سبک‌ترین روز هفته؛
 *  - ساعت اوج مصرف؛
 *  - سهم موبایل از کل مصرف؛
 *  - بودجه‌ی امروز و این‌که با همین روند، سقف بسته چند روز دیگر پر می‌شود.
 */
@Composable
fun InsightsCard(
    insights: ir.marbledu.datausage.ui.InsightsUi,
    persian: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val marble = LocalMarbleColors.current
    val snapshot = insights.snapshot ?: return
    if (snapshot.activeDays == 0 && snapshot.averageDailyBytes == 0L) return

    val (verdictLabel, verdictColor) = when (insights.verdict) {
        UsageInsights.DayVerdict.LIGHT ->
            (if (persian) "سبک‌تر از همیشه" else "Lighter than usual") to marble.wifi
        UsageInsights.DayVerdict.NORMAL ->
            (if (persian) "عادی" else "Normal") to scheme.primary
        UsageInsights.DayVerdict.HEAVY ->
            (if (persian) "سنگین‌تر از عادت" else "Heavier than usual") to Color(0xFFF59E0B)
        UsageInsights.DayVerdict.VERY_HEAVY ->
            (if (persian) "خیلی سنگین" else "Very heavy") to scheme.error
    }

    MarbleCard(modifier, padding = 14.dp) {
        CardHeader(
            title = if (persian) "بینش مصرف" else "Usage insight",
            icon = MIcons.Tune,
            tint = scheme.tertiary,
            subtitle = if (persian) {
                "الگوی ${Formats.integer(snapshot.activeDays.toLong(), true)} روز اخیر"
            } else {
                "Your pattern over ${Formats.integer(snapshot.activeDays.toLong(), false)} days"
            },
            trailing = "${Formats.integer(insights.todayPercentOfAverage.toLong(), persian)}" +
                (if (persian) "٪" else "%"),
        )

        // نوار «امروز در برابر میانگین»: پرش از میانگین را یک‌نگاه نشان می‌دهد.
        val fraction = (insights.todayPercentOfAverage / 200f).coerceIn(0f, 1f)
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(scheme.surfaceContainerHighest),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(listOf(verdictColor, scheme.tertiary))),
            )
            // خط ۱۰۰٪ (میانگین روزهای عادی)
            Box(
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(10.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(Modifier.width(1.5.dp).height(10.dp).background(scheme.onSurfaceVariant.copy(alpha = 0.55f)))
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfoChip(text = verdictLabel, color = verdictColor)
            Text(
                text = (if (persian) "میانگین روزانه " else "Daily avg ") +
                    Formats.bytes(snapshot.averageDailyBytes, persian),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric(
                label = if (persian) "سنگین‌ترین روز" else "Heaviest day",
                value = snapshot.heaviestWeekday?.let {
                    UsageInsights.weekdayName(it, persian)
                } ?: "—",
                color = Color(0xFFFF8A3D),
                modifier = Modifier.weight(1f),
                icon = MIcons.Chart,
            )
            Metric(
                label = if (persian) "ساعت اوج" else "Peak hour",
                value = snapshot.peakHour?.let { UsageInsights.hourLabel(it, persian) } ?: "—",
                color = scheme.tertiary,
                modifier = Modifier.weight(1f),
                icon = MIcons.Timer,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric(
                label = if (persian) "سهم موبایل" else "Mobile share",
                value = Formats.number((snapshot.mobileShare * 100f).toDouble(), persian, 0) +
                    (if (persian) "٪" else "%"),
                color = marble.mobile,
                modifier = Modifier.weight(1f),
                icon = MIcons.Signal,
            )
            Metric(
                label = if (persian) "سبک‌ترین روز" else "Lightest day",
                value = snapshot.lightestWeekday?.let {
                    UsageInsights.weekdayName(it, persian)
                } ?: "—",
                color = marble.wifi,
                modifier = Modifier.weight(1f),
                icon = MIcons.Wifi,
            )
        }

        insights.budget?.let { budget ->
            val over = budget.overBudgetBytes > 0L
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        (if (over) scheme.error else scheme.secondary).copy(alpha = 0.12f),
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape)
                        .background((if (over) scheme.error else scheme.secondary).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (over) MIcons.Shield else MIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (over) scheme.error else scheme.secondary,
                    )
                }
                Text(
                    text = if (over) {
                        if (persian) {
                            "امروز ${Formats.bytes(budget.overBudgetBytes, true)} بیشتر از بودجه‌ی روزانه مصرف کردی."
                        } else {
                            "You're ${Formats.bytes(budget.overBudgetBytes, false)} over today's budget."
                        }
                    } else {
                        if (persian) {
                            "در محدوده‌ی بودجه‌ای؛ ${Formats.integer(budget.percentOfBudget.toLong(), true)}٪ سهم امروز مصرف شده."
                        } else {
                            "Within budget; ${budget.percentOfBudget}% of today's share used."
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (over) scheme.error else scheme.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (insights.daysUntilCap >= 0) {
            Text(
                text = when (insights.daysUntilCap) {
                    0 -> if (persian) "سقف بسته پر شده است." else "Your data plan is used up."
                    1 -> if (persian) "با این روند، سقف بسته فردا پر می‌شود."
                    else if (persian) {
                        "با این روند، سقف بسته حدوداً ${Formats.integer(insights.daysUntilCap.toLong(), true)} روز دیگر پر می‌شود."
                    } else {
                        "At this pace the plan fills up in about ${insights.daysUntilCap} days."
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(2.dp))
    }
}
