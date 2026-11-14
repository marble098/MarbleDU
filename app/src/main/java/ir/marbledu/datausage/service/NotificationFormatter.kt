package ir.marbledu.datausage.service

import ir.marbledu.datausage.data.ConnectionType
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.IspResolver
import ir.marbledu.datausage.data.LiveMonitor
import ir.marbledu.datausage.data.NotificationConfig
import ir.marbledu.datausage.data.NotificationProgressMode
import ir.marbledu.datausage.data.NotificationStyle
import ir.marbledu.datausage.data.NotificationTitleMode

data class NotificationInput(
    val downBytesPerSec: Double = 0.0,
    val upBytesPerSec: Double = 0.0,
    val averageSpeedBps: Double = 0.0,
    val peakSpeedBps: Double = 0.0,
    val operatorName: String = "",
    val connectionType: ConnectionType = ConnectionType.OFFLINE,
    val wifiSourceKind: IspResolver.WifiSourceKind = IspResolver.WifiSourceKind.UNKNOWN,
    val wifiStatus: IspResolver.LookupStatus = IspResolver.LookupStatus.IDLE,
    val mobileTech: String = "",
    val vpnActive: Boolean = false,
    val ipAddress: String = "",
    val publicIp: String = "",
    val todayMobileBytes: Long = 0L,
    val todayWifiBytes: Long = 0L,
    val sessionRxBytes: Long = 0L,
    val sessionTxBytes: Long = 0L,
    val sessionStartMillis: Long = 0L,
    val capTotalBytes: Long = 0L,
    val capUsedBytes: Long = 0L,
    val topAppName: String = "",
    val topAppBytes: Long = 0L,
)

data class NotificationContent(
    val title: String,
    val summaryText: String,
    val expandedText: String,
    val expandedLines: List<String>,
    val subText: String?,
    val progressPercent: Int?,
)

/**
 * قالب‌ساز خالص برای اعلان پایش و پیش‌نمایش زنده در تنظیمات.
 * تمام متن‌ها بر اساس زبان اپ (فارسی با نشانگر راست‌به‌چپ `\u200F` یا انگلیسی) تولید می‌شوند.
 */
object NotificationFormatter {

    private const val RLM = "\u200F"

    fun format(
        input: NotificationInput,
        config: NotificationConfig,
        persian: Boolean,
        appMonitoringTitle: String = if (persian) "MarbleDU در حال پایش است" else "MarbleDU is monitoring",
    ): NotificationContent {
        fun rtl(s: String): String = if (persian && s.isNotEmpty() && !s.startsWith(RLM)) "$RLM$s" else s
        fun fmtSpeed(bps: Double): String =
            if (config.compactUnits) Formats.compactSpeed(bps, persian) else Formats.speed(bps, persian)

        fun fmtBytes(bytes: Long): String =
            if (config.compactUnits) Formats.compactBytes(bytes, persian) else Formats.bytes(bytes, persian)

        val totalSpeed = input.downBytesPerSec + input.upBytesPerSec
        val isIdle = config.hideZeroSpeed &&
            input.downBytesPerSec < 512.0 &&
            input.upBytesPerSec < 512.0

        val speedParts = buildList {
            if (config.showDownloadSpeed) add("↓ ${fmtSpeed(input.downBytesPerSec)}")
            if (config.showUploadSpeed) add("↑ ${fmtSpeed(input.upBytesPerSec)}")
            if (config.showTotalSpeed) add("⇅ ${fmtSpeed(totalSpeed)}")
        }
        val speedText = when {
            speedParts.isEmpty() -> ""
            isIdle -> if (persian) "بدون ترافیک" else "Idle"
            else -> speedParts.joinToString("   ")
        }

        val connLabel = connectionLabel(input.connectionType, persian)
        val sourceKindLabel = wifiSourceLabel(input.wifiSourceKind, persian)
        val operatorLabel = resolveOperatorText(input, persian)

        val techParts = buildList {
            if (config.showNetworkTech) {
                if (input.mobileTech.isNotBlank()) add(input.mobileTech)
                if ((input.connectionType == ConnectionType.WIFI || input.connectionType == ConnectionType.ETHERNET) &&
                    sourceKindLabel.isNotBlank()
                ) add(sourceKindLabel)
                if (input.vpnActive) add("VPN")
            }
        }

        val networkSegment = buildString {
            val baseParts = buildList {
                if (config.showOperator && operatorLabel.isNotBlank()) add(operatorLabel)
                if (config.showConnectionType && connLabel.isNotBlank() &&
                    !connLabel.equals(operatorLabel, ignoreCase = true)
                ) {
                    add(connLabel)
                }
            }
            append(baseParts.joinToString(" • "))
            if (techParts.isNotEmpty()) {
                if (isNotEmpty()) append(" ")
                append("(${techParts.joinToString("، ")})")
            }
        }

        val todayTotal = input.todayMobileBytes + input.todayWifiBytes
        val sessionTotal = input.sessionRxBytes + input.sessionTxBytes

        val todayTotalText = if (config.showTodayTotal) {
            if (persian) "امروز: ${fmtBytes(todayTotal)}" else "Today: ${fmtBytes(todayTotal)}"
        } else ""

        val todaySplitText = if (config.showTodayMobileWifi) {
            if (persian) {
                "موبایل: ${fmtBytes(input.todayMobileBytes)} | وای‌فای: ${fmtBytes(input.todayWifiBytes)}"
            } else {
                "Mobile: ${fmtBytes(input.todayMobileBytes)} | Wi-Fi: ${fmtBytes(input.todayWifiBytes)}"
            }
        } else ""

        val sessionText = if (config.showSessionUsage) {
            val base = if (persian) "نشست: ${fmtBytes(sessionTotal)}" else "Session: ${fmtBytes(sessionTotal)}"
            if (input.sessionStartMillis > 0L) {
                val time = LiveMonitor.formatHourMinute(input.sessionStartMillis, persian)
                if (persian) "$base (از $time)" else "$base (from $time)"
            } else base
        } else ""

        val avgPeakParts = buildList {
            if (config.showAverageSpeed) {
                add(if (persian) "میانگین: ${fmtSpeed(input.averageSpeedBps)}" else "Avg: ${fmtSpeed(input.averageSpeedBps)}")
            }
            if (config.showPeakSpeed) {
                add(if (persian) "اوج: ${fmtSpeed(input.peakSpeedBps)}" else "Peak: ${fmtSpeed(input.peakSpeedBps)}")
            }
        }
        val avgPeakText = avgPeakParts.joinToString(" • ")

        val capText = if (config.showDataCap && input.capTotalBytes > 0L) {
            val remaining = (input.capTotalBytes - input.capUsedBytes).coerceAtLeast(0L)
            val usedPct = (Formats.percentOf(input.capUsedBytes, input.capTotalBytes) * 100f).toInt()
            val pctLabel = "${Formats.integer(usedPct.toLong(), persian)}${if (persian) "٪" else "%"}"
            if (persian) "بسته: ${fmtBytes(remaining)} مانده ($pctLabel مصرف)"
            else "Plan: ${fmtBytes(remaining)} left ($pctLabel used)"
        } else ""

        val topAppText = if (config.showTopApp && input.topAppName.isNotBlank() && input.topAppBytes > 0L) {
            if (persian) "پرمصرف‌ترین: ${input.topAppName} (${fmtBytes(input.topAppBytes)})"
            else "Top app: ${input.topAppName} (${fmtBytes(input.topAppBytes)})"
        } else ""

        val ipValue = input.publicIp.ifBlank { input.ipAddress }
        val ipText = if (config.showIpAddress && ipValue.isNotBlank()) {
            if (persian) "آی‌پی: $ipValue" else "IP: $ipValue"
        } else ""

        // عنوان اصلی
        val rawTitle = when (config.titleMode) {
            NotificationTitleMode.SPEED -> speedText.ifBlank { appMonitoringTitle }
            NotificationTitleMode.OPERATOR -> {
                val opParts = listOfNotNull(
                    operatorLabel.takeIf { it.isNotBlank() },
                    connLabel.takeIf { it.isNotBlank() && !it.equals(operatorLabel, ignoreCase = true) },
                )
                val base = opParts.joinToString(" • ").ifBlank { appMonitoringTitle }
                if (techParts.isNotEmpty()) "$base (${techParts.joinToString("، ")})" else base
            }
            NotificationTitleMode.TODAY_USAGE ->
                if (persian) "مصرف امروز: ${fmtBytes(todayTotal)}" else "Today's usage: ${fmtBytes(todayTotal)}"
            NotificationTitleMode.APP_NAME -> appMonitoringTitle
            NotificationTitleMode.CUSTOM -> renderCustomTitle(
                template = config.customTitle,
                fallback = appMonitoringTitle,
                speed = speedText.ifBlank { fmtSpeed(totalSpeed) },
                down = fmtSpeed(input.downBytesPerSec),
                up = fmtSpeed(input.upBytesPerSec),
                operator = operatorLabel.ifBlank { connLabel },
                network = connLabel,
                today = fmtBytes(todayTotal),
                mobile = fmtBytes(input.todayMobileBytes),
                wifi = fmtBytes(input.todayWifiBytes),
                session = fmtBytes(sessionTotal),
                cap = if (input.capTotalBytes > 0L) {
                    fmtBytes((input.capTotalBytes - input.capUsedBytes).coerceAtLeast(0L))
                } else "—",
            )
        }

        // خط خلاصه (حالت بسته) - از تکرار چیزی که در عنوان آمده جلوگیری می‌کنیم
        val summaryParts = buildList {
            if (config.titleMode != NotificationTitleMode.SPEED && speedText.isNotBlank()) {
                add(speedText)
            }
            if (config.titleMode != NotificationTitleMode.OPERATOR && networkSegment.isNotBlank()) {
                add(networkSegment)
            }
            if (config.style != NotificationStyle.COMPACT) {
                if (config.titleMode != NotificationTitleMode.TODAY_USAGE && todayTotalText.isNotBlank()) {
                    add(todayTotalText)
                } else if (config.showTodayMobileWifi && todaySplitText.isNotBlank()) {
                    add(todaySplitText)
                }
                if (capText.isNotBlank()) add(capText)
                else if (sessionText.isNotBlank()) add(sessionText)
            } else {
                // در حالت فشرده حداکثر یک مورد مکمل اضافه می‌کنیم
                if (config.titleMode != NotificationTitleMode.TODAY_USAGE && todayTotalText.isNotBlank()) {
                    add(todayTotalText)
                } else if (capText.isNotBlank()) {
                    add(capText)
                }
            }
        }
        val rawSummary = summaryParts.joinToString(" • ").ifBlank {
            listOf(speedText, networkSegment, todayTotalText).firstOrNull { it.isNotBlank() } ?: appMonitoringTitle
        }

        // خطوط تفصیلی (فقط برای حالت بازشونده / EXPANDED)
        val expandedLines = if (config.style == NotificationStyle.EXPANDED) {
            buildList {
                if (speedText.isNotBlank() && config.titleMode != NotificationTitleMode.SPEED) {
                    add(if (persian) "سرعت: $speedText" else "Speed: $speedText")
                }
                if (networkSegment.isNotBlank() && config.titleMode != NotificationTitleMode.OPERATOR) {
                    add(if (persian) "شبکه: $networkSegment" else "Network: $networkSegment")
                }
                if (todayTotalText.isNotBlank()) add(todayTotalText)
                if (todaySplitText.isNotBlank()) add(todaySplitText)
                if (sessionText.isNotBlank()) add(sessionText)
                if (avgPeakText.isNotBlank()) add(avgPeakText)
                if (capText.isNotBlank()) add(capText)
                if (topAppText.isNotBlank()) add(topAppText)
                if (ipText.isNotBlank()) add(ipText)
            }.map(::rtl)
        } else {
            emptyList()
        }

        // زیرعنوان بالای اعلان
        val subText = when {
            config.style == NotificationStyle.COMPACT -> null
            config.titleMode == NotificationTitleMode.SPEED && config.showOperator && operatorLabel.isNotBlank() ->
                rtl(if (input.vpnActive) "$operatorLabel • VPN" else operatorLabel)
            input.vpnActive && config.showNetworkTech -> rtl("VPN")
            else -> null
        }

        val progress = when (config.progressMode) {
            NotificationProgressMode.NONE -> null
            NotificationProgressMode.DATA_CAP -> if (input.capTotalBytes > 0L) {
                (Formats.percentOf(input.capUsedBytes, input.capTotalBytes) * 100f).toInt().coerceIn(0, 100)
            } else null
            NotificationProgressMode.MOBILE_SHARE -> if (todayTotal > 0L) {
                ((input.todayMobileBytes.toDouble() / todayTotal) * 100.0).toInt().coerceIn(0, 100)
            } else null
        }

        return NotificationContent(
            title = rtl(rawTitle),
            summaryText = rtl(rawSummary),
            expandedText = expandedLines.joinToString("\n"),
            expandedLines = expandedLines,
            subText = subText,
            progressPercent = progress,
        )
    }

    /**
     * خطوط تفصیلی اعلان بدون خطی که فقط عنوان را تکرار می‌کند.
     *
     * در حالت پیش‌فرض (عنوان = سرعت) اولین خط بدنه هم همان سرعت است؛ در کارت اعلان
     * عنوان کنار لوگو دیده می‌شود و تکرارش زیر آن فقط جا می‌گیرد. نشانگر راست‌به‌چپ و
     * فاصله‌های اضافه در مقایسه نادیده گرفته می‌شوند.
     */
    fun bodyLinesWithoutTitle(body: CharSequence?, title: CharSequence): List<String> {
        fun clean(s: CharSequence) = s.toString().replace(RLM, "").trim()
        val cleanTitle = clean(title)
        return body?.toString().orEmpty()
            .split("\n")
            .filter { line -> clean(line).isNotEmpty() && clean(line) != cleanTitle }
    }

    private fun resolveOperatorText(input: NotificationInput, persian: Boolean): String {
        if (input.connectionType == ConnectionType.OFFLINE) {
            return if (persian) "آفلاین" else "Offline"
        }
        val onWifi = input.connectionType == ConnectionType.WIFI || input.connectionType == ConnectionType.ETHERNET
        if (onWifi && input.wifiStatus == IspResolver.LookupStatus.SEARCHING && input.operatorName.isBlank()) {
            return if (persian) "در حال شناسایی…" else "Detecting…"
        }
        if (onWifi && input.wifiStatus == IspResolver.LookupStatus.SEARCHING &&
            (input.operatorName == "وای‌فای" || input.operatorName == "Wi-Fi")
        ) {
            return if (persian) "در حال شناسایی اپراتور…" else "Detecting ISP…"
        }
        return input.operatorName
    }

    private fun connectionLabel(type: ConnectionType, persian: Boolean): String = when (type) {
        ConnectionType.WIFI -> if (persian) "وای‌فای" else "Wi-Fi"
        ConnectionType.CELLULAR -> if (persian) "داده موبایل" else "Mobile data"
        ConnectionType.ETHERNET -> if (persian) "اترنت" else "Ethernet"
        ConnectionType.VPN -> "VPN"
        ConnectionType.OFFLINE -> if (persian) "بدون اتصال" else "Offline"
        ConnectionType.OTHER -> if (persian) "شبکه" else "Network"
    }

    fun wifiSourceLabel(kind: IspResolver.WifiSourceKind, persian: Boolean): String = when (kind) {
        IspResolver.WifiSourceKind.HOTSPOT_IPHONE -> if (persian) "هات‌اسپات آیفون" else "iPhone Hotspot"
        IspResolver.WifiSourceKind.HOTSPOT_ANDROID -> if (persian) "هات‌اسپات اندروید" else "Android Hotspot"
        IspResolver.WifiSourceKind.HOTSPOT_WINDOWS -> if (persian) "هات‌اسپات" else "Hotspot"
        IspResolver.WifiSourceKind.HOTSPOT_METERED -> if (persian) "هات‌اسپات" else "Hotspot"
        IspResolver.WifiSourceKind.MODEM_ROUTER -> if (persian) "مودم" else "Router"
        IspResolver.WifiSourceKind.ETHERNET -> if (persian) "اترنت" else "Ethernet"
        IspResolver.WifiSourceKind.UNKNOWN -> ""
    }

    private fun renderCustomTitle(
        template: String,
        fallback: String,
        speed: String,
        down: String,
        up: String,
        operator: String,
        network: String,
        today: String,
        mobile: String,
        wifi: String,
        session: String,
        cap: String,
    ): String {
        val clean = template.trim()
        if (clean.isBlank()) return fallback
        return clean
            .replace("{speed}", speed)
            .replace("{down}", down)
            .replace("{up}", up)
            .replace("{operator}", operator)
            .replace("{network}", network)
            .replace("{today}", today)
            .replace("{mobile}", mobile)
            .replace("{wifi}", wifi)
            .replace("{session}", session)
            .replace("{cap}", cap)
    }
}
