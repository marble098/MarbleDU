package ir.marbledu.datausage.data

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * پالت گرادیانی یک اپراتور؛ تنها منبع رنگ برای کارت اپراتور صفحه‌ی خانه، اعلان پایش و ویجت.
 *
 *  - [start] → [middle] → [end]: سه ایستگاه گرادیان مورب، از گوشه‌ی «شروع» (سمت لوگو)
 *    تا گوشه‌ی «پایان»؛
 *  - [glow]: درخشش نرمی که در گوشه‌ی بالای سمت پایان می‌نشیند و به کارت عمق می‌دهد؛
 *  - [glowAlpha]: بیشترین شدت آن درخشش که هنوز متن را روی هر سه ایستگاه خوانا نگه می‌دارد؛
 *  - [accent]: رنگ امضای دوم برند (مثلاً نارنجی همراه اول) برای جزئیات کوچک؛
 *  - [ink]: رنگ متن روی گرادیان. برای برندهای تیره سفید است و برای برندهای روشن
 *    (زرد ایرانسل، لیمویی آرین‌تل، نارنجی داتک…) تقریباً مشکی، تا رنگ اصلی برند
 *    مجبور نشود قهوه‌ای و گل‌آلود شود.
 *
 * قاعده‌ی خوانایی: کنتراست [ink] با هر ایستگاه و با هر ایستگاهی که درخشش رویش افتاده،
 * دست‌کم [OperatorBrand.MIN_TEXT_CONTRAST] (معیار WCAG AA برای متن معمولی) است و این
 * در تست‌ها برای همه‌ی اپراتورهای فهرست بررسی می‌شود.
 */
data class OperatorBrandGradient(
    val start: Int,
    val middle: Int,
    val end: Int,
    val glow: Int,
    val accent: Int,
    val ink: Int,
    val glowAlpha: Float,
) {
    /** متن تیره روی گرادیان روشن (برندهای زرد/لیمویی/نارنجی). */
    val darkInk: Boolean get() = OperatorBrand.luminance(ink) < 0.5

    val stops: List<Int> get() = listOf(start, middle, end)
}

/**
 * هویت رنگی اپراتورها، یکسان در Compose، RemoteViews (ویجت/اعلان) و همه‌ی تم‌ها.
 *
 * اپراتورهای اصلی پالت دست‌چین بر اساس هویت بصری رسمی خودشان دارند (مثلاً همراه اول با
 * آبی‌های هویت ۱۴۰۳، ایرانسل با زرد MTN). برای بقیه، پالت از رنگ برند و رنگ تأکیدی
 * فهرست ([OperatorCatalog]) ساخته می‌شود: رنگ در فضای HSL فقط تا جایی تیره/روشن می‌شود
 * که متن خوانا بماند، رنگ‌های گرم هنگام تیره‌شدن کمی به سمت نارنجی/قرمز می‌چرخند تا
 * زیتونی و قهوه‌ای نشوند، و ایستگاه پایانی از رنگ تأکیدی هم‌خانواده یا یک رنگ مجاور
 * گرفته می‌شود تا گرادیان «زنده» باشد و تخت دیده نشود.
 */
object OperatorBrand {

    val WHITE: Int = 0xFFFFFFFF.toInt()
    private val INK_DARK: Int = 0xFF141414.toInt()

    /** کمترین کنتراست متن روی هر نقطه‌ی گرادیان. */
    const val MIN_TEXT_CONTRAST: Double = 4.5

    /** ایستگاه اصلی برند کمی کنتراست اضافه نگه می‌دارد تا درخشش جا داشته باشد. */
    private const val MID_TARGET = 5.6
    private const val END_TARGET = 5.0

    private const val GLOW_CAP_LIGHT_INK = 0.55f
    private const val GLOW_CAP_DARK_INK = 0.30f

    fun gradient(operator: OperatorCatalog.Operator): OperatorBrandGradient =
        explicit[operator.id] ?: derive(operator.brandColor.toInt(), operator.accentColor.toInt())

    /** پالت خنثی برای وقتی اتصالی نیست (اعلان آفلاین). */
    val offline: OperatorBrandGradient by lazy {
        palette(0xFF1C2433, 0xFF2A3446, 0xFF38445A, 0xFF8EA2C4, 0xFF8EA2C4)
    }

    /**
     * پالت‌های دست‌چین اپراتورهای اصلی. رنگ‌ها از هویت بصری رسمی هر برند آمده‌اند و
     * فقط تا جایی تیره شده‌اند که متن رویشان خوانا بماند.
     */
    private val explicit: Map<String, OperatorBrandGradient> by lazy {
        val shatel = palette(0xFF061A40, 0xFF0B3B85, 0xFF1652B0, 0xFF3A7BE0, 0xFFF16321)
        mapOf(
            // همراه اول، هویت ۱۴۰۳: آبی تیره‌ی 295CAB، آبی 0095DA و نارنجی FF4F00
            "mci" to palette(0xFF0A2463, 0xFF295CAB, 0xFF0B6CB3, 0xFF27B4F2, 0xFFFF4F00),
            // ایرانسل، هویت ۱۴۰۰: زرد MTN (FFCC00) با متن مشکی
            "irancell" to palette(0xFFFFB400, 0xFFFFCC00, 0xFFFFE14D, 0xFFFFFFFF, 0xFF7A5600, 0xFF141414),
            // رایتل: ارغوانی حلقه‌ی لوگو (9B2776) تا صورتی روشن
            "rightel" to palette(0xFF3E0D33, 0xFF8A2470, 0xFFA82E86, 0xFFE35BB8, 0xFFF4A6DA),
            // شاتل و شاتل موبایل: سرمه‌ای و نارنجی نشان شاتل
            "shatel" to shatel,
            "shatel_mobile" to shatel,
            // فناپ موبایل: آبی کاشی سیم‌کارتی و توپ نارنجی
            "fanapmobile" to palette(0xFF0A3463, 0xFF1466A6, 0xFF1873A8, 0xFF47A8EB, 0xFFF39229),
            // آرین‌تل: سبز لیمویی نام تجاری با متن تیره
            "arintel" to palette(0xFF2FBF2A, 0xFF50E827, 0xFF9BF06E, 0xFFFFFFFF, 0xFF4E5A4E, 0xFF0F1A0F),
        )
    }

    private fun palette(
        start: Long,
        middle: Long,
        end: Long,
        glow: Long,
        accent: Long,
        ink: Long = 0xFFFFFFFF,
    ): OperatorBrandGradient = build(
        start.toInt(), middle.toInt(), end.toInt(), glow.toInt(), accent.toInt(), ink.toInt(),
    )

    private fun build(start: Int, middle: Int, end: Int, glow: Int, accent: Int, ink: Int): OperatorBrandGradient {
        val stops = listOf(start, middle, end)
        val cap = if (luminance(ink) < 0.5) GLOW_CAP_DARK_INK else GLOW_CAP_LIGHT_INK
        return OperatorBrandGradient(
            start = start,
            middle = middle,
            end = end,
            glow = glow,
            accent = accent,
            ink = ink,
            glowAlpha = maxLayerAlpha(stops, glow, ink, cap),
        )
    }

    /** ساخت پالت از رنگ برند و رنگ تأکیدی فهرست. */
    internal fun derive(brand: Int, accent: Int): OperatorBrandGradient {
        val (h, s, l) = toHsl(brand)
        val (ah, aS, _) = toHsl(accent)
        val chroma = s * (1 - abs(2 * l - 1))
        val neutral = chroma < 0.12
        val warm = h in 20.0..110.0
        val lightBrand = !neutral && luminance(brand) >= (if (warm) 0.30 else 0.45)

        if (lightBrand) {
            // برند روشن: خود رنگ برند حفظ می‌شود و متن تیره می‌گیرد.
            var ink = fromHsl(h, min(s, 0.55), 0.07)
            if (contrast(ink, brand) < MIN_TEXT_CONTRAST) ink = INK_DARK
            val middle = lightenFor(h, s, l, ink, MID_TARGET)
            val start = lightenFor(h - 9, min(1.0, s * 1.04), max(0.0, l - 0.07), ink, MID_TARGET)
            val end = lightenFor(h + 6, s, min(0.86, l + 0.14), ink, MID_TARGET)
            val deep = fromHsl(h - 4, min(1.0, s), 0.26)
            return build(start, middle, end, WHITE, deep, ink)
        }

        val ink = WHITE
        val sat = if (neutral) s else max(s, 0.58)
        val hueMid = if (luminance(brand) > 0.18) warmShift(h) else h
        val middle = darkenFor(hueMid, sat, l, ink, MID_TARGET)
        val midL = toHsl(middle).third
        val start = darkenFor(hueMid - 10, min(1.0, sat * 1.04), midL * 0.50, ink, MID_TARGET)
        val harmonious = !neutral && aS > 0.25 && hueDistance(ah, h) <= 55
        val endHue = warmShift(if (harmonious) ah else h + 16)
        val endSat = if (harmonious) max(aS, 0.55) else sat
        val end = darkenFor(endHue, min(1.0, endSat), min(0.62, max(midL + 0.10, 0.40)), ink, END_TARGET)
        val glow = if (neutral) fromHsl(h, s, 0.72) else fromHsl(h, min(1.0, sat * 1.05), 0.66)
        val distinctAccent = aS > 0.25 && hueDistance(ah, h) > 25
        val signature = if (distinctAccent) accent else fromHsl(h + 28, min(1.0, sat), 0.60)
        return build(start, middle, end, glow, signature, ink)
    }

    /** زرد و نارنجیِ تیره‌شده زیتونی/قهوه‌ای می‌شود؛ کمی به سمت کهربایی/قرمز می‌چرخد. */
    private fun warmShift(h: Double): Double = if (h in 25.0..70.0) h - 0.35 * (h - 18) else h

    /** بیشترین روشنایی (تا [lMax]) که هنوز با جوهر [ink] کنتراست [ratio] دارد. */
    private fun darkenFor(h: Double, s: Double, lMax: Double, ink: Int, ratio: Double): Int {
        if (contrast(fromHsl(h, s, lMax), ink) >= ratio) return fromHsl(h, s, lMax)
        var lo = 0.0
        var hi = lMax
        repeat(30) {
            val mid = (lo + hi) / 2
            if (contrast(fromHsl(h, s, mid), ink) >= ratio) lo = mid else hi = mid
        }
        return fromHsl(h, s, lo)
    }

    /** کمترین روشنایی (از [lMin] به بالا) که با جوهر تیره کنتراست [ratio] دارد. */
    private fun lightenFor(h: Double, s: Double, lMin: Double, ink: Int, ratio: Double): Int {
        if (contrast(fromHsl(h, s, lMin), ink) >= ratio) return fromHsl(h, s, lMin)
        var lo = lMin
        var hi = 1.0
        repeat(30) {
            val mid = (lo + hi) / 2
            if (contrast(fromHsl(h, s, mid), ink) >= ratio) hi = mid else lo = mid
        }
        return fromHsl(h, s, hi)
    }

    /** بیشترین شفافیت یک لایه‌ی رنگی روی گرادیان که خوانایی [ink] را زیر حد مجاز نبرد. */
    internal fun maxLayerAlpha(stops: List<Int>, layer: Int, ink: Int, cap: Float): Float {
        var alpha = cap
        while (alpha > 0f) {
            if (stops.all { contrast(blend(it, layer, alpha), ink) >= MIN_TEXT_CONTRAST }) {
                return (alpha * 100).roundToInt() / 100f
            }
            alpha -= 0.01f
        }
        return 0f
    }

    // ------------------------------------------------------------------ رنگ‌شناسی پایه

    /** ترکیب خطی دو رنگ مات (کانال آلفا ۲۵۵ می‌ماند). */
    fun blend(from: Int, to: Int, ratio: Float): Int {
        val t = ratio.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            return (a + (b - a) * t).roundToInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    /** کنتراست WCAG بین دو رنگ مات. */
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** روشنایی نسبی رنگ مطابق WCAG. */
    fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((color shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private fun hueDistance(a: Double, b: Double): Double {
        val d = abs(a - b) % 360.0
        return min(d, 360.0 - d)
    }

    /** RGB → HSL؛ فام به درجه، اشباع و روشنایی بین ۰ و ۱. */
    internal fun toHsl(color: Int): Triple<Double, Double, Double> {
        val r = ((color shr 16) and 0xFF) / 255.0
        val g = ((color shr 8) and 0xFF) / 255.0
        val b = (color and 0xFF) / 255.0
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val l = (maxC + minC) / 2
        if (maxC == minC) return Triple(0.0, 0.0, l)
        val d = maxC - minC
        val s = if (l <= 0.5) d / (maxC + minC) else d / (2.0 - maxC - minC)
        val h = when (maxC) {
            r -> ((g - b) / d + (if (g < b) 6 else 0))
            g -> ((b - r) / d + 2)
            else -> ((r - g) / d + 4)
        } * 60.0
        return Triple(h, s, l)
    }

    /** HSL → RGB مات. */
    internal fun fromHsl(hue: Double, saturation: Double, lightness: Double): Int {
        val h = ((hue % 360.0) + 360.0) % 360.0 / 360.0
        val s = saturation.coerceIn(0.0, 1.0)
        val l = lightness.coerceIn(0.0, 1.0)
        if (s == 0.0) {
            val v = (l * 255).roundToInt()
            return (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        val q = if (l < 0.5) l * (1 + s) else l + s - l * s
        val p = 2 * l - q
        fun hueToRgb(t0: Double): Double {
            var t = t0
            if (t < 0) t += 1.0
            if (t > 1) t -= 1.0
            return when {
                t < 1.0 / 6 -> p + (q - p) * 6 * t
                t < 1.0 / 2 -> q
                t < 2.0 / 3 -> p + (q - p) * (2.0 / 3 - t) * 6
                else -> p
            }
        }
        val r = (hueToRgb(h + 1.0 / 3) * 255).roundToInt().coerceIn(0, 255)
        val g = (hueToRgb(h) * 255).roundToInt().coerceIn(0, 255)
        val b = (hueToRgb(h - 1.0 / 3) * 255).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
