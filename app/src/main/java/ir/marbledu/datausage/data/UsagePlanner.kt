package ir.marbledu.datausage.data

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.ceil

/**
 * منطق خالصِ «هشدار مصرف» و «پیش‌بینی بسته» – بدون وابستگی به اندروید تا
 * کاملاً قابل تست باشد.
 */
object UsagePlanner {

    // ------------------------------------------------------------ هشدارها
    enum class Alert { DAILY_LIMIT, CAP_WARNING, CAP_REACHED }

    /** آنچه قبلاً هشدار داده شده، تا هر هشدار فقط یک‌بار بیاید. */
    data class AlertState(
        /** شروعِ روزی که هشدار سقف روزانه برایش داده شده. */
        val dailyDay: Long = 0L,
        /** آخرین سطح هشدار بسته: ۰، ۸۰ یا ۱۰۰. */
        val capLevel: Int = 0,
    )

    data class AlertDecision(val alerts: List<Alert>, val state: AlertState)

    const val CAP_WARNING_PERCENT = 80

    fun decideAlerts(
        state: AlertState,
        dayStart: Long,
        todayMobileBytes: Long,
        dailyLimitBytes: Long,
        capUsedBytes: Long,
        capBytes: Long,
    ): AlertDecision {
        val alerts = ArrayList<Alert>(2)
        var next = state

        if (dailyLimitBytes > 0L && todayMobileBytes >= dailyLimitBytes && state.dailyDay != dayStart) {
            alerts.add(Alert.DAILY_LIMIT)
            next = next.copy(dailyDay = dayStart)
        }

        if (capBytes > 0L) {
            val percent = (capUsedBytes.coerceAtLeast(0L) * 100L / capBytes).coerceAtMost(1000L).toInt()
            val level = when {
                percent >= 100 -> 100
                percent >= CAP_WARNING_PERCENT -> CAP_WARNING_PERCENT
                else -> 0
            }
            if (level > state.capLevel) {
                alerts.add(if (level >= 100) Alert.CAP_REACHED else Alert.CAP_WARNING)
            }
            // با پایین آمدن مصرف (بسته‌ی جدید / دوره‌ی جدید) هشدار دوباره مسلح می‌شود.
            if (level != state.capLevel) next = next.copy(capLevel = level)
        } else if (state.capLevel != 0) {
            next = next.copy(capLevel = 0)
        }
        return AlertDecision(alerts, next)
    }

    // ----------------------------------------------------------- پیش‌بینی
    data class Forecast(
        val remainingBytes: Long,
        /** میانگین مصرف روزانه‌ی اخیر. */
        val averageDailyBytes: Long,
        /** با همین روند، بسته چند روز دیگر دوام می‌آورد؛ ‎-1‎ یعنی مصرفی ثبت نشده. */
        val daysUntilEmpty: Int,
        /** روزهای مانده تا شروع دوره‌ی بعد (حداقل ۱). */
        val daysLeftInCycle: Int,
        /** سهم روزانه‌ای که بسته را تا آخر دوره می‌رساند. */
        val dailyBudgetBytes: Long,
        /** آیا با این روند بسته قبل از پایان دوره تمام می‌شود؟ */
        val willRunOut: Boolean,
    )

    fun forecast(
        capBytes: Long,
        usedBytes: Long,
        recentDailyBytes: List<Long>,
        nowMillis: Long,
        billingDay: Int,
        zone: TimeZone = TimeZone.getDefault(),
    ): Forecast? {
        if (capBytes <= 0L) return null
        val remaining = (capBytes - usedBytes).coerceAtLeast(0L)
        val average = if (recentDailyBytes.isEmpty()) 0L else recentDailyBytes.sum() / recentDailyBytes.size
        val daysLeft = daysLeftInCycle(nowMillis, billingDay, zone)
        val daysUntilEmpty = when {
            average <= 0L -> -1
            else -> ceil(remaining.toDouble() / average).toInt()
        }
        return Forecast(
            remainingBytes = remaining,
            averageDailyBytes = average,
            daysUntilEmpty = daysUntilEmpty,
            daysLeftInCycle = daysLeft,
            dailyBudgetBytes = remaining / daysLeft,
            willRunOut = daysUntilEmpty in 0 until daysLeft,
        )
    }

    fun daysLeftInCycle(nowMillis: Long, billingDay: Int, zone: TimeZone = TimeZone.getDefault()): Int {
        val start = TimeMath.cycleStart(nowMillis, billingDay, zone)
        val end = Calendar.getInstance(zone).apply {
            timeInMillis = start
            add(Calendar.MONTH, 1)
        }.timeInMillis
        val dayMillis = 24L * 60 * 60 * 1000
        return ceil((end - nowMillis).toDouble() / dayMillis).toInt().coerceAtLeast(1)
    }
}
