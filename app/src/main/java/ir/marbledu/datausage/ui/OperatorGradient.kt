package ir.marbledu.datausage.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import ir.marbledu.datausage.data.OperatorBrandGradient

/**
 * پس‌زمینه‌ی گرادیانی اپراتور؛ همان پالتی که اعلان پایش و ویجت هم استفاده می‌کنند.
 *
 *  - گرادیان مورب سه‌ایستگاهی از گوشه‌ی بالای سمت «شروع» (کنار لوگو؛ در فارسی راست)
 *    تا گوشه‌ی پایین سمت «پایان»؛
 *  - یک درخشش نرم شعاعی در گوشه‌ی بالای سمت پایان، با شدتی که [OperatorBrandGradient.glowAlpha]
 *    تعیین می‌کند تا متن هیچ‌جا کم‌خوان نشود.
 *
 * برش گوشه‌ها با خودِ فراخواننده است (مثلاً `clip(shape)` پیش از این modifier).
 */
fun Modifier.operatorGradient(palette: OperatorBrandGradient): Modifier = drawBehind {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@drawBehind
    val rtl = layoutDirection == LayoutDirection.Rtl
    drawRect(
        Brush.linearGradient(
            colors = listOf(Color(palette.start), Color(palette.middle), Color(palette.end)),
            start = if (rtl) Offset(w, 0f) else Offset.Zero,
            end = if (rtl) Offset(0f, h) else Offset(w, h),
        ),
    )
    if (palette.glowAlpha > 0f) {
        val glow = Color(palette.glow)
        drawRect(
            Brush.radialGradient(
                0f to glow.copy(alpha = palette.glowAlpha),
                0.45f to glow.copy(alpha = palette.glowAlpha * 0.42f),
                1f to glow.copy(alpha = 0f),
                center = Offset(if (rtl) w * 0.08f else w * 0.92f, -h * 0.10f),
                radius = maxOf(w, h) * 0.62f,
            ),
        )
    }
}
