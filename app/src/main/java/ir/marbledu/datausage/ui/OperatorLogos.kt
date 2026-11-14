package ir.marbledu.datausage.ui

import androidx.annotation.DrawableRes
import ir.marbledu.datausage.R

/**
 * لوگوی رسمی اپراتورها (drawable-nodpi/op_*.webp).
 * نشان‌ها متعلق به صاحبان‌شان هستند و فقط برای شناسایی اپراتور نمایش داده می‌شوند.
 * منابع در NOTICE-OPERATOR-LOGOS.md آمده است.
 */
object OperatorLogos {

    private val logos: Map<String, Int> = mapOf(
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
        "tci" to R.drawable.op_tci,
        "shatel" to R.drawable.op_shatel,
        "asiatech" to R.drawable.op_asiatech,
        "hiweb" to R.drawable.op_hiweb,
        "parsonline" to R.drawable.op_parsonline,
        "mobinnet" to R.drawable.op_mobinnet,
        "fanapmobile" to R.drawable.op_fanapmobile,
        "arintel" to R.drawable.op_arintel,
        "sabanet" to R.drawable.op_sabanet,
        "respina" to R.drawable.op_respina,
        "datak" to R.drawable.op_datak,
        "pishgaman" to R.drawable.op_pishgaman,
        "fanava" to R.drawable.op_fanava,
        "zitel" to R.drawable.op_zitel,
        "afranet" to R.drawable.op_afranet,
        "mabna" to R.drawable.op_mabna,
        "mahannet" to R.drawable.op_mahannet,
        "laser" to R.drawable.op_laser,
        "tic" to R.drawable.op_tic,
    )

    /**
     * لوگوهایی که خودشان روشن (سفید/کرم) هستند و روی کاشی سفید محو می‌شوند؛
     * این‌ها روی کاشیِ رنگ برند می‌نشینند تا هم دیده شوند و هم خوانا بمانند.
     *
     * معیار انتخاب: میانگین روشنایی پیکسل‌های مات لوگو بالای ۰٬۷ باشد
     * (samantel ۰٬۹۶ / telekish ۰٬۹۲ / aptel ۰٬۹۰ / mabna ۰٬۸۶ / mobinnet ۰٬۷۵ /
     * mtce ۰٬۷۵ / fanava ۰٬۹۱ / laser ۱٬۰۰). بقیه‌ی لوگوها روی کاشی سفید کنتراست
     * کافی دارند.
     */
    private val lightOnBrand: Set<String> = setOf(
        "fanava", "laser", "samantel", "telekish", "aptel", "mabna", "mobinnet", "mtce",
    )

    /** شناسه‌ی همه‌ی اپراتورهایی که لوگوی رسمی دارند (برای تست یکپارچگی). */
    fun idsWithLogo(): Set<String> = logos.keys

    @DrawableRes
    fun logoFor(operatorId: String): Int? = logos[operatorId]

    fun needsBrandTile(operatorId: String): Boolean = operatorId in lightOnBrand
}
