package ir.marbledu.datausage.widget

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * رنگ‌های آماده‌ی ویجت. همه‌ی رنگ‌ها یک‌جا محاسبه می‌شوند تا رسم ویجت فقط
 * «رنگ را بریز سر جایش» باشد و ظاهر روشن/تاریک/AMOLED همیشه خوانا بماند.
 */
data class WidgetColors(
    val dark: Boolean,
    /** عدد بزرگ مصرف امروز. */
    val hero: Int,
    /** متن اصلی و عنوان‌ها. */
    val body: Int,
    /** متن‌های کم‌اهمیت‌تر (یکا، ساعت، توضیح). */
    val muted: Int,
    /** رنگ تأکیدی پالت. */
    val accent: Int,
    /** رنگ ستون داده‌ی موبایل. */
    val mobile: Int,
    /** رنگ ستون وای‌فای. */
    val wifi: Int,
    /** پس‌زمینه‌ی نشان‌ها (کپسول وضعیت و درصد). */
    val chipBg: Int,
    /** متن داخل کپسول. */
    val chipText: Int,
    /** پس‌زمینه‌ی کارت‌های کوچک موبایل/وای‌فای. */
    val cardBg: Int,
    /** رنگ ریل نوار پیشرفت. */
    val track: Int,
    /** پس‌زمینه‌ی دکمه‌های میان‌بر. */
    val actionBg: Int,
    /** متن دکمه‌های میان‌بر. */
    val actionText: Int,
    /** پس‌زمینه‌ی نشان گرد بالای ویجت. */
    val badgeBg: Int,
)

/**
 * انتخاب رنگ ویجت بر اساس تم برنامه و پالت انتخاب‌شده.
 *
 * قاعده‌ی اصلی: عددها و متن‌ها روی سطح روشن یا تاریک باید کنتراست کافی
 * داشته باشند؛ اگر رنگ پالت روی سطح انتخابی کم‌رنگ بود، خودکار به سمت
 * سیاه یا سفید جابه‌جا می‌شود تا خوانا بماند.
 */
object WidgetTheme {

    private val WHITE = 0xFFFFFFFF.toInt()
    private val BLACK = 0xFF000000.toInt()
    private val INK = 0xFF101828.toInt()

    private val DARK_ACCENT = 0xFF4DE8D0.toInt()
    private val DARK_MOBILE = 0xFF5CE9CF.toInt()
    private val DARK_WIFI = 0xFF7FB8FF.toInt()
    private val LIGHT_ACCENT = 0xFF0B7C6E.toInt()
    private val LIGHT_MOBILE = 0xFF0E7C6B.toInt()
    private val LIGHT_WIFI = 0xFF2559C9.toInt()

    private const val WARNING = 0xFFFFB020.toInt()
    private const val CRITICAL = 0xFFFF5C65.toInt()

    /** حداقل کنتراست برای متن‌ها و رنگ‌های تأکیدی ویجت. */
    private const val MIN_CONTRAST = 3.0

    /**
     * آیا ویجت پس‌زمینه‌ی تیره بگیرد؟ تم روشن همیشه روشن است، تم تاریک و
     * AMOLED تیره، و «هم‌رنگ سیستم» از حالت شب گوشی پیروی می‌کند.
     */
    fun isDarkSurface(themeModeName: String, systemNight: Boolean): Boolean = when (themeModeName) {
        "LIGHT" -> false
        "DARK", "AMOLED" -> true
        else -> systemNight
    }

    /** تم AMOLED یعنی پس‌زمینه‌ی کاملاً مشکی. */
    fun isAmoled(themeModeName: String): Boolean = themeModeName == "AMOLED"

    fun colors(
        dark: Boolean,
        amoled: Boolean = false,
        primary: Int? = null,
        secondary: Int? = null,
    ): WidgetColors {
        val surface = if (dark) (if (amoled) BLACK else 0xFF111A2E.toInt()) else 0xFFF7F9FF.toInt()
        val accent = readable(primary ?: if (dark) DARK_ACCENT else LIGHT_ACCENT, surface)
        val mobile = readable(primary ?: if (dark) DARK_MOBILE else LIGHT_MOBILE, surface)
        val wifi = readable(secondary ?: primary ?: if (dark) DARK_WIFI else LIGHT_WIFI, surface)
        return if (dark) {
            WidgetColors(
                dark = true,
                hero = WHITE,
                body = 0xFFEAF0FF.toInt(),
                muted = 0xFFA6B2CF.toInt(),
                accent = accent,
                mobile = mobile,
                wifi = wifi,
                chipBg = 0x1FFFFFFF,
                chipText = 0xFFEAF0FF.toInt(),
                cardBg = 0x14FFFFFF,
                track = 0x33FFFFFF,
                actionBg = withAlpha(accent, 0x2E),
                actionText = 0xFFEAF1FF.toInt(),
                badgeBg = withAlpha(accent, 0x33),
            )
        } else {
            WidgetColors(
                dark = false,
                hero = INK,
                body = 0xFF1B2437.toInt(),
                muted = 0xFF5B6579.toInt(),
                accent = accent,
                mobile = mobile,
                wifi = wifi,
                chipBg = 0x14101828,
                chipText = 0xFF1B2437.toInt(),
                cardBg = 0x0A101828,
                track = 0x1F101828,
                actionBg = withAlpha(accent, 0x1C),
                actionText = 0xFF152036.toInt(),
                badgeBg = withAlpha(accent, 0x22),
            )
        }
    }

    /** رنگ نوار پیشرفت بسته؛ با نزدیک شدن به پایان، به نارنجی/قرمز می‌رود. */
    fun capColor(state: CapState, colors: WidgetColors): Int {
        val surface = if (colors.dark) 0xFF111A2E.toInt() else 0xFFF7F9FF.toInt()
        return when (state) {
            CapState.CALM -> colors.accent
            CapState.WARNING -> readable(WARNING, surface)
            CapState.CRITICAL -> readable(CRITICAL, surface)
        }
    }

    /** ترکیب دو رنگ با نسبت ۰ تا ۱ (کانال آلفا هم لحاظ می‌شود). */
    fun blend(from: Int, to: Int, ratio: Float): Int {
        val t = ratio.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            return (a + (b - a) * t).roundToInt().coerceIn(0, 255)
        }
        return (channel(24) shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    /** همان رنگ با آلفای دلخواه. */
    fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)

    /**
     * اگر رنگ روی سطح انتخابی کم‌کنتراست بود، آن را به سمت سفید (روی سطح
     * تیره) یا سیاه (روی سطح روشن) هل می‌دهد تا حداقل کنتراست لازم برسد.
     */
    fun readable(color: Int, surface: Int, minRatio: Double = MIN_CONTRAST): Int {
        if (contrast(color, surface) >= minRatio) return color
        val surfaceLum = luminance(surface)
        val target = if (surfaceLum > 0.5) BLACK else WHITE
        var low = 0f
        var high = 1f
        repeat(16) {
            val mid = (low + high) / 2f
            if (contrast(blend(color, target, mid), surface) >= minRatio) high = mid else low = mid
        }
        return blend(color, target, high)
    }

    /** کنتراست WCAG بین دو رنگ. */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** روشنایی نسبی رنگ مطابق استاندارد WCAG. */
    fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((color shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
