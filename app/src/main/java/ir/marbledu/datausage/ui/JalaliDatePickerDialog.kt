package ir.marbledu.datausage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.JalaliDate
import ir.marbledu.datausage.data.TimeMath
import java.util.Calendar

private val WEEKDAYS_FA = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
private val WEEKDAYS_EN = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

// ترتیب روزهای هفته از شنبه برای جلالی؛ یکشنبه اول برای میلادیِ انگلیسی
private val ORDER_FA = listOf(7, 1, 2, 3, 4, 5, 6) // Calendar.SATURDAY=7، SUNDAY=1 ...
private val ORDER_EN = listOf(1, 2, 3, 4, 5, 6, 7)

/**
 * انتخابگر تاریخ شمسی ساده و زیبا (ماه شمسی، شنبه تا جمعه).
 * تاریخ‌های بعد از [maxMillis] غیرفعال‌اند (آمار آینده معنی ندارد).
 */
@Composable
fun JalaliDatePickerDialog(
    persian: Boolean,
    maxMillis: Long = System.currentTimeMillis(),
    minMillis: Long? = null,
    initialMillis: Long? = null,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val today = JalaliDate.fromMillis(maxMillis)
    val initial = (initialMillis ?: maxMillis).let { JalaliDate.fromMillis(it) }
    var year by remember { mutableStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.month) }
    var selectedDay by remember { mutableStateOf(initial.day.takeIf { initialMillis != null }) }

    val monthLength = JalaliDate.monthLength(year, month)
    val firstMillis = runCatching { JalaliDate.toMillis(year, month, 1) }.getOrDefault(maxMillis)
    val firstWeekday = Calendar.getInstance().apply { timeInMillis = firstMillis }.get(Calendar.DAY_OF_WEEK)
    val order = if (persian) ORDER_FA else ORDER_EN
    val leading = order.indexOf(firstWeekday).coerceAtLeast(0)
    val weeks = (leading + monthLength + 6) / 7
    val weekdays = if (persian) WEEKDAYS_FA else WEEKDAYS_EN

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    if (month == 1) {
                        month = 12
                        year -= 1
                    } else {
                        month -= 1
                    }
                }) {
                    Icon(MIcons.ChevronStart, contentDescription = if (persian) "ماه قبل" else "Previous month")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (persian) "${JalaliDate(year, month, 1).monthNameFa()} ${Formats.toPersianDigits("$year")}"
                        else "${JalaliDate(year, month, 1).monthNameEn()} $year",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    val greg = if (persian) {
                        JalaliDate.gregorianLong(firstMillis, true)
                    } else {
                        JalaliDate.gregorianLong(firstMillis, false)
                    }
                    Text(
                        text = greg,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                val isCurrentMonth = year == today.year && month == today.month
                IconButton(
                    onClick = {
                        if (month == 12) {
                            month = 1
                            year += 1
                        } else {
                            month += 1
                        }
                    },
                    enabled = !isCurrentMonth,
                ) {
                    Icon(MIcons.ChevronEnd, contentDescription = if (persian) "ماه بعد" else "Next month")
                }
            }
        },
        text = {
            Column {
                Row(Modifier.fillMaxWidth()) {
                    weekdays.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                repeat(weeks) { week ->
                    Row(Modifier.fillMaxWidth()) {
                        for (column in 0 until 7) {
                            val cell = week * 7 + column - leading + 1
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (cell in 1..monthLength) {
                                    val dayStart = runCatching { JalaliDate.toMillis(year, month, cell) }.getOrNull()
                                    val isFuture = dayStart != null && dayStart > TimeMath.dayStart(maxMillis)
                                    val isBeforeMin = minMillis != null && dayStart != null &&
                                        dayStart < TimeMath.dayStart(minMillis)
                                    val cellEnabled = !isFuture && !isBeforeMin
                                    val isToday = year == today.year && month == today.month && cell == today.day
                                    val isSelected = selectedDay == cell
                                    Box(
                                        Modifier
                                            .padding(3.dp)
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isSelected -> scheme.primary
                                                    isToday -> scheme.primaryContainer
                                                    else -> Color.Transparent
                                                },
                                            )
                                            .then(
                                                if (isToday && !isSelected) {
                                                    Modifier.border(
                                                        1.2.dp,
                                                        scheme.primary,
                                                        RoundedCornerShape(12.dp),
                                                    )
                                                } else {
                                                    Modifier
                                                },
                                            )
                                            .clickable(enabled = cellEnabled) {
                                                selectedDay = cell
                                                dayStart?.let { onPick(it) }
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (persian) Formats.toPersianDigits("$cell") else "$cell",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                !cellEnabled -> scheme.onSurfaceVariant.copy(alpha = 0.35f)
                                                isSelected -> scheme.onPrimary
                                                else -> scheme.onSurface
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        onPick(TimeMath.dayStart(maxMillis))
                    }) {
                        Text(if (persian) "امروز" else "Today")
                    }
                    Text(
                        text = if (persian) "روز را انتخاب کن" else "Pick a day",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (persian) "بستن" else "Close")
            }
        },
    )
}
