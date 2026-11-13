package ir.marbledu.datausage.util

import android.content.Context
import android.net.Uri
import ir.marbledu.datausage.data.AppLanguage
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.AppTextSize
import ir.marbledu.datausage.data.NotificationLockVisibility
import ir.marbledu.datausage.data.NotificationProgressMode
import ir.marbledu.datausage.data.NotificationStyle
import ir.marbledu.datausage.data.NotificationTitleMode
import ir.marbledu.datausage.data.ThemeMode
import ir.marbledu.datausage.data.UiMode
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider

/**
 * پشتیبان‌گیری و بازیابی تنظیمات (قابلیت تازه‌ی ۱.۵).
 *
 * چرا لازم است؟ کاربری که گوشی عوض می‌کند یا اپ را دوباره نصب می‌کند، مجبور
 * نبود تنظیماتش (سقف بسته، تم، اعلان، هشدارها و…) را دوباره از صفر بچیند.
 *
 * فایل خروجی یک JSON خوانا با نسخه‌بندی است؛ فقط تنظیمات در آن ذخیره می‌شود
 * و هیچ داده‌ی مصرفی یا شناسه‌ی شخصی در آن نیست.
 */
object SettingsBackup {

    const val VERSION = 1
    private const val FILE_PREFIX = "MarbleDU-settings"

    /** ساخت فایل پشتیبان و برگرداندن Uri برای اشتراک‌گذاری/ذخیره. */
    suspend fun export(context: Context, settings: AppSettings): Uri = withContext(Dispatchers.IO) {
        val json = encode(settings)
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "$FILE_PREFIX-${System.currentTimeMillis()}.json")
        file.writeText(json)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** تبدیل تنظیمات به JSON؛ برای تست و بازبینی هم استفاده می‌شود. */
    fun encode(settings: AppSettings): String {
        val n = settings.notification
        return JSONObject().apply {
            put("app", "MarbleDU")
            put("type", "settings")
            put("version", VERSION)
            put("language", settings.language.name)
            put("themeMode", settings.themeMode.name)
            put("palette", settings.palette)
            put("dynamicColor", settings.dynamicColor)
            put("textSize", settings.textSize.name)
            put("uiMode", settings.uiMode.name)
            put("dataCapBytes", settings.dataCapBytes)
            put("billingCycleDay", settings.billingCycleDay)
            put("operatorOverride", settings.operatorOverride)
            put("monitorInBackground", settings.monitorInBackground)
            put("monitorIntervalSeconds", settings.monitorIntervalSeconds)
            put("dailyLimitMb", settings.dailyLimitMb)
            put("usageAlerts", settings.usageAlerts)
            put("spikeAlertEnabled", settings.spikeAlertEnabled)
            put("weeklyDigestEnabled", settings.weeklyDigestEnabled)
            put("tariffPerGbToman", settings.tariffPerGbToman)
            put("widgetShowOperator", settings.widgetShowOperator)
            put("widgetShowCap", settings.widgetShowCap)
            put("widgetDetailed", settings.widgetDetailed)
            put("widgetBigText", settings.widgetBigText)
            put("widgetFollowTheme", settings.widgetFollowTheme)
            put("widgetQuickActions", settings.widgetQuickActions)
            put("sessionsCardEnabled", settings.sessionsCardEnabled)
            put("sessionsTabsEnabled", settings.sessionsTabsEnabled)
            put("sessionsVisibleCount", settings.sessionsVisibleCount)
            put("speedOverlay", settings.speedOverlay)
            put("speedOverlayChart", settings.speedOverlayChart)
            put("showForeignOperators", settings.showForeignOperators)
            put("notification", JSONObject().apply {
                put("style", n.style.name)
                put("titleMode", n.titleMode.name)
                put("customTitle", n.customTitle)
                put("showDownloadSpeed", n.showDownloadSpeed)
                put("showUploadSpeed", n.showUploadSpeed)
                put("showTotalSpeed", n.showTotalSpeed)
                put("compactUnits", n.compactUnits)
                put("hideZeroSpeed", n.hideZeroSpeed)
                put("showOperator", n.showOperator)
                put("showConnectionType", n.showConnectionType)
                put("showNetworkTech", n.showNetworkTech)
                put("showTodayTotal", n.showTodayTotal)
                put("showTodayMobileWifi", n.showTodayMobileWifi)
                put("showSessionUsage", n.showSessionUsage)
                put("showAverageSpeed", n.showAverageSpeed)
                put("showPeakSpeed", n.showPeakSpeed)
                put("showDataCap", n.showDataCap)
                put("showTopApp", n.showTopApp)
                put("showIpAddress", n.showIpAddress)
                put("progressMode", n.progressMode.name)
                put("lockVisibility", n.lockVisibility.name)
                put("actionRecheckIsp", n.actionRecheckIsp)
                put("actionStop", n.actionStop)
                put("actionOpenReports", n.actionOpenReports)
            })
        }.toString(2)
    }

    /**
     * خواندن تنظیمات از متن JSON. ورودی نامعتبر یا فایلِ ناشناخته، null
     * برمی‌گرداند تا هیچ‌وقت تنظیمات فعلی را با داده‌ی خراب از دست ندهیم.
     */
    fun decode(text: String, base: AppSettings): AppSettings? = runCatching<AppSettings?> {
        val root = JSONObject(text)
        val looksLikeOurs = root.optString("app") == "MarbleDU" &&
            root.optInt("version", 0) <= VERSION
        if (!looksLikeOurs) return@runCatching null
        val n = root.optJSONObject("notification")
        val notification = if (n == null) base.notification else base.notification.copy(
            style = enumOr(n.optString("style"), base.notification.style),
            titleMode = enumOr(n.optString("titleMode"), base.notification.titleMode),
            customTitle = n.optString("customTitle", base.notification.customTitle),
            showDownloadSpeed = n.optBoolean("showDownloadSpeed", base.notification.showDownloadSpeed),
            showUploadSpeed = n.optBoolean("showUploadSpeed", base.notification.showUploadSpeed),
            showTotalSpeed = n.optBoolean("showTotalSpeed", base.notification.showTotalSpeed),
            compactUnits = n.optBoolean("compactUnits", base.notification.compactUnits),
            hideZeroSpeed = n.optBoolean("hideZeroSpeed", base.notification.hideZeroSpeed),
            showOperator = n.optBoolean("showOperator", base.notification.showOperator),
            showConnectionType = n.optBoolean("showConnectionType", base.notification.showConnectionType),
            showNetworkTech = n.optBoolean("showNetworkTech", base.notification.showNetworkTech),
            showTodayTotal = n.optBoolean("showTodayTotal", base.notification.showTodayTotal),
            showTodayMobileWifi = n.optBoolean("showTodayMobileWifi", base.notification.showTodayMobileWifi),
            showSessionUsage = n.optBoolean("showSessionUsage", base.notification.showSessionUsage),
            showAverageSpeed = n.optBoolean("showAverageSpeed", base.notification.showAverageSpeed),
            showPeakSpeed = n.optBoolean("showPeakSpeed", base.notification.showPeakSpeed),
            showDataCap = n.optBoolean("showDataCap", base.notification.showDataCap),
            showTopApp = n.optBoolean("showTopApp", base.notification.showTopApp),
            showIpAddress = n.optBoolean("showIpAddress", base.notification.showIpAddress),
            progressMode = enumOr(n.optString("progressMode"), base.notification.progressMode),
            lockVisibility = enumOr(n.optString("lockVisibility"), base.notification.lockVisibility),
            actionRecheckIsp = n.optBoolean("actionRecheckIsp", base.notification.actionRecheckIsp),
            actionStop = n.optBoolean("actionStop", base.notification.actionStop),
            actionOpenReports = n.optBoolean("actionOpenReports", base.notification.actionOpenReports),
        )
        base.copy(
            language = enumOr(root.optString("language"), base.language),
            themeMode = enumOr(root.optString("themeMode"), base.themeMode),
            palette = root.optString("palette", base.palette),
            dynamicColor = root.optBoolean("dynamicColor", base.dynamicColor),
            textSize = enumOr(root.optString("textSize"), base.textSize),
            uiMode = enumOr(root.optString("uiMode"), base.uiMode),
            dataCapBytes = root.optLong("dataCapBytes", base.dataCapBytes),
            billingCycleDay = root.optInt("billingCycleDay", base.billingCycleDay).coerceIn(1, 28),
            operatorOverride = root.optString("operatorOverride", base.operatorOverride),
            monitorInBackground = root.optBoolean("monitorInBackground", base.monitorInBackground),
            monitorIntervalSeconds = root.optInt("monitorIntervalSeconds", base.monitorIntervalSeconds)
                .coerceIn(1, 60),
            dailyLimitMb = root.optInt("dailyLimitMb", base.dailyLimitMb).coerceIn(0, 1_000_000),
            usageAlerts = root.optBoolean("usageAlerts", base.usageAlerts),
            spikeAlertEnabled = root.optBoolean("spikeAlertEnabled", base.spikeAlertEnabled),
            weeklyDigestEnabled = root.optBoolean("weeklyDigestEnabled", base.weeklyDigestEnabled),
            tariffPerGbToman = root.optLong("tariffPerGbToman", base.tariffPerGbToman)
                .coerceIn(0L, 100_000_000L),
            widgetShowOperator = root.optBoolean("widgetShowOperator", base.widgetShowOperator),
            widgetShowCap = root.optBoolean("widgetShowCap", base.widgetShowCap),
            widgetDetailed = root.optBoolean("widgetDetailed", base.widgetDetailed),
            widgetBigText = root.optBoolean("widgetBigText", base.widgetBigText),
            widgetFollowTheme = root.optBoolean("widgetFollowTheme", base.widgetFollowTheme),
            widgetQuickActions = root.optBoolean("widgetQuickActions", base.widgetQuickActions),
            sessionsCardEnabled = root.optBoolean("sessionsCardEnabled", base.sessionsCardEnabled),
            sessionsTabsEnabled = root.optBoolean("sessionsTabsEnabled", base.sessionsTabsEnabled),
            sessionsVisibleCount = root.optInt("sessionsVisibleCount", base.sessionsVisibleCount)
                .coerceIn(2, 10),
            speedOverlay = root.optBoolean("speedOverlay", base.speedOverlay),
            speedOverlayChart = root.optBoolean("speedOverlayChart", base.speedOverlayChart),
            showForeignOperators = root.optBoolean("showForeignOperators", base.showForeignOperators),
            notification = notification,
        )
    }.getOrNull()

    /** خواندن مستقیم از Uri انتخاب‌شده با Document picker. */
    suspend fun readFrom(context: Context, uri: Uri, base: AppSettings): AppSettings? =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                if (text.isNullOrBlank()) null else decode(text, base)
            }.getOrNull()
        }

    private inline fun <reified T : Enum<T>> enumOr(name: String, fallback: T): T =
        runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)

    /** فقط برای مستندسازی نوع‌هایی که در JSON ذخیره می‌شوند. */
    @Suppress("unused")
    private val storedEnums = listOf(
        AppLanguage::class.java,
        ThemeMode::class.java,
        AppTextSize::class.java,
        UiMode::class.java,
        NotificationStyle::class.java,
        NotificationTitleMode::class.java,
        NotificationProgressMode::class.java,
        NotificationLockVisibility::class.java,
    )
}
