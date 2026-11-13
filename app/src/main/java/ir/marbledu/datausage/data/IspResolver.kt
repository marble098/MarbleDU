package ir.marbledu.datausage.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URL

/**
 * شناسایی چندلایه اپراتور اینترنت وای‌فای / هات‌اسپات / مودم.
 *
 * استراتژی‌های ترکیبی:
 *  ۱) درخواست موازی و سریع به چند سرویس GeoIP روی شبکه‌ی فیزیکی وای‌فای
 *  ۲) دریافت IP عمومی بدون نیاز به DNS (از `1.1.1.1` / `1.0.0.1`) و IPv4-only و
 *     سپس پیدا کردن AS و نام ISP از API رسمی RIPE NCC (`stat.ripe.net` / RDAP)
 *  ۳) اگر VPN با Kill-Switch مانع درخواست مستقیم شود: بررسی سرور DNS اختصاصی
 *     اپراتور و پیشوند IPv6 عمومی در `LinkProperties` از طریق RIPE NCC
 *  ۴) سرنخ‌های محلی فوری: دامنه‌ی DHCP، نام وای‌فای (SSID) و تشخیص هات‌اسپات از مودم
 *  ۵) وضعیت صریح (در حال بررسی / نیازمند ورود / مسدود توسط VPN / ناموفق) تا هیچ‌وقت
 *     روی «در حال شناسایی…» قفل نشود.
 */
object IspResolver {

    enum class LookupStatus {
        IDLE,
        SEARCHING,
        RESOLVED,
        LOCAL_HINT,
        CAPTIVE_PORTAL,
        VPN_BLOCKED,
        FAILED,
    }

    enum class WifiSourceKind {
        UNKNOWN,
        MODEM_ROUTER,
        HOTSPOT_ANDROID,
        HOTSPOT_IPHONE,
        HOTSPOT_WINDOWS,
        HOTSPOT_METERED,
        ETHERNET,
    }

    enum class DetectionMethod {
        NONE,
        PUBLIC_IP_ASN,
        DNS_SERVER_ISP,
        IPV6_PREFIX,
        DHCP_DOMAIN,
        WIFI_SSID,
    }

    data class Result(
        val operator: OperatorCatalog.Operator,
        val rawIsp: String,
        val asn: Int,
        val publicIp: String,
        val countryCode: String,
        val networkKey: Long,
        val fetchedAt: Long,
        val status: LookupStatus = LookupStatus.RESOLVED,
        val sourceKind: WifiSourceKind = WifiSourceKind.UNKNOWN,
        val detectionMethod: DetectionMethod = DetectionMethod.PUBLIC_IP_ASN,
        val wifiSsid: String = "",
    )

    /** نتیجه‌ی خام یک سرویس، قبل از تطبیق با کاتالوگ. */
    data class Lookup(val isp: String, val asn: Int, val ip: String, val countryCode: String)

    /** سرنخ‌های استخراج‌شده از شبکه‌ی محلی وای‌فای بدون نیاز به دسترسی اضافه. */
    data class NetworkClues(
        val sourceKind: WifiSourceKind = WifiSourceKind.UNKNOWN,
        val ssid: String = "",
        val dhcpDomain: String = "",
        val gatewayIp: String = "",
        val ispDnsCandidates: List<String> = emptyList(),
        val globalIpv6Candidates: List<String> = emptyList(),
        val isCaptivePortal: Boolean = false,
        val isEthernet: Boolean = false,
        val isMetered: Boolean = false,
    )

    data class NetworkState(
        val networkKey: Long = 0L,
        val status: LookupStatus = LookupStatus.IDLE,
        val sourceKind: WifiSourceKind = WifiSourceKind.UNKNOWN,
        val wifiSsid: String = "",
        val result: Result? = null,
    )

    @Volatile
    var enabled: Boolean = true

    @Volatile
    private var cached: Result? = null

    @Volatile
    private var currentState: NetworkState = NetworkState()

    @Volatile
    private var job: Job? = null

    @Volatile
    private var lastAttemptAt: Long = 0L

    @Volatile
    private var lastAttemptKey: Long = Long.MIN_VALUE

    private val _updates = MutableStateFlow(0L)
    val updates: StateFlow<Long> = _updates.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private const val MAX_AGE_MS = 20L * 60 * 1000
    private const val RETRY_MS = 35L * 1000
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android) MarbleDU/1.2"
    private val IPV4_REGEX = Regex("^(\\d{1,3}\\.){3}\\d{1,3}$")

    /** رده‌ی اول: سرویس‌های کامل GeoIP که با هم (موازی) صدا زده می‌شوند. */
    private val geoIpBatch1 = listOf(
        "https://ipwho.is/",
        "https://get.geojs.io/v1/ip/geo.json",
        "https://api.ip.sb/geoip",
        "https://ipinfo.io/json",
        "https://freeipapi.com/api/json",
    )

    /** رده‌ی دوم GeoIP در صورت خطا یا محدودیت نرخ در رده‌ی اول. */
    private val geoIpBatch2 = listOf(
        "https://api.my-ip.io/v2/ip.json",
        "https://ipapi.co/json/",
    )

    /**
     * رده‌ی IP-فقط: شامل آدرس‌های عددی بدون نیاز به DNS (`1.1.1.1` و `1.0.0.1`)
     * و سرویس‌های IPv4-فقط تا در صورت خرابی DNS یا IPv6 هم سریع جواب بدهند.
     */
    private val ipEchoEndpoints = listOf(
        "https://1.1.1.1/cdn-cgi/trace",
        "https://1.0.0.1/cdn-cgi/trace",
        "https://cloudflare.com/cdn-cgi/trace",
        "https://api.ipify.org?format=json",
        "https://v4.ident.me",
        "https://checkip.amazonaws.com",
        "https://icanhazip.com",
    )

    /** DNSهای عمومی جهانی و ضدهرم/تحریم‌شکن ایران که نباید به‌جای ISP اشتباه گرفته شوند. */
    private val publicResolverIps = setOf(
        "8.8.8.8", "8.8.4.4",
        "1.1.1.1", "1.0.0.1", "1.1.1.2", "1.0.0.2", "1.1.1.3", "1.0.0.3",
        "9.9.9.9", "149.112.112.112",
        "4.2.2.1", "4.2.2.2", "4.2.2.3", "4.2.2.4", "4.2.2.5", "4.2.2.6",
        "208.67.222.222", "208.67.220.220",
        "94.140.14.14", "94.140.15.15",
        "45.90.28.0", "45.90.30.0",
        "2001:4860:4860::8888", "2001:4860:4860::8844",
        "2606:4700:4700::1111", "2606:4700:4700::1001",
        // تحریم‌شکن‌های معروف ایران
        "178.22.122.100", "185.51.200.2",        // شکن
        "10.202.10.202", "10.202.10.102",        // ۴۰۳
        "10.202.10.10", "10.202.10.11",          // رادار
        "78.157.42.100", "78.157.42.101",        // الکترو
        "185.55.226.26", "185.55.225.25",        // بگذر
        "94.103.125.157", "94.103.125.158",      // شلتر
    )

    private fun notifyUpdated() {
        _updates.value = _updates.value + 1L
    }

    /** نتیجه‌ی کش‌شده برای شبکه‌ی فعلی (اگر اپراتور با موفقیت پیدا شده باشد). */
    fun current(networkKey: Long): Result? {
        if (!enabled) return null
        val c = cached ?: return null
        return if (c.networkKey == networkKey &&
            (c.status == LookupStatus.RESOLVED || c.status == LookupStatus.LOCAL_HINT)
        ) c else null
    }

    /** وضعیت کامل شبکه‌ی فعلی (شامل در حال جستجو، ناموفق، مسدود توسط VPN و سرنخ هات‌اسپات). */
    fun stateFor(networkKey: Long): NetworkState {
        if (!enabled) return NetworkState(networkKey = networkKey, status = LookupStatus.IDLE)
        val state = currentState
        return if (state.networkKey == networkKey) state
        else NetworkState(networkKey = networkKey, status = LookupStatus.IDLE)
    }

    /** شبکه‌ی فیزیکی وای‌فای/اترنت (بدون VPN)، اگر وصل باشد. */
    @Suppress("DEPRECATION")
    fun physicalFixedNetwork(context: Context): Network? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        return runCatching {
            val active = cm.activeNetwork
            val activeCaps = active?.let { cm.getNetworkCapabilities(it) }
            if (active != null && activeCaps != null &&
                !activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                (activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            ) {
                return@runCatching active
            }
            val candidates = cm.allNetworks.mapNotNull { net ->
                val caps = cm.getNetworkCapabilities(net) ?: return@mapNotNull null
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return@mapNotNull null
                if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                    !caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                ) return@mapNotNull null
                net to caps
            }
            candidates.firstOrNull { (_, c) ->
                c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }?.first
                ?: candidates.firstOrNull { (_, c) ->
                    c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }?.first
                ?: candidates.firstOrNull()?.first
        }.getOrNull()
    }

    private fun isVpnActive(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        return runCatching {
            val activeCaps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            if (activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) return@runCatching true
            @Suppress("DEPRECATION")
            cm.allNetworks.any { net ->
                cm.getNetworkCapabilities(net)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            }
        }.getOrDefault(false)
    }

    /**
     * اگر لازم بود، در پس‌زمینه اپراتور وای‌فای/هات‌اسپات را پیدا می‌کند.
     * صدا زدنش ارزان است و تغییرات را بلافاصله از طریق [updates] اطلاع می‌دهد.
     */
    fun ensureFresh(context: Context, force: Boolean = false) {
        if (!enabled) return
        if (!force && job?.isActive == true) return
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        val network = physicalFixedNetwork(context) ?: return
        val key = network.networkHandle
        val caps = runCatching { cm.getNetworkCapabilities(network) }.getOrNull()
        val lp = runCatching { cm.getLinkProperties(network) }.getOrNull()
        val clues = extractLocalClues(context, caps, lp)
        val now = System.currentTimeMillis()
        val c = cached
        val fresh = c != null && c.networkKey == key &&
            c.status == LookupStatus.RESOLVED &&
            now - c.fetchedAt < MAX_AGE_MS
        if (fresh && !force) return
        if (!force && lastAttemptKey == key && now - lastAttemptAt < RETRY_MS) return

        if (force) job?.cancel()
        lastAttemptAt = now
        lastAttemptKey = key

        val existingValid = c?.takeIf {
            it.networkKey == key && (it.status == LookupStatus.RESOLVED || it.status == LookupStatus.LOCAL_HINT)
        }
        currentState = NetworkState(
            networkKey = key,
            status = LookupStatus.SEARCHING,
            sourceKind = clues.sourceKind,
            wifiSsid = clues.ssid,
            result = existingValid,
        )
        notifyUpdated()

        val appContext = context.applicationContext
        job = scope.launch {
            val vpnOn = isVpnActive(appContext)
            val resolved = resolveAllLayers(network, clues, vpnOn)
            if (resolved != null) {
                val finalResult = resolved.copy(
                    networkKey = key,
                    fetchedAt = System.currentTimeMillis(),
                    sourceKind = clues.sourceKind,
                    wifiSsid = clues.ssid,
                )
                cached = finalResult
                currentState = NetworkState(
                    networkKey = key,
                    status = finalResult.status,
                    sourceKind = clues.sourceKind,
                    wifiSsid = clues.ssid,
                    result = finalResult,
                )
            } else {
                val failStatus = when {
                    clues.isCaptivePortal -> LookupStatus.CAPTIVE_PORTAL
                    vpnOn -> LookupStatus.VPN_BLOCKED
                    else -> LookupStatus.FAILED
                }
                if (cached?.networkKey == key) cached = null
                currentState = NetworkState(
                    networkKey = key,
                    status = failStatus,
                    sourceKind = clues.sourceKind,
                    wifiSsid = clues.ssid,
                    result = null,
                )
            }
            notifyUpdated()
        }
    }

    private suspend fun resolveAllLayers(
        network: Network,
        clues: NetworkClues,
        vpnOn: Boolean,
    ): Result? {
        val allowDefaultWhenNoVpn = !vpnOn

        // لایه‌ی ۱: سرویس‌های سریع GeoIP به‌صورت موازی
        fetchFirstGeoIp(network, geoIpBatch1, allowDefaultWhenNoVpn, timeoutMs = 3500)?.let {
            return buildResult(it, LookupStatus.RESOLVED, DetectionMethod.PUBLIC_IP_ASN)
        }

        // لایه‌ی ۲: دریافت IP عمومی (حتی بدون DNS با 1.1.1.1) + تبدیل با RIPE NCC / RDAP + رده‌ی دوم GeoIP
        fetchFirstGeoIp(network, geoIpBatch2, allowDefaultWhenNoVpn, timeoutMs = 3000)?.let {
            return buildResult(it, LookupStatus.RESOLVED, DetectionMethod.PUBLIC_IP_ASN)
        }

        val publicIp = fetchFirstIp(network, ipEchoEndpoints, allowDefaultWhenNoVpn, timeoutMs = 3000)
        if (!publicIp.isNullOrBlank()) {
            // IP متعلق به همین وای‌فای است؛ بنابراین حتی اگر برای رسیدن به RIPE از
            // مسیر پیش‌فرض/VPN عبور کنیم باز هم اپراتور واقعی وای‌فای را برمی‌گرداند.
            resolveIpDetails(network, publicIp, allowDefaultFallback = true)?.let {
                return buildResult(it, LookupStatus.RESOLVED, DetectionMethod.PUBLIC_IP_ASN)
            }
        }

        // لایه‌ی ۳: اگر درخواست مستقیم (مثلاً به‌خاطر VPN Kill-Switch) ممکن نشد:
        // از آدرس عمومی IPv6 یا DNS اختصاصی اپراتور از طریق RIPE NCC کمک می‌گیریم.
        for (ipv6 in clues.globalIpv6Candidates.take(2)) {
            resolveIpDetails(network, ipv6, allowDefaultFallback = true)?.let {
                return buildResult(it, LookupStatus.LOCAL_HINT, DetectionMethod.IPV6_PREFIX)
            }
        }
        for (dnsIp in clues.ispDnsCandidates.take(2)) {
            resolveIpDetails(network, dnsIp, allowDefaultFallback = true)?.let {
                return buildResult(it, LookupStatus.LOCAL_HINT, DetectionMethod.DNS_SERVER_ISP)
            }
        }

        // لایه‌ی ۴: سرنخ‌های کاملاً آفلاین (دامنه‌ی DHCP و نام وای‌فای)
        matchOperatorFromClues(clues)?.let { (op, method, raw) ->
            return Result(
                operator = op,
                rawIsp = raw,
                asn = op.asns.firstOrNull() ?: 0,
                publicIp = "",
                countryCode = if (op.isIranian) "IR" else "",
                networkKey = 0L,
                fetchedAt = 0L,
                status = LookupStatus.LOCAL_HINT,
                detectionMethod = method,
            )
        }

        return null
    }

    private fun buildResult(
        lookup: Lookup,
        status: LookupStatus,
        method: DetectionMethod,
    ): Result {
        val op = OperatorCatalog.fromAsn(lookup.asn, lookup.isp)
            ?: OperatorCatalog.custom(cleanIspName(lookup.isp))
        return Result(
            operator = op,
            rawIsp = lookup.isp,
            asn = lookup.asn,
            publicIp = lookup.ip,
            countryCode = lookup.countryCode,
            networkKey = 0L,
            fetchedAt = 0L,
            status = status,
            detectionMethod = method,
        )
    }

    private suspend fun fetchFirstGeoIp(
        network: Network,
        urls: List<String>,
        allowDefaultFallback: Boolean,
        timeoutMs: Int,
    ): Lookup? = coroutineScope {
        val channel = Channel<Lookup?>(urls.size)
        for (url in urls) {
            launch {
                val body = httpGet(network, url, allowDefaultFallback, timeoutMs)
                val parsed = body?.let { runCatching { parse(JSONObject(it)) }.getOrNull() }
                    ?.takeIf { it.asn > 0 || it.isp.isNotBlank() }
                channel.send(parsed)
            }
        }
        var found: Lookup? = null
        repeat(urls.size) {
            val item = channel.receive()
            if (item != null && found == null) {
                found = item
                coroutineContext.cancelChildren()
                return@coroutineScope found
            }
        }
        found
    }

    private suspend fun fetchFirstIp(
        network: Network,
        urls: List<String>,
        allowDefaultFallback: Boolean,
        timeoutMs: Int,
    ): String? = coroutineScope {
        val channel = Channel<String?>(urls.size)
        for (url in urls) {
            launch {
                val body = httpGet(network, url, allowDefaultFallback, timeoutMs)
                val ip = body?.let { parsePlainIp(it) }
                channel.send(ip)
            }
        }
        var found: String? = null
        repeat(urls.size) {
            val item = channel.receive()
            if (!item.isNullOrBlank() && found == null) {
                found = item
                coroutineContext.cancelChildren()
                return@coroutineScope found
            }
        }
        found
    }

    /** تبدیل یک IP به نام ISP و شماره‌ی AS از طریق RIPE NCC و GeoIP. */
    private suspend fun resolveIpDetails(
        network: Network,
        ip: String,
        allowDefaultFallback: Boolean,
    ): Lookup? {
        // ۱) RIPE Stat (رسمی، بسیار پایدار و بدون محدودیت نرخ تجاری)
        val ripeNetBody = httpGet(
            network,
            "https://stat.ripe.net/data/network-info/data.json?resource=$ip",
            allowDefaultFallback,
            timeoutMs = 3500,
        )
        val ripeAsn = ripeNetBody?.let { parseRipeNetworkInfoAsn(it) } ?: 0
        if (ripeAsn > 0) {
            // اگر در کاتالوگ آفلاین وجود داشت نیازی به درخواست دوم نیست، وگرنه نام کامل را از as-overview می‌گیریم.
            val known = OperatorCatalog.fromAsn(ripeAsn, null)
            if (known != null) {
                return Lookup(isp = known.en, asn = ripeAsn, ip = ip, countryCode = if (known.isIranian) "IR" else "")
            }
            val overviewBody = httpGet(
                network,
                "https://stat.ripe.net/data/as-overview/data.json?resource=AS$ripeAsn",
                allowDefaultFallback,
                timeoutMs = 3000,
            )
            val holder = overviewBody?.let { parseRipeAsOverviewHolder(it) }.orEmpty()
            if (holder.isNotBlank()) {
                return Lookup(isp = holder, asn = ripeAsn, ip = ip, countryCode = "")
            }
        }

        // ۲) RIPE RDAP
        val rdapBody = httpGet(
            network,
            "https://rdap.db.ripe.net/ip/$ip",
            allowDefaultFallback,
            timeoutMs = 3500,
        )
        rdapBody?.let { parseRdap(it, ip, ripeAsn) }?.let { return it }

        // ۳) سرویس‌های GeoIP با پارامتر IP مشخص
        val byIpUrls = listOf(
            "https://ipwho.is/$ip",
            "https://api.ip.sb/geoip/$ip",
        )
        return fetchFirstGeoIp(network, byIpUrls, allowDefaultFallback, timeoutMs = 3500)
            ?.let { if (it.ip.isBlank()) it.copy(ip = ip) else it }
    }

    private fun httpGet(
        network: Network,
        endpoint: String,
        allowDefaultFallback: Boolean,
        timeoutMs: Int,
    ): String? {
        fun tryOpen(useNetwork: Boolean): String? = runCatching {
            val url = URL(endpoint)
            val connection = (if (useNetwork) network.openConnection(url) else url.openConnection()) as HttpURLConnection
            try {
                connection.connectTimeout = timeoutMs
                connection.readTimeout = timeoutMs
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("Accept", "application/json, text/plain, */*")
                connection.setRequestProperty("User-Agent", USER_AGENT)
                if (connection.responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else null
            } finally {
                connection.disconnect()
            }
        }.getOrNull()

        return tryOpen(useNetwork = true)
            ?: if (allowDefaultFallback) tryOpen(useNetwork = false) else null
    }

    // ------------------------------------------------------------------ سرنخ محلی

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun extractLocalClues(
        context: Context,
        caps: NetworkCapabilities?,
        lp: LinkProperties?,
    ): NetworkClues {
        val isEthernet = caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        val isMetered = caps != null && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val isCaptive = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true

        val gatewayIp = lp?.routes
            ?.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }
            ?.gateway?.hostAddress.orEmpty()

        val dhcpDomain = lp?.domains.orEmpty().trim()

        val dnsCandidates = lp?.dnsServers
            ?.mapNotNull { addr -> addr.hostAddress?.substringBefore('%') }
            ?.filter { isInspectablePublicIp(it) }
            .orEmpty()

        val ipv6Candidates = lp?.linkAddresses
            ?.mapNotNull { it.address as? Inet6Address }
            ?.filter { isGlobalPublicIpv6(it) }
            ?.mapNotNull { it.hostAddress?.substringBefore('%') }
            .orEmpty()

        val rawSsid = run {
            val fromCaps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                (caps?.transportInfo as? WifiInfo)?.ssid
            } else null
            val fromMgr = if (fromCaps.isNullOrBlank()) {
                runCatching {
                    context.applicationContext.getSystemService(WifiManager::class.java)?.connectionInfo?.ssid
                }.getOrNull()
            } else null
            fromCaps ?: fromMgr
        }
        val ssid = cleanSsid(rawSsid)

        val kind = classifySourceKind(
            isEthernet = isEthernet,
            isMetered = isMetered,
            gatewayIp = gatewayIp,
        )

        return NetworkClues(
            sourceKind = kind,
            ssid = ssid,
            dhcpDomain = dhcpDomain,
            gatewayIp = gatewayIp,
            ispDnsCandidates = dnsCandidates,
            globalIpv6Candidates = ipv6Candidates,
            isCaptivePortal = isCaptive,
            isEthernet = isEthernet,
            isMetered = isMetered,
        )
    }

    fun classifySourceKind(
        isEthernet: Boolean,
        isMetered: Boolean,
        gatewayIp: String,
    ): WifiSourceKind = when {
        isEthernet -> WifiSourceKind.ETHERNET
        gatewayIp == "172.20.10.1" -> WifiSourceKind.HOTSPOT_IPHONE
        gatewayIp == "192.168.43.1" -> WifiSourceKind.HOTSPOT_ANDROID
        gatewayIp == "192.168.137.1" -> WifiSourceKind.HOTSPOT_WINDOWS
        isMetered -> WifiSourceKind.HOTSPOT_METERED
        else -> WifiSourceKind.MODEM_ROUTER
    }

    fun cleanSsid(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val cleaned = raw.trim().removeSurrounding("\"").trim()
        if (cleaned.isBlank() ||
            cleaned.equals("<unknown ssid>", ignoreCase = true) ||
            cleaned.equals("0x", ignoreCase = true)
        ) return ""
        return cleaned
    }

    fun matchOperatorFromClues(clues: NetworkClues): Triple<OperatorCatalog.Operator, DetectionMethod, String>? {
        if (clues.dhcpDomain.isNotBlank()) {
            OperatorCatalog.fromName(clues.dhcpDomain)?.let {
                return Triple(it, DetectionMethod.DHCP_DOMAIN, clues.dhcpDomain)
            }
        }
        if (clues.ssid.isNotBlank()) {
            OperatorCatalog.fromName(clues.ssid)?.let {
                return Triple(it, DetectionMethod.WIFI_SSID, clues.ssid)
            }
        }
        return null
    }

    fun isInspectablePublicIp(ip: String): Boolean {
        val clean = ip.trim().substringBefore('%')
        if (clean.isBlank() || publicResolverIps.contains(clean.lowercase())) return false
        val addr = runCatching { InetAddress.getByName(clean) }.getOrNull() ?: return false
        if (addr.isAnyLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress ||
            addr.isSiteLocalAddress || addr.isMulticastAddress
        ) return false
        val bytes = addr.address
        if (addr is Inet4Address && bytes.size == 4) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            // CGNAT 100.64.0.0/10 و رزروها
            if (b0 == 100 && b1 in 64..127) return false
            if (b0 == 0 || b0 == 127 || b0 == 10) return false
            if (b0 == 172 && b1 in 16..31) return false
            if (b0 == 192 && b1 == 168) return false
            if (b0 == 169 && b1 == 254) return false
        }
        if (addr is Inet6Address && !isGlobalPublicIpv6(addr)) return false
        return true
    }

    private fun isGlobalPublicIpv6(addr: Inet6Address): Boolean {
        if (addr.isAnyLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress ||
            addr.isSiteLocalAddress || addr.isMulticastAddress
        ) return false
        val firstByte = addr.address[0].toInt() and 0xFF
        // فقط Unicast جهانی (2000::/3) و رد کردن ULA (fc00::/7)
        return (firstByte and 0xE0) == 0x20
    }

    // ------------------------------------------------------------------ پارسرهای پاسخ

    private fun JSONObject.str(key: String): String =
        optString(key).takeIf { it.isNotBlank() && it != "null" }.orEmpty()

    /** پاسخ سرویس‌های مختلف GeoIP شکل‌های مختلفی دارد؛ همه را می‌فهمیم. */
    fun parse(json: JSONObject): Lookup? {
        if (json.has("success") && !json.optBoolean("success", true)) return null
        if (json.optBoolean("error", false)) return null
        val connection = json.optJSONObject("connection")
        val asnObject = json.optJSONObject("asn")
        val countryObject = json.optJSONObject("country")

        val org = listOf(
            connection?.str("isp").orEmpty(),
            connection?.str("org").orEmpty(),
            asnObject?.str("name").orEmpty(),
            json.str("asn_organization"),
            json.str("asnOrganization"),
            json.str("organization_name"),
            json.str("org"),
            json.str("isp"),
            json.str("organization"),
            json.str("as"),
        ).firstOrNull { it.isNotBlank() }.orEmpty()

        val asnText = listOf(
            connection?.str("asn").orEmpty(),
            asnObject?.str("number").orEmpty(),
            asnObject?.str("asn").orEmpty(),
            json.str("asn"),
            json.str("as"),
            json.str("org"),
            json.str("organization"),
        ).firstOrNull { parseAsn(it) > 0 }.orEmpty()

        val ip = listOf(
            json.str("ip"),
            json.str("ipAddress"),
            json.str("query"),
        ).firstOrNull { it.isNotBlank() }.orEmpty()

        val country = listOf(
            json.str("country_code"),
            json.str("countryCode"),
            countryObject?.str("code").orEmpty(),
            json.str("country"),
        ).firstOrNull { it.length == 2 }.orEmpty()

        return Lookup(isp = org, asn = parseAsn(asnText), ip = ip, countryCode = country)
    }

    /** استخراج IP عمومی از متن ساده، JSON (ipify)، یا خروجی `cdn-cgi/trace`. */
    fun parsePlainIp(body: String): String? {
        val trimmed = body.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("{")) {
            val ip = runCatching { JSONObject(trimmed).str("ip") }.getOrNull()
            if (!ip.isNullOrBlank() && isValidIpLiteral(ip)) return ip
        }
        // فرمت key=value مثل Cloudflare cdn-cgi/trace
        trimmed.lineSequence().forEach { line ->
            if (line.startsWith("ip=")) {
                val candidate = line.removePrefix("ip=").trim()
                if (isValidIpLiteral(candidate)) return candidate
            }
        }
        val singleLine = trimmed.lineSequence().firstOrNull()?.trim().orEmpty()
        return singleLine.takeIf { isValidIpLiteral(it) }
    }

    private fun isValidIpLiteral(text: String): Boolean {
        if (text.isBlank() || text.length > 64 || text.contains(' ') || text.contains('<')) return false
        if (IPV4_REGEX.matches(text)) {
            return text.split('.').all { (it.toIntOrNull() ?: -1) in 0..255 }
        }
        return text.contains(':') && text.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' || it == ':' }
    }

    /** استخراج ASN از پاسخ `stat.ripe.net/data/network-info/data.json`. */
    fun parseRipeNetworkInfoAsn(body: String): Int {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return 0
        val data = root.optJSONObject("data") ?: return 0
        val asns = data.optJSONArray("asns") ?: return 0
        for (i in 0 until asns.length()) {
            val asn = parseAsn(asns.optString(i))
            if (asn > 0) return asn
        }
        return 0
    }

    /** استخراج نام سازمان از پاسخ `stat.ripe.net/data/as-overview/data.json`. */
    fun parseRipeAsOverviewHolder(body: String): String {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return ""
        return root.optJSONObject("data")?.str("holder").orEmpty()
    }

    /** استخراج نام شبکه/سازمان از پاسخ `rdap.db.ripe.net/ip/<ip>`. */
    fun parseRdap(body: String, ip: String, knownAsn: Int = 0): Lookup? {
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val name = json.str("name")
        val country = json.str("country").takeIf { it.length == 2 }.orEmpty()
        val remarksText = buildList {
            val remarks = json.optJSONArray("remarks")
            if (remarks != null) {
                for (i in 0 until remarks.length()) {
                    val desc = remarks.optJSONObject(i)?.optJSONArray("description") ?: continue
                    for (j in 0 until desc.length()) {
                        val s = desc.optString(j)
                        if (s.isNotBlank() && s != "null") add(s)
                    }
                }
            }
        }
        val candidates = remarksText + listOfNotNull(name.takeIf { it.isNotBlank() })
        val matched = candidates.firstOrNull { OperatorCatalog.fromName(it) != null }
        val isp = matched ?: remarksText.firstOrNull() ?: name
        if (isp.isBlank() && knownAsn <= 0) return null
        return Lookup(isp = isp, asn = knownAsn, ip = ip, countryCode = country)
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
