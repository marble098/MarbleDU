package ir.marbledu.datausage.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import ir.marbledu.datausage.MarbleDuApp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.JalaliDate
import ir.marbledu.datausage.data.TimeMath
import ir.marbledu.datausage.data.UsageLedger

/**
 * «خلاصه‌ی هفتگی»: یک‌بار در هفته (هر ۷ روز از اولین مشاهده به بعد) یک اعلان
 * می‌فرستد که می‌گوید این هفته چقدر مصرف کرده‌ای، نسبت به هفته‌ی قبل بیشتر بوده
 * یا کمتر، پرمصرف‌ترین روز کدام بوده و ساعت اوج چه ساعتی بوده است.
 */
object WeeklyDigestNotifier {

    private const val CHANNEL_ID = "marbledu_weekly_digest"
    private const val PREFS = "marbledu_weekly_digest"
    private const val KEY_LAST_SENT = "last_sent_millis"
    private const val ID_DIGEST = 2102
    private const val WEEK_MILLIS = 7L * 86_400_000L

    /** اگر یک هفته از آخرین خلاصه گذشته باشد، خلاصه‌ی تازه می‌سازد و ارسال می‌کند. */
    suspend fun maybePost(context: Context, persian: Boolean, epochMillis: Long = 0L) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST_SENT, 0L)
        if (last == 0L) {
            // اولین اجرا: شمارش هفته را از همین حالا شروع می‌کنیم.
            prefs.edit().putLong(KEY_LAST_SENT, now).apply()
            return
        }
        if (now - last < WEEK_MILLIS - 12L * 3_600_000L) return

        val repo = (context.applicationContext as MarbleDuApp).appUsageRepository
        val days = runCatching { repo.daySummaries(14, epochMillis) }.getOrDefault(emptyList())
        val last7 = days.takeLast(7)
        val total7 = last7.sumOf { it.total }
        // اگر هفته‌ی گذشته اصلاً ترافیکی ثبت نشده، اعلان بی‌معنی است.
        if (total7 <= 0L) {
            prefs.edit().putLong(KEY_LAST_SENT, now).apply()
            return
        }
        val prevTotal = days.take(7).sumOf { it.total }
        val mobile = last7.sumOf { it.mobile.total }
        val wifi = last7.sumOf { it.wifi.total }
        val peakDay = last7.maxByOrNull { it.total }?.takeIf { it.total > 0L }

        // ساعت اوج: جمع مصرف همه‌ی روزهای هفته، ساعت به ساعت.
        val hourTotals = LongArray(24)
        val todayStart = TimeMath.dayStart(now)
        for (daysAgo in 0 until 7) {
            UsageLedger.hourlyForDay(context, todayStart - daysAgo * 86_400_000L).forEach {
                hourTotals[it.hour.coerceIn(0, 23)] += it.total
            }
        }
        val peakHour = hourTotals.indices.maxByOrNull { hourTotals[it] }?.takeIf { hourTotals[it] > 0L }

        val changeText = if (prevTotal > 0L) {
            val pct = ((total7 - prevTotal) * 100.0 / prevTotal)
            val abs = kotlin.math.abs(pct).toInt()
            if (pct >= 0) {
                if (persian) "${Formats.toPersianDigits("$abs")}٪ بیشتر از هفته‌ی قبل"
                else "$abs% more than last week"
            } else {
                if (persian) "${Formats.toPersianDigits("$abs")}٪ کمتر از هفته‌ی قبل"
                else "$abs% less than last week"
            }
        } else {
            if (persian) "اولین هفته‌ی کامل آمارگیری‌ات" else "Your first full tracked week"
        }

        val title = if (persian) "خلاصه‌ی این هفته" else "Your week in review"
        val line1 = (if (persian) "این هفته " else "This week: ") +
            Formats.bytes(total7, persian) +
            (if (persian) " مصرف کردی — " else " used — ") + changeText
        val peakDayText = peakDay?.let {
            JalaliDate.fromMillis(it.startMillis).long(persian)
        }
        val line2 = buildString {
            if (peakDayText != null) {
                append(if (persian) "پرمصرف‌ترین روز: " else "Busiest day: ")
                append(peakDayText)
                append(if (persian) " (" else " (")
                append(Formats.bytes(peakDay.total, persian))
                append(if (persian) ")  •  " else ")  •  ")
            }
            if (peakHour != null) {
                val hourText = "%02d:00".format(java.util.Locale.US, peakHour)
                append(if (persian) "ساعت اوج: " else "Peak hour: ")
                append(if (persian) Formats.toPersianDigits(hourText) else hourText)
                append("  •  ")
            }
            append(if (persian) "موبایل " else "Mobile ")
            append(Formats.bytes(mobile, persian))
            append("  •  ")
            append(if (persian) "وای‌فای " else "Wi-Fi ")
            append(Formats.bytes(wifi, persian))
        }
        val body = line1 + "\n" + line2

        ensureChannel(context, persian)
        post(context, ID_DIGEST, title, body, persian)
        prefs.edit().putLong(KEY_LAST_SENT, now).apply()
    }

    private fun ensureChannel(context: Context, persian: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            if (persian) "خلاصه‌ی هفتگی" else "Weekly digest",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = if (persian) "هر هفته یک جمع‌بندی از مصرف‌ات" else "A weekly summary of your usage"
        }
        manager.createNotificationChannel(channel)
    }

    private fun post(context: Context, id: Int, title: String, text: String, persian: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val notification = AlertNotificationStyle.build(
            context = context,
            channelId = CHANNEL_ID,
            title = title,
            text = text,
            persian = persian,
            tone = AlertNotificationStyle.Tone.INFO,
            footer = if (persian) "جمع‌بندی هفتگی MarbleDU" else "MarbleDU weekly digest",
            actionLabel = if (persian) "دیدن گزارش‌ها" else "Open reports",
            notificationId = id,
        ).build()
        runCatching { manager.notify(id, notification) }
    }
}
