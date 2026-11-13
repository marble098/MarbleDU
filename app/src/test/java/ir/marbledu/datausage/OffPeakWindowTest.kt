package ir.marbledu.datausage

import ir.marbledu.datausage.data.HourTraffic
import ir.marbledu.datausage.data.OffPeakWindow
import ir.marbledu.datausage.data.Traffic
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class OffPeakWindowTest {

    @Test fun validatesAndChecksDirectAndOvernightWindows() {
        assertFalse(OffPeakWindow.isValidWindow(2, 2))
        assertFalse(OffPeakWindow.isValidWindow(-1, 7))
        assertTrue(OffPeakWindow.isValidWindow(2, 7))
        assertTrue(OffPeakWindow.isValidWindow(23, 6))

        // بازه‌ی ۲ تا ۷ بامداد: ۲..۶ داخل، ۷ بیرون
        assertFalse(OffPeakWindow.containsHour(1, 2, 7))
        assertTrue(OffPeakWindow.containsHour(2, 2, 7))
        assertTrue(OffPeakWindow.containsHour(6, 2, 7))
        assertFalse(OffPeakWindow.containsHour(7, 2, 7))

        // بازه‌ی عبوری از نیمه‌شب (۲۳ تا ۶): ۲۳، ۰، ۵ داخل؛ ۶ و ۲۲ بیرون
        assertFalse(OffPeakWindow.containsHour(22, 23, 6))
        assertTrue(OffPeakWindow.containsHour(23, 23, 6))
        assertTrue(OffPeakWindow.containsHour(0, 23, 6))
        assertTrue(OffPeakWindow.containsHour(5, 23, 6))
        assertFalse(OffPeakWindow.containsHour(6, 23, 6))
    }

    @Test fun splitsDailyHoursBetweenMainAndOffPeak() {
        val hours = (0..23).map { hour ->
            HourTraffic(
                hour = hour,
                mobile = Traffic(rx = if (hour == 3) 400L else 10L, tx = 0L),
                wifi = Traffic(rx = if (hour == 15) 100L else 0L, tx = 0L),
            )
        }
        val split = OffPeakWindow.splitHours(hours, startHour = 2, endHour = 7)
        // ساعت ۲، ۳، ۴، ۵، ۶ => 400 + 4*10 = 440 موبایل شبانه
        assertEquals(440L, split.offPeakMobileBytes)
        // ۱۹ ساعت باقی‌مانده × ۱۰ = ۱۹۰ موبایل عادی
        assertEquals(190L, split.mainMobileBytes)
        assertEquals(100L, split.mainWifiBytes)
        assertEquals(0L, split.offPeakWifiBytes)
        assertEquals(730L, split.totalBytes)
        assertEquals(60, split.offPeakSharePercent)
    }

    @Test fun generatesExactSlicesForDirectAndMidnightWrapRanges() {
        val utc = TimeZone.getTimeZone("UTC")
        fun utcMillis(day: Int, hour: Int): Long = Calendar.getInstance(utc).apply {
            set(2026, Calendar.NOVEMBER, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // دو روز کامل با بازه‌ی ۲ تا ۷
        val directSlices = OffPeakWindow.rangeSlices(
            fromMillis = utcMillis(10, 0),
            toMillis = utcMillis(12, 0),
            startHour = 2,
            endHour = 7,
            zone = utc,
        )
        assertEquals(2, directSlices.size)
        assertEquals(utcMillis(10, 2) to utcMillis(10, 7), directSlices[0])
        assertEquals(utcMillis(11, 2) to utcMillis(11, 7), directSlices[1])

        // یک روز با بازه‌ی عبوری از نیمه‌شب (۲۳ تا ۶)
        val wrapSlices = OffPeakWindow.rangeSlices(
            fromMillis = utcMillis(10, 0),
            toMillis = utcMillis(11, 0),
            startHour = 23,
            endHour = 6,
            zone = utc,
        )
        assertEquals(2, wrapSlices.size)
        assertEquals(utcMillis(10, 0) to utcMillis(10, 6), wrapSlices[0])
        assertEquals(utcMillis(10, 23) to utcMillis(11, 0), wrapSlices[1])
    }
}
