package ir.marbledu.datausage.data

import java.util.Calendar
import java.util.TimeZone

/**
 * منطق خالص بازه‌ی «ساعات رایگان / شبانه» (مثل ۲ تا ۷ بامداد).
 *
 *  - از بازه‌های مستقیم (مثلاً ۲ تا ۷) و بازه‌های عبوری از نیمه‌شب (مثلاً ۲۳ تا ۶) پشتیبانی می‌کند؛
 *  - ساعات خارج از بازه به‌عنوان «ساعات اصلی» و داخل بازه به‌عنوان «ساعات رایگان / شبانه» شمرده می‌شوند؛
 *  - برای محاسبه‌ی سریع روی بازه‌های چندروزه، فقط برش‌های زمانیِ دقیقِ همان بازه در هر روز تولید می‌شوند.
 */
object OffPeakWindow {

    data class Preset(
        val startHour: Int,
        val endHour: Int,
        val fa: String,
        val en: String,
    )

    val presets: List<Preset> = listOf(
        Preset(2, 7, "۲ تا ۷ بامداد", "02:00 – 07:00"),
        Preset(1, 6, "۱ تا ۶ بامداد", "01:00 – 06:00"),
        Preset(6, 12, "۶ تا ۱۲ صبح", "06:00 – 12:00"),
        Preset(23, 6, "۲۳ تا ۶ بامداد", "23:00 – 06:00"),
    )

    data class SplitResult(
        val mainMobileBytes: Long = 0L,
        val offPeakMobileBytes: Long = 0L,
        val mainWifiBytes: Long = 0L,
        val offPeakWifiBytes: Long = 0L,
    ) {
        val mainTotalBytes: Long get() = mainMobileBytes + mainWifiBytes
        val offPeakTotalBytes: Long get() = offPeakMobileBytes + offPeakWifiBytes
        val totalBytes: Long get() = mainTotalBytes + offPeakTotalBytes

        val offPeakSharePercent: Int
            get() = if (totalBytes <= 0L) 0
            else ((offPeakTotalBytes * 100.0) / totalBytes).toInt().coerceIn(0, 100)

        val offPeakMobileSharePercent: Int
            get() {
                val mobileTotal = mainMobileBytes + offPeakMobileBytes
                return if (mobileTotal <= 0L) 0
                else ((offPeakMobileBytes * 100.0) / mobileTotal).toInt().coerceIn(0, 100)
            }
    }

    fun isValidWindow(startHour: Int, endHour: Int): Boolean =
        startHour in 0..23 && endHour in 0..23 && startHour != endHour

    fun containsHour(hour: Int, startHour: Int, endHour: Int): Boolean {
        if (hour !in 0..23 || !isValidWindow(startHour, endHour)) return false
        return if (startHour < endHour) {
            hour in startHour until endHour
        } else {
            hour >= startHour || hour < endHour
        }
    }

    fun splitHours(hours: List<HourTraffic>, startHour: Int, endHour: Int): SplitResult {
        if (!isValidWindow(startHour, endHour)) {
            return SplitResult(
                mainMobileBytes = hours.sumOf { it.mobile.total },
                mainWifiBytes = hours.sumOf { it.wifi.total },
            )
        }
        var mainMob = 0L
        var offMob = 0L
        var mainWifi = 0L
        var offWifi = 0L
        hours.forEach { h ->
            if (containsHour(h.hour, startHour, endHour)) {
                offMob += h.mobile.total
                offWifi += h.wifi.total
            } else {
                mainMob += h.mobile.total
                mainWifi += h.wifi.total
            }
        }
        return SplitResult(
            mainMobileBytes = mainMob,
            offPeakMobileBytes = offMob,
            mainWifiBytes = mainWifi,
            offPeakWifiBytes = offWifi,
        )
    }

    /**
     * برش‌های زمانی دقیقِ داخل بازه‌ی [startHour, endHour) را در طول [fromMillis, toMillis)
     * برمی‌گرداند؛ با این روش برای یک ماه هم حداکثر ۳۰ تا ۶۰ برش کافی است.
     */
    fun rangeSlices(
        fromMillis: Long,
        toMillis: Long,
        startHour: Int,
        endHour: Int,
        zone: TimeZone = TimeZone.getDefault(),
    ): List<Pair<Long, Long>> {
        if (!isValidWindow(startHour, endHour) || toMillis <= fromMillis) return emptyList()
        val cal = Calendar.getInstance(zone).apply {
            timeInMillis = fromMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val out = ArrayList<Pair<Long, Long>>()
        var guard = 0
        while (cal.timeInMillis < toMillis && guard < 65) {
            guard++
            val dayStart = cal.timeInMillis
            fun hourAt(hour: Int): Long = (cal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, hour)
            }.timeInMillis

            cal.add(Calendar.DAY_OF_MONTH, 1)
            val nextDayStart = cal.timeInMillis

            fun addClamped(rawStart: Long, rawEnd: Long) {
                val s = maxOf(rawStart, fromMillis)
                val e = minOf(rawEnd, toMillis)
                if (e > s) out.add(s to e)
            }

            if (startHour < endHour) {
                addClamped(hourAt(startHour), hourAt(endHour))
            } else {
                // بازه‌ی عبوری از نیمه‌شب در دو تکه‌ی همان روز: [00:00, endHour) و [startHour, 24:00)
                addClamped(dayStart, hourAt(endHour))
                addClamped(hourAt(startHour), nextDayStart)
            }
        }
        return out
    }

    fun windowLabel(startHour: Int, endHour: Int, persian: Boolean): String {
        val raw = "%02d:00 – %02d:00".format(
            java.util.Locale.US,
            startHour.coerceIn(0, 23),
            endHour.coerceIn(0, 23),
        )
        return if (persian) Formats.toPersianDigits(raw) else raw
    }

    fun hourText(hour: Int, persian: Boolean): String {
        val raw = "%02d:00".format(java.util.Locale.US, hour.coerceIn(0, 23))
        return if (persian) Formats.toPersianDigits(raw) else raw
    }
}
