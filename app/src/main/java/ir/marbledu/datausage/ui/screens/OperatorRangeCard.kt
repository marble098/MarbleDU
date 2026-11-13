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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.JalaliDate
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.SimIdentity
import ir.marbledu.datausage.ui.CardHeader
import ir.marbledu.datausage.ui.JalaliDatePickerDialog
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.MarbleCard
import ir.marbledu.datausage.ui.OperatorBadge
import ir.marbledu.datausage.ui.theme.LocalMarbleColors

private fun rangeSimLabel(sim: SimIdentity?, persian: Boolean): String =
    sim?.let {
        val slot = it.slotIndex?.let { s -> if (persian) "سیم‌کارت ${s + 1}" else "SIM ${s + 1}" }
        val name = it.displayName?.takeIf { n -> n.isNotBlank() }
        listOfNotNull(slot, name).joinToString(" · ")
    }.orEmpty()

/**
 * کارت «اپراتورها و نوع اتصال»: مصرف هر اپراتور را برای امروز، ۷ روز اخیر
 * یا یک تاریخ شمسی انتخابی نشان می‌دهد و سهم موبایل/وای‌فای هرکدام را جدا می‌کند.
 * با زدن روی هر اپراتور، صفحه‌ی کامل جزئیات ۳۰ روزه باز می‌شود.
 */
@Composable
fun OperatorRangeCard(
    vm: MainViewModel,
    persian: Boolean,
    onOpenOperatorDetail: (operatorId: String, sim: SimIdentity?) -> Unit,
    /** نمایش ردیف‌های تخمینی/ناشناخته هم (حالت «سفر»). */
) {
    val context = LocalContext.current
    val range by vm.operatorRange.collectAsState()
    val marble = LocalMarbleColors.current
    val scheme = MaterialTheme.colorScheme
    var pickerOpen by remember { mutableStateOf(false) }
    var pickedDay by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        if (!range.loadedOnce) vm.loadOperatorRange(range.mode, pickedDay)
    }

    MarbleCard {
        CardHeader(
            title = if (persian) "اپراتورها و نوع اتصال" else "Operators & transport",
            icon = MIcons.Carrier,
            tint = scheme.tertiary,
            subtitle = if (persian) "مصرف هر اپراتور با تفکیک موبایل و وای‌فای"
            else "Per-operator usage, split by mobile and Wi-Fi",
        )

        // انتخاب بازه: امروز / ۷ روز اخیر / انتخاب تاریخ شمسی
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = range.mode == 0,
                onClick = { vm.loadOperatorRange(0, null) },
                label = { Text(if (persian) "امروز" else "Today") },
            )
            FilterChip(
                selected = range.mode == 1,
                onClick = { vm.loadOperatorRange(1, null) },
                label = { Text(if (persian) "۷ روز اخیر" else "7 days") },
            )
            FilterChip(
                selected = range.mode == 2,
                onClick = { pickerOpen = true },
                label = { Text(if (persian) "انتخاب تاریخ" else "Pick date") },
            )
        }
        if (range.mode == 2) {
            val label = pickedDay?.let { JalaliDate.fromMillis(it).long(persian) }
                ?: if (persian) "هنوز روزی انتخاب نشده" else "No day picked yet"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.secondaryContainer.copy(alpha = 0.45f))
                    .clickable { pickerOpen = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(MIcons.Calendar, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSecondaryContainer,
                )
            }
        }

        if (range.loading) {
            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 3.dp)
            }
        } else if (range.rows.isEmpty()) {
            Text(
                text = if (persian) {
                    "برای این بازه مصرفی ثبت نشده. پایش پس‌زمینه را روشن نگه دار."
                } else {
                    "No usage recorded for this range yet. Keep background monitoring on."
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        } else {
            // Keep measured unknown traffic visible; omit empty placeholder rows.
            val visibleRows = range.rows.filter {
                it.operatorId != OperatorCatalog.ID_UNKNOWN || it.total.total > 0L
            }
            if (visibleRows.isEmpty()) {
                Text(
                    text = if (persian) {
                        "برای این بازه مصرفی ثبت نشده. پایش پس‌زمینه را روشن نگه دار."
                    } else {
                        "No usage recorded for this range yet. Keep background monitoring on."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            val grandTotal = visibleRows.sumOf { it.total.total }.coerceAtLeast(1L)
            visibleRows.sortedByDescending { it.total.total }.forEach { row ->
                OperatorTransportRow(
                    row = row,
                    grandTotal = grandTotal,
                    persian = persian,
                    mobileColor = marble.mobile,
                    wifiColor = marble.wifi,
                    onClick = { onOpenOperatorDetail(row.operatorId, row.sim) },
                )
            }
        }
    }

    if (pickerOpen) {
        JalaliDatePickerDialog(
            persian = persian,
            maxMillis = System.currentTimeMillis(),
            initialMillis = pickedDay,
            onPick = { dayStart ->
                pickedDay = dayStart
                pickerOpen = false
                vm.loadOperatorRange(2, dayStart)
            },
            onDismiss = { pickerOpen = false },
        )
    }
}

@Composable
private fun OperatorTransportRow(
    row: ir.marbledu.datausage.data.OperatorRangeRow,
    grandTotal: Long,
    persian: Boolean,
    mobileColor: androidx.compose.ui.graphics.Color,
    wifiColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val operator = OperatorCatalog.byId(row.operatorId)
    val mobile = row.mobile.total
    val wifi = row.wifi.total
    val total = (mobile + wifi).coerceAtLeast(0L)
    val maxBar = grandTotal.toFloat()
    val mobileFraction = if (maxBar > 0f) mobile / maxBar else 0f
    val wifiFraction = if (maxBar > 0f) wifi / maxBar else 0f
    val wifiName = if (persian) "وای‌فای" else "Wi-Fi"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OperatorBadge(operator, size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = operator.name(persian),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                val simText = rangeSimLabel(row.sim, persian)
                if (simText.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = simText,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = Formats.bytes(total, persian),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                    maxLines = 1,
                )
            }
            // نوار دوبخشی: سهم موبایل (تیره‌تر) و وای‌فای (روشن‌تر) نسبت به مجموع بازه
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(99.dp)),
            ) {
                if (mobileFraction > 0f) {
                    Box(
                        Modifier
                            .weight(mobileFraction)
                            .height(7.dp)
                            .background(mobileColor),
                    )
                }
                if (wifiFraction > 0f) {
                    Box(
                        Modifier
                            .weight(wifiFraction)
                            .height(7.dp)
                            .background(wifiColor),
                    )
                }
                if (mobileFraction <= 0f && wifiFraction <= 0f) {
                    Box(Modifier.weight(1f).height(7.dp).background(scheme.surfaceVariant))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Canvas(Modifier.size(8.dp)) {
                        drawCircle(mobileColor)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = (if (persian) "موبایل " else "Mobile ") + Formats.bytes(mobile, persian),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Canvas(Modifier.size(8.dp)) {
                        drawCircle(wifiColor)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$wifiName " + Formats.bytes(wifi, persian),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Icon(
                    MIcons.ChevronEnd,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
