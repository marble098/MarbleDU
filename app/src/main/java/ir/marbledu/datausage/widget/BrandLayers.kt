package ir.marbledu.datausage.widget

import android.view.View
import android.widget.RemoteViews
import ir.marbledu.datausage.data.OperatorBrandGradient
import kotlin.math.roundToInt

/**
 * گرادیان اپراتور در RemoteViews (ویجت و اعلان پایش).
 *
 * RemoteViews گرادیانِ پویا ندارد و فرستادن بیت‌مپ با هر به‌روزرسانی هم سنگین است (در
 * اعلانی که هر ثانیه به‌روز می‌شود، خطرناک). برای همین گرادیان از چند ImageView روی هم
 * ساخته می‌شود که هر کدام یک شکل سفیدِ ثابت دارند و رنگشان با `setColorFilter` (و شدت
 * درخشش با `setImageAlpha`) تزریق می‌شود؛ هر دو از اندروید ۸ در دسترس‌اند:
 *
 *  1. پایه: رنگ میانی، یکدست؛
 *  2. شروع: در گوشه‌ی بالای سمت «شروع» (کنار لوگو) مات و تا وسط محو؛
 *  3. پایان: از وسط تا گوشه‌ی پایین سمت «پایان» مات؛
 *  4. درخشش: در گوشه‌ی بالای سمت پایان؛
 *  5. لبه‌ی شیشه‌ای نازک.
 *
 * لایه‌های ۱ تا ۳ روی هم دقیقاً همان گرادیان سه‌رنگ کارت صفحه‌ی خانه را می‌سازند
 * ([ir.marbledu.datausage.ui.operatorGradient])؛ در چیدمان راست‌به‌چپ شکل‌ها قرینه
 * می‌شوند تا رنگ عمیق کنار لوگو (سمت راست) بنشیند.
 */
internal object BrandLayers {

    /** شناسه‌ی ImageViewها و شکل‌های هر سطح (ویجت یا اعلان). */
    class Ids(
        val base: Int,
        val start: Int,
        val end: Int,
        val glow: Int,
        val rim: Int,
        val fadeTopLeft: Int,
        val fadeTopRight: Int,
        val fadeBottomLeft: Int,
        val fadeBottomRight: Int,
        val glowTopLeft: Int,
        val glowTopRight: Int,
    ) {
        val layers: IntArray get() = intArrayOf(base, start, end, glow, rim)
    }

    fun show(views: RemoteViews, ids: Ids, palette: OperatorBrandGradient, rtl: Boolean) {
        ids.layers.forEach { views.setViewVisibility(it, View.VISIBLE) }
        views.setInt(ids.base, "setColorFilter", palette.middle)
        views.setImageViewResource(ids.start, if (rtl) ids.fadeTopRight else ids.fadeTopLeft)
        views.setInt(ids.start, "setColorFilter", palette.start)
        views.setImageViewResource(ids.end, if (rtl) ids.fadeBottomLeft else ids.fadeBottomRight)
        views.setInt(ids.end, "setColorFilter", palette.end)
        val glowAlpha = (palette.glowAlpha * 255).roundToInt().coerceIn(0, 255)
        if (glowAlpha > 0) {
            views.setImageViewResource(ids.glow, if (rtl) ids.glowTopLeft else ids.glowTopRight)
            views.setInt(ids.glow, "setColorFilter", palette.glow)
            views.setInt(ids.glow, "setImageAlpha", glowAlpha)
        } else {
            views.setViewVisibility(ids.glow, View.GONE)
        }
        // لبه‌ی شیشه‌ای: روی گرادیان روشن (متن تیره) کمی تیره، روی گرادیان تیره کمی روشن.
        views.setInt(ids.rim, "setColorFilter", if (palette.darkInk) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        views.setInt(ids.rim, "setImageAlpha", if (palette.darkInk) 30 else 46)
    }

    fun hide(views: RemoteViews, ids: Ids) {
        ids.layers.forEach { views.setViewVisibility(it, View.GONE) }
    }
}
