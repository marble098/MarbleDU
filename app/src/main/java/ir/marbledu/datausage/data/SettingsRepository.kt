package ir.marbledu.datausage.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "marbledu_settings")

/** زبان رابط کاربری. */
enum class AppLanguage { SYSTEM, PERSIAN, ENGLISH }

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,

    /** تم سیاه مطلق برای پنل‌های OLED: پس‌زمینه کاملاً مشکی و مصرف باتری کمتر. */
    AMOLED,
    ;
}

/** اندازه‌ی متن رابط کاربری؛ پیش‌فرض «کوچک» است و از تنظیمات قابل تغییر است. */
enum class AppTextSize(val fa: String, val en: String, val scale: Float) {
    SMALL("کوچک", "Small", 0.82f),
    MEDIUM("متوسط", "Medium", 1f),
    LARGE("بزرگ", "Large", 1.18f),
}

/** سبک نمایش اعلان پایش. */
enum class NotificationStyle { COMPACT, BALANCED, EXPANDED }

/** منبع عنوان اصلی اعلان. */
enum class NotificationTitleMode { SPEED, OPERATOR, TODAY_USAGE, APP_NAME, CUSTOM }

/** نوار پیشرفت داخل اعلان. */
enum class NotificationProgressMode { NONE, DATA_CAP, MOBILE_SHARE }

/** نمایش اعلان در صفحه قفل. */
enum class NotificationLockVisibility { PUBLIC, PRIVATE, HIDDEN }

/** پیش‌تنظیم‌های آماده‌ی اعلان. */
enum class NotificationPreset { FULL_DASHBOARD, MINIMAL_SPEED, DATA_CAP_FOCUS, OPERATOR_FOCUS }

/** پیکربندی کامل محتوا و ظاهر اعلان پایش. */
data class NotificationConfig(
    val style: NotificationStyle = NotificationStyle.EXPANDED,
    val titleMode: NotificationTitleMode = NotificationTitleMode.SPEED,
    val customTitle: String = "",
    val showDownloadSpeed: Boolean = true,
    val showUploadSpeed: Boolean = true,
    val showTotalSpeed: Boolean = false,
    val compactUnits: Boolean = true,
    val hideZeroSpeed: Boolean = false,
    val showOperator: Boolean = true,
    val showConnectionType: Boolean = true,
    val showNetworkTech: Boolean = true,
    val showTodayTotal: Boolean = true,
    val showTodayMobileWifi: Boolean = true,
    val showSessionUsage: Boolean = true,
    val showAverageSpeed: Boolean = false,
    val showPeakSpeed: Boolean = false,
    val showDataCap: Boolean = true,
    val showTopApp: Boolean = false,
    val showIpAddress: Boolean = false,
    val progressMode: NotificationProgressMode = NotificationProgressMode.DATA_CAP,
    val lockVisibility: NotificationLockVisibility = NotificationLockVisibility.PUBLIC,
    val actionRecheckIsp: Boolean = true,
    val actionStop: Boolean = true,
    val actionOpenReports: Boolean = false,
) {
    companion object {
        fun fromPreset(preset: NotificationPreset): NotificationConfig = when (preset) {
            NotificationPreset.FULL_DASHBOARD -> NotificationConfig(
                style = NotificationStyle.EXPANDED,
                titleMode = NotificationTitleMode.SPEED,
                showDownloadSpeed = true,
                showUploadSpeed = true,
                showTotalSpeed = false,
                compactUnits = true,
                hideZeroSpeed = false,
                showOperator = true,
                showConnectionType = true,
                showNetworkTech = true,
                showTodayTotal = true,
                showTodayMobileWifi = true,
                showSessionUsage = true,
                showAverageSpeed = true,
                showPeakSpeed = true,
                showDataCap = true,
                showTopApp = true,
                showIpAddress = false,
                progressMode = NotificationProgressMode.DATA_CAP,
                lockVisibility = NotificationLockVisibility.PUBLIC,
                actionRecheckIsp = true,
                actionStop = true,
                actionOpenReports = true,
            )

            NotificationPreset.MINIMAL_SPEED -> NotificationConfig(
                style = NotificationStyle.COMPACT,
                titleMode = NotificationTitleMode.SPEED,
                showDownloadSpeed = true,
                showUploadSpeed = true,
                showTotalSpeed = false,
                compactUnits = true,
                hideZeroSpeed = true,
                showOperator = true,
                showConnectionType = false,
                showNetworkTech = false,
                showTodayTotal = false,
                showTodayMobileWifi = false,
                showSessionUsage = false,
                showAverageSpeed = false,
                showPeakSpeed = false,
                showDataCap = false,
                showTopApp = false,
                showIpAddress = false,
                progressMode = NotificationProgressMode.NONE,
                lockVisibility = NotificationLockVisibility.PUBLIC,
                actionRecheckIsp = false,
                actionStop = false,
                actionOpenReports = false,
            )

            NotificationPreset.DATA_CAP_FOCUS -> NotificationConfig(
                style = NotificationStyle.EXPANDED,
                titleMode = NotificationTitleMode.TODAY_USAGE,
                showDownloadSpeed = true,
                showUploadSpeed = true,
                showTotalSpeed = false,
                compactUnits = true,
                hideZeroSpeed = false,
                showOperator = true,
                showConnectionType = true,
                showNetworkTech = false,
                showTodayTotal = true,
                showTodayMobileWifi = true,
                showSessionUsage = false,
                showAverageSpeed = false,
                showPeakSpeed = false,
                showDataCap = true,
                showTopApp = true,
                showIpAddress = false,
                progressMode = NotificationProgressMode.DATA_CAP,
                lockVisibility = NotificationLockVisibility.PUBLIC,
                actionRecheckIsp = false,
                actionStop = true,
                actionOpenReports = true,
            )

            NotificationPreset.OPERATOR_FOCUS -> NotificationConfig(
                style = NotificationStyle.BALANCED,
                titleMode = NotificationTitleMode.OPERATOR,
                showDownloadSpeed = true,
                showUploadSpeed = true,
                showTotalSpeed = false,
                compactUnits = true,
                hideZeroSpeed = false,
                showOperator = true,
                showConnectionType = true,
                showNetworkTech = true,
                showTodayTotal = true,
                showTodayMobileWifi = false,
                showSessionUsage = true,
                showAverageSpeed = false,
                showPeakSpeed = false,
                showDataCap = false,
                showTopApp = false,
                showIpAddress = true,
                progressMode = NotificationProgressMode.MOBILE_SHARE,
                lockVisibility = NotificationLockVisibility.PUBLIC,
                actionRecheckIsp = true,
                actionStop = true,
                actionOpenReports = false,
            )
        }
    }
}

/** حالت نمایشی اپ: «ساده» برای مبتدیان و کاربران عادی (رابط آرام، مرتب و کم‌جزئیات) و «پیشرفته» برای کاربران حرفه‌ای. */
enum class UiMode(val fa: String, val en: String) {
    MINIMAL("ساده", "Minimal"),
    ADVANCED("پیشرفته", "Advanced"),
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val dataCapBytes: Long = 0L,
    val billingCycleDay: Int = 1,
    val operatorOverride: String = "",
    val monitorInBackground: Boolean = true,
    val monitorIntervalSeconds: Int = 5,
    val onboarded: Boolean = false,
    /** نام پالت رنگی (ThemePalette). */
    val palette: String = "MARBLE",
    /** تنظیمات شخصی‌سازی پیشرفته اعلان. */
    val notification: NotificationConfig = NotificationConfig(),
    /** هشدار مصرف (سقف روزانه و ۸۰٪/۱۰۰٪ بسته). */
    val usageAlerts: Boolean = true,
    /** سقف مصرف روزانه‌ی دیتای موبایل بر حسب مگابایت؛ ۰ یعنی خاموش. */
    val dailyLimitMb: Int = 0,
    /** حالت نمایشی اپ: ساده یا پیشرفته. */
    val uiMode: UiMode = UiMode.ADVANCED,
    /** ویجت: نمایش نام و لوگو اپراتور فعلی. */
    val widgetShowOperator: Boolean = true,
    /** ویجت: نمایش سقف بسته و نوار پیشرفت. */
    val widgetShowCap: Boolean = true,
    /** ویجت: حالت کامل (جزئیات بیشتر، مثل ساعت پرمصرف و بقیه‌ی جزئیات). */
    val widgetDetailed: Boolean = false,
    /** ویجت: اندازه متن بزرگتر. */
    val widgetBigText: Boolean = false,
    /** اندازه‌ی متن رابط کاربری؛ پیش‌فرض کوچک. */
    val textSize: AppTextSize = AppTextSize.SMALL,
    /**
     * شروع آمارگیری: روزهایی قبل از این زمان در گزارش‌ها و نمودارها محاسبه و نشان داده نمی‌شوند.
     * ۰ یعنی حالت پیش‌فرض (۳۰ روز اخیر).
     */
    val statsEpochMillis: Long = 0L,
    /** تعرفه‌ی هر گیگابایت بر حسب تومان؛ ۰ یعنی تخمین هزینه خاموش است. */
    val tariffPerGbToman: Long = 0L,
    /** هشدار «مصرف غیرعادیِ این ساعت» روی اعلان پایش. */
    val spikeAlertEnabled: Boolean = true,
    /** خلاصه‌ی هفتگی: یک‌بار در هفته آمار هفته‌ی گذشته را اعلان می‌کند. */
    val weeklyDigestEnabled: Boolean = true,
    /** ویجت: رنگ پس‌زمینه از تم فعلی اپ گرفته شود (همیشه هم‌رنگ اپ). */
    val widgetFollowTheme: Boolean = true,
    /** ویجت: دکمه‌های میان‌بر توقف/ادامه‌ی پایش روی ویجت. */
    val widgetQuickActions: Boolean = true,
    /** عینک مصرف: پنجره‌ی شیشه‌ای همیشه-روی-صفحه که سرعت را به‌صورت شناور نشان می‌دهد. */
    val speedOverlay: Boolean = false,
    /** اعلان نوار پیشرفت زنده در پنجره‌ی عینک مصرف. */
    val speedOverlayChart: Boolean = true,
    /** کارت «نشست‌های اتصال» در خانه دیده شود. */
    val sessionsCardEnabled: Boolean = true,
    /** تب‌بندی نشست‌ها در خانه: همه / داده موبایل / وای‌فای / هر اپراتور جدا. */
    val sessionsTabsEnabled: Boolean = true,
    /** حداکثر نشست‌های دیده‌شده در کارت خانه (بیشتر از این، اسکرول می‌شود). */
    val sessionsVisibleCount: Int = 4,
) {
    val dailyLimitBytes: Long get() = dailyLimitMb.toLong() * 1024L * 1024L
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC = booleanPreferencesKey("dynamic")
        val DATA_CAP = longPreferencesKey("data_cap")
        val BILLING_DAY = intPreferencesKey("billing_day")
        val OPERATOR = stringPreferencesKey("operator")
        val BACKGROUND = booleanPreferencesKey("background")
        val INTERVAL = intPreferencesKey("interval")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val PALETTE = stringPreferencesKey("palette")
        val USAGE_ALERTS = booleanPreferencesKey("usage_alerts")
        val DAILY_LIMIT_MB = intPreferencesKey("daily_limit_mb")

        val UI_MODE = stringPreferencesKey("ui_mode")
        val WIDGET_OPERATOR = booleanPreferencesKey("widget_operator")
        val WIDGET_CAP = booleanPreferencesKey("widget_cap")
        val WIDGET_DETAILED = booleanPreferencesKey("widget_detailed")
        val WIDGET_BIG_TEXT = booleanPreferencesKey("widget_big_text")

        val TEXT_SIZE = stringPreferencesKey("text_size")
        val STATS_EPOCH = longPreferencesKey("stats_epoch")
        val TARIFF_PER_GB = longPreferencesKey("tariff_per_gb_toman")
        val SPIKE_ALERT = booleanPreferencesKey("spike_alert")
        val WEEKLY_DIGEST = booleanPreferencesKey("weekly_digest")
        val WIDGET_FOLLOW_THEME = booleanPreferencesKey("widget_follow_theme")
        val WIDGET_QUICK_ACTIONS = booleanPreferencesKey("widget_quick_actions")
        val SPEED_OVERLAY = booleanPreferencesKey("speed_overlay")
        val SPEED_OVERLAY_CHART = booleanPreferencesKey("speed_overlay_chart")
        val SESSIONS_CARD = booleanPreferencesKey("sessions_card")
        val SESSIONS_TABS = booleanPreferencesKey("sessions_tabs")
        val SESSIONS_VISIBLE = intPreferencesKey("sessions_visible")

        val NOTIF_STYLE = stringPreferencesKey("notif_style")
        val NOTIF_TITLE_MODE = stringPreferencesKey("notif_title_mode")
        val NOTIF_CUSTOM_TITLE = stringPreferencesKey("notif_custom_title")
        val NOTIF_DOWN = booleanPreferencesKey("notif_down")
        val NOTIF_UP = booleanPreferencesKey("notif_up")
        val NOTIF_TOTAL_SPEED = booleanPreferencesKey("notif_total_speed")
        val NOTIF_COMPACT_UNITS = booleanPreferencesKey("notif_compact_units")
        val NOTIF_HIDE_ZERO = booleanPreferencesKey("notif_hide_zero")
        val NOTIF_OPERATOR = booleanPreferencesKey("notif_operator")
        val NOTIF_CONN_TYPE = booleanPreferencesKey("notif_conn_type")
        val NOTIF_NET_TECH = booleanPreferencesKey("notif_net_tech")
        val NOTIF_TODAY_TOTAL = booleanPreferencesKey("notif_today_total")
        val NOTIF_TODAY_SPLIT = booleanPreferencesKey("notif_today_split")
        val NOTIF_SESSION = booleanPreferencesKey("notif_session")
        val NOTIF_AVG_SPEED = booleanPreferencesKey("notif_avg_speed")
        val NOTIF_PEAK_SPEED = booleanPreferencesKey("notif_peak_speed")
        val NOTIF_DATA_CAP = booleanPreferencesKey("notif_data_cap")
        val NOTIF_TOP_APP = booleanPreferencesKey("notif_top_app")
        val NOTIF_IP = booleanPreferencesKey("notif_ip")
        val NOTIF_PROGRESS = stringPreferencesKey("notif_progress")
        val NOTIF_LOCK_VIS = stringPreferencesKey("notif_lock_vis")
        val NOTIF_ACT_RECHECK = booleanPreferencesKey("notif_act_recheck")
        val NOTIF_ACT_STOP = booleanPreferencesKey("notif_act_stop")
        val NOTIF_ACT_REPORTS = booleanPreferencesKey("notif_act_reports")
    }

    private fun readNotificationConfig(p: Preferences): NotificationConfig = NotificationConfig(
        style = p[Keys.NOTIF_STYLE]?.let { runCatching { NotificationStyle.valueOf(it) }.getOrNull() }
            ?: NotificationStyle.EXPANDED,
        titleMode = p[Keys.NOTIF_TITLE_MODE]?.let { runCatching { NotificationTitleMode.valueOf(it) }.getOrNull() }
            ?: NotificationTitleMode.SPEED,
        customTitle = p[Keys.NOTIF_CUSTOM_TITLE].orEmpty(),
        showDownloadSpeed = p[Keys.NOTIF_DOWN] ?: true,
        showUploadSpeed = p[Keys.NOTIF_UP] ?: true,
        showTotalSpeed = p[Keys.NOTIF_TOTAL_SPEED] ?: false,
        compactUnits = p[Keys.NOTIF_COMPACT_UNITS] ?: true,
        hideZeroSpeed = p[Keys.NOTIF_HIDE_ZERO] ?: false,
        showOperator = p[Keys.NOTIF_OPERATOR] ?: true,
        showConnectionType = p[Keys.NOTIF_CONN_TYPE] ?: true,
        showNetworkTech = p[Keys.NOTIF_NET_TECH] ?: true,
        showTodayTotal = p[Keys.NOTIF_TODAY_TOTAL] ?: true,
        showTodayMobileWifi = p[Keys.NOTIF_TODAY_SPLIT] ?: true,
        showSessionUsage = p[Keys.NOTIF_SESSION] ?: true,
        showAverageSpeed = p[Keys.NOTIF_AVG_SPEED] ?: false,
        showPeakSpeed = p[Keys.NOTIF_PEAK_SPEED] ?: false,
        showDataCap = p[Keys.NOTIF_DATA_CAP] ?: true,
        showTopApp = p[Keys.NOTIF_TOP_APP] ?: false,
        showIpAddress = p[Keys.NOTIF_IP] ?: false,
        progressMode = p[Keys.NOTIF_PROGRESS]?.let { runCatching { NotificationProgressMode.valueOf(it) }.getOrNull() }
            ?: NotificationProgressMode.DATA_CAP,
        lockVisibility = p[Keys.NOTIF_LOCK_VIS]?.let { runCatching { NotificationLockVisibility.valueOf(it) }.getOrNull() }
            ?: NotificationLockVisibility.PUBLIC,
        actionRecheckIsp = p[Keys.NOTIF_ACT_RECHECK] ?: true,
        actionStop = p[Keys.NOTIF_ACT_STOP] ?: true,
        actionOpenReports = p[Keys.NOTIF_ACT_REPORTS] ?: false,
    )

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            language = p[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.SYSTEM,
            themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = p[Keys.DYNAMIC] ?: true,
            dataCapBytes = p[Keys.DATA_CAP] ?: 0L,
            billingCycleDay = p[Keys.BILLING_DAY] ?: 1,
            operatorOverride = p[Keys.OPERATOR].orEmpty(),
            monitorInBackground = p[Keys.BACKGROUND] ?: true,
            monitorIntervalSeconds = p[Keys.INTERVAL] ?: 5,
            onboarded = p[Keys.ONBOARDED] ?: false,
            palette = p[Keys.PALETTE] ?: "MARBLE",
            notification = readNotificationConfig(p),
            usageAlerts = p[Keys.USAGE_ALERTS] ?: true,
            dailyLimitMb = p[Keys.DAILY_LIMIT_MB] ?: 0,
            uiMode = p[Keys.UI_MODE]?.let { runCatching { UiMode.valueOf(it) }.getOrNull() }
                ?: UiMode.ADVANCED,
            widgetShowOperator = p[Keys.WIDGET_OPERATOR] ?: true,
            widgetShowCap = p[Keys.WIDGET_CAP] ?: true,
            widgetDetailed = p[Keys.WIDGET_DETAILED] ?: false,
            widgetBigText = p[Keys.WIDGET_BIG_TEXT] ?: false,
            textSize = p[Keys.TEXT_SIZE]?.let { runCatching { AppTextSize.valueOf(it) }.getOrNull() }
                ?: AppTextSize.SMALL,
            statsEpochMillis = p[Keys.STATS_EPOCH] ?: 0L,
            tariffPerGbToman = p[Keys.TARIFF_PER_GB] ?: 0L,
            spikeAlertEnabled = p[Keys.SPIKE_ALERT] ?: true,
            weeklyDigestEnabled = p[Keys.WEEKLY_DIGEST] ?: true,
            widgetFollowTheme = p[Keys.WIDGET_FOLLOW_THEME] ?: true,
            widgetQuickActions = p[Keys.WIDGET_QUICK_ACTIONS] ?: true,
            speedOverlay = p[Keys.SPEED_OVERLAY] ?: false,
            speedOverlayChart = p[Keys.SPEED_OVERLAY_CHART] ?: true,
            sessionsCardEnabled = p[Keys.SESSIONS_CARD] ?: true,
            sessionsTabsEnabled = p[Keys.SESSIONS_TABS] ?: true,
            sessionsVisibleCount = p[Keys.SESSIONS_VISIBLE] ?: 4,
        )
    }

    suspend fun setLanguage(value: AppLanguage) = context.dataStore.edit { it[Keys.LANGUAGE] = value.name }
    suspend fun setTheme(value: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = value.name }
    suspend fun setDynamicColor(value: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC] = value }
    suspend fun setDataCap(value: Long) = context.dataStore.edit { it[Keys.DATA_CAP] = value }
    suspend fun setBillingCycleDay(value: Int) = context.dataStore.edit { it[Keys.BILLING_DAY] = value }
    suspend fun setOperator(value: String) = context.dataStore.edit { it[Keys.OPERATOR] = value }
    suspend fun setMonitorBackground(value: Boolean) = context.dataStore.edit { it[Keys.BACKGROUND] = value }
    suspend fun setMonitorInterval(value: Int) = context.dataStore.edit { it[Keys.INTERVAL] = value }

    /** انتخاب پالت؛ پالت «پویا» همان رنگ والپیپر است. */
    suspend fun setPalette(name: String, dynamic: Boolean) = context.dataStore.edit {
        it[Keys.PALETTE] = name
        it[Keys.DYNAMIC] = dynamic
    }

    suspend fun setUsageAlerts(value: Boolean) = context.dataStore.edit { it[Keys.USAGE_ALERTS] = value }
    suspend fun setDailyLimitMb(value: Int) = context.dataStore.edit { it[Keys.DAILY_LIMIT_MB] = value.coerceIn(0, 1_000_000) }

    suspend fun setUiMode(value: UiMode) = context.dataStore.edit { it[Keys.UI_MODE] = value.name }

    suspend fun setWidgetOperator(value: Boolean) = context.dataStore.edit { it[Keys.WIDGET_OPERATOR] = value }
    suspend fun setWidgetCap(value: Boolean) = context.dataStore.edit { it[Keys.WIDGET_CAP] = value }
    suspend fun setWidgetDetailed(value: Boolean) = context.dataStore.edit { it[Keys.WIDGET_DETAILED] = value }
    suspend fun setWidgetBigText(value: Boolean) = context.dataStore.edit { it[Keys.WIDGET_BIG_TEXT] = value }
    suspend fun setTextSize(value: AppTextSize) = context.dataStore.edit { it[Keys.TEXT_SIZE] = value.name }
    /** شروع آمارگیری را به یک روز مشخص می‌برد؛ ۰ برای بازگشت به ۳۰ روز اخیر. */
    suspend fun setStatsEpoch(value: Long) = context.dataStore.edit { it[Keys.STATS_EPOCH] = value.coerceAtLeast(0L) }
    suspend fun setTariffPerGb(value: Long) = context.dataStore.edit { it[Keys.TARIFF_PER_GB] = value.coerceIn(0L, 100_000_000L) }
    suspend fun setSpikeAlert(value: Boolean) = context.dataStore.edit { it[Keys.SPIKE_ALERT] = value }
    suspend fun setWeeklyDigest(value: Boolean) = context.dataStore.edit { it[Keys.WEEKLY_DIGEST] = value }
    suspend fun setWidgetFollowTheme(value: Boolean) = context.dataStore.edit { it[Keys.WIDGET_FOLLOW_THEME] = value }
    suspend fun setWidgetQuickActions(value: Boolean) = context.dataStore.edit { it[Keys.WIDGET_QUICK_ACTIONS] = value }
    suspend fun setSpeedOverlay(value: Boolean) = context.dataStore.edit { it[Keys.SPEED_OVERLAY] = value }
    suspend fun setSpeedOverlayChart(value: Boolean) = context.dataStore.edit { it[Keys.SPEED_OVERLAY_CHART] = value }
    suspend fun setSessionsCard(value: Boolean) = context.dataStore.edit { it[Keys.SESSIONS_CARD] = value }
    suspend fun setSessionsTabs(value: Boolean) = context.dataStore.edit { it[Keys.SESSIONS_TABS] = value }
    suspend fun setSessionsVisible(value: Int) = context.dataStore.edit {
        it[Keys.SESSIONS_VISIBLE] = value.coerceIn(2, 10)
    }

    suspend fun setOnboarded(value: Boolean) = context.dataStore.edit { it[Keys.ONBOARDED] = value }

    /**
     * جایگزینی همه‌ی تنظیمات با یک نسخه‌ی پشتیبان (بازیابی فایل JSON).
     * داده‌های آماری و وضعیت راه‌اندازی دست‌نخورده می‌مانند.
     */
    suspend fun applyBackup(settings: AppSettings) = context.dataStore.edit { p ->
        p[Keys.LANGUAGE] = settings.language.name
        p[Keys.THEME] = settings.themeMode.name
        p[Keys.DYNAMIC] = settings.dynamicColor
        p[Keys.DATA_CAP] = settings.dataCapBytes
        p[Keys.BILLING_DAY] = settings.billingCycleDay
        p[Keys.OPERATOR] = settings.operatorOverride
        p[Keys.BACKGROUND] = settings.monitorInBackground
        p[Keys.INTERVAL] = settings.monitorIntervalSeconds
        p[Keys.PALETTE] = settings.palette
        p[Keys.USAGE_ALERTS] = settings.usageAlerts
        p[Keys.DAILY_LIMIT_MB] = settings.dailyLimitMb
        p[Keys.UI_MODE] = settings.uiMode.name
        p[Keys.WIDGET_OPERATOR] = settings.widgetShowOperator
        p[Keys.WIDGET_CAP] = settings.widgetShowCap
        p[Keys.WIDGET_DETAILED] = settings.widgetDetailed
        p[Keys.WIDGET_BIG_TEXT] = settings.widgetBigText
        p[Keys.WIDGET_FOLLOW_THEME] = settings.widgetFollowTheme
        p[Keys.WIDGET_QUICK_ACTIONS] = settings.widgetQuickActions
        p[Keys.SESSIONS_CARD] = settings.sessionsCardEnabled
        p[Keys.SESSIONS_TABS] = settings.sessionsTabsEnabled
        p[Keys.SESSIONS_VISIBLE] = settings.sessionsVisibleCount
        p[Keys.SPEED_OVERLAY] = settings.speedOverlay
        p[Keys.SPEED_OVERLAY_CHART] = settings.speedOverlayChart
        p[Keys.TEXT_SIZE] = settings.textSize.name
        p[Keys.TARIFF_PER_GB] = settings.tariffPerGbToman
        p[Keys.SPIKE_ALERT] = settings.spikeAlertEnabled
        p[Keys.WEEKLY_DIGEST] = settings.weeklyDigestEnabled
        val n = settings.notification
        p[Keys.NOTIF_STYLE] = n.style.name
        p[Keys.NOTIF_TITLE_MODE] = n.titleMode.name
        p[Keys.NOTIF_CUSTOM_TITLE] = n.customTitle
        p[Keys.NOTIF_DOWN] = n.showDownloadSpeed
        p[Keys.NOTIF_UP] = n.showUploadSpeed
        p[Keys.NOTIF_TOTAL_SPEED] = n.showTotalSpeed
        p[Keys.NOTIF_COMPACT_UNITS] = n.compactUnits
        p[Keys.NOTIF_HIDE_ZERO] = n.hideZeroSpeed
        p[Keys.NOTIF_OPERATOR] = n.showOperator
        p[Keys.NOTIF_CONN_TYPE] = n.showConnectionType
        p[Keys.NOTIF_NET_TECH] = n.showNetworkTech
        p[Keys.NOTIF_TODAY_TOTAL] = n.showTodayTotal
        p[Keys.NOTIF_TODAY_SPLIT] = n.showTodayMobileWifi
        p[Keys.NOTIF_SESSION] = n.showSessionUsage
        p[Keys.NOTIF_AVG_SPEED] = n.showAverageSpeed
        p[Keys.NOTIF_PEAK_SPEED] = n.showPeakSpeed
        p[Keys.NOTIF_DATA_CAP] = n.showDataCap
        p[Keys.NOTIF_TOP_APP] = n.showTopApp
        p[Keys.NOTIF_IP] = n.showIpAddress
        p[Keys.NOTIF_PROGRESS] = n.progressMode.name
        p[Keys.NOTIF_LOCK_VIS] = n.lockVisibility.name
        p[Keys.NOTIF_ACT_RECHECK] = n.actionRecheckIsp
        p[Keys.NOTIF_ACT_STOP] = n.actionStop
        p[Keys.NOTIF_ACT_REPORTS] = n.actionOpenReports
    }

    suspend fun updateNotification(update: (NotificationConfig) -> NotificationConfig) {
        context.dataStore.edit { p ->
            val current = readNotificationConfig(p)
            val next = update(current)
            p[Keys.NOTIF_STYLE] = next.style.name
            p[Keys.NOTIF_TITLE_MODE] = next.titleMode.name
            p[Keys.NOTIF_CUSTOM_TITLE] = next.customTitle
            p[Keys.NOTIF_DOWN] = next.showDownloadSpeed
            p[Keys.NOTIF_UP] = next.showUploadSpeed
            p[Keys.NOTIF_TOTAL_SPEED] = next.showTotalSpeed
            p[Keys.NOTIF_COMPACT_UNITS] = next.compactUnits
            p[Keys.NOTIF_HIDE_ZERO] = next.hideZeroSpeed
            p[Keys.NOTIF_OPERATOR] = next.showOperator
            p[Keys.NOTIF_CONN_TYPE] = next.showConnectionType
            p[Keys.NOTIF_NET_TECH] = next.showNetworkTech
            p[Keys.NOTIF_TODAY_TOTAL] = next.showTodayTotal
            p[Keys.NOTIF_TODAY_SPLIT] = next.showTodayMobileWifi
            p[Keys.NOTIF_SESSION] = next.showSessionUsage
            p[Keys.NOTIF_AVG_SPEED] = next.showAverageSpeed
            p[Keys.NOTIF_PEAK_SPEED] = next.showPeakSpeed
            p[Keys.NOTIF_DATA_CAP] = next.showDataCap
            p[Keys.NOTIF_TOP_APP] = next.showTopApp
            p[Keys.NOTIF_IP] = next.showIpAddress
            p[Keys.NOTIF_PROGRESS] = next.progressMode.name
            p[Keys.NOTIF_LOCK_VIS] = next.lockVisibility.name
            p[Keys.NOTIF_ACT_RECHECK] = next.actionRecheckIsp
            p[Keys.NOTIF_ACT_STOP] = next.actionStop
            p[Keys.NOTIF_ACT_REPORTS] = next.actionOpenReports
        }
    }
}
