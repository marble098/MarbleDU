package ir.marbledu.datausage

import ir.marbledu.datausage.data.AppUsage
import ir.marbledu.datausage.data.BackgroundDrainAnalyzer
import org.junit.Assert.*
import org.junit.Test

class BackgroundDrainAnalyzerTest {

    private fun usage(
        uid: Int,
        pkg: String,
        label: String,
        bytes: Long,
        fg: Long,
        bg: Long,
    ) = AppUsage(
        uid = uid,
        packageName = pkg,
        label = label,
        bytes = bytes,
        firstTimestamp = 1_000L,
        lastTimestamp = 2_000L,
        foregroundBytes = fg,
        backgroundBytes = bg,
    )

    @Test fun mergesMobileAndWifiAndRanksBackgroundDrainers() {
        val mb = 1024L * 1024L
        val mobile = listOf(
            usage(10101, "com.social.app", "Social", 100 * mb, fg = 20 * mb, bg = 80 * mb),
            usage(10102, "com.video.app", "Video", 200 * mb, fg = 190 * mb, bg = 10 * mb),
            usage(1000, "uid:1000", "Android OS", 50 * mb, fg = 0L, bg = 50 * mb),
        )
        val wifi = listOf(
            usage(10101, "com.social.app", "Social", 60 * mb, fg = 40 * mb, bg = 20 * mb),
        )

        val summary = BackgroundDrainAnalyzer.summarize(mobile, wifi, limit = 5, minBackgroundBytes = mb)

        assertEquals(410 * mb, summary.totalBytes)
        assertEquals(250 * mb, summary.foregroundBytes)
        assertEquals(160 * mb, summary.backgroundBytes)
        assertEquals(140 * mb, summary.mobileBackgroundBytes)
        assertEquals(20 * mb, summary.wifiBackgroundBytes)
        assertEquals(39, summary.backgroundSharePercent)

        assertEquals(3, summary.topDrainers.size)
        assertEquals("com.social.app", summary.topDrainers[0].packageName)
        assertEquals(100 * mb, summary.topDrainers[0].backgroundBytes)
        assertEquals(62, summary.topDrainers[0].backgroundSharePercent)
        assertEquals(80, summary.topDrainers[0].mobileBackgroundSharePercent)
        assertTrue(summary.topDrainers[0].isRestrictableApp)

        assertEquals("uid:1000", summary.topDrainers[1].packageName)
        assertFalse(summary.topDrainers[1].isRestrictableApp)
    }

    @Test fun handlesEmptyAndUnknownBucketStatesSafely() {
        val empty = BackgroundDrainAnalyzer.summarize(emptyList(), emptyList())
        assertEquals(0L, empty.totalBytes)
        assertEquals(0, empty.backgroundSharePercent)
        assertTrue(empty.topDrainers.isEmpty())

        // وقتی سیستم فقط مجموع کل بایت را می‌دهد و STATE_FOREGROUND/DEFAULT ندارد:
        val unknownOnly = BackgroundDrainAnalyzer.summarize(
            mobile = listOf(usage(10105, "com.browser", "Browser", 5_000_000L, fg = 0L, bg = 0L)),
            wifi = emptyList(),
        )
        assertEquals(5_000_000L, unknownOnly.totalBytes)
        assertEquals(0, unknownOnly.backgroundSharePercent)
        assertTrue(unknownOnly.topDrainers.isEmpty())
    }
}
