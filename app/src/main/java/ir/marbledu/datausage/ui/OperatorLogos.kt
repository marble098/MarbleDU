package ir.marbledu.datausage.ui

import androidx.annotation.DrawableRes
import ir.marbledu.datausage.R

/**
 * لوگوی رسمی اپراتورها (drawable-nodpi/op_*.webp).
 *
 * نشان‌ها متعلق به صاحبان‌شان هستند و فقط برای شناسایی اپراتور نمایش داده می‌شوند.
 * منبع هر نشان در NOTICE-OPERATOR-LOGOS.md آمده است.
 *
 * از نسخه‌ی ۱.۶.۶ همه‌ی فایل‌ها از نو و با یک قاعده‌ی یکسان ساخته شده‌اند:
 *  - منبع رسمی و به‌روز (SVG رسمی هر جا که بود؛ مثل لوگوی ۲۰۲۵ همراه اول، بیضی جدید
 *    ایرانسل، نشان رسمی فناپ موبایل و آرین‌تل و هویت بصری تازه‌ی فناوا)؛
 *  - پس‌زمینه‌ی شفاف و بریدن حاشیه‌ی خالی؛
 *  - «اندازه‌گذاری بصری»: هر نشان بر اساس مساحت واقعی جوهرش داخل قاب ۲۵۶×۲۵۶ جا گرفته،
 *    نه بر اساس کادرش، تا نشان‌های پهن، مربعی و پرمتن کنار هم هم‌وزن دیده شوند؛
 *  - حاشیه‌ی امن برای گوشه‌های گرد کاشی.
 *
 * چون اندازه‌گذاری داخل خود فایل‌هاست، همه با یک مقیاس ثابت ([FIT_SCALE]) روی کاشی
 * سفید رسم می‌شوند؛ در برنامه، اعلان و ویجت یکسان.
 */
object OperatorLogos {

    /** نسبت اندازه‌ی لوگو به کاشی سفید؛ برای همه یکسان است. */
    const val FIT_SCALE = 0.92f

    private val logos: Map<String, Int> = mapOf(
        // --- اپراتورهای موبایل و MVNO
        "mci" to R.drawable.op_mci,
        "irancell" to R.drawable.op_irancell,
        "rightel" to R.drawable.op_rightel,
        "shatel_mobile" to R.drawable.op_shatel,
        "taliya" to R.drawable.op_taliya,
        "aptel" to R.drawable.op_aptel,
        "samantel" to R.drawable.op_samantel,
        "lotustel" to R.drawable.op_lotustel,
        "telekish" to R.drawable.op_telekish,
        "mtce" to R.drawable.op_mtce,
        "fanapmobile" to R.drawable.op_fanapmobile,
        "arintel" to R.drawable.op_arintel,
        // --- اینترنت ثابت / فیبر / بی‌سیم
        "tci" to R.drawable.op_tci,
        "shatel" to R.drawable.op_shatel,
        "asiatech" to R.drawable.op_asiatech,
        "hiweb" to R.drawable.op_hiweb,
        "parsonline" to R.drawable.op_parsonline,
        "mobinnet" to R.drawable.op_mobinnet,
        "sabanet" to R.drawable.op_sabanet,
        "datak" to R.drawable.op_datak,
        "pishgaman" to R.drawable.op_pishgaman,
        "fanava" to R.drawable.op_fanava,
        "zitel" to R.drawable.op_zitel,
        "afranet" to R.drawable.op_afranet,
        "mabna" to R.drawable.op_mabna,
        "mahannet" to R.drawable.op_mahannet,
        "laser" to R.drawable.op_laser,
        "respina" to R.drawable.op_respina,
        "tic" to R.drawable.op_tic,
    )

    /** شناسه‌ی همه‌ی اپراتورهایی که لوگوی رسمی دارند (برای تست یکپارچگی). */
    fun idsWithLogo(): Set<String> = logos.keys

    @DrawableRes
    fun logoFor(operatorId: String): Int? = logos[operatorId]

    /**
     * نسبت اندازه‌ی لوگو به کاشی. اندازه‌گذاری بصری داخل خود فایل‌ها انجام شده،
     * پس برای همه‌ی اپراتورها یکسان است؛ پارامتر برای سازگاری با فراخوانی‌ها مانده.
     */
    @Suppress("UNUSED_PARAMETER")
    fun fitScale(operatorId: String): Float = FIT_SCALE
}
