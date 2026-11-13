package ir.marbledu.datausage.data

import java.util.Calendar
import java.util.TimeZone

/**
 * تبدیل تاریخ میلادی به شمسی (جلالی) – خالص و بدون وابستگی به اندروید.
 * همه‌ی گزارش‌ها هر دو تاریخ را کنار هم نشان می‌دهند.
 */
data class JalaliDate(val year: Int, val month: Int, val day: Int) {

    fun monthNameFa(): String = MONTHS_FA[(month - 1).coerceIn(0, 11)]
    fun monthNameEn(): String = MONTHS_EN[(month - 1).coerceIn(0, 11)]

    /** مثل ۱۴۰۵/۰۸/۲۲ */
    fun numeric(persianDigits: Boolean): String {
        val text = "%04d/%02d/%02d".format(java.util.Locale.US, year, month, day)
        return if (persianDigits) Formats.toPersianDigits(text) else text
    }

    /** مثل «۲۲ آبان ۱۴۰۵» یا «22 Aban 1405» */
    fun long(persian: Boolean): String =
        if (persian) Formats.toPersianDigits("$day") + " " + monthNameFa() + " " + Formats.toPersianDigits("$year")
        else "$day ${monthNameEn()} $year"

    /** مثل «۲۲ آبان» */
    fun short(persian: Boolean): String =
        if (persian) Formats.toPersianDigits("$day") + " " + monthNameFa()
        else "$day ${monthNameEn()}"

    companion object {
        private val MONTHS_FA = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
        )
        private val MONTHS_EN = listOf(
            "Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar",
            "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand",
        )
        private val GREGORIAN_MONTHS_EN = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )
        private val GREGORIAN_MONTHS_FA = listOf(
            "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
            "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر",
        )
        private val WEEKDAYS_FA = listOf("یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه")
        private val WEEKDAYS_EN = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        private val WEEKDAYS_SHORT_FA = listOf("ی", "د", "س", "چ", "پ", "ج", "ش")
        private val WEEKDAYS_SHORT_EN = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

        /** الگوریتم استاندارد تبدیل میلادی به جلالی. */
        fun fromGregorian(gy: Int, gm: Int, gd: Int): JalaliDate {
            val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
            val gy2 = if (gm > 2) gy + 1 else gy
            var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 +
                gd + gdm[gm - 1]
            var jy = -1595 + 33 * (days / 12053)
            days %= 12053
            jy += 4 * (days / 1461)
            days %= 1461
            if (days > 365) {
                jy += (days - 1) / 365
                days = (days - 1) % 365
            }
            val jm: Int
            val jd: Int
            if (days < 186) {
                jm = 1 + days / 31
                jd = 1 + days % 31
            } else {
                jm = 7 + (days - 186) / 30
                jd = 1 + (days - 186) % 30
            }
            return JalaliDate(jy, jm, jd)
        }

        fun fromMillis(millis: Long, zone: TimeZone = TimeZone.getDefault()): JalaliDate {
            val cal = Calendar.getInstance(zone).apply { timeInMillis = millis }
            return fromGregorian(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH),
            )
        }

        /** تاریخ میلادی مثل 2026/11/13 */
        fun gregorianNumeric(millis: Long, persianDigits: Boolean, zone: TimeZone = TimeZone.getDefault()): String {
            val cal = Calendar.getInstance(zone).apply { timeInMillis = millis }
            val text = "%04d/%02d/%02d".format(
                java.util.Locale.US,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH),
            )
            return if (persianDigits) Formats.toPersianDigits(text) else text
        }

        /** تاریخ میلادی مثل «13 Nov 2026» یا «۱۳ نوامبر ۲۰۲۶» */
        fun gregorianLong(millis: Long, persian: Boolean, zone: TimeZone = TimeZone.getDefault()): String {
            val cal = Calendar.getInstance(zone).apply { timeInMillis = millis }
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val m = cal.get(Calendar.MONTH).coerceIn(0, 11)
            val y = cal.get(Calendar.YEAR)
            return if (persian) Formats.toPersianDigits("$d") + " " + GREGORIAN_MONTHS_FA[m] + " " + Formats.toPersianDigits("$y")
            else "$d ${GREGORIAN_MONTHS_EN[m]} $y"
        }

        fun weekday(millis: Long, persian: Boolean, zone: TimeZone = TimeZone.getDefault()): String {
            val cal = Calendar.getInstance(zone).apply { timeInMillis = millis }
            val index = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
            return if (persian) WEEKDAYS_FA[index] else WEEKDAYS_EN[index]
        }

        fun weekdayShort(millis: Long, persian: Boolean, zone: TimeZone = TimeZone.getDefault()): String {
            val cal = Calendar.getInstance(zone).apply { timeInMillis = millis }
            val index = (cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
            return if (persian) WEEKDAYS_SHORT_FA[index] else WEEKDAYS_SHORT_EN[index]
        }

        /** روز ماه شمسی (فارسی) یا میلادی (انگلیسی) برای برچسب محور نمودار. */
        fun axisDay(millis: Long, persian: Boolean, zone: TimeZone = TimeZone.getDefault()): String =
            if (persian) Formats.toPersianDigits("${fromMillis(millis, zone).day}")
            else "${Calendar.getInstance(zone).apply { timeInMillis = millis }.get(Calendar.DAY_OF_MONTH)}"

        /** طول یک ماه شمسی (۱۲ فروردین‌ماه‌ها ۳۱ روز، ۷ تا ۱۱، و اسفند ۲۹ یا ۳۰ روز). */
        fun monthLength(year: Int, month: Int): Int {
            require(month in 1..12) { "month must be in 1..12, was $month" }
            if (month <= 6) return 31
            if (month <= 11) return 30
            val start = toMillis(year, 12, 1, zone = TimeZone.getDefault())
            val next = toMillis(year + 1, 1, 1, zone = TimeZone.getDefault())
            return ((next - start) / 86_400_000L).toInt().coerceIn(29, 30)
        }

        fun isValid(year: Int, month: Int, day: Int): Boolean =
            month in 1..12 && day in 1..monthLength(year, month)

        /**
         * شروع روز (نیمه‌شب به وقت محلی) به عنوان millis برای یک تاریخ جلالی.
         * با جستجوی دودویی روی همان fromGregorian موجود پیاده شده تا خروجی همیشه
         * با باقی توابع کلاس سازگار باشد و فرمول جداگانه‌ای لازم نداشته باشد.
         */
        fun toMillis(year: Int, month: Int, day: Int, zone: TimeZone = TimeZone.getDefault()): Long {
            require(month in 1..12) { "month must be in 1..12, was $month" }
            require(day in 1..31) { "day must be in 1..31, was $day" }
            fun compareTriple(y: Int, m: Int, d: Int): Int {
                if (y != year) return y.compareTo(year)
                if (m != month) return m.compareTo(month)
                return d.compareTo(day)
            }
            val base = Calendar.getInstance(zone).apply {
                clear()
                set(1900, Calendar.JANUARY, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            var lo = 0
            var hi = 150_000 // ~410 سال؛ همه‌ی تاریخ‌های معتبر را پوشش می‌دهد
            var result = -1L
            while (lo <= hi) {
                val mid = (lo + hi) / 2
                val cal = (base.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_MONTH, mid)
                }
                val converted = fromGregorian(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH),
                )
                val cmp = compareTriple(converted.year, converted.month, converted.day)
                if (cmp == 0) {
                    result = cal.timeInMillis
                    break
                }
                if (cmp < 0) lo = mid + 1 else hi = mid - 1
            }
            require(result >= 0) { "date out of supported range: $year/$month/$day" }
            return result
        }
    }
}
