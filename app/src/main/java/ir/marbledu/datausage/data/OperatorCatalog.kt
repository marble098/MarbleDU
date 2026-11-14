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
    /** پیشوند شناسه‌ی ISPهای ناشناخته‌ی غیرایرانی. */
    private const val CUSTOM_PREFIX = "x:"

    /** پیشوند ISPهای ناشناخته‌ای که با اطمینان ایرانی هستند (اینترنت ثابت داخلی). */
    private const val IRANIAN_FIXED_PREFIX = "f:"

    /** اپراتورهای موبایل و MVNOها. ترتیب مهم است: نام‌های خاص‌تر جلوتر. */
    val iranian: List<Operator> = listOf(
        Operator(
            "mci", "همراه اول", "MCI", "MCI", 0xFF0095DA, 0xFF295CAB,
            mncs = listOf("11"), asns = listOf(197207),
            keywords = listOf(
                "mci", "hamrah", "همراه", "mobilecommunicationcompanyofiran",
                "irmci", "mcinet", "hamrahaval",
            ),
            isMobile = true,
        ),
        Operator(
            "irancell", "ایرانسل", "Irancell", "MTN", 0xFFFFCC00, 0xFFF39200,
            mncs = listOf("35"), asns = listOf(44244),
            keywords = listOf("irancell", "ایرانسل", "mtn", "mtnirancell", "irancelltd"),
            isMobile = true,
        ),
        Operator(
            "rightel", "رایتل", "Rightel", "RTL", 0xFF9B2776, 0xFFE35BB8,
            mncs = listOf("20"), asns = listOf(57218),
            keywords = listOf("rightel", "رایتل", "tamintelecom"),
            isMobile = true,
        ),
        Operator(
            "shatel_mobile", "شاتل موبایل", "Shatel Mobile", "SHM", 0xFF0B3B85, 0xFFF16321,
            mncs = listOf("08", "50"),
            keywords = listOf("shatelmobile", "شاتلموبایل"),
            isMobile = true,
        ),
        Operator(
            "taliya", "تالیا", "Taliya", "TAL", 0xFFE40613, 0xFFFF5A4E,
            mncs = listOf("32"),
            keywords = listOf("taliya", "تالیا"),
            isMobile = true,
        ),
        Operator(
            "aptel", "آپتل", "ApTel", "APT", 0xFFE03016, 0xFFA21A16,
            keywords = listOf("aptel", "آپتل"),
            isMobile = true,
        ),
        Operator(
            "samantel", "سامانتل", "SamanTel", "SMT", 0xFFF05F22, 0xFF898989,
            keywords = listOf("samantel", "سامانتل"),
            isMobile = true,
        ),
        Operator(
            "lotustel", "لوتوس تل", "Lotus Tel", "LTS", 0xFFF9B12E, 0xFF372A2B,
            keywords = listOf("lotustel", "lotus tel", "lotus-tel", "لوتوستل", "لوتوس تل", "لوتوس‌تل"),
            isMobile = true,
        ),
        Operator(
            "telekish", "تله‌کیش", "TeleKish", "TKC", 0xFF17B7A6, 0xFFFF9800,
            mncs = listOf("14"),
            keywords = listOf("telekish", "tkc", "kifzo", "تلهکیش"),
            isMobile = true,
        ),
        Operator(
            "mtce", "اسپادان", "MTCE (Espadan)", "MTC", 0xFFEC1C24, 0xFF221E1F,
            mncs = listOf("19"),
            keywords = listOf("mtce", "espadan", "اسپادان"),
            isMobile = true,
        ),
        Operator(
            "fanapmobile", "فناپ موبایل", "Fanap Mobile", "FNP", 0xFF1671B7, 0xFFF39229,
            keywords = listOf(
                "fanap", "فناپ", "fanapmobile", "فناپموبایل", "fanap mobile", "فناپ موبایل",
                "fnp", "fnpmobile", "فناپ موبایل-ir",
            ),
            isMobile = true,
        ),
        // نام رسمی «ارتباطات آرین تل» (Ariantel)؛ شناسه‌ی «arintel» برای سازگاری با
        // داده‌های ذخیره‌شده‌ی نسخه‌های قبل عوض نشده و املای قدیمی هم هنوز شناخته می‌شود.
        Operator(
            "arintel", "آرین‌تل", "Ariantel", "ART", 0xFF50E827, 0xFF8C8C8C,
            keywords = listOf(
                "ariantel", "arian tel", "آرینتل", "آرین تل", "ariantelecom",
                "arin", "arintel", "ارینتل", "arin tel", "ارین تل",
                "art", "arintelco",
            ),
            isMobile = true,
        ),
    )


    /** اپراتورهای اینترنت ثابت، فیبر و بی‌سیم / ISPها (برای وای‌فای و مودم). */
    val fixed: List<Operator> = listOf(
        Operator(
            "tci", "مخابرات", "TCI", "TCI", 0xFFE8001A, 0xFF1A1A1A,
            mncs = listOf("70", "99"), asns = listOf(58224),
            keywords = listOf(
                "tci", "mokhaberat", "مخابرات",
                "irantelecommunicationcompany", "telecommunicationcompanyofiran",
                "telecommunicationcompany",
            ),
        ),
        Operator(
            "shatel", "شاتل", "Shatel", "SHT", 0xFF0B3B85, 0xFFF16321,
            asns = listOf(31549),
            keywords = listOf("shatel", "ariashatel", "شاتل"),
        ),
        Operator(
            "asiatech", "آسیاتک", "Asiatech", "AST", 0xFF0188D1, 0xFFFAB301,
            asns = listOf(43754),
            keywords = listOf("asiatech", "آسیاتک"),
        ),
        Operator(
            "hiweb", "های‌وب", "HiWEB", "HiW", 0xFFE60012, 0xFFFF5A3C,
            asns = listOf(56402),
            keywords = listOf("hiweb", "dadehgostar", "avacell", "هایوب", "asrnovin"),
        ),
        Operator(
            "parsonline", "پارس‌آنلاین", "Pars Online", "POL", 0xFFDA261B, 0xFF3E6193,
            asns = listOf(16322),
            keywords = listOf("parsonline", "پارسآنلاین", "پارسانلاین"),
        ),
        Operator(
            "mobinnet", "مبین نت", "Mobin Net", "MBN", 0xFFACCF4F, 0xFF3A3A3A,
            asns = listOf(50810),
            keywords = listOf(
                "mobinnet", "mobin net", "mobin-net", "مبیننت", "مبین نت", "مبین نتم", "مبین‌نت",
            ),
            isMobile = true,
        ),
        Operator(
            "sabanet", "صبانت", "SabaNet", "SBN", 0xFFDA1E25, 0xFFFF6A5E,
            asns = listOf(39501),
            keywords = listOf("sabanet", "nedagostarsaba", "صبانت"),
        ),
        Operator(
            "respina", "رسپینا", "Respina", "RSP", 0xFF018299, 0xFF2BC4D8,
            asns = listOf(42337),
            keywords = listOf("respina", "رسپینا"),
        ),
        Operator(
            "datak", "داتک", "Datak", "DTK", 0xFFF08D04, 0xFFFFB547,
            asns = listOf(25124),
            keywords = listOf("datak", "داتک"),
        ),
        Operator(
            "pishgaman", "پیشگامان", "Pishgaman", "PSH", 0xFFEB323A, 0xFFFF7A6E,
            asns = listOf(49100),
            keywords = listOf("pishgaman", "پیشگامان"),
        ),
        Operator(
            "fanava", "فن‌آوا", "Fanava", "FNV", 0xFF00A3B0, 0xFF202A79,
            asns = listOf(24631, 41881),
            keywords = listOf("fanava", "فنآوا"),
        ),
        Operator(
            "zitel", "زی‌تل", "Zi-Tel", "ZTL", 0xFFAD1457, 0xFFEC407A,
            keywords = listOf("zitel", "soroushrasaneh", "زیتل"),
        ),
        Operator(
            "afranet", "افرانت", "Afranet", "AFR", 0xFF003C99, 0xFFE8412C,
            asns = listOf(25184),
            keywords = listOf("afranet", "افرانت"),
        ),
        Operator(
            "mabna", "مبنا تلکام", "Mabna Telecom", "MBT", 0xFFF08800, 0xFFFFB13D,
            keywords = listOf("mabnatelecom", "mabna", "مبنا"),
        ),
        Operator(
            "petiak", "پتیاک", "Petiak", "PTK", 0xFF00695C, 0xFF4DB6AC,
            keywords = listOf("petiak", "پتیاک"),
        ),
        Operator(
            "mahannet", "ماهان‌نت", "MahanNet", "MHN", 0xFFF37121, 0xFF1D8ACC,
            keywords = listOf("mahannet", "ماهاننت"),
        ),
        Operator(
            "laser", "لیزر", "Laser", "LSR", 0xFF003461, 0xFF1E6BB8,
            keywords = listOf("laser", "لیزر"),
        ),
        Operator(
            "tic", "زیرساخت", "TIC", "TIC", 0xFF23418D, 0xFF5C7BD0,
            asns = listOf(48159, 12880),
            keywords = listOf("telecommunicationinfrastructure", "informationtechnologycompany", "زیرساخت"),
        ),
        // ---- اینترنت ثابت خانگی، فیبر (FTTH/FTTx)، بی‌سیم و ارائه‌دهنده‌های داده
        Operator(
            "irantelekom", "ایران تلکام", "Iran Telekom", "ITK", 0xFF0D47A1, 0xFF1E88E5,
            keywords = listOf("irantelekom", "iran telekom", "ایرانتلکام", "ایران تلکام"),
        ),
        Operator(
            "aryanet", "آرین نت", "Aryan Net", "ARN", 0xFF006064, 0xFF26C6DA,
            keywords = listOf("aryanet", "aryan net", "ariyanet", "آریننت", "آرین نت", "آریانت"),
        ),
        Operator(
            "asianet", "آسیانت", "Asia Net", "ASN", 0xFF283593, 0xFF5C6BC0,
            keywords = listOf("asianet", "asia net", "آسیانت"),
        ),
        Operator(
            "aftabnet", "آفتاب نت", "Aftab Net", "AFT", 0xFFEF6C00, 0xFFFFB74D,
            keywords = listOf("aftabnet", "aftab net", "افتابنت", "آفتابنت", "آفتاب نت"),
        ),
        Operator(
            "datisnet", "داتیس نت", "Datis Net", "DTS", 0xFF6A1B9A, 0xFFBA68C8,
            keywords = listOf("datisnet", "datis net", "داتیسنت", "داتیس نت"),
        ),
        Operator(
            "iranserver", "ایران سرور", "Iran Server", "IRS", 0xFF2E7D32, 0xFF81C784,
            keywords = listOf("iranserver", "iran server", "ایرانسرور", "ایران سرور"),
        ),
        Operator(
            "arvancloud", "ابر آروان", "Arvan Cloud", "ARV", 0xFF00838F, 0xFF4DD0E1,
            keywords = listOf("arvan", "arvancloud", "arvan cloud", "آروان", "ابرآروان", "ابر آروان"),
        ),
        Operator(
            "parspack", "پارس پک", "ParsPack", "PPK", 0xFF37474F, 0xFF78909C,
            keywords = listOf("parspack", "pars pack", "پارسپک", "پارس پک"),
        ),
        Operator(
            "hostiran", "هاست ایران", "HostIran", "HIR", 0xFFAD1457, 0xFFF06292,
            keywords = listOf("hostiran", "host iran", "هاستایران", "هاست ایران"),
        ),
        Operator(
            "mihanwebhost", "میهن وب‌هاست", "Mihan Web Host", "MWH", 0xFF01579B, 0xFF4FC3F7,
            keywords = listOf("mihanwebhost", "mihanweb", "mihan web host", "میهنوبهاست", "میهن وب‌هاست"),
        ),
        Operator(
            "mizbanfa", "میزبان فا", "MizbanFa", "MZF", 0xFF33691E, 0xFF8BC34A,
            keywords = listOf("mizbanfa", "mizban fa", "میزبانفا", "میزبان فا"),
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
            // نام کامل فارسی/انگلیسی خودِ اپراتور هم نشانه‌ی تشخیص است؛ به همین
            // دلیل «Iran Telekom» یا «ابر آروان» حتی بدون کلیدواژه هم شناخته
            // می‌شوند. نام‌های کوتاه‌تر از ۴ حرف (TCI/MCI/MTN) نادیده گرفته
            // می‌شوند تا با متن‌های تصادفی اشتباه نشوند.
            for (name in listOf(op.fa, op.en)) {
                val nname = collapse(name)
                if (nname.length >= 4 && collapsed.contains(nname)) return op
            }
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

    /**
     * اپراتور ناشناس با نام دلخواه.
     *
     * [iranianFixed] را وقتی `true` می‌دهیم که مطمئنیم این ISP داخل ایران است
     * (کد کشور IR یا نشانه‌های نام). در این حالت نشان «اینترنت ثابت داخلی»
     * می‌گیرد و رنگ ثابت و شناسه‌ی ذخیره‌شدنی‌اش با `f:` شروع می‌شود تا بعد از
     * راه‌اندازی مجدد هم همین‌طور شناسایی شود.
     */
    fun custom(name: String, iranianFixed: Boolean = false): Operator {
        val clean = name.trim()
        if (clean.isBlank()) return unknown
        val letters = clean.filter { it.isLetterOrDigit() }
        val mark = letters.take(3).uppercase().ifBlank { "?" }
        val color = genericColors[(clean.hashCode() and 0x7fffffff) % genericColors.size]
        val prefix = if (iranianFixed) IRANIAN_FIXED_PREFIX else CUSTOM_PREFIX
        return Operator(prefix + clean, clean, clean, mark, color, isIranian = iranianFixed)
    }

    /**
     * آیا این نام به یک ارائه‌دهنده‌ی ایرانی اشاره می‌کند؟ برای وقتی است که
     * ISP در فهرست نیست و می‌خواهیم بدانیم نشان «داخلی» به آن بدهیم یا نه.
     */
    fun looksIranian(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val normalized = normalizePersian(raw)
        if (normalized.contains(".ir")) return true
        if (normalized.contains("ایران") || normalized.contains("تهران") || normalized.contains("کیش")) return true
        val collapsed = collapse(raw)
        return collapsed.contains("iran") ||
            collapsed.contains("tehran") ||
            collapsed.contains("persian") ||
            collapsed.contains("kish")
    }

    /** برعکس [Operator.id]: از شناسه‌ی ذخیره‌شده دوباره اپراتور می‌سازد. */
    fun byId(id: String?): Operator = when {
        id.isNullOrBlank() || id == ID_UNKNOWN -> unknown
        id == ID_WIFI -> wifi
        id == ID_MOBILE -> mobile
        id.startsWith(IRANIAN_FIXED_PREFIX) -> custom(id.removePrefix(IRANIAN_FIXED_PREFIX), iranianFixed = true)
        id.startsWith(CUSTOM_PREFIX) -> custom(id.removePrefix(CUSTOM_PREFIX))
        else -> all.firstOrNull { it.id == id } ?: custom(id, iranianFixed = looksIranian(id))
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
