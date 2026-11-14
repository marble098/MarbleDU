package ir.marbledu.datausage.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import ir.marbledu.datausage.MarbleDuApp
import ir.marbledu.datausage.data.AppLanguage
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.ConnectionType
import ir.marbledu.datausage.data.LocaleHelper
import ir.marbledu.datausage.data.NetworkStatsReader
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.OperatorBrand
import ir.marbledu.datausage.data.ThemeMode
import ir.marbledu.datausage.service.MonitorService
import ir.marbledu.datausage.ui.theme.ThemePalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * ویجت «مصرف امروز» صفحه‌ی اصلی.
 *
 * مسئولیت این کلاس فقط «گرفتن تازه‌ترین داده و به‌روزرسانی ویجت» است؛ ظاهر و
 * چیدمان در [WidgetRenderer]، تصمیم اندازه در [WidgetLayout]، قالب عددها در
 * [WidgetFormat] و رنگ‌ها در [WidgetTheme] است. به همین دلیل هر تکه‌ی آن جدا
 * قابل تست است.
 *
 * مسیرهای تازه‌سازی:
 *  ۱) سرویس پایش هر چند ثانیه یک‌بار با [push] ویجت را زنده نگه می‌دارد،
 *  ۲) دکمه‌ی تازه‌سازی روی خود ویجت ([ACTION_REFRESH])،
 *  ۳) اندروید هر ۳۰ دقیقه (updatePeriodMillis) و هنگام تغییر اندازه.
 */
class UsageWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        renderImmediately(context)
        refreshAsync(context, goAsync(), forceWidgetCheck = true)
    }

    /** اولین‌بار که ویجت اضافه می‌شود، داده را فوری بیاور. */
    override fun onEnabled(context: Context) {
        renderImmediately(context)
        refreshAsync(context, goAsync(), forceWidgetCheck = true)
    }

    /**
     * پیش از هر خواندن سنگین، ویجت را با «آخرین تصویر شناخته‌شده» (یا چیدمان
     * حداقلی) رسم می‌کند.
     *
     * چرا؟ لانچر بعد از `onUpdate` انتظار دارد ویوها را تحویل بگیرد؛ اگر رسم
     * کامل (خواندن آمار + ساخت چیدمان) طول بکشد یا شکست بخورد، جعبه‌ی قرمز
     * «Can't load widget» نمایش داده می‌شود. این تابع بدون هیچ I/O و در همان
     * فراخوانی، یک ویجت سالم روی صفحه می‌گذارد.
     */
    private fun renderImmediately(context: Context) {
        val appContext = context.applicationContext
        runCatching {
            val cached = lastSnapshot
            if (cached != null) {
                WidgetRenderer.renderAll(appContext, cached)
            } else {
                WidgetRenderer.renderFallback(
                    appContext,
                    persian = LocaleHelper.isPersian(AppLanguage.SYSTEM),
                    darkSurface = WidgetTheme.isDarkSurface(
                        ThemeMode.SYSTEM.name,
                        systemNight(appContext),
                    ),
                )
            }
        }
    }

    /** با تغییر اندازه، چیدمان همان لحظه دوباره چیده می‌شود. */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        val cached = lastSnapshot
        if (cached != null) {
            WidgetRenderer.renderAll(context.applicationContext, cached)
        } else {
            refreshAsync(context, goAsync(), forceWidgetCheck = true)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_MONITOR -> {
                toggleMonitoring(context)
                return
            }
            ACTION_REFRESH -> {
                refreshAsync(context, goAsync(), forceWidgetCheck = true)
                return
            }
        }
        super.onReceive(context, intent)
    }

    /**
     * توقف/ادامه‌ی پایش با یک ضربه. مقدار تنظیم اول ذخیره و بعد سرویس
     * روشن/خاموش می‌شود؛ اگر روشن‌کردن سرویس اجازه نداشت (محدودیت اندروید ۱۲+)
     * مقدار به حالت قبل برمی‌گردد تا ویجت وضعیت نادرست نشان ندهد.
     */
    private fun toggleMonitoring(context: Context) {
        val pending = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
                val app = appContext as? MarbleDuApp
                val current = runCatching { app?.settingsRepository?.settings?.first() }
                    .getOrNull()?.monitorInBackground ?: false
                val target = !current
                runCatching { app?.settingsRepository?.setMonitorBackground(target) }
                if (target) {
                    if (!MonitorService.start(appContext)) {
                        runCatching { app?.settingsRepository?.setMonitorBackground(false) }
                    }
                } else {
                    MonitorService.stop(appContext)
                }
                withTimeoutOrNull(5_000) { refreshNow(appContext) }
            } finally {
                runCatching { pending.finish() }
            }
        }
    }

    companion object {

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        const val ACTION_TOGGLE_MONITOR = "ir.marbledu.datausage.widget.TOGGLE_MONITOR"
        const val ACTION_REFRESH = "ir.marbledu.datausage.widget.REFRESH"

        private const val BROADCAST_TIMEOUT_MS = 8_000L
        private const val STATS_TIMEOUT_MS = 6_000L

        /** حداقل فاصله‌ی دو رسم تکراری با داده‌ی یکسان (صرفه‌جویی باتری). */
        private const val IDLE_RENDER_MS = 45_000L

        @Volatile
        private var lastSnapshot: WidgetSnapshot? = null

        @Volatile
        private var lastRenderAt = 0L

        @Volatile
        private var widgetsCheckedAt = 0L

        @Volatile
        private var widgetsPresent = false

        /** حداقل فاصله‌ی دو پرس‌وجوی فهرست ویجت‌ها (سرویس هر چند ثانیه صدا می‌زند). */
        private const val WIDGET_CHECK_MS = 30_000L

        /**
         * آیا کاربر حداقل یک ویجت روی صفحه‌ی اصلی دارد؟ نتیجه کمی کش می‌شود تا
         * حلقه‌ی پایش هر چند ثانیه یک پرس‌وجوی سیستمی نفرستد.
         */
        fun hasWidgets(context: Context): Boolean {
            val now = System.currentTimeMillis()
            if (now - widgetsCheckedAt < WIDGET_CHECK_MS) return widgetsPresent
            val present = runCatching {
                val manager = AppWidgetManager.getInstance(context) ?: return false
                manager.getAppWidgetIds(ComponentName(context, UsageWidgetProvider::class.java))
                    .isNotEmpty()
            }.getOrDefault(false)
            widgetsCheckedAt = now
            widgetsPresent = present
            return present
        }

        /**
         * به‌روزرسانی ویجت از سمت برنامه (تغییر تنظیمات، تم، زبان، بازیابی
         * پشتیبان و…). بلافاصله برمی‌گردد و کار در پس‌زمینه انجام می‌شود.
         */
        fun refresh(context: Context) = refreshAsync(context, null, forceWidgetCheck = true)

        /**
         * اجرای تازه‌سازی در پس‌زمینه. اگر از داخل یک BroadcastReceiver صدا
         * زده شود، [pending] باعث می‌شود سیستم تا پایان کار پروسه را زنده نگه دارد.
         */
        private fun refreshAsync(
            context: Context,
            pending: BroadcastReceiver.PendingResult?,
            forceWidgetCheck: Boolean = false,
        ) {
            val appContext = context.applicationContext
            if (forceWidgetCheck) widgetsCheckedAt = 0L
            scope.launch {
                try {
                    val rendered = runCatching {
                        withTimeoutOrNull(BROADCAST_TIMEOUT_MS) { refreshNow(appContext) } ?: false
                    }.getOrDefault(false)
                    if (!rendered) {
                        // داده نیامد (بی‌دسترسی، خطای خواندن یا تمام‌شدن مهلت)؛
                        // به‌جای رهاکردن ویجت روی پیام خطای لانچر، همان تصویر
                        // قبلی را نگه می‌داریم و اگر هیچ تصویری نیست، حداقلی رسم می‌کنیم.
                        val cached = lastSnapshot
                        runCatching {
                            if (cached != null) {
                                WidgetRenderer.renderAll(appContext, cached)
                            } else {
                                WidgetRenderer.renderFallback(
                                    appContext,
                                    persian = LocaleHelper.isPersian(AppLanguage.SYSTEM),
                                    darkSurface = WidgetTheme.isDarkSurface(
                                        ThemeMode.SYSTEM.name,
                                        systemNight(appContext),
                                    ),
                                )
                            }
                        }
                    }
                } finally {
                    pending?.let { runCatching { it.finish() } }
                }
            }
        }

        /** خواندن تازه‌ی آمار و رسم همه‌ی ویجت‌ها. */
        suspend fun refreshNow(context: Context): Boolean {
            val appContext = context.applicationContext
            if (!hasWidgets(appContext)) return false
            val app = appContext as? MarbleDuApp ?: return false
            val settings = runCatching { app.settingsRepository.settings.first() }.getOrNull()
                ?: return false
            val persian = LocaleHelper.isPersian(settings.language)

            val snapshot = withContext(Dispatchers.IO) {
                val repository = app.appUsageRepository
                val hasAccess = runCatching { repository.hasUsageAccess() }.getOrDefault(false)
                val today = withTimeoutOrNull(STATS_TIMEOUT_MS) {
                    runCatching { repository.todayDeviceTraffic() }.getOrNull()
                }
                val capUsed = if (settings.dataCapBytes > 0L) {
                    withTimeoutOrNull(STATS_TIMEOUT_MS) {
                        runCatching { repository.monthMobileTotal(settings.statsEpochMillis) }.getOrNull()
                    } ?: 0L
                } else {
                    0L
                }
                val mobile = today?.first
                val wifi = today?.second
                buildSnapshot(
                    context = appContext,
                    settings = settings,
                    persian = persian,
                    mobileBytes = mobile?.total ?: 0L,
                    wifiBytes = wifi?.total ?: 0L,
                    rxBytes = (mobile?.rx ?: 0L) + (wifi?.rx ?: 0L),
                    txBytes = (mobile?.tx ?: 0L) + (wifi?.tx ?: 0L),
                    operatorName = operatorName(appContext, persian, settings.operatorOverride),
                    capUsedBytes = capUsed,
                    monitoring = settings.monitorInBackground,
                    hasUsageAccess = hasAccess,
                )
            }
            commit(appContext, snapshot)
            return true
        }

        /**
         * سرویس پایش ویجت را با آمار آماده صدا می‌زند. اگر داده از دفعه‌ی قبل
         * تغییری نکرده باشد و ویجت تازه باشد، رسم تکرار نمی‌شود.
         */
        fun push(context: Context, snapshot: WidgetSnapshot) {
            val appContext = context.applicationContext
            if (!hasWidgets(appContext)) return
            val previous = lastSnapshot
            val now = System.currentTimeMillis()
            val unchanged = previous != null &&
                previous.copy(updatedAtMillis = 0L) == snapshot.copy(updatedAtMillis = 0L)
            if (unchanged && now - lastRenderAt < IDLE_RENDER_MS) return
            commit(appContext, snapshot)
        }

        private fun commit(context: Context, snapshot: WidgetSnapshot) {
            lastSnapshot = snapshot
            lastRenderAt = System.currentTimeMillis()
            WidgetRenderer.renderAll(context, snapshot)
        }

        /** ساخت تصویر ویجت از تنظیمات و آمار؛ مشترک بین سرویس و خود ویجت. */
        fun buildSnapshot(
            context: Context,
            settings: AppSettings,
            persian: Boolean,
            mobileBytes: Long,
            wifiBytes: Long,
            rxBytes: Long,
            txBytes: Long,
            operatorName: String,
            capUsedBytes: Long,
            monitoring: Boolean,
            hasUsageAccess: Boolean,
            now: Long = System.currentTimeMillis(),
        ): WidgetSnapshot {
            val follow = settings.widgetFollowTheme
            val palette = ThemePalette.of(settings.palette)
            val network = NetworkStatsReader.currentNetwork(context, persian)
            val operator = if (network.type == ConnectionType.CELLULAR && settings.operatorOverride.isNotBlank()) {
                OperatorCatalog.resolveName(settings.operatorOverride)
            } else network.operator
            // ویجت هم‌رنگ اپراتور: هنگام اتصال، کارت همان گرادیان کارت صفحه‌ی خانه را می‌گیرد؛
            // بدون اتصال، سطح تم با رنگ‌های تأکیدی پالت برنامه.
            val branded = follow && network.type != ConnectionType.OFFLINE
            return WidgetSnapshot(
                persian = persian,
                mobileBytes = mobileBytes,
                wifiBytes = wifiBytes,
                rxBytes = rxBytes,
                txBytes = txBytes,
                operatorName = operatorName,
                capUsedBytes = capUsedBytes,
                capTotalBytes = settings.dataCapBytes,
                monitoring = monitoring,
                hasUsageAccess = hasUsageAccess,
                updatedAtMillis = now,
                showOperator = settings.widgetShowOperator,
                showCap = settings.widgetShowCap,
                detailed = settings.widgetDetailed,
                bigText = settings.widgetBigText,
                quickActions = settings.widgetQuickActions,
                followTheme = follow,
                accentPrimary = if (follow && !branded) palette.primary.toInt() else null,
                accentSecondary = if (follow && !branded) palette.tertiary.toInt() else null,
                darkSurface = WidgetTheme.isDarkSurface(settings.themeMode.name, systemNight(context)),
                amoled = WidgetTheme.isAmoled(settings.themeMode.name),
                brand = if (branded) OperatorBrand.gradient(operator) else null,
            )
        }

        /** نام اپراتور/شبکه‌ی فعال برای نمایش در ویجت. */
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

        /** حالت شبِ گوشی؛ برای تم «هم‌رنگ سیستم». */
        private fun systemNight(context: Context): Boolean =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }
}
