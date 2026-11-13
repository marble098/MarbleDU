package ir.marbledu.datausage

import ir.marbledu.datausage.data.UsagePlanner
import ir.marbledu.datausage.data.UsagePlanner.Alert
import ir.marbledu.datausage.data.UsagePlanner.AlertState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class UsagePlannerTest {

    private val gb = 1024L * 1024 * 1024
    private val zone = TimeZone.getTimeZone("Asia/Tehran")

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        val cal = Calendar.getInstance(zone)
        cal.set(year, month - 1, day, hour, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun daily_limit_fires_once_per_day() {
        val first = UsagePlanner.decideAlerts(AlertState(), 1000L, 600, 500, 0, 0)
        assertEquals(listOf(Alert.DAILY_LIMIT), first.alerts)
        val again = UsagePlanner.decideAlerts(first.state, 1000L, 900, 500, 0, 0)
        assertTrue(again.alerts.isEmpty())
        val nextDay = UsagePlanner.decideAlerts(again.state, 2000L, 700, 500, 0, 0)
        assertEquals(listOf(Alert.DAILY_LIMIT), nextDay.alerts)
    }

    @Test
    fun daily_limit_silent_below_limit_or_disabled() {
        assertTrue(UsagePlanner.decideAlerts(AlertState(), 1L, 499, 500, 0, 0).alerts.isEmpty())
        assertTrue(UsagePlanner.decideAlerts(AlertState(), 1L, 10_000, 0, 0, 0).alerts.isEmpty())
    }

    @Test
    fun cap_warns_at_80_then_100_and_rearms() {
        val warn = UsagePlanner.decideAlerts(AlertState(), 1L, 0, 0, 8 * gb, 10 * gb)
        assertEquals(listOf(Alert.CAP_WARNING), warn.alerts)
        assertEquals(80, warn.state.capLevel)
        val same = UsagePlanner.decideAlerts(warn.state, 1L, 0, 0, 9 * gb, 10 * gb)
        assertTrue(same.alerts.isEmpty())
        val reached = UsagePlanner.decideAlerts(same.state, 1L, 0, 0, 10 * gb, 10 * gb)
        assertEquals(listOf(Alert.CAP_REACHED), reached.alerts)
        val reset = UsagePlanner.decideAlerts(reached.state, 1L, 0, 0, 1 * gb, 10 * gb)
        assertTrue(reset.alerts.isEmpty())
        assertEquals(0, reset.state.capLevel)
        val warnAgain = UsagePlanner.decideAlerts(reset.state, 1L, 0, 0, 8 * gb, 10 * gb)
        assertEquals(listOf(Alert.CAP_WARNING), warnAgain.alerts)
    }

    @Test
    fun cap_jumping_straight_to_100_reports_reached_only() {
        val d = UsagePlanner.decideAlerts(AlertState(), 1L, 0, 0, 12 * gb, 10 * gb)
        assertEquals(listOf(Alert.CAP_REACHED), d.alerts)
        assertEquals(100, d.state.capLevel)
    }

    @Test
    fun forecast_null_without_cap() {
        assertNull(UsagePlanner.forecast(0, 0, listOf(1L), millis(2026, 11, 13), 1, zone))
    }

    @Test
    fun forecast_detects_running_out_before_cycle_end() {
        // ۱۰ گیگ مانده، روزی ۲ گیگ → ۵ روز؛ تا شروع دوره‌ی بعد (۱ دسامبر) ۱۸ روز مانده.
        val f = UsagePlanner.forecast(30 * gb, 20 * gb, List(7) { 2 * gb }, millis(2026, 11, 13), 1, zone)!!
        assertEquals(10 * gb, f.remainingBytes)
        assertEquals(5, f.daysUntilEmpty)
        assertEquals(18, f.daysLeftInCycle)
        assertTrue(f.willRunOut)
        assertEquals(10 * gb / 18, f.dailyBudgetBytes)
    }

    @Test
    fun forecast_ok_when_usage_is_light_or_unknown() {
        val light = UsagePlanner.forecast(30 * gb, 5 * gb, List(7) { gb / 2 }, millis(2026, 11, 13), 1, zone)!!
        assertFalse(light.willRunOut)
        val none = UsagePlanner.forecast(30 * gb, 0, emptyList(), millis(2026, 11, 13), 1, zone)!!
        assertEquals(-1, none.daysUntilEmpty)
        assertFalse(none.willRunOut)
    }

    @Test
    fun days_left_is_at_least_one() {
        assertEquals(1, UsagePlanner.daysLeftInCycle(millis(2026, 11, 30, 23), 1, zone))
        assertTrue(UsagePlanner.daysLeftInCycle(millis(2026, 11, 1, 0), 1, zone) >= 29)
    }
}
