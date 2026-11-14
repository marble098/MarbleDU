package ir.marbledu.datausage.widget

import ir.marbledu.datausage.data.Formats
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/** عدد و یکای جدا برای «عدد بزرگ» ویجت (مثلاً «۱۲.۴» و «گیگابایت»). */
data class WidgetAmount(val value: String, val unit: String) {
    val isEmpty: Boolean get() = value.isBlank()
}

/** وضعیت مصرف نسبت به سقف بسته؛ رنگ نوار پیشرفت از همین تعیین می‌شود. */
enum class CapState { CALM, WARNING, CRITICAL }

/**
 * قالب‌بندی و محاسبه‌های خالصِ ویجت: عددها، درصد، ساعت به‌روزرسانی و وضعیت سقف.
 * بدون وابستگی به اندروید تا مستقیم قابل تست باشد.
 */
object WidgetFormat {

    /** جای خالی عدد نامعلوم. */
    const val PLACEHOLDER = "—"

    private const val SPACE = ' '

    /**
     * عدد مصرف را به «عدد» و «یکا» می‌شکند تا در ویجت عدد درشت و یکا ریز
     * نمایش داده شود. اگر دسترسی مصرف وجود نداشته باشد، جای خالی برمی‌گردد.
     */
    fun amount(bytes: Long, persian: Boolean, hasData: Boolean = true): WidgetAmount {
        if (!hasData) return WidgetAmount(PLACEHOLDER, "")
        val text = Formats.bytes(bytes.coerceAtLeast(0L), persian)
        val split = text.lastIndexOf(SPACE)
        return if (split <= 0) {
            WidgetAmount(text, "")
        } else {
            WidgetAmount(text.substring(0, split), text.substring(split + 1))
        }
    }

    /** درصد مصرف از سقف، بین ۰ و ۱۰۰. */
    fun percent(used: Long, cap: Long): Int {
        if (cap <= 0L) return 0
        val ratio = used.coerceAtLeast(0L).toDouble() / cap.toDouble() * 100.0
        return ratio.roundToInt().coerceIn(0, 100)
    }

    /** متن درصد با ارقام فارسی/لاتین و علامت درصد. */
    fun percentText(percent: Int, persian: Boolean): String {
        val value = Formats.integer(percent.coerceIn(0, 100).toLong(), persian)
        return if (persian) "$value٪" else "$value%"
    }

    /** وضعیت سقف: آرام، نزدیک پایان (>۸۰٪) یا تمام‌شده (۱۰۰٪). */
    fun capState(percent: Int): CapState = when {
        percent >= 100 -> CapState.CRITICAL
        percent >= 80 -> CapState.WARNING
        else -> CapState.CALM
    }

    /** باقی‌مانده‌ی بسته، بدون عدد منفی. */
    fun remaining(used: Long, cap: Long): Long = (cap - used).coerceAtLeast(0L)

    /** ساعت:دقیقه‌ی محلی؛ خالی اگر زمانی ثبت نشده باشد. */
    fun clock(millis: Long): String {
        if (millis <= 0L) return ""
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return String.format(
            Locale.US,
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
        )
    }

    /** ساعت با ارقام فارسی در صورت لازم. */
    fun clockText(millis: Long, persian: Boolean): String {
        val raw = clock(millis)
        return if (raw.isEmpty() || !persian) raw else Formats.toPersianDigits(raw)
    }
}
