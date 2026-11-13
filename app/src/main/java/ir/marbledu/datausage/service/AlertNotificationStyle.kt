package ir.marbledu.datausage.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ir.marbledu.datausage.MainActivity
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.Formats

/**
 * ظاهر یکسان برای همه‌ی اعلان‌های هشدار (سقف مصرف، جهش مصرف، خلاصه‌ی هفتگی).
 *
 * هر هشدار یک «امضا» دارد: عنوان کوتاه، متن، خط کمکی و رنگ تاکید. این کلاس
 * ساختار بصری مشترک را می‌سازد تا هشدارها هم‌خانواده و مرتب دیده شوند:
 *
 * ┌──────────────────────────────────────────┐
 * │ MarbleDU · هشدار مصرف            (آیکون) │
 * │ ۸۰٪ بسته مصرف شد                        │  ← عنوان درشت
 * │ ۹٫۲ از ۱۱ گ.ب مصرف شده…                  │
 * │ ── نوار پیشرفت (در صورت وجود) ──          │
 * │ [گزارش بسته]  [تنظیمات]                   │
 * └──────────────────────────────────────────┘
 */
object AlertNotificationStyle {

    enum class Tone(val accent: Int, val bgTint: Int) {
        /** هشدار جدی: قرمز-نارنجی */
        CRITICAL(0xFFFF4D5E.toInt(), 0x33FF4D5E),

        /** هشدار متوسط: نارنجی زعفرانی */
        WARNING(0xFFFFA726.toInt(), 0x33FFA726),

        /** اطلاع‌رسانی: آبی/فیروزه‌ای */
        INFO(0xFF2FBFEF.toInt(), 0x332FBFEF),
    }

    /**
     * اعلان هشدار شیک با عنوان، متن، نوار پیشرفت اختیاری و دکمه‌ی اقدام.
     *
     * @param progressPercent درصد ۰..۱۰۰ برای نوار پیشرفت؛ null یعنی بدون نوار.
     */
    fun build(
        context: Context,
        channelId: String,
        title: String,
        text: String,
        persian: Boolean,
        tone: Tone = Tone.INFO,
        progressPercent: Int? = null,
        footer: String? = null,
        actionLabel: String? = null,
        notificationId: Int = 0,
        autoCancel: Boolean = true,
    ): NotificationCompat.Builder {
        val open = PendingIntent.getActivity(
            context,
            notificationId + 400,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val body = buildString {
            append("\u200F".takeIf { persian }.orEmpty())
            append(text)
            if (!footer.isNullOrBlank()) {
                append("\n\n")
                append(if (persian) "• " else "• ")
                append(footer)
            }
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (persian) "\u200F$title" else title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(autoCancel)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(tone.accent)
            .setColorized(false)
        runCatching {
            builder.setLargeIcon(
                android.graphics.drawable.Icon.createWithBitmap(
                    MonitoringNotification.accentDot(context, tone.accent),
                ),
            )
        }
        if (progressPercent != null) {
            builder.setProgress(100, progressPercent.coerceIn(0, 100), false)
        }
        if (!actionLabel.isNullOrBlank()) {
            builder.addAction(R.drawable.ic_notif_action_reports, actionLabel, open)
        }
        return builder
    }

    /** خط کمکی «امشب / این دوره» که در همه‌ی هشدارها یکسان است. */
    fun footline(
        persian: Boolean,
        usedBytes: Long? = null,
        limitBytes: Long? = null,
    ): String = when {
        usedBytes != null && limitBytes != null && limitBytes > 0 -> {
            val pct = (usedBytes * 100.0 / limitBytes).toInt().coerceIn(0, 200)
            if (persian) {
                "${Formats.bytes(usedBytes, true)} از ${Formats.bytes(limitBytes, true)} — " +
                    "${Formats.integer(pct.toLong(), true)}٪ مصرف شده"
            } else {
                "${Formats.bytes(usedBytes, false)} of ${Formats.bytes(limitBytes, false)} — $pct% used"
            }
        }
        else -> if (persian) "MarbleDU · پایش مصرف" else "MarbleDU · usage monitor"
    }
}
