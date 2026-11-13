package ir.marbledu.datausage.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ir.marbledu.datausage.MainActivity
import ir.marbledu.datausage.MarbleDuApp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.NetworkStatsReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.max

/**
 * سرویس پیش‌زمینه‌ی سبک: هر چند ثانیه شمارنده‌های شبکه را می‌خواند و سرعت را
 * در نوار اعلان نشان می‌دهد. هیچ VPN یا دسترسی خاصی لازم نیست.
 */
class MonitorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification(0.0, 0.0, "--"))
        loop?.cancel()
        loop = scope.launch {
            // فاصله‌ی به‌روزرسانی از تنظیمات خوانده می‌شود (نه ثابت ۵ ثانیه).
            val intervalSeconds = runCatching {
                runBlocking {
                    (application as MarbleDuApp).settingsRepository.settings.first()
                        .monitorIntervalSeconds
                }
            }.getOrDefault(5).coerceIn(1, 60)
            var lastRx = 0L
            var lastTx = 0L
            var lastTime = 0L
            var initialized = false
            while (isActive) {
                val (rx, tx) = NetworkStatsReader.totalInterfaceBytes()
                val now = System.currentTimeMillis()
                if (!initialized) {
                    lastRx = rx; lastTx = tx; lastTime = now; initialized = true
                }
                // سرعت با زمان واقعی سپری‌شده حساب می‌شود.
                val elapsed = (now - lastTime).coerceAtLeast(1)
                val down = max(0L, rx - lastRx) * 1000.0 / elapsed
                val up = max(0L, tx - lastTx) * 1000.0 / elapsed
                lastRx = rx; lastTx = tx; lastTime = now
                val snapshot = NetworkStatsReader.currentNetwork(this@MonitorService)
                notificationManager().notify(
                    NOTIFICATION_ID,
                    buildNotification(down, up, snapshot.carrierName),
                )
                delay(intervalSeconds * 1000L)
            }
        }
        return START_STICKY
    }

    private fun buildNotification(down: Double, up: Double, carrier: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val persian = resources.configuration.locales[0].language == "fa"
        val speedLine = "↓ ${Formats.speed(down, persian)}   ↑ ${Formats.speed(up, persian)}"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText("$speedLine • $carrier")
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        val manager = notificationManager()
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun notificationManager(): NotificationManager =
        getSystemService(NotificationManager::class.java)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "marbledu_monitor"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            runCatching { context.startForegroundService(intent) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }
    }
}
