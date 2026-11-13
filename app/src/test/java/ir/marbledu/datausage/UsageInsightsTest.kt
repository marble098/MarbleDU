package ir.marbledu.datausage

import ir.marbledu.datausage.data.UsageInsights
import ir.marbledu.datausage.data.UsageInsights.DayVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageInsightsTest {

    private val mb = 1024L * 1024

    @Test
    fun build_reports_averages_peak_and_mobile_share() {
        val days = listOf(
            UsageInsights.Day(dayStart = 0, mobileBytes = 100 * mb, wifiBytes = 0),
            UsageInsights.Day(dayStart = 86_400_000, mobileBytes = 200 * mb, wifiBytes = 200 * mb),
            UsageInsights.Day(dayStart = 2 * 86_400_000, mobileBytes = 0, wifiBytes = 0),
        )
        val hours = MutableList(24) { 0L }
        hours[21] = 500 * mb
        val snapshot = UsageInsights.build(days, hours, weekdayOf = { it.toInt() })
        // دو روزِ دارای مصرف: (۱۰۰) و (۲۰۰+۲۰۰) → میانگین ۲۵۰، میانگین موبایل ۱۵۰.
        assertEquals(250 * mb, snapshot.averageDailyBytes)
        assertEquals(150 * mb, snapshot.averageMobileBytes)
        assertEquals(21, snapshot.peakHour)
        assertEquals(2, snapshot.activeDays)
        assertEquals(500 * mb, snapshot.peakHourBytes)
        // ۳۰۰ موبایل از ۵۰۰ کل.
        assertEquals(0.6f, snapshot.mobileShare, 0.001f)
    }

    @Test
    fun empty_history_is_safe() {
        val snapshot = UsageInsights.build(emptyList(), emptyList())
        assertEquals(0L, snapshot.averageDailyBytes)
        assertNull(snapshot.peakHour)
        assertNull(snapshot.heaviestWeekday)
        assertEquals(0f, snapshot.mobileShare, 0.0001f)
    }

    @Test
    fun verdict_thresholds() {
        val avg = 1000L
        assertEquals(DayVerdict.LIGHT, UsageInsights.classify(400, avg))
        assertEquals(DayVerdict.NORMAL, UsageInsights.classify(1000, avg))
        assertEquals(DayVerdict.HEAVY, UsageInsights.classify(1600, avg))
        assertEquals(DayVerdict.VERY_HEAVY, UsageInsights.classify(3500, avg))
        // روزِ نیمه‌کاره: ۵۰۰ بایت در نیمه‌ی روز انتظار ۵۰۰ است، پس عادی.
        assertEquals(DayVerdict.NORMAL, UsageInsights.classify(520, avg, dayFraction = 0.5f))
    }

    @Test
    fun days_until_cap_handles_zero_and_remaining() {
        assertEquals(0, UsageInsights.daysUntilCap(0, 100))
        assertEquals(-1, UsageInsights.daysUntilCap(1000, 0))
        assertEquals(5, UsageInsights.daysUntilCap(500, 100))
        assertEquals(1, UsageInsights.daysUntilCap(1, 100))
    }

    @Test
    fun budget_reports_overrun() {
        val b = UsageInsights.budget(todayBytes = 300, dailyBudgetBytes = 200)
        assertEquals(100L, b.overBudgetBytes)
        assertEquals(150, b.percentOfBudget)
        val under = UsageInsights.budget(todayBytes = 100, dailyBudgetBytes = 200)
        assertEquals(0L, under.overBudgetBytes)
        assertEquals(50, under.percentOfBudget)
        assertEquals(0, UsageInsights.budget(50, 0).percentOfBudget)
    }

    @Test
    fun percent_change_and_roughly_equal() {
        assertEquals(50, UsageInsights.percentChange(150, 100))
        assertEquals(-25, UsageInsights.percentChange(75, 100))
        assertEquals(0, UsageInsights.percentChange(50, 0))
        assertTrue(UsageInsights.roughlyEqual(100, 104))
        assertFalse(UsageInsights.roughlyEqual(100, 130))
        assertTrue(UsageInsights.roughlyEqual(0, 0))
    }

    @Test
    fun weekday_and_hour_labels_are_localized() {
        assertEquals("شنبه", UsageInsights.weekdayName(6, persian = true))
        assertEquals("Saturday", UsageInsights.weekdayName(6, persian = false))
        assertEquals("۲۱:۰۰", UsageInsights.hourLabel(21, persian = true))
        assertEquals("21:00", UsageInsights.hourLabel(21, persian = false))
    }

    @Test
    fun heaviest_weekday_is_identified() {
        val day = 86_400_000L
        val days = (0..6).map { index ->
            UsageInsights.Day(dayStart = index * day, mobileBytes = (index + 1) * mb, wifiBytes = 0)
        }
        val snapshot = UsageInsights.build(days, emptyList(), weekdayOf = { (it / day).toInt() })
        assertEquals(6, snapshot.heaviestWeekday)
        assertEquals(0, snapshot.lightestWeekday)
    }
}
