package ir.marbledu.datausage.data

import android.app.usage.NetworkStats
import android.telephony.TelephonyManager

/**
 * دیتابیس اپراتورهای ایرانی (سیم‌کارت + اینترنت ثابت + MVNOها).
 *
 * راه‌های شناسایی:
 *  ۱) MCC/MNC سیم‌کارت (بدون هیچ مجوزی)،
 *  ۲) نام خام اپراتور، نام وای‌فای (SSID) یا دامنه‌ی DHCP،
 *  ۳) شماره‌ی AS و نام ISP از روی IP عمومی، DNS اپراتور یا پیشوند IPv6.
 */
object OperatorCatalog {

    data class Operator(
        val id: String,
        val fa: String,
        val en: String,
        /** حروف کوتاه روی نشان اپراتور. */
        val mark: String,
        val brandColor: Long,
        val accentColor: Long = brandColor,
        val mncs: List<String> = emptyList(),
        val asns: List<Int> = emptyList(),
        val keywords: List<String> = emptyList(),
        val isMobile: Boolean = false,
        val isIranian: Boolean = true,
    ) {
        val mnc: String get() = mncs.firstOrNull().orEmpty()
        fun name(persian: Boolean): String = if (persian) fa else en
    }

    const val ID_WIFI = "wifi"
    const val ID_MOBILE = "mobile"
    const val ID_UNKNOWN = "unknown"
    private const val CUSTOM_PREFIX = "x:"

    /** اپراتورهای موبایل و MVNOها. ترتیب مهم است: نام‌های خاص‌تر جلوتر. */
    val iranian: List<Operator> = listOf(
        Operator(
            "mci", "همراه اول", "MCI", "MCI", 0xFF00A6C8, 0xFF0B4F8A,
            mncs = listOf("11"), asns = listOf(197207),
            keywords = listOf(
                "mci", "hamrah", "همراه", "mobilecommunicationcompanyofiran",
                "irmci", "mcinet", "hamrahaval",
            ),
            isMobile = true,
        ),
        Operator(
            "irancell", "ایرانسل", "Irancell", "MTN", 0xFFFFC61A, 0xFFF39200,
            mncs = listOf("35"), asns = listOf(44244),
            keywords = listOf("irancell", "ایرانسل", "mtn", "mtnirancell", "irancelltd"),
            isMobile = true,
        ),
        Operator(
            "rightel", "رایتل", "Rightel", "RTL", 0xFF9B2FD6, 0xFFE0218A,
            mncs = listOf("20"), asns = listOf(57218),
            keywords = listOf("rightel", "رایتل", "tamintelecom"),
            isMobile = true,
        ),
        Operator(
            "shatel_mobile", "شاتل موبایل", "Shatel Mobile", "SHM", 0xFFE4002B, 0xFFFF6B4A,
            mncs = listOf("08", "50"),
            keywords = listOf("shatelmobile", "شاتلموبایل"),
            isMobile = true,
        ),
        Operator(
            "taliya", "تالیا", "Taliya", "TAL", 0xFF8D6E63, 0xFFBF8F6B,
            mncs = listOf("32"),
            keywords = listOf("taliya", "تالیا"),
            isMobile = true,
        ),
        Operator(
            "aptel", "آپتل", "ApTel", "APT", 0xFF00838F, 0xFF26C6DA,
            keywords = listOf("aptel", "آپتل"),
            isMobile = true,
        ),
        Operator(
            "samantel", "سامانتل", "SamanTel", "SMT", 0xFF2E7D32, 0xFF66BB6A,
            keywords = listOf("samantel", "سامانتل"),
            isMobile = true,
        ),
        Operator(
            "lotustel", "لوتوس تل", "Lotus Tel", "LTS", 0xFF5E35B1, 0xFF9575CD,
            keywords = listOf("lotustel", "lotus tel", "lotus-tel", "لوتوستل", "لوتوس تل", "لوتوس‌تل"),
            isMobile = true,
        ),
        Operator(
            "anarestan", "انارستان", "Anarestan", "ANR", 0xFFD81B60, 0xFFF06292,
            keywords = listOf("anarestan", "انارستان"),
            isMobile = true,
        ),
        Operator(
            "telekish", "تله‌کیش", "TeleKish", "TKC", 0xFF00897B, 0xFF4DB6AC,
            mncs = listOf("14"),
            keywords = listOf("telekish", "tkc", "kifzo", "تلهکیش"),
            isMobile = true,
        ),
        Operator(
            "mtce", "اسپادان", "MTCE (Espadan)", "MTC", 0xFF5D4037, 0xFFA1887F,
            mncs = listOf("19"),
            keywords = listOf("mtce", "espadan", "اسپادان"),
            isMobile = true,
        ),
        Operator(
            "fanapmobile", "فناپ موبایل", "Fanap Mobile", "FNP", 0xFF6D4C41, 0xFF9E9E9E,
            keywords = listOf(
                "fanap", "فناپ", "fanapmobile", "فناپموبایل", "fanap mobile", "فناپ موبایل",
                "fnp", "fnpmobile", "فناپ موبایل-ir",
            ),
            isMobile = true,
        ),
        Operator(
            "arintel", "ارین تل", "Arin Tel", "ART", 0xFF455A64, 0xFF78909C,
            keywords = listOf(
                "arin", "ارین", "arintel", "ارینتل", "arin tel", "ارین تل",
                "art", "arintelco",
            ),
            isMobile = true,
        ),
    )


    /** اپراتورهای اینترنت ثابت، فیبر و بی‌سیم / ISPها (برای وای‌فای و مودم). */
    val fixed: List<Operator> = listOf(
        Operator(
            "tci", "مخابرات", "TCI", "TCI", 0xFF1565C0, 0xFF42A5F5,
            mncs = listOf("70", "99"), asns = listOf(58224),
            keywords = listOf(
                "tci", "mokhaberat", "مخابرات",
                "irantelecommunicationcompany", "telecommunicationcompanyofiran",
                "telecommunicationcompany",
            ),
        ),
        Operator(
            "shatel", "شاتل", "Shatel", "SHT", 0xFFD81B36, 0xFF1E4FA3,
            asns = listOf(31549),
            keywords = listOf("shatel", "ariashatel", "شاتل"),
        ),
        Operator(
            "asiatech", "آسیاتک", "Asiatech", "AST", 0xFF1E88E5, 0xFF00B8D4,
            asns = listOf(43754),
            keywords = listOf("asiatech", "آسیاتک"),
        ),
        Operator(
            "hiweb", "های‌وب", "HiWEB", "HiW", 0xFFF57C00, 0xFFFFB300,
            asns = listOf(56402),
            keywords = listOf("hiweb", "dadehgostar", "avacell", "هایوب", "asrnovin"),
        ),
        Operator(
            "parsonline", "پارس‌آنلاین", "Pars Online", "POL", 0xFF00796B, 0xFF26A69A,
            asns = listOf(16322),
            keywords = listOf("parsonline", "پارسآنلاین", "پارسانلاین"),
        ),
        Operator(
            "mobinnet", "مبین نت", "Mobin Net", "MBN", 0xFF6A1B9A, 0xFFAB47BC,
            asns = listOf(50810),
            keywords = listOf(
                "mobinnet", "mobin net", "mobin-net", "مبیننت", "مبین نت", "مبین نتم", "مبین‌نت",
            ),
            isMobile = true,
        ),
        Operator(
            "sabanet", "صبانت", "SabaNet", "SBN", 0xFF2E7D32, 0xFF9CCC65,
            asns = listOf(39501),
            keywords = listOf("sabanet", "nedagostarsaba", "صبانت"),
        ),
        Operator(
            "respina", "رسپینا", "Respina", "RSP", 0xFFC62828, 0xFFEF5350,
            asns = listOf(42337),
            keywords = listOf("respina", "رسپینا"),
        ),
        Operator(
            "datak", "داتک", "Datak", "DTK", 0xFF283593, 0xFF5C6BC0,
            asns = listOf(25124),
            keywords = listOf("datak", "داتک"),
        ),
        Operator(
            "pishgaman", "پیشگامان", "Pishgaman", "PSH", 0xFF00838F, 0xFF4DD0E1,
            asns = listOf(49100),
            keywords = listOf("pishgaman", "پیشگامان"),
        ),
        Operator(
            "fanava", "فن‌آوا", "Fanava", "FNV", 0xFF4527A0, 0xFF7E57C2,
            asns = listOf(24631, 41881),
            keywords = listOf("fanava", "فنآوا"),
        ),
        Operator(
            "zitel", "زی‌تل", "Zi-Tel", "ZTL", 0xFFAD1457, 0xFFEC407A,
            keywords = listOf("zitel", "soroushrasaneh", "زیتل"),
        ),
        Operator(
            "afranet", "افرانت", "Afranet", "AFR", 0xFF0277BD, 0xFF29B6F6,
            asns = listOf(25184),
            keywords = listOf("afranet", "افرانت"),
        ),
        Operator(
            "mabna", "مبنا تلکام", "Mabna Telecom", "MBT", 0xFF37474F, 0xFF78909C,
            keywords = listOf("mabnatelecom", "mabna", "مبنا"),
        ),
        Operator(
            "petiak", "پتیاک", "Petiak", "PTK", 0xFF00695C, 0xFF4DB6AC,
            keywords = listOf("petiak", "پتیاک"),
        ),
        Operator(
            "mahannet", "ماهان‌نت", "MahanNet", "MHN", 0xFF1A237E, 0xFF5C6BC0,
            keywords = listOf("mahannet", "ماهاننت"),
        ),
        Operator(
            "laser", "لیزر", "Laser", "LSR", 0xFFBF360C, 0xFFFF7043,
            keywords = listOf("laser", "لیزر"),
        ),
        Operator(
            "tic", "زیرساخت", "TIC", "TIC", 0xFF455A64, 0xFF90A4AE,
            asns = listOf(48159, 12880),
            keywords = listOf("telecommunicationinfrastructure", "informationtechnologycompany", "زیرساخت"),
        ),
    )

    val all: List<Operator> = iranian + fixed

    private val wifi = Operator(ID_WIFI, "وای‌فای", "Wi-Fi", "WiFi", 0xFF00A99D, 0xFF2F6FED, isIranian = false)
    private val mobile = Operator(ID_MOBILE, "داده موبایل", "Mobile data", "SIM", 0xFF2F6FED, 0xFF7B5CFA, isIranian = false)
    private val unknown = Operator(ID_UNKNOWN, "نامشخص", "Unknown", "?", 0xFF78909C, 0xFFB0BEC5, isIranian = false)

    // MCC ایران 432 است.
    private const val IRAN_MCC = "432"

    private val genericColors = longArrayOf(
        0xFF5C6BC0, 0xFF26A69A, 0xFFEF6C00, 0xFFAB47BC, 0xFF29B6F6, 0xFFEC407A, 0xFF7CB342, 0xFF8D6E63,
    )

    /** یکسان‌سازی حروف عربی/فارسی و حذف نیم‌فاصله برای تطبیق دقیق SSID و نام ISP. */
    private fun normalizePersian(raw: String): String = buildString(raw.length) {
        for (ch in raw.lowercase()) {
            when (ch) {
                'ي', 'ئ' -> append('ی')
                'ك' -> append('ک')
                'ة' -> append('ه')
                'أ', 'إ', 'آ' -> append('ا')
                'ؤ' -> append('و')
                '\u200C', '\u200D', '\u200E', '\u200F' -> Unit
                else -> append(ch)
            }
        }
    }

    private fun collapse(raw: String): String =
        normalizePersian(raw).filter { it.isLetterOrDigit() }

    private fun words(raw: String): List<String> =
        normalizePersian(raw).split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }

    /**
     * اپراتور از روی نام خام. کلیدواژه‌های کوتاه (تا ۳ حرف مثل mci/tci/mtn) فقط
     * وقتی قبول می‌شوند که یک «کلمه‌ی کامل» باشند تا اشتباهی داخل اسم‌های دیگر
     * پیدا نشوند.
     */
    fun fromName(raw: String?): Operator? {
        if (raw.isNullOrBlank()) return null
        val collapsed = collapse(raw)
        val tokens = words(raw)
        if (collapsed.isEmpty()) return null
        for (op in all) {
            for (kw in op.keywords) {
                val nkw = collapse(kw)
                val hit = if (nkw.length <= 3) tokens.contains(nkw) || collapsed == nkw
                else collapsed.contains(nkw)
                if (hit) return op
            }
            // شناسه‌ی کوتاهِ نمایشی (mark) هم نشانِ تشخیص است (مثل FNP / ART)
            val mark = collapse(op.mark)
            if (mark.isNotEmpty() && (tokens.contains(mark) || collapsed == mark)) return op
        }
        return null
    }

    /** نام نمایشی فارسی اپراتور از روی متن خام سیستم. */
    fun normalizeDisplay(raw: String): String = fromName(raw)?.fa ?: raw

    /** اپراتور از روی MCC+MNC. */
    fun fromMccMnc(mcc: String?, mnc: String?): Operator? {
        if (mcc != IRAN_MCC || mnc.isNullOrBlank()) return null
        val normalized = mnc.trim().let { if (it.length == 1) "0$it" else it }
        return all.firstOrNull { op -> op.mncs.contains(normalized) }
    }

    /** اپراتور از روی رشته‌ی عددی MCC+MNC (مثل "43235"). */
    fun fromNumeric(numeric: String?): Operator? {
        if (numeric == null || numeric.length < 5) return null
        return fromMccMnc(numeric.substring(0, 3), numeric.substring(3))
    }

    /** اپراتور از روی شماره‌ی AS و نام سازمان/ISP. */
    fun fromAsn(asn: Int, org: String?): Operator? {
        if (asn > 0) all.firstOrNull { it.asns.contains(asn) }?.let { return it }
        return fromName(org)
    }

    /** اپراتور ناشناس با نام دلخواه (مثلاً ISP خارجی). */
    fun custom(name: String): Operator {
        val clean = name.trim()
        if (clean.isBlank()) return unknown
        val letters = clean.filter { it.isLetterOrDigit() }
        val mark = letters.take(3).uppercase().ifBlank { "?" }
        val color = genericColors[(clean.hashCode() and 0x7fffffff) % genericColors.size]
        return Operator(CUSTOM_PREFIX + clean, clean, clean, mark, color, isIranian = false)
    }

    /** برعکس [Operator.id]: از شناسه‌ی ذخیره‌شده دوباره اپراتور می‌سازد. */
    fun byId(id: String?): Operator = when {
        id.isNullOrBlank() || id == ID_UNKNOWN -> unknown
        id == ID_WIFI -> wifi
        id == ID_MOBILE -> mobile
        id.startsWith(CUSTOM_PREFIX) -> custom(id.removePrefix(CUSTOM_PREFIX))
        else -> all.firstOrNull { it.id == id } ?: custom(id)
    }

    /** از روی نام خام: اگر شناخته‌شده بود همان، وگرنه یک اپراتور سفارشی. */
    fun resolveName(raw: String?): Operator =
        if (raw.isNullOrBlank()) unknown else byIdOrName(raw)

    private fun byIdOrName(raw: String): Operator {
        all.firstOrNull { it.id == raw }?.let { return it }
        return fromName(raw) ?: custom(raw)
    }

    /** نام بومی برای شناسه‌های سیستمیِ اندروید در تفکیک مصرف اپ‌ها. */
    fun specialUidLabel(uid: Int, persian: Boolean): String? = when (uid) {
        NetworkStats.Bucket.UID_REMOVED -> if (persian) "اپ‌های حذف‌شده" else "Removed apps"
        NetworkStats.Bucket.UID_TETHERING -> if (persian) "هات‌اسپات و اشتراک‌گذاری" else "Hotspot / tethering"
        0 -> if (persian) "هسته‌ی سیستم" else "System core"
        1000 -> if (persian) "سیستم اندروید" else "Android system"
        1001 -> if (persian) "سرویس تلفن و سیم‌کارت" else "Phone / Radio"
        1013 -> if (persian) "سرویس رسانه" else "Media server"
        else -> null
    }

    fun networkTypeName(fa: Boolean, networkType: Int): String {
        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_NR -> if (fa) "۵G" else "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
            TelephonyManager.NETWORK_TYPE_UNKNOWN -> if (fa) "نامشخص" else "Unknown"
            else -> "—"
        }
    }
}
