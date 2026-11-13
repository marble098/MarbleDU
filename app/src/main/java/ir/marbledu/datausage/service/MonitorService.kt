package ir.marbledu.datausage.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ir.marbledu.datausage.MainActivity
import ir.marbledu.datausage.MarbleDuApp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.ConnectionType
import ir.marbledu.datausage.data.IspResolver
import ir.marbledu.datausage.data.LocaleHelper
import ir.marbledu.datausage.data.NotificationStyle
import ir.marbledu.datausage.data.NetworkStatsReader
import ir.marbledu.datausage.data.NotificationLockVisibility
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.Traffic
import ir.marbledu.datausage.data.UsageLedger
import ir.marbledu.datausage.data.TimeMath
import ir.marbledu.datausage.ui.theme.ThemePalette
import ir.marbledu.datausage.widget.UsageWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.max

/**
 * سرویس پیش‌زمینه‌ی سبک: شمارنده‌های شبکه را می‌خواند، دفتر اپراتور را ثبت می‌کند
 * و اعلانِ قابل شخصی‌سازی و صددرصد بومی‌سازی‌شده را نمایش می‌دهد.
 */
class MonitorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    private var settingsJob: Job? = null
    private var ispJob: Job? = null
    private var loop: Job? = null

    @Volatile
    private var latestSettings: AppSettings = AppSettings()

    @Volatile
    private var lastChannelTag: String = ""

    @Volatile
    private var lastRx: Long = 0L

    @Volatile
    private var lastTx: Long = 0L

    @Volatile
    private var lastTime: Long = 0L

    @Volatile
    private var initialized: Boolean = false

    @Volatile
    private var peakSpeed: Double = 0.0

    private val speedHistory = ArrayDeque<Double>()

    @Volatile
    private var lastDown: Double = 0.0

    @Volatile
    private var lastUp: Double = 0.0

    @Volatile
    private var cachedTodayMobile: Traffic = Traffic()

    @Volatile
    private var cachedTodayWifi: Traffic = Traffic()

    @Volatile
    private var cachedMonthMobileBytes: Long = 0L

    @Volatile
    private var lastStatsFetchAt: Long = 0L

    @Volatile
    private var cachedTopApp: Pair<String, Long>? = null

    @Volatile
    private var lastTopAppFetchAt: Long = 0L

    @Volatile
    private var lastTopAppPersian: Boolean? = null

    @Volatile
    private var lastSpikeCheckAt: Long = 0L

    @Volatile
    private var lastDigestCheckAt: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val repo = (application as MarbleDuApp).settingsRepository
        when (intent?.action) {
            ACTION_STOP_MONITOR -> {
                scope.launch {
                    runCatching { repo.setMonitorBackground(false) }
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
            ACTION_RECHECK_ISP -> {
                IspResolver.ensureFresh(this, force = true)
            }
        }

        if (latestSettings == AppSettings()) {
            runCatching {
                latestSettings = runBlocking { repo.settings.first() }
            }
        }
        val initialTag = LocaleHelper.resolveLanguageTag(latestSettings.language)
        val initialCtx = LocaleHelper.localizedContext(this, initialTag)
        createChannel(initialCtx, initialTag)
        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                context = initialCtx,
                persian = initialTag == "fa",
                settings = latestSettings,
                down = lastDown,
                up = lastUp,
            ),
        )

        if (settingsJob == null) {
            settingsJob = scope.launch {
                repo.settings.collect { next ->
                    val prev = latestSettings
                    latestSettings = next
                    if (!next.monitorInBackground) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@collect
                    }
                    val newTag = LocaleHelper.resolveLanguageTag(next.language)
                    val localized = LocaleHelper.localizedContext(this@MonitorService, newTag)
                    if (newTag != lastChannelTag) {
                        createChannel(localized, newTag)
                    }
                    if (prev != next) {
                        refreshStatsIfNeeded(next, newTag == "fa", forceStats = false)
                        postCurrentNotification()
                        if (prev.monitorIntervalSeconds != next.monitorIntervalSeconds) {
                            restartLoop()
                        }
                    }
                }
            }
        }

        if (ispJob == null) {
            ispJob = scope.launch {
                var first = true
                IspResolver.updates.collect {
                    if (first) {
                        first = false
                        return@collect
                    }
                    postCurrentNotification()
                }
            }
        }

        if (loop == null || loop?.isActive != true) {
            restartLoop()
        }
        return START_STICKY
    }

    private fun restartLoop() {
        loop?.cancel()
        loop = scope.launch {
            IspResolver.enabled = true
            while (isActive) {
                val settings = latestSettings
                val intervalSeconds = settings.monitorIntervalSeconds.coerceIn(1, 60)
                val counters = NetworkStatsReader.byteCounters()
                val rx = counters.totalRxBytes
                val tx = counters.totalTxBytes
                val now = System.currentTimeMillis()
                if (!initialized) {
                    lastRx = rx
                    lastTx = tx
                    lastTime = now
                    initialized = true
                }
                val elapsed = (now - lastTime).coerceAtLeast(1)
                val dRx = max(0L, rx - lastRx)
                val dTx = max(0L, tx - lastTx)
                lastRx = rx
                lastTx = tx
                lastTime = now

                val down = dRx * 1000.0 / elapsed
                val up = dTx * 1000.0 / elapsed
                lastDown = down
                lastUp = up
                synchronized(speedHistory) {
                    speedHistory.addLast(down + up)
                    if (speedHistory.size > 30) speedHistory.removeFirst()
                }
                peakSpeed = max(peakSpeed, down + up)

                val persian = LocaleHelper.isPersian(settings.language)
                IspResolver.ensureFresh(this@MonitorService)
                val snapshot = NetworkStatsReader.currentNetwork(this@MonitorService, persian)
                UsageLedger.sample(this@MonitorService, counters, snapshot, now)

                refreshStatsIfNeeded(settings, persian, forceStats = false)
                postCurrentNotification(down = down, up = up)
                if (lastStatsFetchAt > 0L) {
                    runCatching {
                        UsageAlertNotifier.check(
                            context = this@MonitorService,
                            settings = settings,
                            persian = persian,
                            todayMobileBytes = cachedTodayMobile.total,
                            capUsedBytes = cachedMonthMobileBytes,
                        )
                        checkSpikeAlert(settings, persian)
                        checkWeeklyDigest(settings, persian)
                        val palette = ThemePalette.of(settings.palette)
                        val widgetPrimary = palette.primary.toInt()
                        val widgetSecondary = palette.tertiary.toInt()
                        UsageWidgetProvider.push(
                            this@MonitorService,
                            UsageWidgetProvider.Snapshot(
                                persian = persian,
                                mobileBytes = cachedTodayMobile.total,
                                wifiBytes = cachedTodayWifi.total,
                                operatorName = UsageWidgetProvider.operatorName(
                                    this@MonitorService, persian, settings.operatorOverride,
                                ),
                                capUsedBytes = cachedMonthMobileBytes,
                                capTotalBytes = settings.dataCapBytes,
                                showOperator = settings.widgetShowOperator,
                                showCap = settings.widgetShowCap,
                                bigText = settings.widgetBigText,
                                detailed = settings.widgetDetailed,
                                rxBytes = cachedTodayMobile.rx + cachedTodayWifi.rx,
                                txBytes = cachedTodayMobile.tx + cachedTodayWifi.tx,
                                themePrimary = if (settings.widgetFollowTheme) widgetPrimary else null,
                                themeSecondary = if (settings.widgetFollowTheme) widgetSecondary else null,
                                quickActions = settings.widgetQuickActions,
                                monitoring = settings.monitorInBackground,
                            ),
                        )
                    }
                }
                delay(intervalSeconds * 1000L)
            }
        }
    }

    /**
     * هشدار مصرف غیرعادی: اگر مصرف همین ساعت خیلی بیشتر از میانگین همان ساعت
     * در ۷ روز گذشته (بر اساس دفتر ثبت اپراتور) باشد، یک اعلان می‌دهد.
     * حداکثر هر ۵ دقیقه یک‌بار بررسی می‌شود تا هزینه‌ی محاسباتی نداشته باشد.
     */
    private fun checkSpikeAlert(settings: AppSettings, persian: Boolean) {
        if (!settings.spikeAlertEnabled) return
        val now = System.currentTimeMillis()
        if (now - lastSpikeCheckAt < 5 * 60 * 1000L) return
        lastSpikeCheckAt = now
        val hourStart = TimeMath.dayStart(now).let { dayStart ->
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = now
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }
        val currentHour = java.util.Calendar.getInstance().apply { timeInMillis = now }
            .get(java.util.Calendar.HOUR_OF_DAY)
        val hourKey = (TimeMath.dayStart(now) / 86_400_000L).toString() + "-" + currentHour
        var hourBytes = 0L
        var sameHourAverage = 0L
        val dayMillis = 86_400_000L
        var samples = 0
        val todayStart = TimeMath.dayStart(now)
        for (daysAgo in 0..7) {
            val day = todayStart - daysAgo * dayMillis
            UsageLedger.hourlyForDay(this, day).filter { it.hour == currentHour }.forEach {
                hourBytes += if (daysAgo == 0) it.total else 0L
                if (daysAgo > 0) sameHourAverage += it.total
            }
            if (daysAgo > 0) samples++
        }
        if (samples > 0) sameHourAverage /= samples
        // مصرف همین ساعت را حداقل از ساعت اخیر می‌خواهیم دقیق داشته باشیم.
        if (hourBytes <= 0L && now - hourStart >= 0) {
            // دفتر در چند ثانیه‌ی اخیر هنوز نمونه نگرفته؛ بررسی بعدی جبران می‌کند.
        }
        if (hourBytes <= 0L) return
        UsageSpikeNotifier.checkAndAlert(
            context = this,
            persian = persian,
            hourBytes = hourBytes,
            averageSameHourBytes = sameHourAverage,
            hourKey = hourKey,
        )
    }

    /**
     * خلاصه‌ی هفتگی: هر ساعت یک‌بار بررسی می‌کند که آیا یک هفته از آخرین خلاصه
     * گذشته است یا نه؛ در صورت گذشته‌بودن، اعلان جمع‌بندی هفته ارسال می‌شود.
     */
    private suspend fun checkWeeklyDigest(settings: AppSettings, persian: Boolean) {
        if (!settings.weeklyDigestEnabled) return
        val now = System.currentTimeMillis()
        if (now - lastDigestCheckAt < 60 * 60 * 1000L) return
        lastDigestCheckAt = now
        runCatching {
            WeeklyDigestNotifier.maybePost(this, persian, settings.statsEpochMillis)
        }
    }

    private suspend fun refreshStatsIfNeeded(
        settings: AppSettings,
        persian: Boolean,
        forceStats: Boolean,
    ) {
        val now = System.currentTimeMillis()
        val appRepo = (application as MarbleDuApp).appUsageRepository
        val notif = settings.notification
        val needsUsageStats = notif.showTodayTotal || notif.showTodayMobileWifi ||
            notif.showDataCap || notif.progressMode != ir.marbledu.datausage.data.NotificationProgressMode.NONE ||
            (settings.usageAlerts && (settings.dailyLimitMb > 0 || settings.dataCapBytes > 0L)) ||
            UsageWidgetProvider.hasWidgets(this)
        if (needsUsageStats && (forceStats || now - lastStatsFetchAt >= STATS_REFRESH_MS)) {
            runCatching {
                val (m, w) = appRepo.todayDeviceTraffic()
                cachedTodayMobile = m
                cachedTodayWifi = w
                if (settings.dataCapBytes > 0L) {
                    cachedMonthMobileBytes = appRepo.monthMobileTotal(settings.statsEpochMillis)
                }
                lastStatsFetchAt = now
            }
        }
        if (notif.showTopApp &&
            (forceStats || lastTopAppPersian != persian || now - lastTopAppFetchAt >= TOP_APP_REFRESH_MS)
        ) {
            runCatching {
                cachedTopApp = appRepo.topAppToday(persian)
                lastTopAppFetchAt = now
                lastTopAppPersian = persian
            }
        }
    }

    private fun postCurrentNotification(
        down: Double = lastDown,
        up: Double = lastUp,
    ) {
        val settings = latestSettings
        val tag = LocaleHelper.resolveLanguageTag(settings.language)
        val localized = LocaleHelper.localizedContext(this, tag)
        val persian = tag == "fa"
        notificationManager().notify(
            NOTIFICATION_ID,
            buildNotification(
                context = localized,
                persian = persian,
                settings = settings,
                down = down,
                up = up,
            ),
        )
    }

    private fun buildNotification(
        context: Context,
        persian: Boolean,
        settings: AppSettings,
        down: Double,
        up: Double,
    ): Notification {
        val snapshot = NetworkStatsReader.currentNetwork(context, persian)
        val override = settings.operatorOverride
        val operatorName = when {
            snapshot.type == ConnectionType.CELLULAR && override.isNotBlank() ->
                OperatorCatalog.resolveName(override).name(persian)
            snapshot.operator.id == OperatorCatalog.ID_UNKNOWN -> snapshot.carrierName
            else -> snapshot.operator.name(persian)
        }
        val avgSpeed = synchronized(speedHistory) {
            if (speedHistory.isEmpty()) down + up else speedHistory.average()
        }
        val connectionSession = UsageLedger.activeSession(this)
        val input = NotificationInput(
            downBytesPerSec = down,
            upBytesPerSec = up,
            averageSpeedBps = avgSpeed,
            peakSpeedBps = peakSpeed,
            operatorName = operatorName,
            connectionType = snapshot.type,
            wifiSourceKind = snapshot.wifiSourceKind,
            wifiStatus = snapshot.wifiLookupStatus,
            mobileTech = snapshot.mobileTech,
            vpnActive = snapshot.vpnActive,
            ipAddress = snapshot.ipAddress,
            publicIp = snapshot.publicIp,
            todayMobileBytes = cachedTodayMobile.total,
            todayWifiBytes = cachedTodayWifi.total,
            sessionRxBytes = connectionSession?.rxBytes ?: 0L,
            sessionTxBytes = connectionSession?.txBytes ?: 0L,
            sessionStartMillis = connectionSession?.startMillis ?: 0L,
            capTotalBytes = settings.dataCapBytes,
            capUsedBytes = cachedMonthMobileBytes,
            topAppName = cachedTopApp?.first.orEmpty(),
            topAppBytes = cachedTopApp?.second ?: 0L,
        )

        val config = settings.notification
        val content = NotificationFormatter.format(
            input = input,
            config = config,
            persian = persian,
            appMonitoringTitle = context.getString(R.string.notif_title),
        )

        val openMain = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val visibility = when (config.lockVisibility) {
            NotificationLockVisibility.PUBLIC -> NotificationCompat.VISIBILITY_PUBLIC
            NotificationLockVisibility.PRIVATE -> NotificationCompat.VISIBILITY_PRIVATE
            NotificationLockVisibility.HIDDEN -> NotificationCompat.VISIBILITY_SECRET
        }

        val palette = ThemePalette.of(settings.palette)
        val accent = MonitoringNotification.accentColor(palette)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(content.summaryText)
            .setContentIntent(openMain)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setVisibility(visibility)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .setColorized(true)
            .setColor(accent)

        // آیکون بزرگِ رنگی + رنگ لهجه؛ اعلان ظاهر تازه و هم‌رنگ اپ می‌گیرد.
        runCatching {
            builder.setLargeIcon(
                Icon.createWithBitmap(MonitoringNotification.largeIcon(this, accent)),
            )
        }

        if (!content.subText.isNullOrBlank()) {
            builder.setSubText(content.subText)
        }
        val structured = if (config.style == NotificationStyle.EXPANDED &&
            content.expandedLines.isNotEmpty()
        ) {
            MonitoringNotification.expandedBody(input, config, content, persian)
        } else {
            content.expandedText
        }
        if (structured.isNotBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(structured))
        }
        if (content.progressPercent != null) {
            builder.setProgress(100, content.progressPercent, false)
        }

        if (config.actionRecheckIsp) {
            val recheckPending = PendingIntent.getService(
                this,
                1,
                Intent(this, MonitorService::class.java).setAction(ACTION_RECHECK_ISP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(R.drawable.ic_notif_action_check, context.getString(R.string.notif_action_recheck), recheckPending)
        }
        if (config.actionOpenReports) {
            val reportsPending = PendingIntent.getActivity(
                this,
                2,
                Intent(this, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_START_TAB, MainActivity.TAB_REPORTS)
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(R.drawable.ic_notif_action_reports, context.getString(R.string.notif_action_reports), reportsPending)
        }
        if (config.actionStop) {
            val stopPending = PendingIntent.getService(
                this,
                3,
                Intent(this, MonitorService::class.java).setAction(ACTION_STOP_MONITOR),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(R.drawable.ic_notif_action_stop, context.getString(R.string.notif_action_stop), stopPending)
        }

        return builder.build()
    }

    private fun createChannel(localized: Context, tag: String) {
        val manager = notificationManager()
        val channel = NotificationChannel(
            CHANNEL_ID,
            localized.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = localized.getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
        lastChannelTag = tag
    }

    private fun notificationManager(): NotificationManager =
        getSystemService(NotificationManager::class.java)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "marbledu_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val STATS_REFRESH_MS = 15_000L
        private const val TOP_APP_REFRESH_MS = 45_000L
        private const val ACTION_RECHECK_ISP = "ir.marbledu.datausage.action.RECHECK_ISP"
        private const val ACTION_STOP_MONITOR = "ir.marbledu.datausage.action.STOP_MONITOR"

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            runCatching { context.startForegroundService(intent) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }
}
