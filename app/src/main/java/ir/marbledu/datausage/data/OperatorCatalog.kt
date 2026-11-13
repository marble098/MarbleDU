package ir.marbledu.datausage.data

/**
 * دیتابیس اپراتورهای ایرانی + بین‌المللی.
 * نام خام اپراتور (که سیستم یا سیم‌کارت برمی‌گرداند) به نام تمیز فارسی/انگلیسی
 * و رنگ برند تبدیل می‌شود؛ همین‌طور پیشوندهای MCC/MNC رایج ایران.
 */
object OperatorCatalog {

    data class Operator(
        val id: String,
        val fa: String,
        val en: String,
        val mnc: String,
        val brandColor: Long,
        val isIranian: Boolean = true,
    )

    val iranian = listOf(
        Operator("mci", "همراه اول", "MCI", "11", 0xFF0A6ABF),
        Operator("irancell", "ایرانسل", "Irancell", "35", 0xFFFFC400),
        Operator("rightel", "رایتل", "Rightel", "20", 0xFF7B2FBE),
        Operator("shatel", "شاتل موبایل", "Shatel Mobile", "32", 0xFFE4002B),
        Operator("aptel", "آپتل", "ApTel", "14", 0xFF00838F),
        Operator("samanTel", "سامانتل", "SamanTel", "98", 0xFF2E7D32),
        Operator("taliya", "تالیا", "Taliya", "19", 0xFF8D6E63),
        Operator("kishTc", "کیش‌تل", "Kish TC", "93", 0xFF00897B),
        Operator("asiatech", "آسیاتک", "Asiatech", "99", 0xFF1E88E5),
        Operator("parsTel", "پارس‌تل", "ParsTel", "94", 0xFF6A1B9A),
    )

    // MCC ایران 432 است؛ MNCهای دو رقمی بالا داخل 432 معنی دارند.
    private const val IRAN_MCC = "432"

    /** نام نمایشی اپراتور از روی متن خام سیستم. */
    fun normalizeDisplay(raw: String): String {
        val key = raw.lowercase().replace(" ", "").replace("-", "")
        return when {
            key.contains("irancell") || key.contains("mtn") -> iranian[1].fa
            key.contains("mci") || key.contains("hamrahe") || key.contains("همراه") -> iranian[0].fa
            key.contains("rightel") || key.contains("رایتل") -> iranian[2].fa
            key.contains("taliya") -> iranian[6].fa
            key.contains("shatel") -> iranian[3].fa
            else -> raw
        }
    }

    /** اپراتور از روی MCC+MNC (اگر دسترسی داده شود). */
    fun fromMccMnc(mcc: String?, mnc: String?): Operator? {
        if (mcc != IRAN_MCC) return null
        return iranian.firstOrNull { it.mnc == mnc }
    }

    fun networkTypeName(fa: Boolean, networkType: Int): String {
        return when (networkType) {
            android.telephony.TelephonyManager.NETWORK_TYPE_NR -> if (fa) "۵G" else "5G"
            android.telephony.TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP,
            android.telephony.TelephonyManager.NETWORK_TYPE_HSPA,
            android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA,
            android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA,
            android.telephony.TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
            android.telephony.TelephonyManager.NETWORK_TYPE_EDGE,
            android.telephony.TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
            android.telephony.TelephonyManager.NETWORK_TYPE_UNKNOWN -> if (fa) "نامشخص" else "Unknown"
            else -> "—"
        }
    }
}
