package ir.marbledu.datausage.ui

import android.app.Application
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
import ir.marbledu.datausage.data.Traffic
import ir.marbledu.datausage.data.HourUsage
import ir.marbledu.datausage.data.IranOptions
import ir.marbledu.datausage.data.LiveMonitor
import ir.marbledu.datausage.data.LiveTick
import ir.marbledu.datausage.data.NetworkStatsReader
import ir.marbledu.datausage.data.PingResult
import ir.marbledu.datausage.data.ThemeMode
import ir.marbledu.datausage.data.TimeMath
import ir.marbledu.datausage.data.TimeRange
import ir.marbledu.datausage.service.MonitorService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as MarbleDuApp).appUsageRepository
    private val monitor: LiveMonitor = (app as MarbleDuApp).liveMonitor
    private val settingsRepo = (app as MarbleDuApp).settingsRepository

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

    private val _peakHours = MutableStateFlow<List<HourUsage>>(emptyList())
    val peakHours: StateFlow<List<HourUsage>> = _peakHours.asStateFlow()

    private val _speedHistory = MutableStateFlow<List<SpeedPoint>>(emptyList())
    val speedHistory: StateFlow<List<SpeedPoint>> = _speedHistory.asStateFlow()

    private val _history = MutableStateFlow(HistoryUi())
    val history: StateFlow<HistoryUi> = _history.asStateFlow()

    private val _dayReport = MutableStateFlow(DayReportUi())
    val dayReport: StateFlow<DayReportUi> = _dayReport.asStateFlow()

    private var historyJob: Job? = null
    private var dayJob: Job? = null

    private var liveJob: Job? = null
    private var pingJob: Job? = null

    init {
        viewModelScope.launch {
            settings.collect { IspResolver.enabled = it.wifiIspLookup }
        }
        startLive()
        // پیش از این فقط داشبورد لود می‌شد و لیست اپ‌ها/گزارش‌ها تا وقتی کاربر
        // دکمه‌ی «به‌روزرسانی» را می‌زد خالی می‌ماند.
        refreshAll()
    }

    /**
     * یک نقطه‌ی واحد برای تازه کردن همه‌چیز. از ON_RESUME صدا زده می‌شود تا
     * بعد از برگشتن از «دسترسی به داده‌های مصرف» همان لحظه داده‌ها ظاهر شوند.
     */
    fun refreshAll() {
        refreshDashboard()
        loadUsage(_usage.value.range)
        loadPeakHours(_usage.value.range)
        loadHistory()
    }

    // ------------------------------------------------------------- 30 days
    /** فهرست ۳۰ روز اخیر؛ اگر هنوز روزی انتخاب نشده، «امروز» انتخاب می‌شود. */
    fun loadHistory() {
        if (historyJob?.isActive == true) return
        historyJob = viewModelScope.launch {
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
            monitor.stream(1000).collect { value ->
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
            _ping.value = emptyList()
            val results = mutableListOf<PingResult>()
            monitor.ping().collect { result ->
                results.add(result)
                _ping.value = results.toList()
            }
            val avg = results.map { it.averageMs }.filter { it > 0 }
            _dashboard.value = _dashboard.value.copy(
                averagePingMs = if (avg.isEmpty()) -1.0 else avg.average(),
            )
        }
    }

    // ------------------------------------------------------------- dashboard
    fun refreshDashboard() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dayStart = TimeMath.dayStart(now)
            val monthStart = now - TimeRange.MONTH.millis()

            val todayMobile = repository.usageByApp(dayStart, now, ConnectionType.CELLULAR)
            val todayWifi = repository.usageByApp(dayStart, now, ConnectionType.WIFI)
            val monthMobile = repository.usageByApp(monthStart, now, ConnectionType.CELLULAR)
            val monthWifi = repository.usageByApp(monthStart, now, ConnectionType.WIFI)

            val snapshot = NetworkStatsReader.currentNetwork(getApplication())
            val averageSpeed = _tick.value?.averageSpeed ?: 0.0

            val todaySummary = repository.daySummaries(1).lastOrNull()
            val week = repository.dailyTotals(7, ConnectionType.CELLULAR)
            val todayTotal = week.lastOrNull()?.bytes ?: 0L
            val yesterdayTotal = week.dropLast(1).lastOrNull()?.bytes ?: 0L
            val variation = when {
                yesterdayTotal <= 0L -> 0
                else -> (((todayTotal - yesterdayTotal).toDouble() / yesterdayTotal) * 100).toInt()
            }

            _dashboard.value = DashboardUi(
                todayMobileBytes = todayMobile.sumOf { it.bytes },
                todayWifiBytes = todayWifi.sumOf { it.bytes },
                monthMobileBytes = monthMobile.sumOf { it.bytes },
                monthWifiBytes = monthWifi.sumOf { it.bytes },
                todayVariationPercent = variation,
                week = week,
                hasUsageAccess = repository.hasUsageAccess(),
                operatorName = snapshot.carrierName,
                averagePingMs = _dashboard.value.averagePingMs,
                averageSpeed = averageSpeed,
                todayMobile = todaySummary?.mobile ?: Traffic(),
                todayWifi = todaySummary?.wifi ?: Traffic(),
            )
        }
    }

    /** تغییر بازه فقط یک بار لود می‌کند (قبل از این دو coroutine روی هم ساخته می‌شد). */
    fun setRange(range: TimeRange) = loadUsage(range)

    /** ساعت‌های پرمصرف بر اساس مصرف واقعی هر ساعت (نه ساعت آخرین استفاده‌ی اپ). */
    fun loadPeakHours(range: TimeRange = _usage.value.range) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val from = TimeMath.rangeStart(range, now, settings.value.billingCycleDay)
            _peakHours.value = repository.hourlyTotals(from, now, ConnectionType.CELLULAR)
        }
    }

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
    fun setExpressive(on: Boolean) = viewModelScope.launch { settingsRepo.setExpressive(on) }
    fun setDynamicColor(on: Boolean) = viewModelScope.launch { settingsRepo.setDynamicColor(on) }
    fun setPalette(name: String, dynamic: Boolean) = viewModelScope.launch { settingsRepo.setPalette(name, dynamic) }
    fun setWifiIspLookup(on: Boolean) = viewModelScope.launch { settingsRepo.setWifiIspLookup(on) }
    fun setDataCap(bytes: Long) = viewModelScope.launch { settingsRepo.setDataCap(bytes) }
    fun setBillingDay(day: Int) = viewModelScope.launch { settingsRepo.setBillingCycleDay(day.coerceIn(1, 28)) }
    fun setMonitorInterval(seconds: Int) = viewModelScope.launch {
        settingsRepo.setMonitorInterval(seconds.coerceIn(1, 60))
    }
    fun setOperator(name: String) = viewModelScope.launch { settingsRepo.setOperator(name) }
    fun setMonitorBackground(on: Boolean) = viewModelScope.launch {
        settingsRepo.setMonitorBackground(on)
        val context = getApplication<Application>()
        if (on) MonitorService.start(context) else MonitorService.stop(context)
    }

    fun updateIran(transform: (IranOptions) -> IranOptions) =
        viewModelScope.launch { settingsRepo.updateIran(transform) }

    fun completeOnboarding() = viewModelScope.launch { settingsRepo.setOnboarded(true) }
}
