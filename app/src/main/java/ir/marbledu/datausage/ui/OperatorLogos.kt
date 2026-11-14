package ir.marbledu.datausage.ui

import androidx.annotation.DrawableRes
import ir.marbledu.datausage.R

/**
 * لوگوی رسمی اپراتورها (drawable-nodpi/op_*.webp).
 *
 * نشان‌ها متعلق به صاحبان‌شان هستند و فقط برای شناسایی اپراتور نمایش داده می‌شوند.
 * منبع هر نشان در NOTICE-OPERATOR-LOGOS.md آمده است.
 *
 * همه‌ی فایل‌ها با یک قاعده‌ی یکسان آماده شده‌اند تا در کنار هم «یک‌دست» دیده شوند:
 *  - پس‌زمینه شفاف،
 *  - بریدن حاشیه‌ی خالی (trim)،
 *  - جای‌دادن نشان در یک قاب مربعی ۲۵۶×۲۵۶ با حاشیه‌ی برابر از چهار طرف،
 *  - وکتور بودن منبع (SVG) و رندر در اندازه‌ی بزرگ، تا در هیچ اندازه‌ای پیکسلی نشود.
 *
 * برای همین دیگر لازم نیست لوگوهای روشن روی «کاشی رنگ برند» بنشینند؛ کاشی سفید
 * برای همه یکسان است و رنگ برند فقط در حلقه و هاله‌ی دور کاشی دیده می‌شود.
 */
object OperatorLogos {

    /**
     * مقیاس نمایش لوگو داخل کاشی.
     *
     * [Tight] برای نشان‌های پهن/کم‌ارتفاع (نام تجاری افقی) کمی بزرگ‌تر رسم می‌شود و
     * [Roomy] برای نشان‌های مربعی و پرمتن کمی کوچک‌تر، تا «وزن بصری» همه یکسان شود.
     */
    enum class Fit { Tight, Balanced, Roomy }

    private data class Entry(@DrawableRes val res: Int, val fit: Fit = Fit.Balanced)

    private val logos: Map<String, Entry> = mapOf(
        // --- اپراتورهای موبایل و MVNO
        "mci" to Entry(R.drawable.op_mci, Fit.Balanced),
        "irancell" to Entry(R.drawable.op_irancell, Fit.Tight),
        "rightel" to Entry(R.drawable.op_rightel, Fit.Tight),
        "shatel_mobile" to Entry(R.drawable.op_shatel, Fit.Roomy),
        "taliya" to Entry(R.drawable.op_taliya, Fit.Tight),
        "aptel" to Entry(R.drawable.op_aptel, Fit.Roomy),
        "samantel" to Entry(R.drawable.op_samantel, Fit.Tight),
        "lotustel" to Entry(R.drawable.op_lotustel, Fit.Balanced),
        "telekish" to Entry(R.drawable.op_telekish, Fit.Balanced),
        "mtce" to Entry(R.drawable.op_mtce, Fit.Tight),
        "fanapmobile" to Entry(R.drawable.op_fanapmobile, Fit.Balanced),
        "arintel" to Entry(R.drawable.op_arintel, Fit.Balanced),
        // --- اینترنت ثابت / فیبر / بی‌سیم
        "tci" to Entry(R.drawable.op_tci, Fit.Balanced),
        "shatel" to Entry(R.drawable.op_shatel, Fit.Roomy),
        "asiatech" to Entry(R.drawable.op_asiatech, Fit.Tight),
        "hiweb" to Entry(R.drawable.op_hiweb, Fit.Roomy),
        "parsonline" to Entry(R.drawable.op_parsonline, Fit.Tight),
        "mobinnet" to Entry(R.drawable.op_mobinnet, Fit.Balanced),
        "sabanet" to Entry(R.drawable.op_sabanet, Fit.Tight),
        "datak" to Entry(R.drawable.op_datak, Fit.Balanced),
        "pishgaman" to Entry(R.drawable.op_pishgaman, Fit.Balanced),
        "fanava" to Entry(R.drawable.op_fanava, Fit.Tight),
        "zitel" to Entry(R.drawable.op_zitel, Fit.Tight),
        "afranet" to Entry(R.drawable.op_afranet, Fit.Tight),
        "mabna" to Entry(R.drawable.op_mabna, Fit.Balanced),
        "mahannet" to Entry(R.drawable.op_mahannet, Fit.Tight),
        "laser" to Entry(R.drawable.op_laser, Fit.Tight),
        "respina" to Entry(R.drawable.op_respina, Fit.Balanced),
        "tic" to Entry(R.drawable.op_tic, Fit.Balanced),
    )

    /** شناسه‌ی همه‌ی اپراتورهایی که لوگوی رسمی دارند (برای تست یکپارچگی). */
    fun idsWithLogo(): Set<String> = logos.keys

    @DrawableRes
    fun logoFor(operatorId: String): Int? = logos[operatorId]?.res

    /**
     * نسبت اندازه‌ی لوگو به کاشی.
     * لوگوهای افقی پهن‌تر رسم می‌شوند تا کوچک به‌نظر نرسند؛ نشان‌های مربعی
     * کمی کوچک‌تر تا کاشی «شلوغ» نشود.
     */
    fun fitScale(operatorId: String): Float = when (logos[operatorId]?.fit) {
        Fit.Tight -> 0.94f
        Fit.Roomy -> 0.78f
        else -> 0.86f
    }

    /**
     * دیگر هیچ لوگویی روی کاشی رنگ برند نمی‌نشیند: همه‌ی نشان‌ها روی کاشی سفید
     * (یا کاشی سطح در تم تیره) خوانا هستند و این کار ظاهر فهرست را یک‌دست می‌کند.
     * تابع برای سازگاری با فراخوانی‌های قبلی نگه داشته شده است.
     */
    @Deprecated("همه‌ی لوگوها روی کاشی یکسان نمایش داده می‌شوند", ReplaceWith("false"))
    fun needsBrandTile(operatorId: String): Boolean = false
}
