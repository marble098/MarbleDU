package ir.marbledu.datausage

import ir.marbledu.datausage.data.JalaliDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class JalaliDateTest {

    @Test
    fun converts_known_dates() {
        assertEquals(JalaliDate(1405, 8, 22), JalaliDate.fromGregorian(2026, 11, 13))
        assertEquals(JalaliDate(1405, 1, 1), JalaliDate.fromGregorian(2026, 3, 21))
        assertEquals(JalaliDate(1404, 1, 1), JalaliDate.fromGregorian(2025, 3, 21))
        assertEquals(JalaliDate(1403, 1, 1), JalaliDate.fromGregorian(2024, 3, 20))
        assertEquals(JalaliDate(1357, 11, 22), JalaliDate.fromGregorian(1979, 2, 11))
        assertEquals(JalaliDate(1378, 10, 11), JalaliDate.fromGregorian(2000, 1, 1))
    }

    @Test
    fun handles_leap_years() {
        // ۱۴۰۳ کبیسه است: ۳۰ اسفند وجود دارد.
        assertEquals(JalaliDate(1403, 12, 30), JalaliDate.fromGregorian(2025, 3, 20))
        // ۱۴۰۴ کبیسه نیست: آخرین روز ۲۹ اسفند است.
        assertEquals(JalaliDate(1404, 12, 29), JalaliDate.fromGregorian(2026, 3, 20))
        assertEquals(JalaliDate(1402, 12, 10), JalaliDate.fromGregorian(2024, 2, 29))
    }

    @Test
    fun formats_in_both_languages() {
        val date = JalaliDate(1405, 8, 22)
        assertEquals("1405/08/22", date.numeric(false))
        assertEquals("۱۴۰۵/۰۸/۲۲", date.numeric(true))
        assertEquals("۲۲ آبان ۱۴۰۵", date.long(true))
        assertEquals("22 Aban 1405", date.long(false))
    }

    @Test
    fun converts_from_millis_in_tehran() {
        val zone = TimeZone.getTimeZone("Asia/Tehran")
        val cal = Calendar.getInstance(zone).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 13, 23, 30, 0)
        }
        assertEquals(JalaliDate(1405, 8, 22), JalaliDate.fromMillis(cal.timeInMillis, zone))
        assertEquals("2026/11/13", JalaliDate.gregorianNumeric(cal.timeInMillis, false, zone))
        assertEquals("Friday", JalaliDate.weekday(cal.timeInMillis, false, zone))
        assertEquals("جمعه", JalaliDate.weekday(cal.timeInMillis, true, zone))
    }
}
