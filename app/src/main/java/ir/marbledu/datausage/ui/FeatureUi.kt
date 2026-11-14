package ir.marbledu.datausage.ui

import ir.marbledu.datausage.data.AppBudget
import ir.marbledu.datausage.data.SurgePrediction
import ir.marbledu.datausage.data.UsageForecast

/**
 * وضعیت صفحه‌ی «سهم اپ‌ها» (قابلیت تازه).
 *
 * @param plan نقشه‌ی سهم هر اپ از باقی‌مانده‌ی بسته
 * @param forecast تخمین مصرف تا پایان دوره
 * @param todayEstimate برآورد مصرف امروز
 */
data class AppBudgetUi(
    val loading: Boolean = false,
    val loadedAt: Long = 0L,
    val plan: AppBudget.Plan? = null,
    val forecast: UsageForecast.CycleEstimate? = null,
    val todayEstimate: UsageForecast.Estimate? = null,
    val hasUsageAccess: Boolean = false,
)

/**
 * وضعیت «هشدار زودهنگام جهش مصرف» (قابلیت تازه).
 *
 * @param result نتیجه‌ی تحلیل ساعت جاری
 * @param topApp اپی که بیشترین سهم را در همین ساعت دارد
 */
data class SurgeWatchUi(
    val loading: Boolean = false,
    val checkedAt: Long = 0L,
    val result: SurgePrediction.Result? = null,
    val topAppLabel: String = "",
    val topAppBytes: Long = 0L,
    val remainingCapBytes: Long = 0L,
)
