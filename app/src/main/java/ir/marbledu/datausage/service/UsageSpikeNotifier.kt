package ir.marbledu.datausage.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.Formats

/**
 * هشدار «مصرف غیرعادی»: وقتی حجم مصرف‌شده در همین ساعت خیلی بیشتر از
 * میانگین همان ساعت در روزهای گذشته باشد، یک اعلان می‌دهد.
 * حداکثر روزی یک‌بار برای هر ساعت اعلان داده می‌شود.
 */
object UsageSpikeNotifier {

    private const val CHANNEL_ID = "marbledu_spike_alerts"
    private const val PREFS = "marbledu_spike_alerts"
    private const val KEY_ALERTED = "alerted_keys"
    private const val ID_SPIKE = 2101

    fun checkAndAlert(
        context: Context,
        persian: Boolean,
        hourBytes: Long,
        averageSameHourBytes: Long,
        hourKey: String,
    ) {
        // حداقل کمتر از این مقدار اصلاً غیرعادی حساب نمی‌شود.
        val minimum = 60L * 1024 * 1024
        val threshold = if (averageSameHourBytes > 0L) {
            maxOf(minimum, (averageSameHourBytes * 2.5).toLong())
        } else {
            minimum * 2
        }
        if (hourBytes < threshold) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val alerted = prefs.getStringSet(KEY_ALERTED, emptySet()) ?: emptySet()
        if (alerted.contains(hourKey)) return
        val updated = LinkedHashSet(alerted) + hourKey
        // فقط کلیدهای همان روز نگه داشته می‌شوند تا پیر نشوند.
        prefs.edit().putStringSet(KEY_ALERTED, updated.toList().takeLast(8).toSet()).apply()
        ensureChannel(context, persian)
        val title = if (persian) "مصرف غیرعادی در این ساعت" else "Unusual usage this hour"
        val text = if (persian) {
            "در این ساعت ${Formats.bytes(hourBytes, true)} مصرف شده؛ " +
                "میانگین همین ساعت در روزهای اخیر ${Formats.bytes(averageSameHourBytes, true)} بوده است."
        } else {
            "You used ${Formats.bytes(hourBytes, false)} this hour; " +
                "the same hour recently averaged ${Formats.bytes(averageSameHourBytes, false)}."
        }
        post(context, ID_SPIKE, title, text, persian, hourBytes, averageSameHourBytes)
    }

    private fun ensureChannel(context: Context, persian: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            if (persian) "هشدار مصرف غیرعادی" else "Unusual usage alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = if (persian) "اعلان وقتی مصرف یک ساعت غیرعادی بالا می‌رود"
            else "Alert when one hour's usage is unusually high"
        }
        manager.createNotificationChannel(channel)
    }

    private fun post(
        context: Context,
        id: Int,
        title: String,
        text: String,
        persian: Boolean,
        hourBytes: Long,
        averageBytes: Long,
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val ratio = if (averageBytes > 0) hourBytes.toDouble() / averageBytes else 0.0
        val footer = if (averageBytes > 0) {
            if (persian) {
                "${Formats.number(ratio, true, 1)} برابرِ معمولِ همین ساعت"
            } else {
                "${String.format(java.util.Locale.US, "%.1f", ratio)}× the usual for this hour"
            }
        } else if (persian) "MarbleDU · پایش مصرف" else "MarbleDU · usage monitor"
        val progress = if (averageBytes > 0) {
            ((hourBytes * 100.0) / (averageBytes * 3)).toInt().coerceIn(0, 100)
        } else null
        val notification = AlertNotificationStyle.build(
            context = context,
            channelId = CHANNEL_ID,
            title = title,
            text = text,
            persian = persian,
            tone = AlertNotificationStyle.Tone.WARNING,
            progressPercent = progress,
            footer = footer,
            actionLabel = if (persian) "دیدن جزئیات" else "See details",
            notificationId = id,
        ).build()
        runCatching { manager.notify(id, notification) }
    }
}
