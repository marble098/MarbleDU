package ir.marbledu.datausage.data

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * «پیش‌بینی جهش مصرف» — به‌جای هشدارِ بعد از واقعه، قبل از تمام‌شدن بسته خبر می‌دهد.
 *
 * به این موضوع دقت می‌کند: یک اپ می‌تواند امروز عادی باشد ولی «شیب» مصرفش
 * تند شده باشد (مثلاً به‌روزرسانی خودکار یا پخش ویدیو در پس‌زمینه). این کلاس
 * مصرف ساعت‌به‌ساعت را با میانگین همان ساعت در روزهای گذشته مقایسه می‌کند و
 * اگر سرعت مصرف در یک بازه‌ی کوتاه به‌طور معناداری بالا رفته باشد، «هشدار
 * زودهنگام» تولید می‌کند: چند دقیقه دیگر چقدر مصرف می‌شود و چند درصد از
 * باقی‌مانده‌ی بسته را می‌بلعد.
 *
 * خالص و بدون اندروید؛ با تست واحد سنجیده می‌شود.
 */
object SurgePrediction {

    enum class Risk { NONE, WATCH, WARNING, CRITICAL }

    data class Result(
        val risk: Risk,
        /** مصرف ساعت جاری. */
        val currentHourBytes: Long,
        /** میانگین همان ساعت در روزهای قبل. */
        val baselineHourBytes: Long,
        /** ضریب افزایش نسبت به میانگین همان ساعت (۰ = داده‌ی کافی نیست). */
        val multiplier: Float,
        /** تخمین مصرف در [horizonMinutes] دقیقه‌ی آینده با همین سرعت. */
        val projectedSoonBytes: Long,
        /** درصدی از باقی‌مانده‌ی بسته که در همین بازه خورده می‌شود. */
        val shareOfRemainingPercent: Int,
    ) {
        val isAlert: Boolean get() = risk == Risk.WARNING || risk == Risk.CRITICAL
    }

    /**
     * @param currentHourBytes مصرف از ابتدای ساعت جاری تا الان
     * @param baselineHourBytes میانگین مصرف همین ساعت در روزهای گذشته (۰ = ناشناخته)
     * @param minutesIntoHour دقیقه‌ی سپری‌شده از ساعت جاری (۱..۶۰)
     * @param remainingCapBytes باقی‌مانده‌ی بسته (۰ = سقفی تعیین نشده)
     * @param multiplierThreshold آستانه‌ی ضریب برای «شدت»
     * @param horizonMinutes بازه‌ی پیش‌بینی (پیش‌فرض ۱۵ دقیقه)
     */
    fun evaluate(
        currentHourBytes: Long,
        baselineHourBytes: Long,
        minutesIntoHour: Int,
        remainingCapBytes: Long,
        multiplierThreshold: Float = 2.5f,
        horizonMinutes: Int = 15,
    ): Result {
        val minutes = minutesIntoHour.coerceIn(1, 60)
        val perMinute = currentHourBytes.toDouble() / minutes
        val projectedSoon = (perMinute * horizonMinutes.coerceAtLeast(1)).toLong()
        if (currentHourBytes <= 0L) {
            return Result(Risk.NONE, 0L, baselineHourBytes, 0f, 0L, 0)
        }
        // میانگین روزهای قبل برای همین ساعت، تا همین لحظه از ساعت (نه کل ساعت).
        val expectedSoFar = if (baselineHourBytes <= 0L) 0.0
        else baselineHourBytes.toDouble() * minutes / 60.0

        val multiplier = when {
            expectedSoFar <= 0.0 -> if (currentHourBytes >= LARGE_ENOUGH_BYTES) 99f else 0f
            else -> (currentHourBytes / expectedSoFar).toFloat()
        }
        val shareOfRemaining = when {
            remainingCapBytes <= 0L -> 0
            else -> ((projectedSoon * 100.0) / remainingCapBytes).roundToInt().coerceIn(0, 100)
        }
        val risk = when {
            // حجم ناچیز (چند صد کیلوبایت) هیچ‌وقت هشدار نمی‌گیرد.
            currentHourBytes < LARGE_ENOUGH_BYTES && projectedSoon < LARGE_ENOUGH_BYTES -> Risk.NONE
            multiplier >= multiplierThreshold * 2.2f -> Risk.CRITICAL
            multiplier >= multiplierThreshold -> Risk.WARNING
            multiplier >= max(1.4f, multiplierThreshold * 0.6f) -> Risk.WATCH
            else -> Risk.NONE
        }
        // اگر باقی‌مانده‌ی بسته در همین چند دقیقه واقعاً به‌خطر می‌افتد، شدت بالا می‌رود.
        val riskAdjusted = when {
            risk == Risk.NONE -> risk
            remainingCapBytes > 0L && shareOfRemaining >= 25 -> Risk.CRITICAL
            remainingCapBytes > 0L && shareOfRemaining >= 10 && risk == Risk.WATCH -> Risk.WARNING
            else -> risk
        }
        return Result(
            risk = riskAdjusted,
            currentHourBytes = currentHourBytes,
            baselineHourBytes = baselineHourBytes,
            multiplier = multiplier.coerceAtMost(99f),
            projectedSoonBytes = projectedSoon,
            shareOfRemainingPercent = shareOfRemaining,
        )
    }

    /** آستانه‌ی «حجم معنادار»: ۲۰ مگابایت در یک ساعت. */
    private const val LARGE_ENOUGH_BYTES = 20L * 1024 * 1024
}
