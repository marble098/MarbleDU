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
import ir.marbledu.datausage.service.MonitorService
import ir.marbledu.datausage.ui.theme.ThemePalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

/**
 * ویجت صفحه‌ی اصلی: مصرف امروز (موبایل / وای‌فای)، اپراتور فعلی و وضعیت بسته.
 *
 * چیدمان تازه‌ی ۱.۵: چیدمان ساختارمند با بدنه‌ی شیشه‌ای، عدد بزرگ و دو ستون
 * موبایل/وای‌فای، رنگ‌گرفته از تم فعلی اپ و دو میان‌بر (توقف/ادامه‌ی پایش و
 * باز کردن گزارش‌ها). سرویس پایش آن را زنده نگه می‌دارد؛ بدون سرویس هم هر
 * ۳۰ دقیقه تازه می‌شود.
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

    /** دکمه‌ی «توقف/ادامه» روی ویجت. */
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_MONITOR -> {
                scope.launch {
                    val app = context.applicationContext as? MarbleDuApp ?: return@launch
                    val settings = runCatching { app.settingsRepository.settings.first() }.getOrNull()
                    val running = settings?.monitorInBackground == true
                    runCatching { app.settingsRepository.setMonitorBackground(!running) }
                    if (running) MonitorService.stop(context) else MonitorService.start(context)
                    runCatching { withTimeoutOrNull(6_000) { refresh(context) } }
                }
                return
            }
            ACTION_REFRESH -> {
                val pending = goAsync()
                scope.launch {
                    try {
                        withTimeoutOrNull(8_000) { refresh(context.applicationContext) }
                    } finally {
                        runCatching { pending.finish() }
                    }
                }
                return
            }
        }
        super.onReceive(context, intent)
    }

    data class Snapshot(
        val persian: Boolean,
        val mobileBytes: Long,
        val wifiBytes: Long,
        val operatorName: String,
        val capUsedBytes: Long,
        val capTotalBytes: Long,
        val showOperator: Boolean = true,
        val showCap: Boolean = true,
        val bigText: Boolean = false,
        val detailed: Boolean = false,
        val rxBytes: Long = 0L,
        val txBytes: Long = 0L,
        /** رنگ اصلی و رنگ دومِ تم اپ؛ ویجت با همین دو رنگ گرادیان می‌گیرد. */
        val themePrimary: Int? = null,
        val themeSecondary: Int? = null,
        val darkTheme: Boolean = true,
        /** میان‌برهای ویجت (توقف/ادامه‌ی پایش و گزارش‌ها). */
        val quickActions: Boolean = true,
        val monitoring: Boolean = true,
    )

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        const val ACTION_TOGGLE_MONITOR = "ir.marbledu.datausage.widget.TOGGLE_MONITOR"
        const val ACTION_REFRESH = "ir.marbledu.datausage.widget.REFRESH"

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
                runCatching { app.appUsageRepository.monthMobileTotal(settings.statsEpochMillis) }.getOrDefault(0L)
            } else 0L
            val palette = ThemePalette.of(settings.palette)
            val follow = settings.widgetFollowTheme
            render(
                context,
                Snapshot(
                    persian = persian,
                    mobileBytes = mobile.total,
                    wifiBytes = wifi.total,
                    operatorName = operatorName(context, persian, settings.operatorOverride),
                    capUsedBytes = capUsed,
                    capTotalBytes = settings.dataCapBytes,
                    showOperator = settings.widgetShowOperator,
                    showCap = settings.widgetShowCap,
                    bigText = settings.widgetBigText,
                    detailed = settings.widgetDetailed,
                    rxBytes = mobile.rx + wifi.rx,
                    txBytes = mobile.tx + wifi.tx,
                    themePrimary = if (follow) palette.primary.toInt() else null,
                    themeSecondary = if (follow) palette.tertiary.toInt() else null,
                    quickActions = settings.widgetQuickActions,
                    monitoring = settings.monitorInBackground,
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

            // ---- رنگ: اگر «هم‌رنگ تم» روشن باشد، گرادیان ویجت از پالت اپ ساخته می‌شود.
            applyTheme(views, s)

            views.setTextViewText(R.id.widget_title, if (p) "مصرف امروز" else "Today")
            views.setTextViewTextSize(R.id.widget_total, 2, if (s.bigText) 32f else 26f)
            views.setTextViewText(R.id.widget_total, Formats.bytes(s.mobileBytes + s.wifiBytes, p))

            if (s.showOperator) {
                views.setViewVisibility(R.id.widget_operator, View.VISIBLE)
                views.setTextViewText(R.id.widget_operator, s.operatorName.ifBlank { "MarbleDU" })
            } else {
                views.setViewVisibility(R.id.widget_operator, View.GONE)
            }

            views.setTextViewText(R.id.widget_mobile_label, if (p) "موبایل" else "Mobile")
            views.setTextViewText(R.id.widget_mobile, Formats.bytes(s.mobileBytes, p))
            views.setTextViewText(R.id.widget_wifi_label, if (p) "وای‌فای" else "Wi-Fi")
            views.setTextViewText(R.id.widget_wifi, Formats.bytes(s.wifiBytes, p))

            if (s.detailed) {
                views.setViewVisibility(R.id.widget_detail_row, View.VISIBLE)
                views.setTextViewText(R.id.widget_rx, "↓ " + Formats.bytes(s.rxBytes, p))
                views.setTextViewText(R.id.widget_tx, "↑ " + Formats.bytes(s.txBytes, p))
            } else {
                views.setViewVisibility(R.id.widget_detail_row, View.GONE)
            }

            if (s.showCap && s.capTotalBytes > 0L) {
                val percent = (Formats.percentOf(s.capUsedBytes, s.capTotalBytes) * 100f).roundToInt()
                    .coerceIn(0, 100)
                views.setViewVisibility(R.id.widget_cap_row, View.VISIBLE)
                views.setProgressBar(R.id.widget_cap_progress, 100, percent, false)
                val left = Formats.bytes((s.capTotalBytes - s.capUsedBytes).coerceAtLeast(0L), p)
                views.setTextViewText(R.id.widget_cap_text, if (p) "$left مانده" else "$left left")
            } else {
                views.setViewVisibility(R.id.widget_cap_row, View.GONE)
            }

            // ---- میان‌برها: توقف/ادامه‌ی پایش + گزارش ۳۰ روز
            if (s.quickActions) {
                views.setViewVisibility(R.id.widget_actions_row, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_action_toggle,
                    if (s.monitoring) {
                        if (p) "توقف پایش" else "Pause"
                    } else {
                        if (p) "شروع پایش" else "Resume"
                    },
                )
                views.setTextViewText(
                    R.id.widget_action_reports,
                    if (p) "گزارش ۳۰ روز" else "Reports",
                )
                views.setOnClickPendingIntent(
                    R.id.widget_action_toggle,
                    PendingIntent.getBroadcast(
                        context,
                        52,
                        Intent(context, UsageWidgetProvider::class.java).setAction(ACTION_TOGGLE_MONITOR),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    ),
                )
                views.setOnClickPendingIntent(
                    R.id.widget_action_reports,
                    PendingIntent.getActivity(
                        context,
                        53,
                        Intent(context, MainActivity::class.java).apply {
                            putExtra(MainActivity.EXTRA_START_TAB, MainActivity.TAB_REPORTS)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        },
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    ),
                )
            } else {
                views.setViewVisibility(R.id.widget_actions_row, View.GONE)
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

        /**
         * رنگ ویجت: تاکیدهای ویجت (عدد موبایل/وای‌فای، آیکون و دکمه‌ها) از پالت
         * فعلی اپ گرفته می‌شوند تا ویجت هم‌رنگ بقیه‌ی برنامه باشد. پس‌زمینه‌ی
         * شیشه‌ایِ ویجت ثابت می‌ماند تا متن روشن همیشه خوانا بماند.
         */
        private fun applyTheme(views: RemoteViews, s: Snapshot) {
            val primary = s.themePrimary ?: return
            val secondary = s.themeSecondary ?: primary
            val mobile = blend(primary, 0xFFFFFFFF.toInt(), 0.46f)
            val wifi = blend(secondary, 0xFFFFFFFF.toInt(), 0.52f)
            val soft = blend(primary, 0xFFFFFFFF.toInt(), 0.78f)
            views.setTextColor(R.id.widget_mobile, mobile)
            views.setTextColor(R.id.widget_wifi, wifi)
            views.setTextColor(R.id.widget_total, 0xFFFFFFFF.toInt())
            views.setTextColor(R.id.widget_action_toggle, soft)
            views.setTextColor(R.id.widget_action_reports, soft)
            views.setInt(R.id.widget_icon, "setColorFilter", mobile)
        }

        private fun blend(from: Int, to: Int, t: Float): Int {
            val a1 = (from shr 24) and 0xFF
            val r1 = (from shr 16) and 0xFF
            val g1 = (from shr 8) and 0xFF
            val b1 = from and 0xFF
            val a2 = (to shr 24) and 0xFF
            val r2 = (to shr 16) and 0xFF
            val g2 = (to shr 8) and 0xFF
            val b2 = to and 0xFF
            val c = { x: Int, y: Int -> (x + (y - x) * t).roundToInt().coerceIn(0, 255) }
            return (c(a1, a2) shl 24) or (c(r1, r2) shl 16) or (c(g1, g2) shl 8) or c(b1, b2)
        }
    }
}
