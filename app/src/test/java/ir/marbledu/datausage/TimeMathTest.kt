package ir.marbledu.datausage

import ir.marbledu.datausage.data.TimeMath
import ir.marbledu.datausage.data.TimeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TimeMathTest {

    private val zone = TimeZone.getTimeZone("Asia/Tehran")

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long {
        val cal = Calendar.getInstance(zone)
        cal.set(year, month - 1, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun dayStart_is_midnight_of_same_day() {
        val now = millis(2026, 11, 13, 15, 30)
        val start = TimeMath.dayStart(now, zone)
        assertEquals(millis(2026, 11, 13, 0, 0), start)
    }

    @Test
    fun today_range_starts_at_midnight_not_24h_ago() {
        val now = millis(2026, 11, 13, 1, 0) // ساعت ۱ بامداد
        val start = TimeMath.rangeStart(TimeRange.TODAY, now, 1, zone)
        // باید نیمه‌شب امروز باشد، نه ساعت ۱ دیروز
        assertEquals(millis(2026, 11, 13, 0, 0), start)
    }

    @Test
    fun cycleStart_returns_billing_day_of_current_month() {
        val now = millis(2026, 11, 13, 12, 0)
        val start = TimeMath.cycleStart(now, 1, zone)
        assertEquals(millis(2026, 11, 1, 0, 0), start)
    }

    @Test
    fun cycleStart_goes_to_previous_month_when_billing_day_not_reached() {
        val now = millis(2026, 11, 5, 12, 0)
        val start = TimeMath.cycleStart(now, 15, zone)
        // روز ۱۵ هنوز نرسیده، پس دوره از ۱۵ اکتبر شروع شده
        assertEquals(millis(2026, 10, 15, 0, 0), start)
    }

    @Test
    fun cycleStart_clamps_day_to_28() {
        val now = millis(2026, 2, 10, 12, 0)
        val start = TimeMath.cycleStart(now, 31, zone)
        // ۳۱ به ۲۸ clamp می‌شود
        assertEquals(millis(2026, 1, 28, 0, 0), start)
    }

    @Test
    fun cycle_range_uses_billing_day() {
        val now = millis(2026, 11, 20, 12, 0)
        val start = TimeMath.rangeStart(TimeRange.CYCLE, now, 15, zone)
        assertEquals(millis(2026, 11, 15, 0, 0), start)
    }

    @Test
    fun week_and_month_are_relative() {
        val now = millis(2026, 11, 13, 12, 0)
        val weekStart = TimeMath.rangeStart(TimeRange.WEEK, now, 1, zone)
        assertEquals(now - TimeRange.WEEK.millis(), weekStart)
        val monthStart = TimeMath.rangeStart(TimeRange.MONTH, now, 1, zone)
        assertEquals(now - TimeRange.MONTH.millis(), monthStart)
    }

    @Test
    fun cycleStart_is_always_before_or_equal_to_now() {
        val now = millis(2026, 11, 13, 12, 0)
        for (day in 1..28) {
            val start = TimeMath.cycleStart(now, day, zone)
            assertTrue("day=$day start=$start now=$now", start <= now)
        }
    }
}
