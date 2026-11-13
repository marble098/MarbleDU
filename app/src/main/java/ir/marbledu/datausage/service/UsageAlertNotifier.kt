package ir.marbledu.datausage.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ir.marbledu.datausage.MainActivity
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.TimeMath
import ir.marbledu.datausage.data.UsagePlanner

/**
 * هشدارهای مصرف: رسیدن به سقف روزانه‌ی دیتای موبایل و ۸۰٪ / ۱۰۰٪ سقف بسته.
 * هر هشدار فقط یک‌بار داده می‌شود (وضعیت در SharedPreferences می‌ماند).
 */
object UsageAlertNotifier {

    private const val CHANNEL_ID = "marbledu_usage_alerts"
    private const val PREFS = "marbledu_usage_alerts"
    private const val KEY_DAILY_DAY = "daily_day"
    private const val KEY_CAP_LEVEL = "cap_level"
    private const val ID_DAILY = 2001
    private const val ID_CAP = 2002

    fun check(
        context: Context,
        settings: AppSettings,
        persian: Boolean,
        todayMobileBytes: Long,
        capUsedBytes: Long,
        now: Long = System.currentTimeMillis(),
    ) {
        if (!settings.usageAlerts) return
        if (settings.dailyLimitMb <= 0 && settings.dataCapBytes <= 0L) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val state = UsagePlanner.AlertState(
            dailyDay = prefs.getLong(KEY_DAILY_DAY, 0L),
            capLevel = prefs.getInt(KEY_CAP_LEVEL, 0),
        )
        val decision = UsagePlanner.decideAlerts(
            state = state,
            dayStart = TimeMath.dayStart(now),
            todayMobileBytes = todayMobileBytes,
            dailyLimitBytes = settings.dailyLimitBytes,
            capUsedBytes = capUsedBytes,
            capBytes = settings.dataCapBytes,
        )
        if (decision.state != state) {
            prefs.edit()
                .putLong(KEY_DAILY_DAY, decision.state.dailyDay)
                .putInt(KEY_CAP_LEVEL, decision.state.capLevel)
                .apply()
        }
        if (decision.alerts.isEmpty()) return
        ensureChannel(context, persian)
        for (alert in decision.alerts) {
            val (title, text) = message(alert, settings, persian, todayMobileBytes, capUsedBytes)
            post(context, if (alert == UsagePlanner.Alert.DAILY_LIMIT) ID_DAILY else ID_CAP, title, text)
        }
    }

    fun message(
        alert: UsagePlanner.Alert,
        settings: AppSettings,
        persian: Boolean,
        todayMobileBytes: Long,
        capUsedBytes: Long,
    ): Pair<String, String> = when (alert) {
        UsagePlanner.Alert.DAILY_LIMIT -> if (persian) {
            "از سقف مصرف روزانه گذشتی" to
                "امروز ${Formats.bytes(todayMobileBytes, true)} دیتای موبایل مصرف شده؛ " +
                "سقفی که گذاشته بودی ${Formats.bytes(settings.dailyLimitBytes, true)} بود."
        } else {
            "Daily data limit reached" to
                "${Formats.bytes(todayMobileBytes, false)} of mobile data used today; " +
                "your limit is ${Formats.bytes(settings.dailyLimitBytes, false)}."
        }
        UsagePlanner.Alert.CAP_WARNING -> if (persian) {
            "۸۰٪ بسته مصرف شد" to
                "${Formats.bytes(capUsedBytes, true)} از ${Formats.bytes(settings.dataCapBytes, true)} مصرف شده. " +
                "${Formats.bytes((settings.dataCapBytes - capUsedBytes).coerceAtLeast(0), true)} باقی مانده."
        } else {
            "80% of your data plan is used" to
                "${Formats.bytes(capUsedBytes, false)} of ${Formats.bytes(settings.dataCapBytes, false)} used. " +
                "${Formats.bytes((settings.dataCapBytes - capUsedBytes).coerceAtLeast(0), false)} left."
        }
        UsagePlanner.Alert.CAP_REACHED -> if (persian) {
            "بسته تمام شد" to
                "مصرف دیتای موبایل به ${Formats.bytes(capUsedBytes, true)} رسید و از سقف " +
                "${Formats.bytes(settings.dataCapBytes, true)} گذشت. مراقب مصرف آزاد باش."
        } else {
            "Data plan used up" to
                "Mobile data reached ${Formats.bytes(capUsedBytes, false)}, past your " +
                "${Formats.bytes(settings.dataCapBytes, false)} plan. Watch out for out-of-plan charges."
        }
    }

    private fun ensureChannel(context: Context, persian: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            if (persian) "هشدار مصرف" else "Usage alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = if (persian) "هشدار سقف روزانه و تمام شدن بسته" else "Daily limit and data plan warnings"
        }
        manager.createNotificationChannel(channel)
    }

    private fun post(context: Context, id: Int, title: String, text: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        runCatching { manager.notify(id, notification) }
    }
}
