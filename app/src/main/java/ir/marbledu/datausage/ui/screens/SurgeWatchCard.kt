package ir.marbledu.datausage.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.SurgePrediction
import ir.marbledu.datausage.ui.CardHeader
import ir.marbledu.datausage.ui.MIcons
import ir.marbledu.datausage.ui.MarbleCard
import ir.marbledu.datausage.ui.SurgeWatchUi
import ir.marbledu.datausage.ui.theme.LocalMarbleColors
import ir.marbledu.datausage.ui.theme.accessibleColor

/**
 * «هشدار زودهنگام جهش مصرف» — قابلیت تازه‌ی ۱.۶.۴.
 *
 * بقیه‌ی اپ‌ها بعد از تمام‌شدن بسته خبر می‌دهند؛ این کارت مصرف ساعت جاری را با
 * همان ساعت در روزهای گذشته مقایسه می‌کند و اگر شیب مصرف بالا رفته باشد،
 * از قبل می‌گوید «تا ۱۵ دقیقه‌ی آینده این‌قدر مصرف می‌شود؛ کدام اپ هم دارد
 * می‌خورد». وقتی همه‌چیز عادی است، کارت جمع و آرام می‌ماند تا صفحه شلوغ نشود.
 */
@Composable
fun SurgeWatchCard(
    watch: SurgeWatchUi,
    persian: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = watch.result ?: return
    val scheme = MaterialTheme.colorScheme
    val marble = LocalMarbleColors.current
    val warn = accessibleColor(Color(0xFFF97316), scheme.surfaceContainerLow)
    val calm = marble.wifi
    val tone = when (result.risk) {
        SurgePrediction.Risk.CRITICAL -> accessibleColor(Color(0xFFDC2626), scheme.surfaceContainerLow)
        SurgePrediction.Risk.WARNING -> warn
        SurgePrediction.Risk.WATCH -> accessibleColor(Color(0xFFEAB308), scheme.surfaceContainerLow)
        SurgePrediction.Risk.NONE -> calm
    }

    MarbleCard(
        modifier = modifier,
        container = if (result.isAlert) tone.copy(alpha = 0.10f) else scheme.surfaceContainerLow,
        padding = 14.dp,
    ) {
        CardHeader(
            title = if (persian) "پایش لحظه‌ای جهش مصرف" else "Live surge watch",
            icon = if (result.isAlert) MIcons.Bell else MIcons.Speed,
            tint = tone,
            subtitle = when (result.risk) {
                SurgePrediction.Risk.CRITICAL ->
                    if (persian) "مصرف این ساعت به‌شدت بالاست" else "Usage this hour is far above normal"
                SurgePrediction.Risk.WARNING ->
                    if (persian) "مصرف این ساعت بیشتر از حد معمول است" else "Usage this hour is above normal"
                SurgePrediction.Risk.WATCH ->
                    if (persian) "شیب مصرف در حال بالا رفتن است" else "Usage is trending upward"
                SurgePrediction.Risk.NONE ->
                    if (persian) "همه‌چیز عادی است" else "Everything looks normal"
            },
            trailing = if (result.multiplier > 0f) {
                "×" + Formats.integer(result.multiplier.toLong().coerceAtLeast(1L), persian)
            } else null,
        )

        if (result.isAlert) {
            // نبض ملایم دور نقطه‌ی هشدار: توجه را جلب می‌کند بدون این‌که آزاردهنده باشد.
            val pulse by rememberInfiniteTransition(label = "surge").animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "pulse",
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tone.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .alpha(pulse)
                        .clip(CircleShape)
                        .background(tone),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (persian) {
                            "تا ۱۵ دقیقه‌ی آینده حدود ${Formats.bytes(result.projectedSoonBytes, true)} مصرف می‌شود"
                        } else {
                            "About ${Formats.bytes(result.projectedSoonBytes, false)} in the next 15 minutes"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (result.shareOfRemainingPercent > 0) {
                        Text(
                            text = if (persian) {
                                "${Formats.integer(result.shareOfRemainingPercent.toLong(), true)}٪ از باقی‌مانده‌ی بسته"
                            } else {
                                "${result.shareOfRemainingPercent}% of what is left"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = tone,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            if (watch.topAppLabel.isNotBlank() && watch.topAppBytes > 0L) {
                Text(
                    text = if (persian) {
                        "مصرف‌کننده‌ی اصلی این ساعت: ${watch.topAppLabel} با ${Formats.bytes(watch.topAppBytes, true)}"
                    } else {
                        "Biggest consumer this hour: ${watch.topAppLabel} with ${Formats.bytes(watch.topAppBytes, false)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurface,
                )
            }
        } else {
            Text(
                text = if (persian) {
                    "مصرف این ساعت ${Formats.compactBytes(result.currentHourBytes, true)} است" +
                        if (result.baselineHourBytes > 0L) {
                            "، نزدیک به معمولِ همین ساعت (${Formats.compactBytes(result.baselineHourBytes, true)})"
                        } else {
                            ""
                        }
                } else {
                    "${Formats.compactBytes(result.currentHourBytes, false)} this hour" +
                        if (result.baselineHourBytes > 0L) {
                            ", close to the usual ${Formats.compactBytes(result.baselineHourBytes, false)}"
                        } else {
                            ""
                        }
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }

        TextButton(onClick = onRefresh) {
            Text(if (persian) "بررسی دوباره" else "Check again")
        }
    }
}
