package ir.marbledu.datausage

import ir.marbledu.datausage.data.AppListFilter
import ir.marbledu.datausage.data.DaySummary
import ir.marbledu.datausage.data.Traffic
import ir.marbledu.datausage.data.WeeklyComparison
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ConsumptionToolsTest {
    @Test fun searchSupportsPackagesWhitespaceAndPersianKeyboardVariants() {
        assertTrue(AppListFilter.matches("کیف", "ir.example.wallet", " كيف "))
        assertTrue(AppListFilter.matches("Wallet", "ir.example.wallet", "EXAMPLE"))
        assertTrue(AppListFilter.matches("Wallet", "ir.example.wallet", "  "))
        assertFalse(AppListFilter.matches("Wallet", "ir.example.wallet", "browser"))
    }

    private fun day(offset: Int, bytes: Long): DaySummary {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 13, 0, 0, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, offset)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return DaySummary(start, calendar.timeInMillis, Traffic(bytes, 0), Traffic())
    }

    @Test fun comparesCompleteWeeksAndExcludesToday() {
        val days = (-14..-1).map { day(it, if (it < -7) 100 else 200) } + day(0, 99999)
        val result = WeeklyComparison.build(days, day(0, 0).startMillis)!!
        assertEquals(1400L, result.recent)
        assertEquals(700L, result.previous)
        assertEquals(100.0, result.changePercent!!, 0.001)
    }

    @Test fun handlesMissingDaysAndZeroBaseline() {
        assertNull(WeeklyComparison.build((-13..-1).map { day(it, 1) }, day(0, 0).startMillis))
        val result = WeeklyComparison.build((-14..-1).map { day(it, if (it < -7) 0 else 1) }, day(0, 0).startMillis)!!
        assertNull(result.changePercent)
        assertNull(WeeklyComparison.build(emptyList(), day(0, 0).startMillis))
    }
}
