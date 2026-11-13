package ir.marbledu.datausage.data

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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

    /**
     * تست تأخیر واقعی به چند مقصد داخلی و بین‌المللی.
     * اگر VPN روشن باشد، یک‌بار از مسیر VPN و یک‌بار مستقیم (روی شبکه‌ی فیزیکی)
     * اندازه می‌گیرد تا تفاوت دو مسیر دیده شود. جزئیات روش در [PingEngine].
     */
    fun ping(): Flow<PingResult> = flow {
        val vpn = PingEngine.isVpnActive(context)
        val defaultRoute = if (vpn) PingRoute.VPN else PingRoute.DIRECT
        for (target in PingEngine.targets) {
            emit(PingEngine.measure(target, null, defaultRoute))
        }
        if (vpn) {
            val physical = PingEngine.physicalNetwork(context)
            if (physical != null) {
                for (target in PingEngine.targets) {
                    emit(PingEngine.measure(target, physical, PingRoute.DIRECT))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

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
