package ir.marbledu.datausage.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ir.marbledu.datausage.MainActivity
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
        post(context, ID_SPIKE, title, text)
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
