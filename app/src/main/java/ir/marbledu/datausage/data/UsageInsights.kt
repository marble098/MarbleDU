package ir.marbledu.datausage.data

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * «عینکِ مصرف» — تحلیل خالص و قابل آزمون از الگوی مصرف روزانه.
 *
 * ورودی‌ها فقط اعداد خام‌اند (بدون وابستگی به اندروید) تا هم در واحدهای
 * تست و هم در ویجت و صفحه‌ی خانه قابل استفاده باشند.
 *
 * سه خروجی اصلی:
 *  - [build] : خلاصه‌ی الگو (سنگین‌ترین روز هفته، ساعت اوج، میانگین)
 *  - [classify] : نوع مصرف امروز (عادی / سنگین / سبک) نسبت به روزهای قبل
 *  - [remainingAtPace] : با همین سرعت، سقف بسته چند روز دیگر پر می‌شود
 */
object UsageInsights {

    /** یک روز از تاریخچه: شروع روز + مصرف موبایل و وای‌فای. */
    data class Day(val dayStart: Long, val mobileBytes: Long, val wifiBytes: Long) {
        val total: Long get() = mobileBytes + wifiBytes
    }

    data class Snapshot(
        /** میانگین مصرف کل روز در روزهای دارای داده. */
        val averageDailyBytes: Long,
        /** میانگین مصرف موبایل روزانه. */
        val averageMobileBytes: Long,
        /** سنگین‌ترین روز هفته (۰ = یکشنبه … ۶ = شنبه) و میانگین مصرف همان روز. */
        val heaviestWeekday: Int?,
        val heaviestWeekdayBytes: Long,
        /** سبک‌ترین روز هفته. */
        val lightestWeekday: Int?,
        val lightestWeekdayBytes: Long,
        /** ساعت اوج مصرف (۰ تا ۲۳) در بازه‌ی داده‌شده. */
        val peakHour: Int?,
        val peakHourBytes: Long,
        /** نسبت موبایل به کل مصرف (۰..۱). */
        val mobileShare: Float,
        /** روزهای دارای داده. */
        val activeDays: Int,
        /** بزرگ‌ترین پرش روزانه نسبت به میانگین (درصد). */
        val largestJumpPercent: Int,
    )

    /** تبِ رنگی متناسب با وضعیت امروز. */
    enum class DayVerdict { LIGHT, NORMAL, HEAVY, VERY_HEAVY }

    /**
     * ساخت خلاصه از تاریخچه‌ی روزانه + هیستوگرام ساعتی.
     *
     * @param days حداکثر ۳۰ روز اخیر (قدیمی‌ترین اول).
     * @param hourTotals ۲۴ عدد؛ مصرف هر ساعت شبانه‌روز (جمع چند روز).
     * @param weekdayOf تابع تبدیل روز به شماره‌ی روز هفته (پیش‌فرض: تقویم پیش‌فرض).
     */
    fun build(
        days: List<Day>,
        hourTotals: List<Long> = emptyList(),
        weekdayOf: (Long) -> Int = { millis ->
            java.util.Calendar.getInstance().apply { timeInMillis = millis }
                .get(java.util.Calendar.DAY_OF_WEEK) - 1
        },
    ): Snapshot {
        val active = days.filter { it.total > 0L }
        val totalBytes = active.sumOf { it.total }
        val average = if (active.isEmpty()) 0L else totalBytes / active.size
        val averageMobile = if (active.isEmpty()) 0L else active.sumOf { it.mobileBytes } / active.size
        val mobileSum = active.sumOf { it.mobileBytes }

        val byWeekday = active.groupBy { weekdayOf(it.dayStart) }
        val weekdayAverages = byWeekday.mapValues { (_, list) -> list.sumOf { it.total } / list.size }
        val heaviest = weekdayAverages.maxByOrNull { it.value }
        val lightest = weekdayAverages.minByOrNull { it.value }

        val peakIndex = hourTotals.indices.maxByOrNull { hourTotals[it] }?.takeIf { hourTotals[it] > 0L }

        val jump = if (average <= 0L) 0 else {
            active.maxOfOrNull { day ->
                ((day.total - average) * 100.0 / average).roundToLong()
            }?.coerceAtLeast(0L)?.toInt() ?: 0
        }

        return Snapshot(
            averageDailyBytes = average,
            averageMobileBytes = averageMobile,
            heaviestWeekday = heaviest?.key,
            heaviestWeekdayBytes = heaviest?.value ?: 0L,
            lightestWeekday = lightest?.key,
            lightestWeekdayBytes = lightest?.value ?: 0L,
            peakHour = peakIndex,
            peakHourBytes = peakIndex?.let { hourTotals[it] } ?: 0L,
            mobileShare = if (totalBytes <= 0L) 0f else (mobileSum.toFloat() / totalBytes).coerceIn(0f, 1f),
            activeDays = active.size,
            largestJumpPercent = jump,
        )
    }

    /**
     * وضعیت امروز نسبت به میانگین: کمتر از ۵۰٪ = سبک، تا ۱۳۰٪ = عادی،
     * تا ۲۰۰٪ = سنگین و بیشتر = خیلی سنگین. آستانه‌ها عمداً سخاوتمندانه‌اند
     * تا هشدارِ الکی ندهیم؛ روزِ نیمه‌کاره هم با [dayFraction] مقایسه می‌شود.
     */
    fun classify(
        todayBytes: Long,
        averageDailyBytes: Long,
        dayFraction: Float = 1f,
    ): DayVerdict {
        if (averageDailyBytes <= 0L) return DayVerdict.NORMAL
        val expected = (averageDailyBytes * dayFraction.coerceIn(0.05f, 1f).toDouble())
            .coerceAtLeast(1.0)
        return when {
            todayBytes < expected * 0.5 -> DayVerdict.LIGHT
            todayBytes <= expected * 1.3 -> DayVerdict.NORMAL
            todayBytes <= expected * 2.0 -> DayVerdict.HEAVY
            else -> DayVerdict.VERY_HEAVY
        }
    }

    /** چند روز تا پر شدن سقف با میانگین فعلی؛ ‎-1‎ یعنی مصرفی ثبت نشده. */
    fun daysUntilCap(remainingBytes: Long, averageDailyBytes: Long): Int {
        if (remainingBytes <= 0L) return 0
        if (averageDailyBytes <= 0L) return -1
        val days = (remainingBytes + averageDailyBytes - 1) / averageDailyBytes
        return days.coerceAtMost(3650).toInt()
    }

    /**
     * «بودجه‌ی امروز»: چقدر از امروز می‌توانی مصرف کنی تا با همین روند به
     * پایان دوره برسی — و اگر بیشتر از آن مصرف شود چقدر عقب می‌افتی.
     */
    data class Budget(val todayBytes: Long, val overBudgetBytes: Long, val percentOfBudget: Int)

    fun budget(todayBytes: Long, dailyBudgetBytes: Long): Budget {
        if (dailyBudgetBytes <= 0L) return Budget(todayBytes, 0L, 0)
        val over = (todayBytes - dailyBudgetBytes).coerceAtLeast(0L)
        val percent = ((todayBytes * 100.0) / dailyBudgetBytes).toInt().coerceIn(0, 400)
        return Budget(todayBytes, over, percent)
    }

    /** «امروز چند درصد از میانگین یک روز عادی مصرف شده» (۰..۴۰۰). */
    fun todayPercentOfAverage(todayBytes: Long, averageDailyBytes: Long): Int =
        if (averageDailyBytes <= 0L) 0 else ((todayBytes * 100.0) / averageDailyBytes).toInt().coerceIn(0, 400)

    /** اختلاف نسبی دو عدد به درصد (مثبت = بیشتر شده). */
    fun percentChange(current: Long, previous: Long): Int =
        if (previous <= 0L) 0 else (((current - previous) * 100.0) / previous).toInt()

    /** نام روز هفته به فارسی/انگلیسی از شماره‌ی ۰..۶ (۰ = یکشنبه). */
    fun weekdayName(index: Int, persian: Boolean): String {
        val fa = listOf("یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه")
        val en = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        return if (persian) fa[index.mod(7)] else en[index.mod(7)]
    }

    /** ساعت خوانا؛ در فارسی ارقام فارسی می‌شوند. */
    fun hourLabel(hour: Int, persian: Boolean): String {
        val text = "%02d:00".format(java.util.Locale.US, hour.coerceIn(0, 23))
        return if (persian) Formats.toPersianDigits(text) else text
    }

    /** آیا دو عدد «تقریباً برابر» هستند (برای جلوگیری از نمایش مقایسه‌ی بی‌معنا). */
    fun roughlyEqual(a: Long, b: Long, tolerancePercent: Int = 8): Boolean {
        if (a == 0L && b == 0L) return true
        val base = maxOf(abs(a), abs(b)).coerceAtLeast(1L)
        return abs(a - b) * 100 / base <= tolerancePercent
    }
}
