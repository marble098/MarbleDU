package ir.marbledu.datausage

import ir.marbledu.datausage.data.ConnectionType
import ir.marbledu.datausage.data.IspResolver
import ir.marbledu.datausage.data.NotificationConfig
import ir.marbledu.datausage.data.NotificationPreset
import ir.marbledu.datausage.data.NotificationProgressMode
import ir.marbledu.datausage.data.NotificationStyle
import ir.marbledu.datausage.data.NotificationTitleMode
import ir.marbledu.datausage.service.NotificationFormatter
import ir.marbledu.datausage.service.NotificationInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationFormatterTest {

    private fun sampleInput(
        operatorName: String = "ایرانسل",
        connectionType: ConnectionType = ConnectionType.WIFI,
        down: Double = 2.0 * 1024 * 1024,
        up: Double = 512.0 * 1024,
    ) = NotificationInput(
        downBytesPerSec = down,
        upBytesPerSec = up,
        averageSpeedBps = 1024.0 * 1024,
        peakSpeedBps = 4.0 * 1024 * 1024,
        operatorName = operatorName,
        connectionType = connectionType,
        wifiSourceKind = IspResolver.WifiSourceKind.HOTSPOT_IPHONE,
        wifiStatus = IspResolver.LookupStatus.RESOLVED,
        mobileTech = "",
        vpnActive = true,
        ipAddress = "172.20.10.2",
        publicIp = "5.112.42.10",
        todayMobileBytes = 300L * 1024 * 1024,
        todayWifiBytes = 700L * 1024 * 1024,
        sessionRxBytes = 100L * 1024 * 1024,
        sessionTxBytes = 20L * 1024 * 1024,
        sessionStartMillis = 1_700_000_000_000L,
        capTotalBytes = 10L * 1024 * 1024 * 1024,
        capUsedBytes = 4L * 1024 * 1024 * 1024,
        topAppName = "تلگرام",
        topAppBytes = 250L * 1024 * 1024,
    )

    @Test
    fun persian_notification_uses_rtl_mark_and_persian_digits() {
        val out = NotificationFormatter.format(
            input = sampleInput(),
            config = NotificationConfig.fromPreset(NotificationPreset.FULL_DASHBOARD),
            persian = true,
        )
        assertTrue(out.title.startsWith("\u200F"))
        assertTrue(out.title.contains("↓"))
        assertTrue(out.title.contains("↑"))
        assertFalse(out.title.any { it in '0'..'9' })
        assertTrue(out.summaryText.contains("ایرانسل"))
        assertTrue(out.summaryText.contains("هات‌اسپات آیفون"))
        assertTrue(out.summaryText.contains("VPN"))
        assertTrue(out.expandedLines.any { it.contains("تلگرام") })
        assertTrue(out.expandedLines.any { it.contains("بسته:") })
        assertEquals(40, out.progressPercent)
    }

    @Test
    fun avoids_duplicate_wifi_label_when_operator_unresolved() {
        val out = NotificationFormatter.format(
            input = sampleInput(operatorName = "وای‌فای", connectionType = ConnectionType.WIFI).copy(
                wifiStatus = IspResolver.LookupStatus.FAILED,
                wifiSourceKind = IspResolver.WifiSourceKind.MODEM_ROUTER,
            ),
            config = NotificationConfig(style = NotificationStyle.BALANCED, titleMode = NotificationTitleMode.OPERATOR),
            persian = true,
        )
        assertFalse(out.title.contains("وای‌فای • وای‌فای"))
    }

    @Test
    fun minimal_speed_preset_is_compact_and_hides_zero_speed() {
        val out = NotificationFormatter.format(
            input = sampleInput(down = 100.0, up = 50.0),
            config = NotificationConfig.fromPreset(NotificationPreset.MINIMAL_SPEED),
            persian = true,
        )
        assertTrue(out.title.contains("بدون ترافیک"))
        assertTrue(out.expandedLines.isEmpty())
        assertNull(out.progressPercent)
    }

    @Test
    fun custom_template_substitutes_all_tokens() {
        val cfg = NotificationConfig(
            style = NotificationStyle.BALANCED,
            titleMode = NotificationTitleMode.CUSTOM,
            customTitle = "{operator} | {down} | امروز {today} | مانده {cap}",
            progressMode = NotificationProgressMode.MOBILE_SHARE,
        )
        val out = NotificationFormatter.format(sampleInput(), cfg, persian = false)
        assertTrue(out.title.contains("ایرانسل"))
        assertTrue(out.title.contains("2.0 MB/s") || out.title.contains("2 MB/s"))
        assertEquals(30, out.progressPercent)
    }

    @Test
    fun english_notification_has_no_rtl_or_persian_words() {
        val out = NotificationFormatter.format(
            input = sampleInput(operatorName = "Irancell").copy(topAppName = "Telegram"),
            config = NotificationConfig.fromPreset(NotificationPreset.FULL_DASHBOARD),
            persian = false,
        )
        assertFalse(out.title.startsWith("\u200F"))
        assertTrue(out.summaryText.contains("Irancell"))
        assertTrue(out.summaryText.contains("Today:"))
        assertTrue(out.expandedLines.any { it.contains("Top app:") })
        assertNotNull(out.subText)
    }

    @Test
    fun `body drops the line that only repeats the title`() {
        val rlm = "\u200F"
        val title = "$rlm↓ ۲٫۴ م.ب/ث   ↑ ۳۱۰ ک.ب/ث"
        val body = "$rlm↓ ۲٫۴ م.ب/ث   ↑ ۳۱۰ ک.ب/ث\n${rlm}امروز: ۱٫۲ گ.ب\n\n${rlm}بسته: ۱۸ گ.ب مانده (۴۰٪)"
        val lines = NotificationFormatter.bodyLinesWithoutTitle(body, title)
        assertEquals(listOf("${rlm}امروز: ۱٫۲ گ.ب", "${rlm}بسته: ۱۸ گ.ب مانده (۴۰٪)"), lines)
        assertTrue(NotificationFormatter.bodyLinesWithoutTitle(null, title).isEmpty())
        assertEquals(listOf("Today: 1.2 GB"), NotificationFormatter.bodyLinesWithoutTitle("Today: 1.2 GB", "Idle"))
    }
}
