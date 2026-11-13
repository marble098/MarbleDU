package ir.marbledu.datausage.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import ir.marbledu.datausage.ui.theme.LocalMarbleColors
import kotlin.math.max

/**
 * لایه‌ی «شیشه» برای نوارها و کارت‌های شناور.
 *
 *  - پس‌زمینه‌ی نیمه‌شفاف با گرادیان عمودی که از بالا شفاف‌تر شروع می‌شود؛
 *  - یک هاله‌ی رنگی نرم از پالت فعلی که روی شیشه «می‌درخشد»؛
 *  - هایلایت سفید در لبه‌ی بالا و خط نازک اطراف تا حس شیشه‌ی واقعی بدهد.
 *
 * در تم AMOLED نسخه‌ی تیره‌تر و کم‌درخشان‌تر رسم می‌شود تا سیاهی مطلق حفظ شود.
 */
@Composable
fun Modifier.glassSurface(
    shape: Shape,
    tint: Color = MaterialTheme.colorScheme.primary,
    alpha: Float = 0.78f,
): Modifier {
    val scheme = MaterialTheme.colorScheme
    val marble = LocalMarbleColors.current
    val base = scheme.surfaceContainerHigh
    val top = when {
        marble.amoled -> base.copy(alpha = alpha * 0.92f)
        marble.dark -> lerp(base, tint, 0.10f).copy(alpha = alpha)
        else -> lerp(base, Color.White, 0.34f).copy(alpha = alpha)
    }
    val bottom = when {
        marble.amoled -> base.copy(alpha = alpha * 0.98f)
        marble.dark -> lerp(base, tint, 0.04f).copy(alpha = alpha)
        else -> lerp(base, Color.White, 0.10f).copy(alpha = alpha)
    }
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .then(
            if (marble.amoled) {
                Modifier.border(1.dp, Color.White.copy(alpha = 0.09f), shape)
            } else {
                Modifier.border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (marble.dark) 0.22f else 0.85f),
                            Color.White.copy(alpha = if (marble.dark) 0.05f else 0.35f),
                            tint.copy(alpha = 0.20f),
                        ),
                    ),
                    shape,
                )
            },
        )
}

/**
 * نوار ناوبری شیشه‌ای: شیشه‌ی مات با هاله‌ی رنگی، بازتاب نور و خط لبه.
 * محتوای صفحه از زیرش رد می‌شود و همین حس «شیشه‌ی شناور» را کامل می‌کند.
 */
@Composable
fun GlassBar(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    tint: Color = MaterialTheme.colorScheme.primary,
    alpha: Float? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val marble = LocalMarbleColors.current
    // رنگ‌ها را بیرون از Canvas می‌گیریم تا در scope غیر-Composable هم در دسترس باشند.
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val highlight = if (marble.amoled) 0.06f else if (marble.dark) 0.16f else 0.55f
    val surfaceAlpha = alpha ?: if (marble.amoled) 0.90f else 0.72f
    Box(modifier.clip(shape)) {
        // بدنه‌ی شیشه
        Box(Modifier.fillMaxSize().glassSurface(shape, tint, alpha = surfaceAlpha))
        // هاله‌ی رنگی نرم داخل شیشه (چپ و راست) تا شیشه «زنده» دیده شود
        Canvas(Modifier.fillMaxSize().clip(shape)) {
            val w = size.width
            val h = max(size.height, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = highlight * 0.9f), Color.Transparent),
                    center = Offset(w * 0.18f, h * 0.1f),
                    radius = w * 0.55f,
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.18f, h * 0.1f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tertiaryColor.copy(alpha = highlight * 0.55f),
                        Color.Transparent,
                    ),
                    center = Offset(w * 0.86f, h * 0.9f),
                    radius = w * 0.5f,
                ),
                radius = w * 0.5f,
                center = Offset(w * 0.86f, h * 0.9f),
            )
            // بازتاب نور در لبه‌ی بالا
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = highlight * 0.5f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = h * 0.45f,
                ),
                size = androidx.compose.ui.geometry.Size(w, h * 0.45f),
            )
        }
        content()
    }
}

/**
 * پنل شیشه‌ای برای کارت‌های ویژه‌ی خانه (مثل نوار اتصال فعلی)؛
 * کمی «مات» و با درخشش رنگ پالت.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit,
) {
    val marble = LocalMarbleColors.current
    Box(modifier.clip(shape)) {
        Box(Modifier.fillMaxSize().glassSurface(shape, tint, alpha = if (marble.amoled) 0.92f else 0.72f))
        Canvas(Modifier.fillMaxSize().clip(shape)) {
            val w = size.width
            val h = max(size.height, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = if (marble.dark) 0.20f else 0.10f), Color.Transparent),
                    center = Offset(w * 0.05f, 0f),
                    radius = w * 0.75f,
                ),
                radius = w * 0.75f,
                center = Offset(w * 0.05f, 0f),
            )
        }
        content()
    }
}

/** نقطه‌ی نور تزئینی؛ در کارت‌های ویژه پخش می‌شود. */
@Composable
fun GlowDot(
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(size)
            .blur(radius = size / 2, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .background(color, androidx.compose.foundation.shape.CircleShape),
    )
}
