package ir.marbledu.datausage.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs

/** مقصد داخل ایران است یا خارج. */
enum class PingScope { DOMESTIC, GLOBAL }

/** مسیری که اندازه‌گیری از آن انجام شده. */
enum class PingRoute { DIRECT, VPN }

data class PingTarget(
    val id: String,
    val fa: String,
    val en: String,
    val host: String,
    val scope: PingScope,
    val path: String = "/",
) {
    fun name(persian: Boolean): String = if (persian) fa else en
}

data class PingResult(
    val target: PingTarget,
    val route: PingRoute,
    /** نمونه‌های موفق بر حسب میلی‌ثانیه. */
    val samples: List<Double>,
    val sent: Int,
    val averageMs: Double,
    val minMs: Double,
    val maxMs: Double,
    val jitterMs: Double,
    val lossPercent: Int,
) {
    val host: String get() = target.host
    val ok: Boolean get() = samples.isNotEmpty()
}

/**
 * اندازه‌گیری «تأخیر واقعی» رفت‌وبرگشت تا سرور.
 *
 * چرا ICMP و TCP-connect غلط بودند؟ وقتی VPN روشن است (تقریباً همیشه در ایران)،
 * برنامه‌ی VPN یک کارت شبکه‌ی مجازی (tun) می‌سازد و خودش **به‌صورت محلی** به
 * ICMP echo و به SYNِ اتصال TCP جواب می‌دهد. نتیجه: «پینگ» ۲ تا ۵ میلی‌ثانیه که
 * فقط زمان رفت‌وبرگشت داخل خودِ گوشی است، نه اینترنت.
 *
 * راه درست: روی یک اتصال باز، یک درخواست کوچک HTTP می‌فرستیم و زمان رسیدن
 * اولین بایت پاسخ را می‌سنجیم. پاسخ فقط از خودِ سرور می‌تواند بیاید، پس این
 * زمان حتماً شامل کل مسیر (گوشی ← VPN ← سرور ← برگشت) است. اولین درخواست هر
 * اتصال (گرم‌کردن مسیر/پروکسی) دور ریخته می‌شود.
 */
object PingEngine {

    private const val CONNECT_TIMEOUT_MS = 3500
    private const val READ_TIMEOUT_MS = 3500
    private const val MAX_HEADER_BYTES = 16 * 1024
    private const val EOF = -2.0
    private const val FAILED = -1.0

    val targets: List<PingTarget> = listOf(
        PingTarget("aparat", "آپارات (داخلی)", "Aparat (Iran)", "www.aparat.com", PingScope.DOMESTIC),
        PingTarget("digikala", "دیجی‌کالا (داخلی)", "Digikala (Iran)", "www.digikala.com", PingScope.DOMESTIC),
        PingTarget("google", "گوگل (بین‌الملل)", "Google (global)", "www.google.com", PingScope.GLOBAL, "/generate_204"),
        PingTarget("cloudflare", "کلادفلر (بین‌الملل)", "Cloudflare (global)", "cp.cloudflare.com", PingScope.GLOBAL, "/generate_204"),
    )

    private class Conn(val socket: Socket) {
        val input: InputStream = BufferedInputStream(socket.getInputStream(), 2048)
        val output: OutputStream = socket.getOutputStream()
        fun close() {
            runCatching { socket.close() }
        }
    }

    /** آیا شبکه‌ی فعال یک VPN است؟ */
    fun isVpnActive(context: Context): Boolean = runCatching {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }.getOrDefault(false)

    /** شبکه‌ی فیزیکی (وای‌فای/موبایل) زیرِ VPN؛ برای اندازه‌گیری مستقیم بدون VPN. */
    @Suppress("DEPRECATION")
    fun physicalNetwork(context: Context): Network? = runCatching {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val candidates = cm.allNetworks.mapNotNull { network ->
            val caps = cm.getNetworkCapabilities(network) ?: return@mapNotNull null
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return@mapNotNull null
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return@mapNotNull null
            network to caps
        }
        // وای‌فای/اترنت بر موبایل مقدم است؛ اندروید هم همین را ترجیح می‌دهد.
        candidates.firstOrNull { (_, caps) ->
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }?.first ?: candidates.firstOrNull()?.first
    }.getOrNull()

    /**
     * [count] نمونه‌ی معتبر (به‌علاوه‌ی یک نمونه‌ی گرم‌کردن) از [target] می‌گیرد.
     * اگر [network] داده شود، سوکت به همان شبکه بسته می‌شود (دور زدن VPN).
     */
    fun measure(target: PingTarget, network: Network?, route: PingRoute, count: Int = 5): PingResult {
        val attempts = ArrayList<Double>(count + 1)
        var conn: Conn? = null
        try {
            repeat(count + 1) { index ->
                var value = FAILED
                // اگر سرور اتصال keep-alive را بسته بود، یک‌بار با اتصال تازه تکرار می‌کنیم.
                for (round in 0 until 2) {
                    val active = conn ?: runCatching { open(target, network) }.getOrNull()
                    if (active == null) {
                        value = FAILED
                        break
                    }
                    conn = active
                    value = runCatching { exchange(active, target) }.getOrDefault(FAILED)
                    if (value >= 0.0 && !active.socket.isClosed) break
                    active.close()
                    conn = null
                    if (value >= 0.0 || value != EOF) break
                }
                attempts.add(if (value == EOF) FAILED else value)
                if (index < count) runCatching { Thread.sleep(150) }
            }
        } finally {
            conn?.close()
        }
        return summarize(target, route, attempts.drop(1))
    }

    /** آمار نهایی از فهرست تلاش‌ها (مقدار منفی = ناموفق). */
    fun summarize(target: PingTarget, route: PingRoute, attempts: List<Double>): PingResult {
        val ok = attempts.filter { it > 0.0 }
        val sent = attempts.size
        return PingResult(
            target = target,
            route = route,
            samples = ok,
            sent = sent,
            averageMs = if (ok.isEmpty()) -1.0 else ok.average(),
            minMs = ok.minOrNull() ?: -1.0,
            maxMs = ok.maxOrNull() ?: -1.0,
            jitterMs = jitter(ok),
            lossPercent = if (sent == 0) 100 else ((sent - ok.size) * 100) / sent,
        )
    }

    /** نوسان: میانگین قدرمطلق اختلاف نمونه‌های پشت‌سرهم (مثل RFC 3550). */
    fun jitter(samples: List<Double>): Double {
        if (samples.size < 2) return 0.0
        var sum = 0.0
        for (i in 1 until samples.size) sum += abs(samples[i] - samples[i - 1])
        return sum / (samples.size - 1)
    }

    private fun open(target: PingTarget, network: Network?): Conn {
        val addresses: Array<InetAddress> =
            if (network != null) network.getAllByName(target.host) else InetAddress.getAllByName(target.host)
        val address = addresses.firstOrNull { it is Inet4Address } ?: addresses.first()
        val socket = if (network != null) network.socketFactory.createSocket() else Socket()
        try {
            socket.tcpNoDelay = true
            socket.soTimeout = READ_TIMEOUT_MS
            socket.connect(InetSocketAddress(address, 80), CONNECT_TIMEOUT_MS)
            return Conn(socket)
        } catch (t: Throwable) {
            runCatching { socket.close() }
            throw t
        }
    }

    /** یک درخواست HEAD؛ زمان از ارسال تا اولین بایت پاسخ (میلی‌ثانیه). */
    private fun exchange(conn: Conn, target: PingTarget): Double {
        val request = (
            "HEAD ${target.path} HTTP/1.1\r\n" +
                "Host: ${target.host}\r\n" +
                "User-Agent: MarbleDU-latency/1\r\n" +
                "Accept: */*\r\n" +
                "Connection: keep-alive\r\n\r\n"
            ).toByteArray(Charsets.US_ASCII)
        val start = System.nanoTime()
        conn.output.write(request)
        conn.output.flush()
        val first = conn.input.read()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        if (first < 0) return EOF
        // بقیه‌ی سرآیند را تا خط خالی می‌خوانیم تا اتصال برای نمونه‌ی بعد تمیز باشد.
        var matched = 0
        var read = 1
        var closeAfter = false
        val line = StringBuilder().append(first.toChar())
        while (matched < 4 && read < MAX_HEADER_BYTES) {
            val b = conn.input.read()
            if (b < 0) {
                closeAfter = true
                break
            }
            read++
            val c = b.toChar()
            matched = if ((c == '\r' && (matched == 0 || matched == 2)) || (c == '\n' && (matched == 1 || matched == 3))) {
                matched + 1
            } else if (c == '\r') 1 else 0
            if (c == '\n') {
                if (line.toString().trim().equals("connection: close", ignoreCase = true)) closeAfter = true
                line.setLength(0)
            } else if (line.length < 256) {
                line.append(c)
            }
        }
        if (closeAfter || matched < 4) conn.close()
        return elapsedMs.coerceAtLeast(0.1)
    }
}
