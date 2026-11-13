package ir.marbledu.datausage.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.marbledu.datausage.MarbleDuApp
import ir.marbledu.datausage.data.AppLanguage
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.AppUsage
import ir.marbledu.datausage.data.ConnectionType
import ir.marbledu.datausage.data.DayReport
import ir.marbledu.datausage.data.DaySummary
import ir.marbledu.datausage.data.IspResolver
import ir.marbledu.datausage.data.LiveMonitor
import ir.marbledu.datausage.data.LiveTick
import ir.marbledu.datausage.data.LocaleHelper
import ir.marbledu.datausage.data.NetworkStatsReader
import ir.marbledu.datausage.data.NotificationConfig
import ir.marbledu.datausage.data.NotificationPreset
import ir.marbledu.datausage.data.PingResult
import ir.marbledu.datausage.data.ThemeMode
import ir.marbledu.datausage.data.TimeMath
import ir.marbledu.datausage.data.HourOperatorTraffic
import ir.marbledu.datausage.data.SimIdentity
import ir.marbledu.datausage.data.UiMode
import ir.marbledu.datausage.data.AppTextSize
import ir.marbledu.datausage.data.OperatorDayStat
import ir.marbledu.datausage.data.OperatorRangeRow
import ir.marbledu.datausage.data.UsageInsights
import ir.marbledu.datausage.data.UsageLedger
import ir.marbledu.datausage.data.UsagePlanner
import ir.marbledu.datausage.data.TimeRange
import ir.marbledu.datausage.data.Traffic
import ir.marbledu.datausage.widget.UsageWidgetProvider
import ir.marbledu.datausage.service.MonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUsageUi(
    val range: TimeRange = TimeRange.TODAY,
    val mobile: List<AppUsage> = emptyList(),
    val wifi: List<AppUsage> = emptyList(),
    val loading: Boolean = false,
)

data class DashboardUi(
    val todayMobileBytes: Long = 0,
    val todayWifiBytes: Long = 0,
    val monthMobileBytes: Long = 0,
    val monthWifiBytes: Long = 0,
    val todayVariationPercent: Int = 0,
    val week: List<ir.marbledu.datausage.data.DayUsage> = emptyList(),
    val hasUsageAccess: Boolean = false,
    val operatorName: String = "",
    val averagePingMs: Double = -1.0,
    val averageSpeed: Double = 0.0,
    val todayMobile: Traffic = Traffic(),
    val todayWifi: Traffic = Traffic(),
)

/** یک نقطه از نمودار سرعت زنده. */
data class SpeedPoint(val down: Double, val up: Double)

/** فهرست ۳۰ روز اخیر (قدیمی‌ترین اول). */
data class HistoryUi(
    val days: List<DaySummary> = emptyList(),
    val loading: Boolean = false,
    val loadedAt: Long = 0L,
)

/** گزارش کامل روز انتخاب‌شده. */
data class DayReportUi(
    val dayStart: Long = 0L,
    val report: DayReport? = null,
    val loading: Boolean = false,
)

/** صفحه‌ی کامل جزئیات یک اپراتور در ۳۰ روز اخیر. */
data class OperatorDetailUi(
    val operatorId: String = "",
    val sim: SimIdentity? = null,
    val loading: Boolean = false,
    val days: List<OperatorDayStat> = emptyList(),
    /** هیستوگرام ساعتی جمع کل ۳۰ روز. */
    val hours: List<HourOperatorTraffic> = emptyList(),
    /** روز انتخاب‌شده برای نمودار ۲۴ ساعته (۰ یعنی هیچکدام). */
    val selectedDayStart: Long = 0L,
    val selectedDayHours: List<HourOperatorTraffic> = emptyList(),
    val selectedDayLoading: Boolean = false,
)

/** کارت «اپراتورها»: مصرف هر اپراتور در بازه‌ی دلخواه. */
data class OperatorRangeUi(
    /** ۰ = امروز، ۱ = ۷ روز اخیر، ۲ = انتخاب تاریخ. */
    val mode: Int = 0,
    val from: Long = 0L,
    val to: Long = 0L,
    val rows: List<OperatorRangeRow> = emptyList(),
    val loading: Boolean = false,
    val loadedOnce: Boolean = false,
)

/**
 * «بینش مصرف»: تحلیل الگوی ۳۰ روز اخیر برای صفحه‌ی خانه.
 * کلاس خالص [UsageInsights] محاسبات را انجام می‌دهد و اینجا فقط داده جمع می‌شود.
 */
data class InsightsUi(
    val loading: Boolean = false,
    val loadedAt: Long = 0L,
    val snapshot: ir.marbledu.datausage.data.UsageInsights.Snapshot? = null,
    val verdict: ir.marbledu.datausage.data.UsageInsights.DayVerdict =
        ir.marbledu.datausage.data.UsageInsights.DayVerdict.NORMAL,
    val todayPercentOfAverage: Int = 0,
    val budget: ir.marbledu.datausage.data.UsageInsights.Budget? = null,
    val daysUntilCap: Int = -1,
)

private const val HISTORY_CACHE_MS = 2L * 60 * 1000

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as MarbleDuApp).appUsageRepository
    private val monitor: LiveMonitor = (app as MarbleDuApp).liveMonitor
    private val settingsRepo = (app as MarbleDuApp).settingsRepository

    /** مجوزهای رن‌تایم که کاربر اجازه داده است (برای نمایش وضعیت در تنظیمات و راهنمایی). */
    val permissions: StateFlow<Set<String>> = MarbleDuApp.instance.permissionStateFlow

    fun hasPermission(permission: String): Boolean =
        MarbleDuApp.instance.permissionState.value.contains(permission)

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _tick = MutableStateFlow<LiveTick?>(null)
    val tick: StateFlow<LiveTick?> = _tick.asStateFlow()

    private val _dashboard = MutableStateFlow(DashboardUi())
    val dashboard: StateFlow<DashboardUi> = _dashboard.asStateFlow()

    private val _usage = MutableStateFlow(AppUsageUi())
    val usage: StateFlow<AppUsageUi> = _usage.asStateFlow()

    private val _ping = MutableStateFlow<List<PingResult>>(emptyList())
    val ping: StateFlow<List<PingResult>> = _ping.asStateFlow()

    private val _pingRunning = MutableStateFlow(false)
    val pingRunning: StateFlow<Boolean> = _pingRunning.asStateFlow()

    private val _speedHistory = MutableStateFlow<List<SpeedPoint>>(emptyList())
    val speedHistory: StateFlow<List<SpeedPoint>> = _speedHistory.asStateFlow()

    private val _history = MutableStateFlow(HistoryUi())
    val history: StateFlow<HistoryUi> = _history.asStateFlow()

    private val _dayReport = MutableStateFlow(DayReportUi())
    val dayReport: StateFlow<DayReportUi> = _dayReport.asStateFlow()


    private val _operatorDetail = MutableStateFlow(OperatorDetailUi())
    val operatorDetail: StateFlow<OperatorDetailUi> = _operatorDetail.asStateFlow()

    private val _insights = MutableStateFlow(InsightsUi())
    val insights: StateFlow<InsightsUi> = _insights.asStateFlow()

    private var insightsJob: Job? = null

    private val _operatorRange = MutableStateFlow(OperatorRangeUi())
    val operatorRange: StateFlow<OperatorRangeUi> = _operatorRange.asStateFlow()

    private var historyJob: Job? = null
    private var dayJob: Job? = null
    private var operatorDetailJob: Job? = null
    private var operatorRangeJob: Job? = null
    private var liveJob: Job? = null
    private var lastRefreshAllAt = 0L
    private var pingJob: Job? = null

    init {
        // دسترسی را سبک و فوری بخوان تا راه‌اندازی، مجوزِ از قبل فعال را اشتباهاً درخواست نکند.
        _dashboard.value = _dashboard.value.copy(hasUsageAccess = repository.hasUsageAccess())
        IspResolver.enabled = true
        // با هر تغییر نتیجه‌ی شناسایی وای‌فای یا تغییر زبان، کارت اتصال بلافاصله تازه می‌شود.
        viewModelScope.launch(Dispatchers.IO) {
            IspResolver.updates.collect {
                refreshLiveNetworkSnapshot()
            }
        }
        viewModelScope.launch {
            var lastLanguage: AppLanguage? = null
            settings.map { it.language }.distinctUntilChanged().collect { language ->
                if (lastLanguage != null && lastLanguage != language) {
                    refreshLiveNetworkSnapshot()
                    refreshDashboard()
                }
                lastLanguage = language
            }
        }
        // با تغییر شروعِ آمارگیری (ریست یا انتخاب روز)، داشبورد و تاریخچه خودبه‌خود تازه می‌شوند.
        viewModelScope.launch {
            var seen = false
            settings.map { it.statsEpochMillis }.distinctUntilChanged().collect {
                if (!seen) {
                    seen = true
                    return@collect
                }
                refreshDashboard()
                if (_history.value.loadedAt > 0L) loadHistory(force = true)
                if (_operatorRange.value.loadedOnce) {
                    loadOperatorRange(_operatorRange.value.mode)
                }
            }
        }
    }

    private fun refreshLiveNetworkSnapshot() {
        val current = _tick.value ?: return
        val persian = LocaleHelper.isPersian(settings.value.language)
        val snapshot = NetworkStatsReader.currentNetwork(getApplication(), persian)
        _tick.value = current.copy(network = snapshot)
    }

    /**
     * یک نقطه‌ی واحد برای تازه کردن همه‌چیز. از ON_RESUME صدا زده می‌شود تا
     * بعد از برگشتن از «دسترسی به داده‌های مصرف» همان لحظه داده‌ها ظاهر شوند.
     */
    fun refreshAll() {
        val now = SystemClock.elapsedRealtime()
        // LaunchedEffect و ON_RESUME می‌توانند هم‌زمان در شروع رابط اجرا شوند.
        if (lastRefreshAllAt != 0L && now - lastRefreshAllAt < 750L) return
        lastRefreshAllAt = now
        refreshDashboard()
        loadInsights()
        loadUsage(_usage.value.range)
        // تاریخچه‌ی ۳۰روزه فقط بعد از باز شدن تب گزارش خوانده می‌شود؛ بار اول
        // ورود به خانه نباید ده‌ها query تاریخی را هم‌زمان اجرا کند.
        if (_history.value.loadedAt > 0L) loadHistory()
    }

    /** مجوزهای اعطاشده را از روی دستگاه می‌خواند (به‌جای اتکا به callback نصب). */
    fun refreshPermissions() {
        val app = getApplication<Application>()
        val granted = mutableSetOf<String>()
        if (app.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) granted += android.Manifest.permission.POST_NOTIFICATIONS
        if (app.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) granted += android.Manifest.permission.READ_PHONE_STATE
        MarbleDuApp.instance.permissionState.value = granted
    }

    // ------------------------------------------------------------- 30 days
    /** فهرست ۳۰ روز اخیر؛ فقط در اولین ورود یا پس از منقضی شدن cache دوباره خوانده می‌شود. */
    fun loadHistory(force: Boolean = false) {
        if (historyJob?.isActive == true) return
        if (!force && _history.value.loadedAt > 0L &&
            System.currentTimeMillis() - _history.value.loadedAt < HISTORY_CACHE_MS
        ) return
        historyJob = viewModelScope.launch(Dispatchers.IO) {
            _history.value = _history.value.copy(loading = true)
            val days = repository.daySummaries(30, settings.value.statsEpochMillis)
            _history.value = HistoryUi(days = days, loading = false, loadedAt = System.currentTimeMillis())
            val selected = _dayReport.value.dayStart
            val target = days.firstOrNull { it.startMillis == selected } ?: days.lastOrNull()
            if (target != null) selectDay(target.startMillis)
        }
    }

    fun selectDay(dayStart: Long) {
        val summary = _history.value.days.firstOrNull { it.startMillis == dayStart } ?: return
        dayJob?.cancel()
        dayJob = viewModelScope.launch {
            val keep = _dayReport.value.report?.takeIf { _dayReport.value.dayStart == dayStart }
            _dayReport.value = DayReportUi(dayStart = dayStart, report = keep, loading = true)
            val report = repository.dayReport(summary, settings.value.statsEpochMillis)
            _dayReport.value = DayReportUi(dayStart = dayStart, report = report, loading = false)
        }
    }

    /**
     * تحلیل الگوی مصرف ۳۰ روز اخیر: سنگین‌ترین روز هفته، ساعت اوج، سهم موبایل
     * و این‌که امروز نسبت به یک روز عادی چقدر است. نتیجه برای کارت «بینش» در
     * خانه استفاده می‌شود و با هر تازه‌سازی داشبورد فقط اگر لازم باشد دوباره
     * محاسبه می‌شود (کش ۵ دقیقه‌ای).
     */
    fun loadInsights(force: Boolean = false) {
        if (!repository.hasUsageAccess()) return
        if (insightsJob?.isActive == true) return
        val loaded = _insights.value.loadedAt
        if (!force && loaded > 0L && System.currentTimeMillis() - loaded < 5 * 60 * 1000L) return
        insightsJob = viewModelScope.launch(Dispatchers.IO) {
            _insights.value = _insights.value.copy(loading = true)
            val epoch = settings.value.statsEpochMillis
            val summaries = repository.daySummaries(30, epoch)
            val days = summaries.map {
                UsageInsights.Day(it.startMillis, it.mobile.total, it.wifi.total)
            }
            val hours = runCatching {
                repository.hourlyTotals(
                    fromMillis = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000,
                    toMillis = System.currentTimeMillis(),
                    type = ConnectionType.CELLULAR,
                    maxBuckets = 24 * 14,
                ).let { list ->
                    val totals = LongArray(24)
                    list.forEach { totals[it.hour.coerceIn(0, 23)] += it.bytes }
                    totals.toList()
                }
            }.getOrDefault(emptyList())

            val snapshot = UsageInsights.build(days, hours)
            val now = System.currentTimeMillis()
            val todayStart = TimeMath.dayStart(now)
            val today = summaries.firstOrNull { it.startMillis == todayStart }
            val todayBytes = (today?.mobile?.total ?: 0L) + (today?.wifi?.total ?: 0L)
            val elapsedFraction = (((now - todayStart).toFloat()) / (24f * 60 * 60 * 1000f))
                .coerceIn(0.05f, 1f)
            val verdict = UsageInsights.classify(todayBytes, snapshot.averageDailyBytes, elapsedFraction)
            val cap = settings.value.dataCapBytes
            val usedMobile = summaries.sumOf { it.mobile.total }
            val forecast = if (cap > 0L) {
                UsagePlanner.forecast(
                    capBytes = cap,
                    usedBytes = usedMobile,
                    recentDailyBytes = summaries.takeLast(7).map { it.mobile.total },
                    nowMillis = now,
                    billingDay = settings.value.billingCycleDay,
                )
            } else null
            _insights.value = InsightsUi(
                loading = false,
                loadedAt = now,
                snapshot = snapshot,
                verdict = verdict,
                todayPercentOfAverage = UsageInsights.todayPercentOfAverage(
                    todayBytes,
                    snapshot.averageDailyBytes,
                ),
                budget = forecast?.let { UsageInsights.budget(todayBytes, it.dailyBudgetBytes) },
                daysUntilCap = forecast?.daysUntilUnlimited()
                    ?: UsageInsights.daysUntilCap(
                        (cap - usedMobile).coerceAtLeast(0L),
                        snapshot.averageMobileBytes,
                    ),
            )
        }
    }

    /** میان‌بر محاسبه‌ی روزهای مانده تا پر شدن سقف از پیش‌بینی موجود. */
    private fun UsagePlanner.Forecast.daysUntilUnlimited(): Int =
        if (remainingBytes <= 0L) 0 else daysUntilEmpty

    /** دوباره پیدا کردنِ اپراتور وای‌فای (دکمه‌ی «بررسی دوباره»). */
    fun refreshWifiOperator() {
        IspResolver.ensureFresh(getApplication(), force = true)
    }

    // ----------------------------------------------------------------- live
    fun startLive() {
        if (liveJob?.isActive == true) return
        liveJob = viewModelScope.launch {
            monitor.stream(
                intervalMillis = 1000,
                persianProvider = { LocaleHelper.isPersian(settings.value.language) },
                recordUsageProvider = { !settings.value.monitorInBackground },
            ).collect { value ->
                _tick.value = value
                _speedHistory.value = (_speedHistory.value + SpeedPoint(value.downBytesPerSecond, value.upBytesPerSecond))
                    .takeLast(60)
            }
        }
    }

    fun stopLive() {
        liveJob?.cancel()
        liveJob = null
    }

    fun runPingTest() {
        if (pingJob?.isActive == true) return
        pingJob = viewModelScope.launch {
            _pingRunning.value = true
            try {
                _ping.value = emptyList()
                val results = mutableListOf<PingResult>()
                monitor.ping().collect { result ->
                    results.add(result)
                    _ping.value = results.toList()
                }
                // «میانگین پینگ» خانه = مسیری که کاربر واقعاً از آن استفاده می‌کند.
                val defaultRoute = results.firstOrNull()?.route
                val avg = results.filter { it.route == defaultRoute && it.ok }.map { it.averageMs }
                _dashboard.value = _dashboard.value.copy(
                    averagePingMs = if (avg.isEmpty()) -1.0 else avg.average(),
                )
            } finally {
                _pingRunning.value = false
            }
        }
    }

    // ------------------------------------------------------------- dashboard
    fun refreshDashboard() {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val dayStart = TimeMath.dayStart(now)
            val monthStart = now - TimeRange.MONTH.millis()

            // آمار دستگاه (source of truth) را مستقیم می‌خوانیم؛ جمع UIDهای اپ‌ها
            // برای داشبورد کندتر و در بعضی نسخه‌های اندروید ناقص است.
            val epoch = settings.value.statsEpochMillis
            val todayMobile = repository.aggregateTraffic(dayStart, now, ConnectionType.CELLULAR, epoch)
            val todayWifi = repository.aggregateTraffic(dayStart, now, ConnectionType.WIFI, epoch)
            val monthMobile = repository.aggregateTraffic(monthStart, now, ConnectionType.CELLULAR, epoch)
            val monthWifi = repository.aggregateTraffic(monthStart, now, ConnectionType.WIFI, epoch)
            val week = repository.dailyTotals(7, ConnectionType.CELLULAR, epoch)

            val persian = LocaleHelper.isPersian(settings.value.language)
            val snapshot = NetworkStatsReader.currentNetwork(getApplication(), persian)
            val yesterdayTotal = week.dropLast(1).lastOrNull()?.bytes ?: 0L
            val variation = if (yesterdayTotal <= 0L) 0 else
                (((todayMobile.total - yesterdayTotal).toDouble() / yesterdayTotal) * 100).toInt()

            _dashboard.value = DashboardUi(
                todayMobileBytes = todayMobile.total,
                todayWifiBytes = todayWifi.total,
                monthMobileBytes = monthMobile.total,
                monthWifiBytes = monthWifi.total,
                todayVariationPercent = variation,
                week = week,
                hasUsageAccess = repository.hasUsageAccess(),
                operatorName = snapshot.carrierName,
                averagePingMs = _dashboard.value.averagePingMs,
                averageSpeed = _tick.value?.averageSpeed ?: 0.0,
                todayMobile = todayMobile,
                todayWifi = todayWifi,
            )
        }
    }

    /** تغییر بازه فقط یک بار لود می‌کند. */
    fun setRange(range: TimeRange) = loadUsage(range)

    fun loadUsage(range: TimeRange = _usage.value.range) {
        viewModelScope.launch {
            _usage.value = _usage.value.copy(range = range, loading = true)
            val now = System.currentTimeMillis()
            val from = TimeMath.rangeStart(range, now, settings.value.billingCycleDay)
            val (mobile, wifi) = repository.detailedBreakdown(from, now, settings.value.statsEpochMillis)
            _usage.value = AppUsageUi(
                range = range,
                mobile = mobile,
                wifi = wifi,
                loading = false,
            )
        }
    }

    // -------------------------------------------------------------- settings
    fun setLanguage(language: AppLanguage) = viewModelScope.launch { settingsRepo.setLanguage(language) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settingsRepo.setTheme(mode) }
    fun setPalette(name: String, dynamic: Boolean) = viewModelScope.launch { settingsRepo.setPalette(name, dynamic) }
    fun setDataCap(bytes: Long) = viewModelScope.launch {
        settingsRepo.setDataCap(bytes)
        refreshWidget()
    }
    fun setBillingDay(day: Int) = viewModelScope.launch { settingsRepo.setBillingCycleDay(day.coerceIn(1, 28)) }
    fun setUsageAlerts(on: Boolean) = viewModelScope.launch { settingsRepo.setUsageAlerts(on) }
    fun setDailyLimitMb(mb: Int) = viewModelScope.launch { settingsRepo.setDailyLimitMb(mb) }
    fun setMonitorInterval(seconds: Int) = viewModelScope.launch {
        settingsRepo.setMonitorInterval(seconds.coerceIn(1, 60))
    }
    fun setOperator(name: String) = viewModelScope.launch { settingsRepo.setOperator(name) }

    /** حالت نمایشی اپ (ساده / پیشرفته). */
    fun setUiMode(mode: UiMode) = viewModelScope.launch { settingsRepo.setUiMode(mode) }

    /** ویجت همیشه بعد از تغییر تنظیمات (و ذخیره‌ی مقدار) بلافاصله دوباره رسم می‌شود. */
    private suspend fun refreshWidget() {
        runCatching { UsageWidgetProvider.refresh(getApplication()) }
    }

    fun setWidgetOperator(value: Boolean) = viewModelScope.launch {
        settingsRepo.setWidgetOperator(value)
        refreshWidget()
    }
    fun setWidgetCap(value: Boolean) = viewModelScope.launch {
        settingsRepo.setWidgetCap(value)
        refreshWidget()
    }
    fun setWidgetDetailed(value: Boolean) = viewModelScope.launch {
        settingsRepo.setWidgetDetailed(value)
        refreshWidget()
    }
    fun setWidgetBigText(value: Boolean) = viewModelScope.launch {
        settingsRepo.setWidgetBigText(value)
        refreshWidget()
    }

    fun setWidgetFollowTheme(value: Boolean) = viewModelScope.launch {
        settingsRepo.setWidgetFollowTheme(value)
        refreshWidget()
    }
    fun setWidgetQuickActions(value: Boolean) = viewModelScope.launch {
        settingsRepo.setWidgetQuickActions(value)
        refreshWidget()
    }
    fun setSessionsCard(value: Boolean) = viewModelScope.launch { settingsRepo.setSessionsCard(value) }
    fun setSessionsTabs(value: Boolean) = viewModelScope.launch { settingsRepo.setSessionsTabs(value) }
    fun setSessionsVisible(value: Int) = viewModelScope.launch { settingsRepo.setSessionsVisible(value) }

    /** عینک مصرف (حباب شناور): با روشن شدن، سرویس اجرا و با خاموش شدن متوقف می‌شود. */
    fun setSpeedOverlay(value: Boolean) = viewModelScope.launch {
        settingsRepo.setSpeedOverlay(value)
        val app = getApplication<Application>()
        if (value) {
            if (ir.marbledu.datausage.util.UsageAccess.canDrawOverlays(app)) {
                ir.marbledu.datausage.service.SpeedOverlayService.start(app)
            }
        } else {
            ir.marbledu.datausage.service.SpeedOverlayService.stop(app)
        }
    }

    fun setSpeedOverlayChart(value: Boolean) = viewModelScope.launch { settingsRepo.setSpeedOverlayChart(value) }

    /** بازیابی تنظیمات از فایل پشتیبان و تازه‌سازی فوری ویجت. */
    fun applyBackup(restored: AppSettings) = viewModelScope.launch {
        settingsRepo.applyBackup(restored)
        refreshWidget()
        refreshDashboard()
    }

    fun setTextSize(size: AppTextSize) = viewModelScope.launch { settingsRepo.setTextSize(size) }
    fun setTariffPerGbToman(value: Long) = viewModelScope.launch { settingsRepo.setTariffPerGb(value) }
    fun setSpikeAlert(enabled: Boolean) = viewModelScope.launch { settingsRepo.setSpikeAlert(enabled) }
    fun setWeeklyDigest(enabled: Boolean) = viewModelScope.launch { settingsRepo.setWeeklyDigest(enabled) }

    /** پاک کردن کامل آمار فعلی و شروع آمارگیری از همین لحظه (برگشت‌ناپذیر). */
    fun resetStats() = viewModelScope.launch {
        val app = getApplication<Application>()
        ir.marbledu.datausage.data.UsageLedger.clearAll(app)
        // کلیدهای هشدار مصرف/جهش مصرف هم پاک می‌شوند تا دوباره همان ساعت‌ها اعلان نگیری
        app.getSharedPreferences("marbledu_spike_alerts", android.content.Context.MODE_PRIVATE).edit().clear().apply()
        app.getSharedPreferences("marbledu_usage_alerts", android.content.Context.MODE_PRIVATE).edit().clear().apply()
        settingsRepo.setStatsEpoch(System.currentTimeMillis())
        _history.value = HistoryUi()
        _dayReport.value = DayReportUi()
        refreshDashboard()
        refreshWidget()
    }

    /** شروع آمارگیری از یک روز مشخص؛ آمار قبل از آن دیگر نشان داده نمی‌شود. */
    fun setStatsEpochFromDay(dayStartMillis: Long) = viewModelScope.launch {
        settingsRepo.setStatsEpoch(dayStartMillis)
        refreshDashboard()
        if (_history.value.loadedAt > 0L) loadHistory(force = true)
        refreshWidget()
    }

    /** بازگشت به حالت پیش‌فرض (۳۰ روز اخیر، بدون شروعِ سفارشی). */
    fun clearStatsEpoch() = viewModelScope.launch {
        settingsRepo.setStatsEpoch(0L)
        refreshDashboard()
        if (_history.value.loadedAt > 0L) loadHistory(force = true)
        refreshWidget()
    }

    /** باز شدن صفحه‌ی کامل جزئیات اپراتور (۳۰ روز اخیر از روی آمار دستگاه + دفتر اپراتور). */
    fun openOperatorDetail(operatorId: String, sim: SimIdentity?) {
        operatorDetailJob?.cancel()
        _operatorDetail.value = OperatorDetailUi(operatorId = operatorId, sim = sim, loading = true)
        operatorDetailJob = viewModelScope.launch(Dispatchers.IO) {
            val data = repository.operatorDetail(
                operatorId = operatorId,
                sim = sim,
                days = 30,
                epochMillis = settings.value.statsEpochMillis,
            )
            val selectedDay = data.days.lastOrNull { it.total > 0L }?.dayStart ?: 0L
            val selectedHours = if (selectedDay > 0L) {
                repository.hourlyOperatorTraffic(selectedDay, operatorId, sim)
            } else {
                emptyList()
            }
            _operatorDetail.value = OperatorDetailUi(
                operatorId = operatorId,
                sim = sim,
                loading = false,
                days = data.days,
                hours = data.hours,
                selectedDayStart = selectedDay,
                selectedDayHours = selectedHours,
                selectedDayLoading = false,
            )
        }
    }

    /** انتخاب یک روز در صفحه‌ی جزئیات اپراتور برای نمودار ۲۴ ساعته. */
    fun selectOperatorDetailDay(dayStart: Long) {
        val current = _operatorDetail.value
        val operatorId = current.operatorId
        if (operatorId.isBlank() || dayStart <= 0L) return
        if (current.selectedDayStart == dayStart && current.selectedDayHours.isNotEmpty()) return
        _operatorDetail.value = current.copy(selectedDayStart = dayStart, selectedDayLoading = true)
        operatorDetailJob?.cancel()
        operatorDetailJob = viewModelScope.launch(Dispatchers.IO) {
            val hours = repository.hourlyOperatorTraffic(dayStart, operatorId, current.sim)
            _operatorDetail.value = _operatorDetail.value.copy(
                selectedDayHours = hours,
                selectedDayLoading = false,
            )
        }
    }

    /** کارت «اپراتورها»: امروز / ۷ روز اخیر / انتخاب تاریخ شمسی. */
    fun loadOperatorRange(mode: Int, pickedDayStart: Long? = null) {
        operatorRangeJob?.cancel()
        _operatorRange.value = _operatorRange.value.copy(mode = mode, loading = true)
        operatorRangeJob = viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val dayStart = TimeMath.dayStart(now)
            val dayMillis = 86_400_000L
            val from: Long
            val to: Long
            when (mode) {
                0 -> {
                    from = dayStart
                    to = now
                }
                1 -> {
                    from = dayStart - 6L * dayMillis
                    to = now
                }
                else -> {
                    val picked = pickedDayStart ?: dayStart
                    from = picked
                    to = minOf(picked + dayMillis, now)
                }
            }
            val rows = repository.operatorRange(from, to, settings.value.statsEpochMillis)
            _operatorRange.value = OperatorRangeUi(
                mode = mode,
                from = from,
                to = to,
                rows = rows,
                loading = false,
                loadedOnce = true,
            )
        }
    }
    fun setMonitorBackground(on: Boolean) = viewModelScope.launch {
        settingsRepo.setMonitorBackground(on)
        val context = getApplication<Application>()
        if (on) MonitorService.start(context) else MonitorService.stop(context)
    }

    fun updateNotification(transform: (NotificationConfig) -> NotificationConfig) =
        viewModelScope.launch { settingsRepo.updateNotification(transform) }

    fun applyNotificationPreset(preset: NotificationPreset) =
        viewModelScope.launch { settingsRepo.updateNotification { NotificationConfig.fromPreset(preset) } }

    fun completeOnboarding() = viewModelScope.launch {
        settingsRepo.setOnboarded(true)
        if (settings.value.monitorInBackground) {
            MonitorService.start(getApplication<Application>())
        }
    }
}
