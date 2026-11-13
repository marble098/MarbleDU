package ir.marbledu.datausage.data

import java.util.Calendar
import java.util.TimeZone

/**
 * محاسبات بازه‌ی زمانی به‌صورت خالص (بدون وابستگی به اندروید) تا هم در اپ و هم
 * در تست‌های واحد استفاده شود.
 *
 * «امروز» یعنی از نیمه‌شب، نه ۲۴ ساعت گذشته؛ «دوره‌ی صورتحساب» یعنی از
 * روزی که کاربر در تنظیمات انتخاب کرده (و نه همیشه ۳۰ روز قبل).
 */
object TimeMath {

    fun dayStart(nowMillis: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(zone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /** شروع آخرین دوره‌ی صورتحسابِ آغازشده قبل/در همین لحظه. */
    fun cycleStart(
        nowMillis: Long,
        billingDay: Int,
        zone: TimeZone = TimeZone.getDefault(),
    ): Long {
        val cal = Calendar.getInstance(zone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // فقط ۱..۲۸ مجاز است تا ماه‌های کوتاه همیشه امن باشند.
            set(Calendar.DAY_OF_MONTH, billingDay.coerceIn(1, 28))
        }
        if (cal.timeInMillis > nowMillis) cal.add(Calendar.MONTH, -1)
        return cal.timeInMillis
    }

    fun rangeStart(
        range: TimeRange,
        nowMillis: Long,
        billingDay: Int = 1,
        zone: TimeZone = TimeZone.getDefault(),
    ): Long = when (range) {
        TimeRange.TODAY -> dayStart(nowMillis, zone)
        TimeRange.CYCLE -> cycleStart(nowMillis, billingDay, zone)
        else -> nowMillis - range.millis()
    }
}
