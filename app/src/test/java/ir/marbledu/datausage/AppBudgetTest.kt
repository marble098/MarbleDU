package ir.marbledu.datausage

import ir.marbledu.datausage.data.AppBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBudgetTest {

    private val gb = 1024L * 1024 * 1024

    @Test
    fun shares_add_up_and_use_remaining_cap() {
        // ۸ گیگ از ۱۰ گیگ مصرف شده؛ ۴ گیگ آن اینستاگرام، ۲ گیگ یوتیوب، ۲ گیگ نامشخص.
        // ۲۰ روز گذشته و ۱۰ روز مانده، پس سهم روزانه‌ی کل = ۲ گیگ ÷ ۱۰ = ۲۰۰ مگ.
        val plan = AppBudget.plan(
            spends = listOf(
                AppBudget.AppSpend("instagram", "Instagram", 4 * gb),
                AppBudget.AppSpend("youtube", "YouTube", 2 * gb),
            ),
            capBytes = 10 * gb,
            usedBytes = 8 * gb,
            daysLeftInCycle = 10,
            elapsedDays = 20,
        )
        assertEquals(2 * gb, plan.remainingBytes)
        assertEquals(2 * gb / 10, plan.dailyBudgetBytes)
        val instagram = plan.rows.first { it.key == "instagram" }
        assertEquals(50, instagram.sharePercent)
        // ۵۰٪ از ۲ گیگ باقی‌مانده = ۱ گیگ سهم مجاز.
        assertEquals(gb, instagram.safeLimitBytes)
        // با همین روند (روزی ۲۰۰ مگ) تا آخر دوره ۲ گیگ مصرف می‌کند.
        assertEquals(2 * gb, instagram.keepBytes)
        // آهنگ روزانه = ۴ گیگ ÷ ۲۰ روز = ۲۰۰ مگ (با گرد کردن، پس بازه‌ای می‌سنجیم).
        assertTrue(
            "آهنگ روزانه باید حدود ۲۰۰ مگ باشد",
            instagram.dailyPaceBytes in (2 * gb / 10 - 1)..(2 * gb / 10 + 1),
        )
        // اینستاگرام به‌تنهایی کل سهم روزانه‌ی بسته را می‌خورد؛ یوتیوب نصفش را.
        assertTrue(instagram.overPace)
        assertFalse(plan.rows.first { it.key == "youtube" }.overPace)
        // پیش‌بینی کل (با «سایر») از باقی‌مانده بیشتر است، پس دوره کم می‌آورد.
        assertTrue(plan.projectedBytes > plan.remainingBytes)
        assertTrue(plan.willRunOutEarly)
    }

    @Test
    fun unknown_usage_counts_as_others_so_percentages_stay_honest() {
        val plan = AppBudget.plan(
            spends = listOf(AppBudget.AppSpend("a", "A", gb)),
            capBytes = 0L,
            usedBytes = 4 * gb, // ۳ گیگ آن به اپ مشخصی نسبت داده نشده
            daysLeftInCycle = 5,
            elapsedDays = 5,
        )
        assertEquals(25, plan.rows.single().sharePercent)
        assertEquals(0L, plan.dailyBudgetBytes) // بدون سقف، بودجه‌ای تعریف نمی‌شود
        // بدون سقف هیچ اپی «ناهنجار» شمرده نمی‌شود؛ کارت فقط درصدها را نشان می‌دهد.
        assertFalse(plan.rows.single().overPace)
    }

    @Test
    fun passing_the_cap_marks_the_cycle_short() {
        val plan = AppBudget.plan(
            spends = listOf(AppBudget.AppSpend("netflix", "Netflix", 6 * gb)),
            capBytes = 10 * gb,
            usedBytes = 11 * gb,
            daysLeftInCycle = 4,
            elapsedDays = 26,
        )
        assertEquals(0L, plan.remainingBytes)
        assertEquals(4, plan.shortfallDays)
    }

    @Test
    fun empty_spends_is_safe() {
        val plan = AppBudget.plan(emptyList(), 0L, 0L, 3, 3)
        assertTrue(plan.rows.isEmpty())
        assertEquals(0, plan.shortfallDays)
        assertFalse(plan.willRunOutEarly)
    }

    @Test
    fun days_gained_by_restricting_an_app() {
        // ۲ گیگ باقی مانده و ۴ روز وقت هست؛ روزی ۱ گیگ آزاد می‌شود.
        val gained = AppBudget.daysGained(
            capBytes = 10 * gb,
            usedBytes = 8 * gb,
            daysLeftInCycle = 4,
            freedBytesPerDay = gb,
        )
        assertEquals(2, gained)
        assertEquals(0, AppBudget.daysGained(0L, 0L, 4, gb))
    }

    @Test
    fun shortfall_is_zero_when_usage_fits() {
        val plan = AppBudget.plan(
            spends = listOf(AppBudget.AppSpend("a", "A", gb)),
            capBytes = 50 * gb,
            usedBytes = 2 * gb,
            daysLeftInCycle = 20,
            elapsedDays = 10,
        )
        assertEquals(0, plan.shortfallDays)
        assertFalse(plan.willRunOutEarly)
    }
}
