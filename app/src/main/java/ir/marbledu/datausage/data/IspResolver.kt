package ir.marbledu.datausage.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * شناسایی اپراتورِ اینترنتِ وای‌فای.
 *
 * وقتی گوشی به وای‌فای وصل است سیستم هیچ اطلاعی از اپراتور مودم نمی‌دهد. تنها
 * راه مطمئن این است که ببینیم IP عمومی مال کدام شبکه (AS) است. برای همین یک
 * درخواست کوچک HTTPS به یک سرویس «IP من چیست» فرستاده می‌شود؛ هیچ آمار مصرف یا
 * اطلاعات شخصی‌ای ارسال نمی‌شود و نتیجه تا عوض شدن شبکه کش می‌شود.
 *
 * اگر VPN روشن باشد، درخواست مستقیماً روی خودِ شبکه‌ی وای‌فای فرستاده می‌شود
 * (نه از داخل تونل) تا اپراتور واقعی دیده شود، نه سرور VPN.
 *
 * کاربر می‌تواند این قابلیت را از تنظیمات خاموش کند.
 */
object IspResolver {

    data class Result(
        val operator: OperatorCatalog.Operator,
        val rawIsp: String,
        val asn: Int,
        val publicIp: String,
        val countryCode: String,
        val networkKey: Long,
        val fetchedAt: Long,
    )

    /** نتیجه‌ی خام یک سرویس، قبل از تطبیق با کاتالوگ. */
    data class Lookup(val isp: String, val asn: Int, val ip: String, val countryCode: String)

    @Volatile
    var enabled: Boolean = true

    @Volatile
    private var cached: Result? = null

    @Volatile
    private var job: Job? = null

    @Volatile
    private var lastAttemptAt: Long = 0L

    @Volatile
    private var lastAttemptKey: Long = Long.MIN_VALUE

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private const val MAX_AGE_MS = 20L * 60 * 1000
    private const val RETRY_MS = 45L * 1000

    private val endpoints = listOf(
        "https://ipwho.is/",
        "https://ipapi.co/json/",
        "https://ipinfo.io/json",
        "https://api.my-ip.io/v2/ip.json",
    )

    /** نتیجه‌ی کش‌شده برای شبکه‌ی فعلی (اگر هنوز معتبر باشد). */
    fun current(networkKey: Long): Result? {
        if (!enabled) return null
        val c = cached ?: return null
        return if (c.networkKey == networkKey) c else null
    }

    /** شبکه‌ی فیزیکی وای‌فای/اترنت (بدون VPN)، اگر وصل باشد. */
    @Suppress("DEPRECATION")
    fun physicalFixedNetwork(context: Context): Network? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        return runCatching {
            cm.allNetworks.firstOrNull { network ->
                val caps = cm.getNetworkCapabilities(network) ?: return@firstOrNull false
                !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            }
        }.getOrNull()
    }

    /**
     * اگر لازم بود، در پس‌زمینه اپراتور وای‌فای را پیدا می‌کند. صدا زدنش ارزان
     * است و می‌شود هر ثانیه صدایش زد.
     */
    fun ensureFresh(context: Context, force: Boolean = false) {
        if (!enabled) return
        if (job?.isActive == true) return
        val network = physicalFixedNetwork(context) ?: return
        val key = network.networkHandle
        val now = System.currentTimeMillis()
        val c = cached
        val fresh = c != null && c.networkKey == key && now - c.fetchedAt < MAX_AGE_MS
        if (fresh && !force) return
        if (!force && lastAttemptKey == key && now - lastAttemptAt < RETRY_MS) return
        lastAttemptAt = now
        lastAttemptKey = key
        job = scope.launch {
            val lookup = fetch(network)
            if (lookup != null) {
                val operator = OperatorCatalog.fromAsn(lookup.asn, lookup.isp)
                    ?: OperatorCatalog.custom(cleanIspName(lookup.isp))
                cached = Result(
                    operator = operator,
                    rawIsp = lookup.isp,
                    asn = lookup.asn,
                    publicIp = lookup.ip,
                    countryCode = lookup.countryCode,
                    networkKey = key,
                    fetchedAt = System.currentTimeMillis(),
                )
            }
        }
    }

    private fun fetch(network: Network): Lookup? {
        for (endpoint in endpoints) {
            val body = runCatching {
                val connection = network.openConnection(URL(endpoint)) as HttpURLConnection
                try {
                    connection.connectTimeout = 4000
                    connection.readTimeout = 4000
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("User-Agent", "MarbleDU")
                    if (connection.responseCode in 200..299) {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } else null
                } finally {
                    connection.disconnect()
                }
            }.getOrNull() ?: continue
            val lookup = runCatching { parse(JSONObject(body)) }.getOrNull()
            if (lookup != null && (lookup.asn > 0 || lookup.isp.isNotBlank())) return lookup
        }
        return null
    }

    /** پاسخ سرویس‌های مختلف شکل‌های مختلفی دارد؛ همه را یک‌جا می‌فهمیم. */
    private fun parse(json: JSONObject): Lookup? {
        if (json.has("success") && !json.optBoolean("success", true)) return null
        if (json.optBoolean("error", false)) return null
        val connection = json.optJSONObject("connection")
        val asnObject = json.optJSONObject("asn")
        val org = listOf(
            connection?.optString("isp").orEmpty(),
            connection?.optString("org").orEmpty(),
            asnObject?.optString("name").orEmpty(),
            json.optString("org"),
            json.optString("isp"),
            json.optString("as"),
        ).firstOrNull { it.isNotBlank() && it != "null" }.orEmpty()
        val asnText = listOf(
            connection?.optString("asn").orEmpty(),
            asnObject?.optString("number").orEmpty(),
            asnObject?.optString("asn").orEmpty(),
            json.optString("asn"),
            json.optString("as"),
            json.optString("org"),
        ).firstOrNull { parseAsn(it) > 0 }.orEmpty()
        val ip = json.optString("ip").ifBlank { json.optString("query") }
        val country = listOf(
            json.optString("country_code"),
            json.optString("countryCode"),
            json.optString("country"),
        ).firstOrNull { it.length == 2 }.orEmpty()
        return Lookup(isp = org, asn = parseAsn(asnText), ip = ip, countryCode = country)
    }

    /** «AS44244 Iran Cell ...» یا «44244» → 44244 */
    fun parseAsn(text: String?): Int {
        if (text.isNullOrBlank()) return 0
        val trimmed = text.trim()
        trimmed.toIntOrNull()?.let { return if (it > 0) it else 0 }
        val match = Regex("^[Aa][Ss](\\d{1,10})").find(trimmed) ?: return 0
        return match.groupValues[1].toIntOrNull() ?: 0
    }

    /** «AS31549 Aria Shatel PJSC» → «Aria Shatel» */
    fun cleanIspName(raw: String): String {
        var name = raw.trim().replace(Regex("^[Aa][Ss]\\d+\\s+"), "")
        val suffixes = listOf(
            "PJSC", "PJS", "PLC", "LLC", "Ltd.", "Ltd", "Inc.", "Inc", "Co.", "Company", "Corporation",
            "(Private Joint Stock Company)", "Private Joint Stock Company",
        )
        var changed = true
        while (changed) {
            changed = false
            for (suffix in suffixes) {
                if (name.length > suffix.length + 2 && name.endsWith(" $suffix", ignoreCase = true)) {
                    name = name.dropLast(suffix.length + 1).trim().trimEnd(',', '-')
                    changed = true
                }
            }
        }
        return name.take(28).trim()
    }
}
