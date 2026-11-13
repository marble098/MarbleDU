package ir.marbledu.datausage.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
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
import ir.marbledu.datausage.ui.theme.ThemePalette

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
 * آیکون بزرگ (LargeIcon) با رنگ پالت فعلی اپ نقاشی می‌شود تا اعلان هم‌رنگ
 * برنامه باشد؛ رنگ نوار پیشرفت هم از همان پالت گرفته می‌شود.
 */
object MonitoringNotification {

    /** آیکون بزرگِ اعلان: حلقه‌ی رنگی با فلش‌های دانلود/آپلود. */
    private fun largeIcon(context: Context, accent: Int): android.graphics.Bitmap {
        val size = context.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
            .coerceIn(48, 128)
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val radius = size / 2f

        // پس‌زمینه‌ی دایره‌ای با گرادیان از رنگ پالت به نسخه‌ی تیره‌تر
        val rect = android.graphics.RectF(0f, 0f, size.toFloat(), size.toFloat())
        paint.shader = android.graphics.LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            intArrayOf(accent, blend(accent, 0xFF10131C.toInt(), 0.55f)),
            null,
            android.graphics.Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(radius, radius, radius, paint)
        paint.shader = null

        // حلقه‌ی نور
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = size * 0.055f
        paint.color = 0x33FFFFFF
        canvas.drawCircle(radius, radius, radius * 0.74f, paint)

        // فلش دانلود (پایین) و آپلود (بالا)
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = 0xFFFFFFFF.toInt()
        val w = size * 0.10f
        val arrow = android.graphics.Path()
        // دانلود: از بالا به پایین
        arrow.moveTo(radius - w, size * 0.30f)
        arrow.lineTo(radius + w, size * 0.30f)
        arrow.lineTo(radius + w, size * 0.52f)
        arrow.lineTo(radius + w * 2.1f, size * 0.52f)
        arrow.lineTo(radius, size * 0.70f)
        arrow.lineTo(radius - w * 2.1f, size * 0.52f)
        arrow.lineTo(radius - w, size * 0.52f)
        arrow.close()
        canvas.drawPath(arrow, paint)

        val arrowUp = android.graphics.Path()
        arrowUp.moveTo(radius - w, size * 0.70f)
        arrowUp.lineTo(radius + w, size * 0.70f)
        arrowUp.lineTo(radius + w, size * 0.62f)
        arrowUp.lineTo(radius + w * 1.8f, size * 0.62f)
        arrowUp.lineTo(radius, size * 0.50f)
        arrowUp.lineTo(radius - w * 1.8f, size * 0.62f)
        arrowUp.lineTo(radius - w, size * 0.62f)
        arrowUp.close()
        paint.color = 0x22000000
        canvas.drawPath(arrowUp, paint)
        return bitmap
    }

    private fun blend(from: Int, to: Int, t: Float): Int {
        val a = (from shr 24) and 0xFF
        val r = (from shr 16) and 0xFF
        val g = (from shr 8) and 0xFF
        val b = from and 0xFF
        val r2 = (to shr 16) and 0xFF
        val g2 = (to shr 8) and 0xFF
        val b2 = to and 0xFF
        fun c(x: Int, y: Int) = (x + (y - x) * t).toInt().coerceIn(0, 255)
        return (a shl 24) or (c(r, r2) shl 16) or (c(g, g2) shl 8) or c(b, b2)
    }

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
