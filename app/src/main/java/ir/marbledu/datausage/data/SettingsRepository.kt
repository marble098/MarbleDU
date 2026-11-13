package ir.marbledu.datausage.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "marbledu_settings")

/** زبان رابط کاربری. */
enum class AppLanguage { SYSTEM, PERSIAN, ENGLISH }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** گزینه‌های ویژه‌ی کاربران ایرانی. */
data class IranOptions(
    val detectOperator: Boolean = true,
    val showDataUsageReport: Boolean = true,
    val showAverageSpeed: Boolean = true,
    val showAveragePing: Boolean = true,
    val showSessionTimeline: Boolean = true,
    val showPerAppBreakdown: Boolean = true,
    val showPeakHours: Boolean = true,
    val showBillingCycle: Boolean = true,
)

data class AppSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val expressiveDesign: Boolean = true,
    val dynamicColor: Boolean = true,
    val iran: IranOptions = IranOptions(),
    val dataCapBytes: Long = 0L,
    val billingCycleDay: Int = 1,
    val operatorOverride: String = "",
    val monitorInBackground: Boolean = true,
    val monitorIntervalSeconds: Int = 5,
    val onboarded: Boolean = false,
    /** نام پالت رنگی (ThemePalette). */
    val palette: String = "MARBLE",
    /** شناسایی آنلاینِ اپراتور وای‌فای از روی IP عمومی. */
    val wifiIspLookup: Boolean = true,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val EXPRESSIVE = booleanPreferencesKey("expressive")
        val DYNAMIC = booleanPreferencesKey("dynamic")
        val DATA_CAP = longPreferencesKey("data_cap")
        val BILLING_DAY = intPreferencesKey("billing_day")
        val OPERATOR = stringPreferencesKey("operator")
        val BACKGROUND = booleanPreferencesKey("background")
        val INTERVAL = intPreferencesKey("interval")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val PALETTE = stringPreferencesKey("palette")
        val WIFI_ISP = booleanPreferencesKey("wifi_isp_lookup")

        val IR_OPERATOR = booleanPreferencesKey("ir_operator")
        val IR_REPORT = booleanPreferencesKey("ir_report")
        val IR_SPEED = booleanPreferencesKey("ir_speed")
        val IR_PING = booleanPreferencesKey("ir_ping")
        val IR_TIMELINE = booleanPreferencesKey("ir_timeline")
        val IR_APPS = booleanPreferencesKey("ir_apps")
        val IR_PEAK = booleanPreferencesKey("ir_peak")
        val IR_CYCLE = booleanPreferencesKey("ir_cycle")
        val IR_SCALE = floatPreferencesKey("ir_scale")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            language = p[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.SYSTEM,
            themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            expressiveDesign = p[Keys.EXPRESSIVE] ?: true,
            dynamicColor = p[Keys.DYNAMIC] ?: true,
            dataCapBytes = p[Keys.DATA_CAP] ?: 0L,
            billingCycleDay = p[Keys.BILLING_DAY] ?: 1,
            operatorOverride = p[Keys.OPERATOR].orEmpty(),
            monitorInBackground = p[Keys.BACKGROUND] ?: true,
            monitorIntervalSeconds = p[Keys.INTERVAL] ?: 5,
            onboarded = p[Keys.ONBOARDED] ?: false,
            palette = p[Keys.PALETTE] ?: "MARBLE",
            wifiIspLookup = p[Keys.WIFI_ISP] ?: true,
            iran = IranOptions(
                detectOperator = p[Keys.IR_OPERATOR] ?: true,
                showDataUsageReport = p[Keys.IR_REPORT] ?: true,
                showAverageSpeed = p[Keys.IR_SPEED] ?: true,
                showAveragePing = p[Keys.IR_PING] ?: true,
                showSessionTimeline = p[Keys.IR_TIMELINE] ?: true,
                showPerAppBreakdown = p[Keys.IR_APPS] ?: true,
                showPeakHours = p[Keys.IR_PEAK] ?: true,
                showBillingCycle = p[Keys.IR_CYCLE] ?: true,
            ),
        )
    }

    suspend fun setLanguage(value: AppLanguage) = context.dataStore.edit { it[Keys.LANGUAGE] = value.name }
    suspend fun setTheme(value: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = value.name }
    suspend fun setExpressive(value: Boolean) = context.dataStore.edit { it[Keys.EXPRESSIVE] = value }
    suspend fun setDynamicColor(value: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC] = value }
    suspend fun setDataCap(value: Long) = context.dataStore.edit { it[Keys.DATA_CAP] = value }
    suspend fun setBillingCycleDay(value: Int) = context.dataStore.edit { it[Keys.BILLING_DAY] = value }
    suspend fun setOperator(value: String) = context.dataStore.edit { it[Keys.OPERATOR] = value }
    suspend fun setMonitorBackground(value: Boolean) = context.dataStore.edit { it[Keys.BACKGROUND] = value }
    suspend fun setMonitorInterval(value: Int) = context.dataStore.edit { it[Keys.INTERVAL] = value }
    suspend fun setWifiIspLookup(value: Boolean) = context.dataStore.edit { it[Keys.WIFI_ISP] = value }

    /** انتخاب پالت؛ پالت «پویا» همان رنگ والپیپر است. */
    suspend fun setPalette(name: String, dynamic: Boolean) = context.dataStore.edit {
        it[Keys.PALETTE] = name
        it[Keys.DYNAMIC] = dynamic
    }

    suspend fun setOnboarded(value: Boolean) = context.dataStore.edit { it[Keys.ONBOARDED] = value }

    suspend fun updateIran(update: (IranOptions) -> IranOptions) {
        context.dataStore.edit { p ->
            val current = IranOptions(
                detectOperator = p[Keys.IR_OPERATOR] ?: true,
                showDataUsageReport = p[Keys.IR_REPORT] ?: true,
                showAverageSpeed = p[Keys.IR_SPEED] ?: true,
                showAveragePing = p[Keys.IR_PING] ?: true,
                showSessionTimeline = p[Keys.IR_TIMELINE] ?: true,
                showPerAppBreakdown = p[Keys.IR_APPS] ?: true,
                showPeakHours = p[Keys.IR_PEAK] ?: true,
                showBillingCycle = p[Keys.IR_CYCLE] ?: true,
            )
            val next = update(current)
            p[Keys.IR_OPERATOR] = next.detectOperator
            p[Keys.IR_REPORT] = next.showDataUsageReport
            p[Keys.IR_SPEED] = next.showAverageSpeed
            p[Keys.IR_PING] = next.showAveragePing
            p[Keys.IR_TIMELINE] = next.showSessionTimeline
            p[Keys.IR_APPS] = next.showPerAppBreakdown
            p[Keys.IR_PEAK] = next.showPeakHours
            p[Keys.IR_CYCLE] = next.showBillingCycle
        }
    }
}
