package ir.marbledu.datausage.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ردیف‌های /proc/net/xt_qtaguid/stats یا /proc/net/xt_qtaguid/iface_stat_fmt
 * در اندرویدهای قدیمی و /sys/class/net/<iface>/statistics در اندرویدهای جدید.
 *
 * کل ایده (همان روشی که Xray/NekoBox بدون VPN استفاده می‌کند):
 *  ۱) شمارنده‌های شبکه‌ی سیستم را می‌خوانیم (کل مصرف دستگاه).
 *  ۲) با /proc/net/xt_qtaguid یا NetworkStatsManager مصرف هر اپ را جدا می‌کنیم.
 *  ۳) اختلاف دو نمونه = مصرف بازه‌ی زمانی؛ که با سرعت لحظه‌ای و پینگ ترکیب می‌شود.
 * هیچ VPN، هیچ دسترسی root و هیچ سرویس پس‌زمینه‌ی همیشه‌روشن لازم نیست.
 */
object NetworkStatsReader {

    private val ifaceDir = File("/sys/class/net")

    /** مجموع بایت‌های ارسال/دریافت همه‌ی اینترفیس‌های فیزیکی. */
    fun totalInterfaceBytes(): Pair<Long, Long> {
        var rx = 0L
        var tx = 0L
        ifaceDir.listFiles()?.forEach { iface ->
            val name = iface.name
            if (name == "lo" || name.startsWith("tun") || name.startsWith("ppp") || name == "dummy0") return@forEach
            rx += readLong(File(iface, "statistics/rx_bytes"))
            tx += readLong(File(iface, "statistics/tx_bytes"))
        }
        return rx to tx
    }

    private fun readLong(file: File): Long = runCatching {
        if (file.canRead()) file.readText().trim().toLong() else 0L
    }.getOrDefault(0L)

    /** فقط اینترفیس Wi-Fi. */
    fun wifiInterfaceBytes(): Pair<Long, Long> {
        val candidate = ifaceDir.listFiles()?.map { it.name }?.firstOrNull {
            it.startsWith("wlan") || it.startsWith("ap") || it == "wifi0"
        } ?: return 0L to 0L
        val dir = File(ifaceDir, candidate)
        return readLong(File(dir, "statistics/rx_bytes")) to readLong(File(dir, "statistics/tx_bytes"))
    }

    /** فقط اینترفیس داده‌ی موبایل. */
    fun cellularInterfaceBytes(): Pair<Long, Long> {
        val candidate = ifaceDir.listFiles()?.map { it.name }?.firstOrNull {
            it.startsWith("rmnet") || it.startsWith("ccmni") || it.startsWith("pdp")
        } ?: return 0L to 0L
        val dir = File(ifaceDir, candidate)
        return readLong(File(dir, "statistics/rx_bytes")) to readLong(File(dir, "statistics/tx_bytes"))
    }

    /** وضعیت فعلی شبکه: نوع، نام اپراتور و IP. */
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
        val tm = context.getSystemService(TelephonyManager::class.java)
        return NetworkSnapshot(
            type = type,
            carrierName = carrierName(context, tm, caps),
            ipAddress = localIpAddress(),
            isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            linkDownKbps = caps?.linkDownstreamBandwidthKbps ?: 0,
            linkUpKbps = caps?.linkUpstreamBandwidthKbps ?: 0,
        )
    }

    @SuppressLint("MissingPermission")
    private fun carrierName(
        context: Context,
        tm: TelephonyManager?,
        caps: NetworkCapabilities?,
    ): String {
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) return "Wi-Fi"
        val raw = runCatching { tm?.networkOperatorName }.getOrNull().orEmpty()
        if (raw.isNotBlank()) return OperatorCatalog.normalizeDisplay(raw)
        val sim = runCatching { tm?.simOperatorName }.getOrNull().orEmpty()
        if (sim.isNotBlank()) return OperatorCatalog.normalizeDisplay(sim)
        return context.getString(ir.marbledu.datausage.R.string.network_unknown)
    }

    /** در اندروید ۱۱+ دسترسی به MCC/MNC معمولاً محدود است؛ در صورت وجود استفاده می‌شود. */
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    fun subscriptionIds(context: Context): IntArray {
        val sm = context.getSystemService(SubscriptionManager::class.java) ?: return intArrayOf()
        return runCatching { sm.activeSubscriptionIdList.toIntArray() }.getOrDefault(intArrayOf())
    }

    private fun localIpAddress(): String = runCatching {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull().orEmpty()

    /** مصرف کل دستگاه از NetworkStatsManager (اگر دسترسی داده شود). */
    fun deviceUsageBytes(
        context: Context,
        fromMillis: Long,
        toMillis: Long,
        type: ConnectionType,
    ): Long = runCatching {
        val nsm = context.getSystemService(android.app.usage.NetworkStatsManager::class.java)
        val networkType = when (type) {
            ConnectionType.WIFI -> ConnectivityManager.TYPE_WIFI
            ConnectionType.CELLULAR -> ConnectivityManager.TYPE_MOBILE
            else -> ConnectivityManager.TYPE_MOBILE
        }
        val bucket = nsm.querySummaryForDevice(networkType, null, fromMillis, toMillis)
        bucket.rxBytes + bucket.txBytes
    }.getOrDefault(0L)

    /** شناسه‌ی پکیج همین اپ برای تفکیک ترافیک خودمان. */
    fun selfUid(): Int = Process.myUid()
}

data class NetworkSnapshot(
    val type: ConnectionType,
    val carrierName: String,
    val ipAddress: String,
    val isValidated: Boolean,
    val linkDownKbps: Int,
    val linkUpKbps: Int,
)

enum class ConnectionType { WIFI, CELLULAR, ETHERNET, VPN, OTHER, OFFLINE }
