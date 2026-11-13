package ir.marbledu.datausage.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.marbledu.datausage.MarbleDuApp
import ir.marbledu.datausage.data.AppLanguage
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.AppUsage
import ir.marbledu.datausage.data.ConnectionType
import ir.marbledu.datausage.data.IranOptions
import ir.marbledu.datausage.data.LiveMonitor
import ir.marbledu.datausage.data.LiveTick
import ir.marbledu.datausage.data.NetworkStatsReader
import ir.marbledu.datausage.data.PingResult
import ir.marbledu.datausage.data.ThemeMode
import ir.marbledu.datausage.data.TimeRange
import ir.marbledu.datausage.service.MonitorService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUsageUi(
    val range: TimeRange = TimeRange.TODAY,
    val mobile: List<AppUsage> = emptyList(),
    val wifi: List<AppUsage> = emptyList(),
    val loading: Boolean = false,
)

data class DashboardUi(
    val todayRx: Long = 0,
    val todayTx: Long = 0,
    val monthRx: Long = 0,
    val monthTx: Long = 0,
    val mobileBytes: Long = 0,
    val wifiBytes: Long = 0,
    val hasUsageAccess: Boolean = false,
    val operatorName: String = "",
    val averagePingMs: Double = -1.0,
    val averageSpeed: Double = 0.0,
)

@OptIn(ExperimentalCoroutinesApi::class)
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

    private var liveJob: Job? = null
    private var pingJob: Job? = null

    private val rangeFlow = MutableStateFlow(TimeRange.TODAY)

    init {
        startLive()
        refreshDashboard()
    }

    // ----------------------------------------------------------------- live
    fun startLive() {
        if (liveJob?.isActive == true) return
        liveJob = viewModelScope.launch {
            monitor.stream(1000).collect { value -> _tick.value = value }
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
            val dayStart = now - TimeRange.TODAY.millis()
            val monthStart = now - TimeRange.MONTH.millis()

            val todayMobile = repository.usageByApp(dayStart, now, ConnectionType.CELLULAR)
            val todayWifi = repository.usageByApp(dayStart, now, ConnectionType.WIFI)
            val monthMobile = repository.usageByApp(monthStart, now, ConnectionType.CELLULAR)
            val monthWifi = repository.usageByApp(monthStart, now, ConnectionType.WIFI)

            val snapshot = NetworkStatsReader.currentNetwork(getApplication())
            val averageSpeed = _tick.value?.averageSpeed ?: 0.0

            _dashboard.value = DashboardUi(
                todayRx = todayMobile.sumOf { it.bytes },
                todayTx = todayWifi.sumOf { it.bytes },
                monthRx = monthMobile.sumOf { it.bytes },
                monthTx = monthWifi.sumOf { it.bytes },
                mobileBytes = todayMobile.sumOf { it.bytes },
                wifiBytes = todayWifi.sumOf { it.bytes },
                hasUsageAccess = repository.hasUsageAccess(),
                operatorName = snapshot.carrierName,
                averagePingMs = _dashboard.value.averagePingMs,
                averageSpeed = averageSpeed,
            )
        }
    }

    fun setRange(range: TimeRange) {
        _usage.value = _usage.value.copy(range = range, loading = true)
        viewModelScope.launch { loadUsage(range) }
    }

    fun loadUsage(range: TimeRange = _usage.value.range) {
        viewModelScope.launch {
            _usage.value = _usage.value.copy(range = range, loading = true)
            val now = System.currentTimeMillis()
            val from = now - range.millis()
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
    fun setDataCap(bytes: Long) = viewModelScope.launch { settingsRepo.setDataCap(bytes) }
    fun setBillingDay(day: Int) = viewModelScope.launch { settingsRepo.setBillingCycleDay(day.coerceIn(1, 28)) }
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
