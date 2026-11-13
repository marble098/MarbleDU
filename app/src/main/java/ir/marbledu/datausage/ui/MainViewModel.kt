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
import ir.marbledu.datausage.data.UsageLedger
import ir.marbledu.datausage.data.TimeRange
import ir.marbledu.datausage.data.Traffic
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

/** ترافیک ساعتی یک اپراتور در یک روز خاص. */
data class OperatorHourlyUi(
    val dayStart: Long = 0L,
    val operatorId: String = "",
    val sim: SimIdentity? = null,
    val hours: List<HourOperatorTraffic> = emptyList(),
    val loading: Boolean = false,
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

    private val _operatorHourly = MutableStateFlow(OperatorHourlyUi())
    val operatorHourly: StateFlow<OperatorHourlyUi> = _operatorHourly.asStateFlow()

    private var historyJob: Job? = null
    private var dayJob: Job? = null
    private var operatorHourlyJob: Job? = null
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
        // پایش و گزارش سنگین تا پایان راه‌اندازی/نمایش رابط شروع نمی‌شوند.
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
        loadUsage(_usage.value.range)
        // تاریخچه‌ی ۳۰روزه فقط بعد از باز شدن تب گزارش خوانده می‌شود؛ بار اول
        // ورود به خانه نباید ده‌ها query تاریخی را هم‌زمان اجرا کند.
        if (_history.value.loadedAt > 0L) loadHistory()
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
            val days = repository.daySummaries(30)
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
            val report = repository.dayReport(summary)
            _dayReport.value = DayReportUi(dayStart = dayStart, report = report, loading = false)
        }
    }

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
            val todayMobile = repository.aggregateTraffic(dayStart, now, ConnectionType.CELLULAR)
            val todayWifi = repository.aggregateTraffic(dayStart, now, ConnectionType.WIFI)
            val monthMobile = repository.aggregateTraffic(monthStart, now, ConnectionType.CELLULAR)
            val monthWifi = repository.aggregateTraffic(monthStart, now, ConnectionType.WIFI)
            val week = repository.dailyTotals(7, ConnectionType.CELLULAR)

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
            val (mobile, wifi) = repository.detailedBreakdown(from, now)
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
    fun setDataCap(bytes: Long) = viewModelScope.launch { settingsRepo.setDataCap(bytes) }
    fun setBillingDay(day: Int) = viewModelScope.launch { settingsRepo.setBillingCycleDay(day.coerceIn(1, 28)) }
    fun setUsageAlerts(on: Boolean) = viewModelScope.launch { settingsRepo.setUsageAlerts(on) }
    fun setDailyLimitMb(mb: Int) = viewModelScope.launch { settingsRepo.setDailyLimitMb(mb) }
    fun setMonitorInterval(seconds: Int) = viewModelScope.launch {
        settingsRepo.setMonitorInterval(seconds.coerceIn(1, 60))
    }
    fun setOperator(name: String) = viewModelScope.launch { settingsRepo.setOperator(name) }

    /** حالت نمایشی اپ (ساده / پیشرفته). */
    fun setUiMode(mode: UiMode) = viewModelScope.launch { settingsRepo.setUiMode(mode) }

    fun setWidgetOperator(value: Boolean) = viewModelScope.launch { settingsRepo.setWidgetOperator(value) }
    fun setWidgetCap(value: Boolean) = viewModelScope.launch { settingsRepo.setWidgetCap(value) }
    fun setWidgetDetailed(value: Boolean) = viewModelScope.launch { settingsRepo.setWidgetDetailed(value) }
    fun setWidgetBigText(value: Boolean) = viewModelScope.launch { settingsRepo.setWidgetBigText(value) }

    /** ترافیک ساعتی یک اپراتور در روز انتخاب‌شده (برای گزارش ۲۴ ساعت). */
    fun loadOperatorHourly(dayStart: Long, operatorId: String, sim: SimIdentity?) {
        operatorHourlyJob?.cancel()
        _operatorHourly.value = OperatorHourlyUi(
            dayStart = dayStart,
            operatorId = operatorId,
            sim = sim,
            loading = true,
        )
        operatorHourlyJob = viewModelScope.launch(Dispatchers.IO) {
            val hours = repository.hourlyOperatorTraffic(dayStart, operatorId, sim)
            _operatorHourly.value = OperatorHourlyUi(
                dayStart = dayStart,
                operatorId = operatorId,
                sim = sim,
                hours = hours,
                loading = false,
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
