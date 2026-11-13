package ir.marbledu.datausage.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
            val tone = when (alert) {
                UsagePlanner.Alert.CAP_REACHED -> AlertNotificationStyle.Tone.CRITICAL
                UsagePlanner.Alert.CAP_WARNING -> AlertNotificationStyle.Tone.WARNING
                UsagePlanner.Alert.DAILY_LIMIT -> AlertNotificationStyle.Tone.WARNING
            }
            val progress = when (alert) {
                UsagePlanner.Alert.DAILY_LIMIT -> if (settings.dailyLimitBytes > 0) {
                    ((todayMobileBytes * 100.0 / settings.dailyLimitBytes).toInt()).coerceIn(0, 100)
                } else null
                else -> if (settings.dataCapBytes > 0) {
                    ((capUsedBytes * 100.0 / settings.dataCapBytes).toInt()).coerceIn(0, 100)
                } else null
            }
            val footer = when (alert) {
                UsagePlanner.Alert.DAILY_LIMIT -> if (persian) "سقف روزانه" else "Daily limit"
                else -> if (persian) "سقف بسته" else "Data plan"
            }
            val action = if (persian) "دیدن گزارش و بسته" else "Open usage"
            post(
                context = context,
                id = if (alert == UsagePlanner.Alert.DAILY_LIMIT) ID_DAILY else ID_CAP,
                title = title,
                text = text,
                persian = persian,
                tone = tone,
                progressPercent = progress,
                footer = footer,
                action = action,
            )
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

    private fun post(
        context: Context,
        id: Int,
        title: String,
        text: String,
        persian: Boolean,
        tone: AlertNotificationStyle.Tone,
        progressPercent: Int?,
        footer: String,
        action: String,
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val notification = AlertNotificationStyle.build(
            context = context,
            channelId = CHANNEL_ID,
            title = title,
            text = text,
            persian = persian,
            tone = tone,
            progressPercent = progressPercent,
            footer = footer,
            actionLabel = action,
            notificationId = id,
        ).build()
        runCatching { manager.notify(id, notification) }
    }
}
