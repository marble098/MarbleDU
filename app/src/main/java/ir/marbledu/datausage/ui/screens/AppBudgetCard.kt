package ir.marbledu.datausage.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import ir.marbledu.datausage.data.AppBudget
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.ui.AppBudgetUi
import ir.marbledu.datausage.ui.AppIcon
import ir.marbledu.datausage.ui.CardHeader
import ir.marbledu.datausage.ui.InfoChip
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MarbleCard
import ir.marbledu.datausage.ui.Metric
import ir.marbledu.datausage.ui.theme.LocalMarbleColors
import ir.marbledu.datausage.ui.theme.accessibleColor
import ir.marbledu.datausage.util.UsageAccess

/**
 * «سهم اپ‌ها از بسته» — قابلیت تازه‌ی ۱.۶.۴.
 *
 * برای هر اپ سه چیز نشان می‌دهد: چقدر از دوره مصرف کرده، چند درصد از کل
 * مصرف را ساخته، و با همین روند چقدر از باقی‌مانده‌ی بسته را می‌خورد. اگر اپی
 * بیشتر از سهمش مصرف کند، ردیفش قرمز می‌شود و یک دکمه‌ی «محدودکردن» می‌گیرد
 * که مستقیم به تنظیمات همان اپ در سیستم می‌رود.
 */
@Composable
fun AppBudgetCard(
    budget: AppBudgetUi,
    persian: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val marble = LocalMarbleColors.current
    // بدون دسترسی «مصرف داده» یا بدون داده‌ی این دوره، کارتی نشان نمی‌دهیم
    // تا صفحه‌ی بسته با کارت خالی شلوغ نشود.
    val plan = budget.plan ?: return
    if (!budget.hasUsageAccess || plan.rows.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val warn = accessibleColor(Color(0xFFF59E0B), scheme.surfaceContainerLow)
    val rows = if (expanded) plan.rows else plan.rows.take(3)

    MarbleCard(modifier, padding = 14.dp) {
        CardHeader(
            title = if (persian) "سهم هر اپ از بسته" else "Each app's slice of the plan",
            icon = MIcons.Chart,
            tint = scheme.primary,
            subtitle = if (plan.capBytes > 0L) {
                if (persian) {
                    "از ${Formats.bytes(plan.remainingBytes, true)} باقی‌مانده، ${Formats.integer(plan.daysLeftInCycle.toLong(), true)} روز مانده"
                } else {
                    "${Formats.bytes(plan.remainingBytes, false)} left for ${plan.daysLeftInCycle} days"
                }
            } else {
                if (persian) "سقف بسته را در تنظیمات تعیین کن تا سهم هر اپ حساب شود"
                else "Set a data cap in Settings to compute per-app allowances"
            },
            trailing = null,
        )

        // برآورد امروز: «امروز تا الان X، تا آخر روز Y»
        budget.todayEstimate?.let { estimate ->
            if (estimate.projectedTodayBytes > 0L) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.surfaceContainer)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(MIcons.Timer, null, Modifier.size(15.dp), tint = scheme.tertiary)
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (persian) "برآورد مصرف امروز" else "Today's projection",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (persian) {
                                "با این روند تا آخر امروز حدود " +
                                    Formats.bytes(estimate.projectedTodayBytes, true) + " می‌شود"
                            } else {
                                "At this pace today ends near " +
                                    Formats.bytes(estimate.projectedTodayBytes, false)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        rows.forEach { row ->
            AppBudgetRow(
                row = row,
                plan = plan,
                persian = persian,
                onRestrict = { UsageAccess.openAppDataUsageSettings(context, row.key) },
            )
        }

        if (plan.rows.size > 3) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    if (expanded) {
                        if (persian) "نمایش کمتر" else "Show less"
                    } else {
                        if (persian) "همه‌ی ${Formats.integer(plan.rows.size.toLong(), true)} اپ"
                        else "All ${plan.rows.size} apps"
                    },
                )
            }
        }

        // وضعیت کلی بسته: با همین روند می‌رسد یا نه؟
        if (plan.capBytes > 0L) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (plan.willRunOutEarly) warn.copy(alpha = 0.16f)
                        else marble.wifi.copy(alpha = 0.14f),
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    if (plan.willRunOutEarly) MIcons.Speed else MIcons.Check,
                    null,
                    Modifier.size(15.dp),
                    tint = if (plan.willRunOutEarly) warn else marble.wifi,
                )
                Text(
                    text = when {
                        plan.willRunOutEarly -> if (persian) {
                            "با این روند بسته حدود ${Formats.integer(plan.shortfallDays.toLong(), true)} روز زودتر تمام می‌شود"
                        } else {
                            "At this pace the plan runs out about ${plan.shortfallDays} days early"
                        }
                        else -> if (persian) "با این روند بسته تا پایان دوره می‌رسد"
                        else "At this pace the plan lasts until the cycle ends"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (plan.willRunOutEarly) warn else scheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (plan.dailyBudgetBytes > 0L) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric(
                        label = if (persian) "سهم هر روز" else "Daily allowance",
                        value = Formats.bytes(plan.dailyBudgetBytes, persian),
                        color = scheme.primary,
                        modifier = Modifier.weight(1f),
                        icon = MIcons.Calendar,
                    )
                    Metric(
                        label = if (persian) "روزهای مانده" else "Days left",
                        value = Formats.integer(plan.daysLeftInCycle.toLong(), persian),
                        color = scheme.tertiary,
                        modifier = Modifier.weight(1f),
                        icon = MIcons.Clock,
                    )
                }
            }
        } else {
            InfoChip(
                text = if (persian) "برای محاسبه‌ی سهم دقیق، سقف بسته را وارد کن" else "Set a data cap to get exact allowances",
                color = scheme.tertiary,
            )
        }
    }
}

@Composable
private fun AppBudgetRow(
    row: AppBudget.AppAllowance,
    plan: AppBudget.Plan,
    persian: Boolean,
    onRestrict: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val marble = LocalMarbleColors.current
    val warn = accessibleColor(Color(0xFFF59E0B), scheme.surfaceContainerLow)
    val accent = if (row.overPace) warn else scheme.primary
    // نوار هر ردیف «سرعت» را نشان می‌دهد، نه حجم انبارشده: مصرف روزانه‌ی این اپ
    // نسبت به سهم روزانه‌ی کل بسته. پر شدن نوار یعنی همین اپ دارد بسته را زودتر
    // تمام می‌کند — همان چیزی که رنگ کهربایی هم می‌گوید.
    val fraction = if (plan.dailyBudgetBytes > 0L) {
        (row.dailyPaceBytes.toDouble() / plan.dailyBudgetBytes).toFloat().coerceIn(0f, 1f)
    } else {
        (row.sharePercent / 100f).coerceIn(0f, 1f)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        AppIcon(row.key.substringBefore(':'), row.label, size = 30.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = Formats.integer(row.sharePercent.toLong(), persian) + (if (persian) "٪" else "%"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
            Text(
                text = if (persian) {
                    "مصرف دوره ${Formats.bytes(row.usedBytes, true)} · روزی ${Formats.bytes(row.dailyPaceBytes, true)}" +
                        if (plan.dailyBudgetBytes > 0L) {
                            " از ${Formats.bytes(plan.dailyBudgetBytes, true)} سهم روزانه"
                        } else {
                            ""
                        }
                } else {
                    "Used ${Formats.bytes(row.usedBytes, false)} · ${Formats.bytes(row.dailyPaceBytes, false)}/day" +
                        if (plan.dailyBudgetBytes > 0L) {
                            " of ${Formats.bytes(plan.dailyBudgetBytes, false)} daily share"
                        } else {
                            ""
                        }
                },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearWavyProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = accent,
                trackColor = accent.copy(alpha = 0.18f),
            )
        }
        // اپ‌های واقعی دکمه‌ی «محدود» می‌گیرند؛ ردیف‌های ویژه (سیستم/نامشخص) نه.
        if (row.overPace && row.key.contains('.')) {
            // «اگر همین اپ را تا سهم روزانه‌اش محدود کنی، چند روز به عمر بسته اضافه می‌شود»
            // — همان عددی که تصمیم‌گیری را ساده می‌کند.
            val freed = (row.dailyPaceBytes - plan.dailyBudgetBytes).coerceAtLeast(0L)
            val gained = AppBudget.daysGained(
                capBytes = plan.capBytes,
                usedBytes = plan.usedBytes,
                daysLeftInCycle = plan.daysLeftInCycle,
                freedBytesPerDay = freed,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(warn.copy(alpha = 0.16f))
                        .clickable(onClick = onRestrict)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Text(
                        if (persian) "محدود" else "Limit",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = warn,
                    )
                }
                if (gained > 0) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = if (persian) {
                            "+" + Formats.integer(gained.toLong(), true) + " روز"
                        } else {
                            "+$gained d"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = warn,
                        maxLines = 1,
                    )
                }
            }
        } else if (row.keepBytes > 0L) {
            // اپ منظم: با همین روند تا آخر دوره چقدر مصرف می‌کند.
            Text(
                text = if (persian) {
                    "پیش‌بینی ${Formats.bytes(row.keepBytes, true)}"
                } else {
                    "≈ ${Formats.bytes(row.keepBytes, false)} ahead"
                },
                style = MaterialTheme.typography.labelSmall,
                color = marble.wifi,
                maxLines = 1,
            )
        }
    }
}
