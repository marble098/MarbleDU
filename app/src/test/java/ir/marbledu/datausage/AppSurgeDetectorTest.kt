package ir.marbledu.datausage

import ir.marbledu.datausage.data.AppSurgeDetector
import ir.marbledu.datausage.data.AppUsage
import org.junit.Assert.*
import org.junit.Test

class AppSurgeDetectorTest {

    private val mb = 1024L * 1024L

    private fun app(uid: Int, pkg: String, label: String, bytes: Long) = AppUsage(
        uid = uid,
        packageName = pkg,
        label = label,
        bytes = bytes,
        firstTimestamp = 10L,
        lastTimestamp = 20L,
    )

    @Test fun flagsLargeMultiplesAndNewSpikesWhileIgnoringNormalUsage() {
        val todayMobile = listOf(
            app(10101, "com.surge.app", "SurgeApp", 150 * mb),
            app(10102, "com.steady.app", "SteadyApp", 60 * mb),
            app(10103, "com.newbig.app", "NewBig", 80 * mb),
            app(10104, "com.newsmall.app", "NewSmall", 25 * mb),
        )
        val todayWifi = listOf(
            app(10101, "com.surge.app", "SurgeApp", 50 * mb), // جمع امروز = ۲۰۰ مگ
        )
        val baselineMobile = listOf(
            // ۷ روز = ۳۵۰ مگ => میانگین روزانه ۵۰ مگ (جهش ۴ برابری، +۱۵۰ مگ)
            app(10101, "com.surge.app", "SurgeApp", 350 * mb),
            // ۷ روز = ۳۵۰ مگ => میانگین روزانه ۵۰ مگ (تنها ۱٫۲ برابر -> عادی)
            app(10102, "com.steady.app", "SteadyApp", 350 * mb),
        )

        val inputs = AppSurgeDetector.buildInputs(
            todayMobile = todayMobile,
            todayWifi = todayWifi,
            baselineMobile = baselineMobile,
            baselineWifi = emptyList(),
        )
        val detected = AppSurgeDetector.detect(inputs, baselineDays = 7)

        assertEquals(2, detected.size)
        assertEquals("com.surge.app", detected[0].packageName)
        assertFalse(detected[0].isNewSpike)
        assertEquals(50 * mb, detected[0].baselineDailyBytes)
        assertEquals(150 * mb, detected[0].extraBytes)
        assertEquals(4.0, detected[0].multiplier, 0.001)

        assertEquals("com.newbig.app", detected[1].packageName)
        assertTrue(detected[1].isNewSpike)
        assertEquals(80 * mb, detected[1].extraBytes)
    }

    @Test fun adjustsBaselineDaysAccurately() {
        val inputs = listOf(
            AppSurgeDetector.Input(
                uid = 10200,
                packageName = "com.update.app",
                label = "Updater",
                todayMobileBytes = 120 * mb,
                todayWifiBytes = 0L,
                baselineMobileBytes = 80 * mb, // اگر ۲ روز باشد: میانگین ۴۰ مگ، ۳×
                baselineWifiBytes = 0L,
            ),
        )
        val twoDays = AppSurgeDetector.detect(inputs, baselineDays = 2)
        assertEquals(1, twoDays.size)
        assertEquals(3.0, twoDays[0].multiplier, 0.001)
    }
}
