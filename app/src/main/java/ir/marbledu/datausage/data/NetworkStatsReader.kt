package ir.marbledu.datausage.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * خواندن شمارنده‌های شبکه‌ی سیستم از /sys/class/net/<iface>/statistics.
 *
 * کل ایده (همان روشی که Xray/NekoBox بدون VPN استفاده می‌کند):
 *  ۱) شمارنده‌های شبکه‌ی سیستم را می‌خوانیم (کل مصرف دستگاه).
 *  ۲) با NetworkStatsManager مصرف هر اپ را جدا می‌کنیم.
 *  ۳) اختلاف دو نمونه = مصرف بازه‌ی زمانی؛ که با سرعت لحظه‌ای و پینگ ترکیب می‌شود.
 * هیچ VPN، هیچ دسترسی root و هیچ سرویس پس‌زمینه‌ی همیشه‌روشن لازم نیست.
 */
object NetworkStatsReader {

    private val ifaceDir = File("/sys/class/net")

    /**
     * مجموع بایت‌های ارسال/دریافت همه‌ی اینترفیس‌های فیزیکی.
     *
     * روی اندرویدهای جدید خواندن /sys برای اپ‌ها بسته است و همه‌چیز صفر
     * برمی‌گردد؛ در آن حالت از TrafficStats (API رسمی) استفاده می‌کنیم.
     */
    fun totalInterfaceBytes(): Pair<Long, Long> {
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
        if (rx == 0L && tx == 0L) {
            val totalRx = runCatching { TrafficStats.getTotalRxBytes() }.getOrDefault(-1L)
            val totalTx = runCatching { TrafficStats.getTotalTxBytes() }.getOrDefault(-1L)
            if (totalRx >= 0 && totalTx >= 0) return totalRx to totalTx
        }
        return rx to tx
    }

    private fun readLong(file: File): Long = runCatching {
        if (file.canRead()) file.readText().trim().toLong() else 0L
    }.getOrDefault(0L)

    /** وضعیت فعلی شبکه: نوع، اپراتورِ اینترنت (حتی روی وای‌فای)، سیم‌کارت و IP. */
    @SuppressLint("MissingPermission")
    fun currentNetwork(context: Context): NetworkSnapshot {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val type = when {
            caps == null -> ConnectionType.OFFLINE
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> ConnectionType.VPN
            else -> ConnectionType.OTHER
        }
        val vpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val sim = simOperator(context)

        var ispRaw = ""
        var ispAsn = 0
        var wifiResolved = false
        val operator: OperatorCatalog.Operator = when (type) {
            ConnectionType.CELLULAR -> sim ?: OperatorCatalog.byId(OperatorCatalog.ID_MOBILE)
            ConnectionType.WIFI, ConnectionType.ETHERNET -> {
                // اپراتورِ اینترنت وای‌فای از روی IP عمومی پیدا می‌شود (IspResolver).
                val key = IspResolver.physicalFixedNetwork(context)?.networkHandle
                val result = if (key != null) IspResolver.current(key) else null
                if (result != null) {
                    ispRaw = result.rawIsp
                    ispAsn = result.asn
                    wifiResolved = true
                    result.operator
                } else {
                    OperatorCatalog.byId(OperatorCatalog.ID_WIFI)
                }
            }
            ConnectionType.OFFLINE -> OperatorCatalog.byId(OperatorCatalog.ID_UNKNOWN)
            else -> sim ?: OperatorCatalog.byId(OperatorCatalog.ID_UNKNOWN)
        }
        val persian = isPersian(context)
        return NetworkSnapshot(
            type = type,
            carrierName = if (operator.id == OperatorCatalog.ID_UNKNOWN) {
                context.getString(ir.marbledu.datausage.R.string.network_unknown)
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
        )
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
        // اپراتورهای مجازی (شاتل موبایل، آپتل و …) روی شبکه‌ی میزبان می‌نشینند؛
        // برای همین نامِ سیم‌کارت به کد شبکه اولویت دارد.
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
    /** اپراتوری که همین حالا اینترنت از آن می‌آید (روی وای‌فای = اپراتور مودم). */
    val operator: OperatorCatalog.Operator = OperatorCatalog.byId(OperatorCatalog.ID_UNKNOWN),
    /** اپراتور سیم‌کارتِ داده؛ روی وای‌فای هم پر است. */
    val simOperator: OperatorCatalog.Operator? = null,
    val vpnActive: Boolean = false,
    /** نام خام ISP و شماره‌ی AS (فقط وقتی اپراتور وای‌فای پیدا شده). */
    val ispName: String = "",
    val ispAsn: Int = 0,
    val wifiOperatorResolved: Boolean = false,
)

enum class ConnectionType { WIFI, CELLULAR, ETHERNET, VPN, OTHER, OFFLINE }
