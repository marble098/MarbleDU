package ir.marbledu.datausage.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import ir.marbledu.datausage.R
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * خواندن شمارنده‌های شبکه‌ی سیستم و وضعیت اتصال فعلی بدون VPN و بدون root.
 */
object NetworkStatsReader {

    private val ifaceDir = File("/sys/class/net")

    /**
     * مجموع بایت‌های ارسال/دریافت کل دستگاه (همه‌ی اینترفیس‌ها).
     *
     * منبع اصلی TrafficStats (API رسمی اندروید) است: نیازمند مجوز نیست، روی همه‌ی
     * نسخه‌ها کار می‌کند و نه /sys را می‌خواند نه SELinux denial می‌سازد.
     * خواندن /sys/class/net/*/statistics روی اندرویدهای جدید برای اپ‌ها مسدود است
     * و در هر بار نمونه‌برداری avc denied ایجاد می‌کند (پر کردن لاگ و مصرف باطری/CPU), پس فقط
     * به عنوان آخرین fallback بسیار محدود نگه‌داشته شده است.
     */
    fun totalInterfaceBytes(): Pair<Long, Long> {
        val totalRx = runCatching { TrafficStats.getTotalRxBytes() }.getOrDefault(-1L)
        val totalTx = runCatching { TrafficStats.getTotalTxBytes() }.getOrDefault(-1L)
        if (totalRx >= 0 && totalTx >= 0) return totalRx to totalTx
        // Fallback خیلی محدود: فقط وقتی TrafficStats پاسخ ندهد (عمدتاً دستگاه‌های قدیمی / شبیه‌ساز)
        var rx = 0L
        var tx = 0L
        runCatching {
            ifaceDir.listFiles()?.forEach { iface ->
                val name = iface.name
                if (name == "lo" || name.startsWith("tun") || name.startsWith("ppp") || name == "dummy0") return@forEach
                rx += readLong(File(iface, "statistics/rx_bytes"))
                tx += readLong(File(iface, "statistics/tx_bytes"))
            }
        }
        return rx to tx
    }

    private fun readLong(file: File): Long = runCatching {
        if (file.canRead()) file.readText().trim().toLong() else 0L
    }.getOrDefault(0L)

    /** وضعیت فعلی شبکه: نوع، اپراتورِ اینترنت (حتی روی وای‌فای/هات‌اسپات)، سیم‌کارت و IP. */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun currentNetwork(context: Context, persian: Boolean = isPersian(context)): NetworkSnapshot {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val vpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

        val type = when {
            caps == null -> ConnectionType.OFFLINE
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            vpn -> resolveUnderlyingTransport(context, cm)
            else -> ConnectionType.OTHER
        }
        val sim = simOperator(context)

        var ispRaw = ""
        var ispAsn = 0
        var publicIp = ""
        var wifiResolved = false
        var wifiSourceKind = IspResolver.WifiSourceKind.UNKNOWN
        var wifiStatus = IspResolver.LookupStatus.IDLE
        var wifiMethod = IspResolver.DetectionMethod.NONE
        var wifiSsid = ""

        val operator: OperatorCatalog.Operator = when (type) {
            ConnectionType.CELLULAR -> sim ?: OperatorCatalog.byId(OperatorCatalog.ID_MOBILE)
            ConnectionType.WIFI, ConnectionType.ETHERNET -> {
                val fixedNet = IspResolver.physicalFixedNetwork(context)
                val key = fixedNet?.networkHandle ?: 0L
                val state = IspResolver.stateFor(key)
                val result = IspResolver.current(key)
                wifiSourceKind = state.sourceKind
                wifiStatus = state.status
                wifiSsid = state.wifiSsid
                if (result != null) {
                    ispRaw = result.rawIsp
                    ispAsn = result.asn
                    publicIp = result.publicIp
                    wifiMethod = result.detectionMethod
                    wifiResolved = true
                    result.operator
                } else {
                    OperatorCatalog.byId(OperatorCatalog.ID_WIFI)
                }
            }
            ConnectionType.OFFLINE -> OperatorCatalog.byId(OperatorCatalog.ID_UNKNOWN)
            else -> sim ?: OperatorCatalog.byId(OperatorCatalog.ID_UNKNOWN)
        }

        val localizedUnknown = if (persian) "نامشخص" else "Unknown"
        val mobileTech = if (type == ConnectionType.CELLULAR) mobileTechName(context, persian) else ""

        return NetworkSnapshot(
            type = type,
            carrierName = if (operator.id == OperatorCatalog.ID_UNKNOWN) {
                runCatching { context.getString(R.string.network_unknown) }.getOrDefault(localizedUnknown)
            } else operator.name(persian),
            ipAddress = localIpAddress(),
            isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            linkDownKbps = caps?.linkDownstreamBandwidthKbps ?: 0,
            linkUpKbps = caps?.linkUpstreamBandwidthKbps ?: 0,
            operator = operator,
            simOperator = sim,
            vpnActive = vpn,
            ispName = ispRaw,
            ispAsn = ispAsn,
            wifiOperatorResolved = wifiResolved,
            wifiSourceKind = wifiSourceKind,
            wifiLookupStatus = wifiStatus,
            wifiDetectionMethod = wifiMethod,
            wifiSsid = wifiSsid,
            publicIp = publicIp,
            mobileTech = mobileTech,
        )
    }

    @Suppress("DEPRECATION")
    private fun resolveUnderlyingTransport(context: Context, cm: ConnectivityManager?): ConnectionType {
        if (cm == null) return ConnectionType.VPN
        val fixed = IspResolver.physicalFixedNetwork(context)
        if (fixed != null) {
            val fixedCaps = runCatching { cm.getNetworkCapabilities(fixed) }.getOrNull()
            return if (fixedCaps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) {
                ConnectionType.ETHERNET
            } else {
                ConnectionType.WIFI
            }
        }
        val hasCellular = runCatching {
            cm.allNetworks.any { net ->
                val c = cm.getNetworkCapabilities(net) ?: return@any false
                !c.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                    c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            }
        }.getOrDefault(false)
        return if (hasCellular) ConnectionType.CELLULAR else ConnectionType.VPN
    }

    @SuppressLint("MissingPermission")
    private fun mobileTechName(context: Context, persian: Boolean): String {
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) return ""
        return runCatching {
            val base = context.getSystemService(TelephonyManager::class.java) ?: return ""
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            val tm = if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                base.createForSubscriptionId(subId)
            } else base
            val label = OperatorCatalog.networkTypeName(persian, tm.dataNetworkType)
            if (label == "—" || label == "نامشخص" || label == "Unknown") "" else label
        }.getOrDefault("")
    }

    private fun isPersian(context: Context): Boolean = runCatching {
        context.resources.configuration.locales[0].language == "fa"
    }.getOrDefault(true)

    /**
     * اپراتور سیم‌کارتی که اینترنت موبایل روی آن است. به هیچ مجوزی نیاز ندارد
     * و حتی وقتی گوشی روی وای‌فای است هم جواب می‌دهد.
     */
    fun simOperator(context: Context): OperatorCatalog.Operator? {
        val base = context.getSystemService(TelephonyManager::class.java) ?: return null
        val tm = runCatching {
            val subId = SubscriptionManager.getDefaultDataSubscriptionId()
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) base.createForSubscriptionId(subId) else base
        }.getOrNull() ?: base
        val simNumeric = runCatching<String?> { tm.simOperator }.getOrNull().orEmpty()
        val simName = runCatching<String?> { tm.simOperatorName }.getOrNull().orEmpty()
        val netNumeric = runCatching<String?> { tm.networkOperator }.getOrNull().orEmpty()
        val netName = runCatching<String?> { tm.networkOperatorName }.getOrNull().orEmpty()
        return OperatorCatalog.fromName(simName)
            ?: OperatorCatalog.fromNumeric(simNumeric)
            ?: OperatorCatalog.fromName(netName)
            ?: OperatorCatalog.fromNumeric(netNumeric)
            ?: simName.takeIf { it.isNotBlank() }?.let { OperatorCatalog.custom(it) }
            ?: netName.takeIf { it.isNotBlank() }?.let { OperatorCatalog.custom(it) }
    }

    private fun localIpAddress(): String {
        val address: String? = runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
        }.getOrNull()
        return address ?: ""
    }
}

data class NetworkSnapshot(
    val type: ConnectionType,
    val carrierName: String,
    val ipAddress: String,
    val isValidated: Boolean,
    val linkDownKbps: Int,
    val linkUpKbps: Int,
    /** اپراتوری که همین حالا اینترنت از آن می‌آید (روی وای‌فای = اپراتور مودم/هات‌اسپات). */
    val operator: OperatorCatalog.Operator = OperatorCatalog.byId(OperatorCatalog.ID_UNKNOWN),
    /** اپراتور سیم‌کارتِ داده؛ روی وای‌فای هم پر است. */
    val simOperator: OperatorCatalog.Operator? = null,
    val vpnActive: Boolean = false,
    /** نام خام ISP و شماره‌ی AS (فقط وقتی اپراتور وای‌فای پیدا شده). */
    val ispName: String = "",
    val ispAsn: Int = 0,
    val wifiOperatorResolved: Boolean = false,
    val wifiSourceKind: IspResolver.WifiSourceKind = IspResolver.WifiSourceKind.UNKNOWN,
    val wifiLookupStatus: IspResolver.LookupStatus = IspResolver.LookupStatus.IDLE,
    val wifiDetectionMethod: IspResolver.DetectionMethod = IspResolver.DetectionMethod.NONE,
    val wifiSsid: String = "",
    val publicIp: String = "",
    val mobileTech: String = "",
)

enum class ConnectionType { WIFI, CELLULAR, ETHERNET, VPN, OTHER, OFFLINE }
