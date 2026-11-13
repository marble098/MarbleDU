package ir.marbledu.datausage.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
        val raw = runCatching<String?> { tm?.networkOperatorName }.getOrNull().orEmpty()
        if (raw.isNotBlank()) return OperatorCatalog.normalizeDisplay(raw)
        val sim = runCatching<String?> { tm?.simOperatorName }.getOrNull().orEmpty()
        if (sim.isNotBlank()) return OperatorCatalog.normalizeDisplay(sim)
        return context.getString(ir.marbledu.datausage.R.string.network_unknown)
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
)

enum class ConnectionType { WIFI, CELLULAR, ETHERNET, VPN, OTHER, OFFLINE }
