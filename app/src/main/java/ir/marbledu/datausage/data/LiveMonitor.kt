package ir.marbledu.datausage.data

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * مونیتورینگ زنده: سرعت لحظه‌ای (دانلود/آپلود)، میانگین سرعت، پینگ و
 * بازه‌های مصرف (session) برای نمایش «از ساعت … تا ساعت …».
 */
class LiveMonitor(private val context: Context) {

    /** جریانِ سرعت لحظه‌ای و پینگ، هر ثانیه. */
    fun stream(
        intervalMillis: Long = 1000,
        persianProvider: () -> Boolean = { true },
    ): Flow<LiveTick> = flow {
        var lastRx = 0L
        var lastTx = 0L
        var lastTime = 0L
        var started = false
        var sessionStart = System.currentTimeMillis()
        var sessionRx = 0L
        var sessionTx = 0L
        var peak = 0.0
        val speedHistory = ArrayDeque<Double>()
        while (true) {
            val (rx, tx) = NetworkStatsReader.totalInterfaceBytes()
            val now = System.currentTimeMillis()
            if (!started) {
                lastRx = rx; lastTx = tx; lastTime = now; started = true
                sessionStart = now
            }
            val elapsed = (now - lastTime).coerceAtLeast(1)
            val dRx = max(0L, rx - lastRx)
            val dTx = max(0L, tx - lastTx)
            lastRx = rx; lastTx = tx; lastTime = now
            sessionRx += dRx; sessionTx += dTx
            val down = dRx * 1000.0 / elapsed
            val up = dTx * 1000.0 / elapsed
            speedHistory.addLast(down + up)
            if (speedHistory.size > 30) speedHistory.removeFirst()
            peak = max(peak, down + up)
            IspResolver.ensureFresh(context)
            val snapshot = NetworkStatsReader.currentNetwork(context, persianProvider())
            UsageLedger.sample(context, rx, tx, snapshot, now)
            emit(
                LiveTick(
                    timestamp = now,
                    downBytesPerSecond = down,
                    upBytesPerSecond = up,
                    totalRx = rx,
                    totalTx = tx,
                    sessionRxBytes = sessionRx,
                    sessionTxBytes = sessionTx,
                    sessionStart = sessionStart,
                    averageSpeed = speedHistory.average(),
                    peakSpeed = peak,
                    network = snapshot,
                    uptimeMillis = SystemClock.elapsedRealtime(),
                ),
            )
            delay(intervalMillis)
        }
    }.flowOn(Dispatchers.IO)

    /** پینگ چند بار پشت‌سرهم به چند مقصد – میانگین و آمار. */
    fun ping(hosts: List<String> = listOf("1.1.1.1", "8.8.8.8", "4.2.2.4")): Flow<PingResult> = flow {
        for (host in hosts) {
            val samples = mutableListOf<Long>()
            repeat(4) {
                val ms = tcpPing(host)
                if (ms > 0) samples.add(ms)
                delay(120)
            }
            emit(
                PingResult(
                    host = host,
                    samples = samples,
                    averageMs = if (samples.isEmpty()) -1.0 else samples.average(),
                    minMs = samples.minOrNull() ?: -1,
                    maxMs = samples.maxOrNull() ?: -1,
                    jitterMs = if (samples.size > 1) samples.max() - samples.min() else 0,
                    lossPercent = ((4 - samples.size) * 100) / 4,
                ),
            )
        }
    }.flowOn(Dispatchers.IO)

    /** «پینگ» بدون نیاز به root: اندازه‌گیری زمان برقراری اتصال TCP. */
    private fun tcpPing(host: String): Long {
        val start = System.nanoTime()
        return runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, 443), 1500)
            }
            (System.nanoTime() - start) / 1_000_000
        }.getOrDefault(-1L)
    }

    /** پایش لحظه‌ای تغییر شبکه. */
    fun observeNetwork(persianProvider: () -> Boolean = { true }): Flow<NetworkSnapshot> = flow {
        var last: NetworkSnapshot? = null
        while (true) {
            val snapshot = NetworkStatsReader.currentNetwork(context, persianProvider())
            if (snapshot.type != last?.type || snapshot.operator.id != last?.operator?.id) {
                last = snapshot
                emit(snapshot)
            }
            delay(5_000)
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        fun formatHourMinute(millis: Long): String =
            SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))

        fun formatHourMinute(millis: Long, persian: Boolean): String {
            val raw = formatHourMinute(millis)
            return if (persian) Formats.toPersianDigits(raw) else raw
        }

        fun formatDate(millis: Long): String =
            SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(millis))

        fun formatDate(millis: Long, persian: Boolean): String =
            if (persian) JalaliDate.fromMillis(millis).numeric(true)
            else formatDate(millis)
    }
}

data class LiveTick(
    val timestamp: Long,
    val downBytesPerSecond: Double,
    val upBytesPerSecond: Double,
    val totalRx: Long,
    val totalTx: Long,
    val sessionRxBytes: Long,
    val sessionTxBytes: Long,
    val sessionStart: Long,
    val averageSpeed: Double,
    val peakSpeed: Double,
    val network: NetworkSnapshot,
    val uptimeMillis: Long,
)

data class PingResult(
    val host: String,
    val samples: List<Long>,
    val averageMs: Double,
    val minMs: Long,
    val maxMs: Long,
    val jitterMs: Long,
    val lossPercent: Int,
)
