package ir.marbledu.datausage.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * نمودارهای اپ – همه با Canvas و بدون کتابخانه‌ی اضافی.
 *
 *  - [StackedBarChart]: میله‌ای چندرنگ، قابل لمس (ضربه یا کشیدن انگشت)، با خط
 *    میانگین و برچسب بیشینه. برای ۷ روز، ۳۰ روز و ۲۴ ساعت.
 *  - [DonutChart] + [DonutLegend]: سهم‌ها (موبایل/وای‌فای، اپ‌ها، اپراتورها).
 *  - [SpeedLineChart]: نمودار زنده‌ی سرعت دانلود/آپلود.
 *  - [ArcGauge]: عقربه‌ی نیم‌دایره برای سرعت و سقف بسته.
 *
 * در چیدمان راست‌به‌چپ، محور زمان هم از راست به چپ کشیده می‌شود.
 */

data class BarEntry(val label: String, val values: List<Float>)

@Composable
fun StackedBarChart(
    entries: List<BarEntry>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
    selected: Int? = null,
    onSelect: ((Int) -> Unit)? = null,
    labelEvery: Int = 1,
    maxLabel: String = "",
    averageLabel: String = "",
    showAverage: Boolean = true,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp,
        lineHeight = 13.sp,
    )
    val selectedLabelStyle = labelStyle.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
    )
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val highlight = MaterialTheme.colorScheme.onSurface
    val averageColor = MaterialTheme.colorScheme.tertiary
    val count = entries.size
    val totals = remember(entries) { entries.map { e -> e.values.sum() } }
    val maxValue = (totals.maxOrNull() ?: 0f).coerceAtLeast(1f)
    val average = if (totals.isEmpty()) 0f else totals.sum() / totals.size
    val progress = remember { Animatable(0f) }
    LaunchedEffect(count) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }
    val currentOnSelect by rememberUpdatedState(onSelect)

    fun indexAt(x: Float, width: Float): Int {
        if (count <= 0 || width <= 0f) return 0
        val visual = (x / (width / count)).toInt().coerceIn(0, count - 1)
        return if (rtl) count - 1 - visual else visual
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(count, rtl) {
                detectTapGestures { offset ->
                    if (count > 0) currentOnSelect?.invoke(indexAt(offset.x, size.width.toFloat()))
                }
            }
            .pointerInput(count, rtl) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    if (count > 0) currentOnSelect?.invoke(indexAt(change.position.x, size.width.toFloat()))
                }
            },
    ) {
        if (count == 0) return@Canvas
        val labelHeight = 18.dp.toPx()
        val topPad = 16.dp.toPx()
        val plotHeight = (size.height - labelHeight - topPad).coerceAtLeast(1f)
        val bottom = topPad + plotHeight
        val slot = size.width / count
        val barWidth = (slot * 0.64f).coerceAtMost(28.dp.toPx()).coerceAtLeast(2f)
        val radius = (barWidth / 2f).coerceAtMost(7.dp.toPx())

        // خط پایه و خط بیشینه
        drawLine(gridColor, Offset(0f, bottom), Offset(size.width, bottom), 1.dp.toPx())
        drawLine(
            gridColor,
            Offset(0f, topPad),
            Offset(size.width, topPad),
            1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
        )
        if (maxLabel.isNotEmpty()) {
            val layout = measurer.measure(maxLabel, labelStyle)
            val x = if (rtl) size.width - layout.size.width else 0f
            drawText(layout, topLeft = Offset(x, 0f))
        }

        entries.forEachIndexed { i, entry ->
            val visual = if (rtl) count - 1 - i else i
            val centerX = slot * visual + slot / 2f
            val total = totals[i]
            val isSelected = selected == i
            val alpha = if (selected == null || isSelected) 1f else 0.42f
            if (isSelected) {
                drawRoundRect(
                    color = highlight.copy(alpha = 0.07f),
                    topLeft = Offset(slot * visual, topPad - 4.dp.toPx()),
                    size = Size(slot, plotHeight + 4.dp.toPx()),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
            }
            val left = centerX - barWidth / 2f
            if (total > 0f) {
                val h = (plotHeight * (total / maxValue) * progress.value).coerceAtLeast(3.dp.toPx())
                val top = bottom - h
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(left, top, left + barWidth, bottom),
                            topLeft = CornerRadius(radius),
                            topRight = CornerRadius(radius),
                            bottomRight = CornerRadius(1f),
                            bottomLeft = CornerRadius(1f),
                        ),
                    )
                }
                clipPath(path) {
                    var y = bottom
                    entry.values.forEachIndexed { k, v ->
                        if (v > 0f) {
                            val segment = h * (v / total)
                            val c = colors[k % colors.size]
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(c, c.copy(alpha = 0.74f)),
                                    startY = y - segment,
                                    endY = y,
                                ),
                                topLeft = Offset(left, y - segment),
                                size = Size(barWidth, segment),
                                alpha = alpha,
                            )
                            y -= segment
                        }
                    }
                }
            } else {
                drawRoundRect(
                    color = gridColor,
                    topLeft = Offset(left, bottom - 2.dp.toPx()),
                    size = Size(barWidth, 2.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
            }
            val every = labelEvery.coerceAtLeast(1)
            if (entry.label.isNotEmpty() && (i % every == 0 || isSelected)) {
                val layout = measurer.measure(entry.label, if (isSelected) selectedLabelStyle else labelStyle)
                val x = (centerX - layout.size.width / 2f)
                    .coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
                drawText(layout, topLeft = Offset(x, bottom + 3.dp.toPx()))
            }
        }

        // خط میانگین
        if (showAverage && average > 0f && count > 1) {
            val y = bottom - plotHeight * (average / maxValue) * progress.value
            drawLine(
                averageColor,
                Offset(0f, y),
                Offset(size.width, y),
                1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            )
            if (averageLabel.isNotEmpty()) {
                val layout = measurer.measure(averageLabel, labelStyle.copy(color = averageColor))
                val x = if (rtl) 0f else size.width - layout.size.width
                val labelY = (y - layout.size.height - 1.dp.toPx()).coerceAtLeast(0f)
                drawText(layout, topLeft = Offset(x, labelY))
            }
        }
    }
}

data class DonutSlice(
    val label: String,
    val value: Float,
    val color: Color,
    val detail: String = "",
)

/** نمودار دونات با محتوای وسط. */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    diameter: Dp = 136.dp,
    stroke: Dp = 18.dp,
    selected: Int? = null,
    center: @Composable () -> Unit = {},
) {
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices.size) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 800, easing = FastOutSlowInEasing))
    }
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2f + 3.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val total = slices.sumOf { it.value.toDouble() }.toFloat()
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(strokePx))
            if (total <= 0f) return@Canvas
            val visible = slices.count { it.value > 0f }
            val gap = if (visible > 1) 3f else 0f
            var start = -90f
            slices.forEachIndexed { i, slice ->
                if (slice.value <= 0f) return@forEachIndexed
                val sweep = 360f * (slice.value / total) * progress.value
                val width = if (selected == i) strokePx + 5.dp.toPx() else strokePx
                val alpha = if (selected == null || selected == i) 1f else 0.4f
                drawArc(
                    color = slice.color,
                    startAngle = start + gap / 2f,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.6f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    alpha = alpha,
                    style = Stroke(width, cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }
        center()
    }
}

/** راهنمای دونات: رنگ، نام، درصد و مقدار. با لمس هر ردیف همان برش برجسته می‌شود. */
@Composable
fun DonutLegend(
    slices: List<DonutSlice>,
    persian: Boolean,
    modifier: Modifier = Modifier,
    selected: Int? = null,
    onSelect: ((Int?) -> Unit)? = null,
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        slices.forEachIndexed { i, slice ->
            val percent = if (total > 0f) slice.value / total * 100f else 0f
            val rowModifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (selected == i) slice.color.copy(alpha = 0.14f) else Color.Transparent,
                )
                .let { m ->
                    if (onSelect != null) m.clickable { onSelect(if (selected == i) null else i) } else m
                }
                .padding(horizontal = 6.dp, vertical = 3.dp)
            Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(slice.color))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        slice.label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (slice.detail.isNotEmpty()) {
                        Text(
                            slice.detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = ir.marbledu.datausage.data.Formats.number(percent.toDouble(), persian, if (percent < 10f) 1 else 0) +
                        (if (persian) "٪" else "%"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = slice.color,
                )
            }
        }
    }
}

/** نمودار زنده‌ی سرعت: دو خط نرم با سایه‌ی گرادیانی. */
@Composable
fun SpeedLineChart(
    down: List<Float>,
    up: List<Float>,
    downColor: Color,
    upColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    capacity: Int = 60,
    maxLabel: String = "",
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val measurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp,
        lineHeight = 13.sp,
    )
    val rawMax = maxOf(down.maxOrNull() ?: 0f, up.maxOrNull() ?: 0f, 1024f)
    val animatedMax by animateFloatAsState(rawMax * 1.2f, tween(400), label = "speed-max")

    Canvas(modifier.fillMaxWidth().height(height)) {
        val topPad = 16.dp.toPx()
        val bottom = size.height - 2.dp.toPx()
        val plotHeight = (bottom - topPad).coerceAtLeast(1f)
        val slots = (capacity - 1).coerceAtLeast(1)
        val step = size.width / slots

        for (i in 0..3) {
            val y = topPad + plotHeight * i / 3f
            drawLine(
                gridColor,
                Offset(0f, y),
                Offset(size.width, y),
                1.dp.toPx(),
                pathEffect = if (i == 3) null else PathEffect.dashPathEffect(floatArrayOf(5f, 9f)),
            )
        }
        if (maxLabel.isNotEmpty()) {
            val layout = measurer.measure(maxLabel, labelStyle)
            drawText(layout, topLeft = Offset(if (rtl) size.width - layout.size.width else 0f, 0f))
        }

        fun xOf(index: Int, n: Int): Float {
            // آخرین نمونه همیشه به لبه‌ی «انتهای» نمودار می‌چسبد.
            val fromEnd = (n - 1 - index) * step
            return if (rtl) fromEnd else size.width - fromEnd
        }

        fun yOf(v: Float): Float = bottom - plotHeight * (v / animatedMax).coerceIn(0f, 1f)

        fun series(values: List<Float>, color: Color) {
            val n = values.size
            if (n < 2) return
            val line = Path()
            var prevX = xOf(0, n)
            var prevY = yOf(values[0])
            line.moveTo(prevX, prevY)
            for (i in 1 until n) {
                val x = xOf(i, n)
                val y = yOf(values[i])
                val mid = (prevX + x) / 2f
                line.cubicTo(mid, prevY, mid, y, x, y)
                prevX = x
                prevY = y
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(xOf(n - 1, n), bottom)
                lineTo(xOf(0, n), bottom)
                close()
            }
            drawPath(
                fill,
                Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.34f), color.copy(alpha = 0f)),
                    startY = topPad,
                    endY = bottom,
                ),
            )
            drawPath(
                line,
                color,
                style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawCircle(color.copy(alpha = 0.25f), 7.dp.toPx(), Offset(prevX, prevY))
            drawCircle(color, 3.5.dp.toPx(), Offset(prevX, prevY))
        }

        series(up, upColor)
        series(down, downColor)
    }
}

/** عقربه‌ی قوسی (۲۴۰ درجه) با محتوای وسط – برای سرعت و سقف بسته. */
@Composable
fun ArcGauge(
    fraction: Float,
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 170.dp,
    stroke: Dp = 16.dp,
    center: @Composable () -> Unit = {},
) {
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val animated by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "gauge",
    )
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2f + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = track,
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round),
            )
            if (animated > 0.004f) {
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(startColor, endColor),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f),
                    ),
                    startAngle = 150f,
                    sweepAngle = 240f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokePx, cap = StrokeCap.Round),
                )
            }
        }
        center()
    }
}

/** نوار نسبتِ دو یا چند بخش (مثلاً موبایل / وای‌فای) در یک خط. */
@Composable
fun SplitBar(
    parts: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    val total = parts.sumOf { it.first.toDouble() }.toFloat()
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(track),
    ) {
        if (total > 0f) {
            parts.forEach { (value, color) ->
                if (value > 0f) {
                    Box(
                        Modifier
                            .weight(value / total)
                            .height(height)
                            .background(color),
                    )
                }
            }
        }
    }
}
