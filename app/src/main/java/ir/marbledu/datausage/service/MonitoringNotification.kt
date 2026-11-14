package ir.marbledu.datausage.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.ConnectionType
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.IspResolver
import ir.marbledu.datausage.data.NotificationConfig
import ir.marbledu.datausage.data.NotificationLockVisibility
import ir.marbledu.datausage.data.NotificationProgressMode
import ir.marbledu.datausage.data.NotificationStyle
import ir.marbledu.datausage.data.NotificationTitleMode
import ir.marbledu.datausage.data.OperatorBrand
import ir.marbledu.datausage.data.OperatorBrandGradient
import ir.marbledu.datausage.ui.theme.ThemePalette
import ir.marbledu.datausage.widget.BrandLayers

/**
 * سازنده‌ی اعلان پایش با ظاهر تازه و ساختارمند (نسخه‌ی ۱.۵).
 *
 * به‌جای دو خط متنِ به‌هم‌چسبیده، اعلان حالا ساختار دارد:
 *
 * ┌────────────────────────────────────────────┐
 * │ ⬤ MarbleDU · ایرانسل · 5G            ۱۰:۳۰ │   ← عنوان + زیرعنوان و ساعت
 * │ ↓ ۱٫۴ م.ب/ث   ↑ ۲۱۰ ک.ب/ث                │   ← سرعت‌ها، درشت و رنگی
 * │ ─────────────────────────────              │
 * │ امروز  ۴۲۳ م.ب · موبایل ۱۲۰ / وای‌فای ۳۰۰ │   ← مصرف امروز
 * │ بسته   ۵.۶ گ.ب مانده (۳۸٪)                │   ← نوار پیشرفت + باقی‌مانده
 * └────────────────────────────────────────────┘
 *
 * از نسخه‌ی ۱.۶.۶ محتوای اعلان یک کارت گرادیانی با لوگوی اپراتور است ([brandedViews])؛
 * همان پالتی که کارت اپراتور صفحه‌ی خانه و ویجت استفاده می‌کنند.
 */
object MonitoringNotification {

    /** لایه‌های گرادیان اپراتور در چیدمان‌های اعلان ([R.layout.notif_monitor_small] و big). */
    private val BRAND_LAYERS = BrandLayers.Ids(
        base = R.id.notif_brand_base,
        start = R.id.notif_brand_start,
        end = R.id.notif_brand_end,
        glow = R.id.notif_brand_glow,
        rim = R.id.notif_brand_rim,
        fadeTopLeft = R.drawable.notif_brand_fade_tl,
        fadeTopRight = R.drawable.notif_brand_fade_tr,
        fadeBottomLeft = R.drawable.notif_brand_fade_bl,
        fadeBottomRight = R.drawable.notif_brand_fade_br,
        glowTopLeft = R.drawable.notif_brand_glow_tl,
        glowTopRight = R.drawable.notif_brand_glow_tr,
    )

    /** نمای جمع‌شده و بازِ اعلان پایش. */
    class BrandedViews(val collapsed: RemoteViews, val expanded: RemoteViews)

    /**
     * کارت گرادیانی اعلان پایش؛ همان پالت کارت اپراتور در صفحه‌ی خانه و ویجت.
     *
     * اعلان رنگی سیستم (`setColorized`) فقط یک رنگ یکدست می‌گیرد؛ برای گرادیان واقعی محتوای
     * اعلان با RemoteViews سفارشی ساخته می‌شود و سیستم سربرگ، آیکون و دکمه‌ها را دورش
     * می‌چیند (DecoratedCustomViewStyle). هیچ بیت‌مپی ساخته نمی‌شود؛ رنگ‌ها با فیلتر رنگ
     * روی شکل‌های ثابت تزریق می‌شوند، پس به‌روزرسانی هر ثانیه‌ی اعلان سبک می‌ماند.
     *
     * @param logoRes لوگوی اپراتور؛ نال یعنی اپراتور لوگو ندارد و نماد برنامه نشان داده می‌شود.
     * @param body خطوط تفصیلی (سبک «کامل»)؛ نال یا خالی یعنی فقط عنوان و خلاصه.
     */
    fun brandedViews(
        context: Context,
        palette: OperatorBrandGradient,
        logoRes: Int?,
        title: CharSequence,
        summary: CharSequence,
        body: CharSequence?,
        progressPercent: Int?,
        persian: Boolean,
    ): BrandedViews {
        val ink = palette.ink
        val soft = withAlpha(ink, 0.86f)
        val collapsed = RemoteViews(context.packageName, R.layout.notif_monitor_small)
        val expanded = RemoteViews(context.packageName, R.layout.notif_monitor_big)
        listOf(collapsed, expanded).forEach { views ->
            views.setInt(
                R.id.notif_root,
                "setLayoutDirection",
                if (persian) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR,
            )
            BrandLayers.show(views, BRAND_LAYERS, palette, rtl = persian)
            if (logoRes != null) {
                views.setImageViewResource(R.id.notif_logo, logoRes)
                // فیلتر شفاف یعنی بدون تغییر رنگ؛ لوگوها رنگ اصلی خودشان را نگه می‌دارند.
                views.setInt(R.id.notif_logo, "setColorFilter", 0)
            } else {
                views.setImageViewResource(R.id.notif_logo, R.drawable.ic_notification)
                views.setInt(R.id.notif_logo, "setColorFilter", logoGlyphColor(palette))
            }
            views.setTextViewText(R.id.notif_title, title)
            views.setTextColor(R.id.notif_title, ink)
            views.setTextViewText(R.id.notif_text, summary)
            views.setTextColor(R.id.notif_text, soft)
        }

        val bodyLines = NotificationFormatter.bodyLinesWithoutTitle(body, title)
        val hasBody = bodyLines.isNotEmpty()
        expanded.setViewVisibility(R.id.notif_body, if (hasBody) View.VISIBLE else View.GONE)
        // در سبک «کامل» خطوط تفصیلی جای خلاصه را می‌گیرند (مثل قبل در BigTextStyle).
        expanded.setViewVisibility(R.id.notif_text, if (hasBody) View.GONE else View.VISIBLE)
        if (hasBody) {
            expanded.setTextViewText(R.id.notif_body, bodyLines.joinToString("\n"))
            expanded.setTextColor(R.id.notif_body, withAlpha(ink, 0.94f))
        }
        val onLight = palette.darkInk
        val shownBar = if (onLight) R.id.notif_progress_on_light else R.id.notif_progress_on_dark
        val hiddenBar = if (onLight) R.id.notif_progress_on_dark else R.id.notif_progress_on_light
        expanded.setViewVisibility(hiddenBar, View.GONE)
        if (progressPercent != null) {
            expanded.setViewVisibility(shownBar, View.VISIBLE)
            expanded.setProgressBar(shownBar, 100, progressPercent.coerceIn(0, 100), false)
        } else {
            expanded.setViewVisibility(shownBar, View.GONE)
        }
        return BrandedViews(collapsed, expanded)
    }

    /** رنگ نماد برنامه داخل کاشی سفید وقتی اپراتور لوگو ندارد: تیره‌ترین رنگ خوانای پالت. */
    private fun logoGlyphColor(palette: OperatorBrandGradient): Int =
        palette.stops.minBy { OperatorBrand.luminance(it) }

    private fun withAlpha(color: Int, alpha: Float): Int =
        ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24) or (color and 0x00FFFFFF)

    /**
     * متن ساختاریافته‌ی اعلان برای حالت‌های بازشونده؛ هر خط یک بخش مستقل است
     * تا وقتی سیستم اعلان را جمع می‌کند، خواندنش راحت باشد.
     */
    fun expandedBody(
        input: NotificationInput,
        config: NotificationConfig,
        content: NotificationContent,
        persian: Boolean,
    ): CharSequence {
        val compact = config.compactUnits
        fun bytes(v: Long) = if (compact) Formats.compactBytes(v, persian) else Formats.bytes(v, persian)
        fun speed(v: Double) = if (compact) Formats.compactSpeed(v, persian) else Formats.speed(v, persian)
        val sep = "  ·  "
        val lines = buildList {
            if (config.showDownloadSpeed || config.showUploadSpeed) {
                val parts = buildList {
                    if (config.showDownloadSpeed) add("↓ ${speed(input.downBytesPerSec)}")
                    if (config.showUploadSpeed) add("↑ ${speed(input.upBytesPerSec)}")
                    if (config.showTotalSpeed) add("⇅ ${speed(input.downBytesPerSec + input.upBytesPerSec)}")
                }
                if (parts.isNotEmpty()) add(parts.joinToString("   "))
            }
            val today = input.todayMobileBytes + input.todayWifiBytes
            if (config.showTodayTotal) {
                add((if (persian) "امروز: " else "Today: ") + bytes(today))
            }
            val split = buildList {
                if (config.showTodayMobileWifi) {
                    add((if (persian) "موبایل " else "Mobile ") + bytes(input.todayMobileBytes))
                    add((if (persian) "وای‌فای " else "Wi-Fi ") + bytes(input.todayWifiBytes))
                }
                if (config.showSessionUsage) {
                    val sessionTotal = input.sessionRxBytes + input.sessionTxBytes
                    if (sessionTotal > 0L) {
                        add((if (persian) "نشست " else "Session ") + bytes(sessionTotal))
                    }
                }
            }
            if (split.isNotEmpty()) add(split.joinToString(sep))
            val capBits = buildList {
                if (config.showDataCap && input.capTotalBytes > 0L) {
                    val left = (input.capTotalBytes - input.capUsedBytes).coerceAtLeast(0L)
                    val pct = (Formats.percentOf(input.capUsedBytes, input.capTotalBytes) * 100).toInt()
                    add(
                        (if (persian) "بسته: " else "Plan: ") + bytes(left) +
                            (if (persian) " مانده" else " left") +
                            " (${Formats.integer(pct.toLong(), persian)}${if (persian) "٪" else "%"})",
                    )
                }
                if (config.showTopApp && input.topAppName.isNotBlank() && input.topAppBytes > 0L) {
                    add((if (persian) "پرمصرف: " else "Top: ") + input.topAppName)
                }
                if (config.showAverageSpeed || config.showPeakSpeed) {
                    val avgPeak = buildList {
                        if (config.showAverageSpeed) {
                            add((if (persian) "میانگین " else "avg ") + speed(input.averageSpeedBps))
                        }
                        if (config.showPeakSpeed) {
                            add((if (persian) "اوج " else "peak ") + speed(input.peakSpeedBps))
                        }
                    }
                    add(avgPeak.joinToString(sep))
                }
            }
            if (capBits.isNotEmpty()) add(capBits.joinToString(sep))
        }
        val body = lines.ifEmpty { listOf(content.summaryText) }
        // در فارسی علامت راست‌به‌چپ ابتدای هر خط تا ترکیب فارسی/لاتین به‌هم نریزد.
        return if (persian) body.joinToString("\n") { "\u200F$it" } else body.joinToString("\n")
    }

    /** رنگ تاکید اعلان از پالت اپ. */
    fun accentColor(palette: ThemePalette): Int = palette.primary.toInt()

    /** نقطه‌ی رنگی ساده (برای آیکون بزرگ هشدارها) با هاله‌ی ملایم. */
    fun accentDot(context: Context, accent: Int): android.graphics.Bitmap {
        val size = context.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
            .coerceIn(48, 128)
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color = accent
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.color = 0x33FFFFFF
        canvas.drawCircle(size / 2f, size / 2f, size * 0.28f, paint)
        return bitmap
    }
}
