package ir.marbledu.datausage.data

import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

/** قالب‌بندی حجم، سرعت و اعداد با ارقام فارسی یا لاتین. */
object Formats {

    fun bytes(value: Long, persian: Boolean): String {
        val units = if (persian) listOf("بایت", "کیلوبایت", "مگابایت", "گیگابایت", "ترابایت")
        else listOf("B", "KB", "MB", "GB", "TB")
        if (value <= 0) return "0 ${units[0]}"
        val i = (ln(value.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val scaled = value / 1024.0.pow(i.toDouble())
        return "${number(scaled, persian, if (scaled >= 100) 0 else 1)} ${units[i]}"
    }

    fun speed(bytesPerSecond: Double, persian: Boolean): String {
        val unit = if (persian) "بر ثانیه" else "/s"
        return "${bytes(abs(bytesPerSecond).toLong(), persian)} $unit"
    }

    fun number(value: Double, persian: Boolean, decimals: Int = 1): String {
        val formatted = String.format(Locale.US, "%.${decimals}f", value)
        return if (persian) toPersianDigits(formatted) else formatted
    }

    fun integer(value: Long, persian: Boolean): String {
        val grouped = String.format(Locale.US, "%,d", value)
        return if (persian) toPersianDigits(grouped) else grouped
    }

    fun toPersianDigits(text: String): String {
        val map = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return buildString {
            text.forEach { c -> append(if (c in '0'..'9') map[c - '0'] else c) }
        }
    }

    fun duration(ms: Long, persian: Boolean): String {
        val totalMinutes = ms / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> if (persian) "${integer(hours, true)} ساعت و ${integer(minutes, true)} دقیقه"
            else "${integer(hours, false)} h ${integer(minutes, false)} min"
            else -> if (persian) "${integer(minutes, true)} دقیقه" else "${integer(minutes, false)} min"
        }
    }

    /** درصد مصرف از سقف. */
    fun percentOf(used: Long, cap: Long): Float =
        if (cap <= 0) 0f else (used.toDouble() / cap).toFloat().coerceIn(0f, 1f)

    /** هزینه‌ی تقریبی بر اساس تعرفه‌ی هر گیگابایت (تومان). */
    fun estimateCost(bytes: Long, pricePerGb: Long, persian: Boolean): String {
        val cost = (bytes / 1024.0 / 1024.0 / 1024.0) * pricePerGb
        return integer(cost.toLong(), persian)
    }
}
