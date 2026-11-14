package ir.marbledu.datausage

import ir.marbledu.datausage.data.SurgePrediction
import ir.marbledu.datausage.data.UsageForecast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class UsageForecastTest {

    private val mb = 1024L * 1024
    private val dayMillis = 86_400_000L

    /** روز هفته از یک تاریخ ساختگی (۰ = یکشنبه) — به‌جای تقویم سیستم تا تست قطعی باشد. */
    private fun weekdayOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK) - 1

    @Test
    fun without_history_it_extrapolates_the_current_pace() {
        val todayStart = 0L
        val now = dayMillis / 4 // یک‌چهارم روز
        val estimate = UsageForecast.estimateToday(
            history = emptyList(),
            todayStart = todayStart,
            nowMillis = now,
            todayBytes = 250 * mb,
        )
        assertEquals("تخمین باید خطی باشد", 1000 * mb, estimate.projectedTodayBytes)
        assertEquals(750 * mb, estimate.remainingTodayBytes)
        assertEquals(0, estimate.confidence)
    }

    @Test
    fun never_reports_less_than_what_is_already_used() {
        val todayStart = 0L
        val now = dayMillis / 2
        val history = listOf(UsageForecast.Day(-dayMillis, 100 * mb))
        val estimate = UsageForecast.estimateToday(history, todayStart, now, todayBytes = 900 * mb)
        assertTrue(estimate.projectedTodayBytes >= 900 * mb)
        assertTrue(estimate.remainingTodayBytes >= 0)
    }

    @Test
    fun uses_the_same_weekday_when_available() {
        val todayStart = 30 * dayMillis
        val now = todayStart + dayMillis / 2
        val weekday = weekdayOf(now)
        // دو هفته تاریخچه: روزهای هم‌روز هفته ۲۰۰ مگ، بقیه ۵۰ مگ. اگر تخمین
        // درست کار کند، باید به ۲۰۰ مگ نگاه کند نه به میانگین ۷۱ مگ.
        val history = (1..14).map { index ->
            val start = todayStart - index * dayMillis
            val bytes = if (weekdayOf(start) == weekday) 200 * mb else 50 * mb
            UsageForecast.Day(start, bytes)
        }
        val estimate = UsageForecast.estimateToday(history, todayStart, now, todayBytes = 40 * mb)
        assertTrue(
            "تخمین باید از الگوی همان روز هفته بیاید، نه میانگین کل: ${estimate.projectedTodayBytes}",
            estimate.projectedTodayBytes > 100 * mb,
        )
        assertEquals(14, estimate.basisDays)
        assertEquals(100, estimate.confidence)
        assertTrue(estimate.remainingTodayBytes > 0)
    }

    @Test
    fun cycle_forecast_is_zero_shortfall_when_cap_is_large() {
        val history = (1..10).map { UsageForecast.Day(it * dayMillis, 100 * mb) }
        val estimate = UsageForecast.estimateCycle(
            history = history,
            capBytes = 100_000 * mb,
            usedBytes = 1000 * mb,
            daysLeftInCycle = 20,
            todayMillis = 20 * dayMillis,
        )
        assertEquals(0, estimate.shortfallDays)
        assertTrue(estimate.projectedBytes > 1000 * mb)
    }

    @Test
    fun cycle_forecast_flags_shortfall() {
        val history = (1..10).map { UsageForecast.Day(it * dayMillis, 1000 * mb) }
        val estimate = UsageForecast.estimateCycle(
            history = history,
            capBytes = 5000 * mb,
            usedBytes = 3000 * mb,
            daysLeftInCycle = 10,
            todayMillis = 20 * dayMillis,
        )
        assertTrue("باید کم‌آوردن بسته را تشخیص دهد", estimate.shortfallDays > 0)
    }

    @Test
    fun surge_is_quiet_for_small_volume() {
        val result = SurgePrediction.evaluate(
            currentHourBytes = 512 * 1024, // ۵۱۲ کیلوبایت
            baselineHourBytes = 0L,
            minutesIntoHour = 30,
            remainingCapBytes = 1000 * mb,
        )
        assertEquals(SurgePrediction.Risk.NONE, result.risk)
    }

    @Test
    fun surge_warns_when_far_above_the_same_hour_baseline() {
        val result = SurgePrediction.evaluate(
            currentHourBytes = 300 * mb,
            baselineHourBytes = 100 * mb,
            minutesIntoHour = 20,
            remainingCapBytes = 1000 * mb,
        )
        // انتظار: در ۲۰ دقیقه‌ی اول، ۱۰۰ مگ × ۲۰/۶۰ = ۳۳ مگ انتظار می‌رفت، ۳۰۰ مگ مصرف شده.
        assertTrue(result.isAlert)
        assertTrue(result.multiplier > 2.5f)
        assertTrue(result.projectedSoonBytes > 0)
        assertTrue(result.shareOfRemainingPercent >= 0)
    }

    @Test
    fun surge_escalates_when_the_rest_of_the_plan_is_at_stake() {
        val result = SurgePrediction.evaluate(
            currentHourBytes = 900 * mb,
            baselineHourBytes = 10 * mb,
            minutesIntoHour = 30,
            remainingCapBytes = 1000 * mb,
        )
        assertEquals(SurgePrediction.Risk.CRITICAL, result.risk)
        assertTrue(result.shareOfRemainingPercent > 25)
    }

    @Test
    fun surge_stays_calm_for_normal_usage() {
        val result = SurgePrediction.evaluate(
            currentHourBytes = 30 * mb,
            baselineHourBytes = 60 * mb,
            minutesIntoHour = 30,
            remainingCapBytes = 5000 * mb,
        )
        assertEquals(SurgePrediction.Risk.NONE, result.risk)
    }
}
