package ir.marbledu.datausage.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import ir.marbledu.datausage.MainActivity
import ir.marbledu.datausage.MarbleDuApp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.ConnectionType
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.LocaleHelper
import ir.marbledu.datausage.data.NetworkStatsReader
import ir.marbledu.datausage.data.OperatorCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * ویجت صفحه‌ی اصلی: مصرف امروز (موبایل / وای‌فای)، اپراتور فعلی و وضعیت بسته.
 * سرویس پایش آن را زنده نگه می‌دارد؛ بدون سرویس هم هر ۳۰ دقیقه تازه می‌شود.
 */
class UsageWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                withTimeoutOrNull(8_000) { refresh(context.applicationContext) }
            } finally {
                runCatching { pending.finish() }
            }
        }
    }

    data class Snapshot(
        val persian: Boolean,
        val mobileBytes: Long,
        val wifiBytes: Long,
        val operatorName: String,
        val capUsedBytes: Long,
        val capTotalBytes: Long,
    )

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        @Volatile
        private var lastPushAt = 0L

        fun hasWidgets(context: Context): Boolean = runCatching {
            val manager = AppWidgetManager.getInstance(context) ?: return false
            manager.getAppWidgetIds(ComponentName(context, UsageWidgetProvider::class.java)).isNotEmpty()
        }.getOrDefault(false)

        /** خواندن تازه‌ی آمار و رسم همه‌ی ویجت‌ها. */
        suspend fun refresh(context: Context) {
            if (!hasWidgets(context)) return
            val app = context.applicationContext as? MarbleDuApp ?: return
            val settings = app.settingsRepository.settings.first()
            val persian = LocaleHelper.isPersian(settings.language)
            val (mobile, wifi) = runCatching { app.appUsageRepository.todayDeviceTraffic() }.getOrNull() ?: return
            val capUsed = if (settings.dataCapBytes > 0L) {
                runCatching { app.appUsageRepository.monthMobileTotal() }.getOrDefault(0L)
            } else 0L
            render(
                context,
                Snapshot(
                    persian = persian,
                    mobileBytes = mobile.total,
                    wifiBytes = wifi.total,
                    operatorName = operatorName(context, persian, settings.operatorOverride),
                    capUsedBytes = capUsed,
                    capTotalBytes = settings.dataCapBytes,
                ),
            )
        }

        /** سرویس پایش با آمار آماده صدا می‌زند (حداکثر هر ۲۰ ثانیه یک‌بار). */
        fun push(context: Context, snapshot: Snapshot) {
            val now = System.currentTimeMillis()
            if (now - lastPushAt < 20_000L) return
            if (!hasWidgets(context)) return
            lastPushAt = now
            render(context, snapshot)
        }

        fun operatorName(context: Context, persian: Boolean, override: String): String {
            val network = NetworkStatsReader.currentNetwork(context, persian)
            return when {
                network.type == ConnectionType.OFFLINE -> if (persian) "آفلاین" else "Offline"
                network.type == ConnectionType.CELLULAR && override.isNotBlank() ->
                    OperatorCatalog.resolveName(override).name(persian)
                network.operator.id == OperatorCatalog.ID_UNKNOWN -> network.carrierName
                else -> network.operator.name(persian)
            }
        }

        private fun render(context: Context, s: Snapshot) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, UsageWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = RemoteViews(context.packageName, R.layout.widget_usage)
            val p = s.persian
            views.setTextViewText(R.id.widget_title, if (p) "مصرف امروز" else "Today")
            views.setTextViewText(R.id.widget_total, Formats.bytes(s.mobileBytes + s.wifiBytes, p))
            views.setTextViewText(R.id.widget_mobile_label, if (p) "موبایل" else "Mobile")
            views.setTextViewText(R.id.widget_mobile, Formats.bytes(s.mobileBytes, p))
            views.setTextViewText(R.id.widget_wifi_label, if (p) "وای‌فای" else "Wi-Fi")
            views.setTextViewText(R.id.widget_wifi, Formats.bytes(s.wifiBytes, p))
            views.setTextViewText(R.id.widget_operator, s.operatorName.ifBlank { "MarbleDU" })
            if (s.capTotalBytes > 0L) {
                val percent = (Formats.percentOf(s.capUsedBytes, s.capTotalBytes) * 100f).toInt().coerceIn(0, 100)
                views.setViewVisibility(R.id.widget_cap_row, View.VISIBLE)
                views.setProgressBar(R.id.widget_cap_progress, 100, percent, false)
                val left = Formats.bytes((s.capTotalBytes - s.capUsedBytes).coerceAtLeast(0L), p)
                views.setTextViewText(R.id.widget_cap_text, if (p) "$left مانده" else "$left left")
            } else {
                views.setViewVisibility(R.id.widget_cap_row, View.GONE)
            }
            val open = PendingIntent.getActivity(
                context,
                40,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            runCatching { manager.updateAppWidget(ids, views) }
        }
    }
}
