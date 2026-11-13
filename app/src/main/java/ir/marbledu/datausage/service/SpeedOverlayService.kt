package ir.marbledu.datausage.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import ir.marbledu.datausage.MainActivity
import ir.marbledu.datausage.MarbleDuApp
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.LocaleHelper
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
import kotlin.math.max
import kotlin.math.pow

/**
 * «عینک مصرف» — قابلیت تازه‌ی ۱.۵:
 * یک پنجره‌ی شیشه‌ای کوچک و همیشه-روی-صفحه که سرعت دانلود/آپلودِ همین لحظه
 * و مصرف امروز را نشان می‌دهد؛ روی هر اپی که باشی می‌بینی چقدر اینترنت
 * مصرف می‌شود. با کشیدن جابه‌جا می‌شود و با یک ضربه به برنامه باز می‌شود.
 *
 * بدون نیاز به VPN یا root؛ فقط شمارنده‌های شبکه‌ی سیستم خوانده می‌شوند.
 */
class SpeedOverlayService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)
    private var loop: Job? = null
    private var view: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var windowManager: WindowManager? = null

    private lateinit var downText: TextView
    private lateinit var upText: TextView
    private lateinit var todayText: TextView
    private lateinit var sparkView: MiniSpark

    private var lastRx = 0L
    private var lastTx = 0L
    private var lastTime = 0L
    private var initialized = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildServiceNotification())
        showOverlay()
        startLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (view == null) {
            startForeground(NOTIFICATION_ID, buildServiceNotification())
            showOverlay()
        }
        if (loop?.isActive != true) startLoop()
        return START_STICKY
    }

    // ------------------------------------------------------------------ UI
    private fun showOverlay() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val persian = LocaleHelper.isPersian(runCatching {
            kotlinx.coroutines.runBlocking { androidApp().settingsRepository.settings.first().language }
        }.getOrDefault(ir.marbledu.datausage.data.AppLanguage.SYSTEM))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF01B2338.toInt(), 0xF0111728.toInt()),
            ).apply {
                cornerRadius = dp(20f).toFloat()
                setStroke(dp(1), 0x33FFFFFF)
            }
            elevation = dp(10f).toFloat()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val dot = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF4DE8D0.toInt())
            }
        }
        header.addView(dot, LinearLayout.LayoutParams(dp(6), dp(6)).apply { marginEnd = dp(6) })
        val title = TextView(this).apply {
            text = if (persian) "سرعت زنده" else "Live speed"
            setTextColor(0xCCFFFFFF.toInt())
            textSize = 10f
        }
        header.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val close = TextView(this).apply {
            text = "✕"
            setTextColor(0x99FFFFFF.toInt())
            textSize = 11f
            setPadding(dp(6), dp(2), dp(2), dp(2))
            setOnClickListener { stopSelf() }
        }
        header.addView(close)
        root.addView(header)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        downText = TextView(this).apply {
            setTextColor(0xFF5CE9CF.toInt())
            textSize = 13f
            text = "↓ —"
            setPadding(0, dp(2), dp(8), 0)
        }
        upText = TextView(this).apply {
            setTextColor(0xFF7FB8FF.toInt())
            textSize = 13f
            text = "↑ —"
        }
        row.addView(downText)
        row.addView(upText)
        root.addView(row)

        todayText = TextView(this).apply {
            setTextColor(0x99FFFFFF.toInt())
            textSize = 9f
            text = if (persian) "امروز —" else "Today —"
        }
        root.addView(todayText)

        sparkView = MiniSpark(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(112), dp(20)).apply { topMargin = dp(4) }
            accent = 0xFF4DE8D0.toInt()
        }
        root.addView(sparkView)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(120)
        }
        params = lp

        // کشیدن برای جابه‌جایی + ضربه‌ی کوتاه برای باز کردن اپ
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var dragged = false
        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    touchX = event.rawX
                    touchY = event.rawY
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (kotlin.math.abs(dx) > dp(6) || kotlin.math.abs(dy) > dp(6)) dragged = true
                    lp.x = initialX + dx
                    lp.y = initialY + dy
                    runCatching { wm.updateViewLayout(root, lp) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) {
                        startActivity(
                            Intent(this@SpeedOverlayService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            },
                        )
                    }
                    true
                }
                else -> false
            }
        }

        runCatching {
            wm.addView(root, lp)
            view = root
        }.onFailure {
            // اجازه‌ی نمایش روی اپ‌های دیگر داده نشده است.
            stopSelf()
        }
    }

    private fun startLoop() {
        loop?.cancel()
        loop = scope.launch {
            val app = androidApp()
            while (isActive) {
                val persian = LocaleHelper.isPersian(
                    runCatching { app.settingsRepository.settings.first().language }
                        .getOrDefault(ir.marbledu.datausage.data.AppLanguage.SYSTEM),
                )
                val counters = NetworkStatsReader.byteCounters()
                val now = System.currentTimeMillis()
                if (!initialized) {
                    lastRx = counters.totalRxBytes
                    lastTx = counters.totalTxBytes
                    lastTime = now
                    initialized = true
                }
                val elapsed = (now - lastTime).coerceAtLeast(1)
                val down = max(0L, counters.totalRxBytes - lastRx) * 1000.0 / elapsed
                val up = max(0L, counters.totalTxBytes - lastTx) * 1000.0 / elapsed
                lastRx = counters.totalRxBytes
                lastTx = counters.totalTxBytes
                lastTime = now
                val today = runCatching { app.appUsageRepository.todayDeviceTraffic() }.getOrNull()
                runCatching {
                    launch(Dispatchers.Main) {
                        downText.text = "↓ " + Formats.compactSpeed(down, persian)
                        upText.text = "↑ " + Formats.compactSpeed(up, persian)
                        todayText.text = (if (persian) "امروز " else "Today ") +
                            Formats.compactBytes((today?.first?.total ?: 0L) + (today?.second?.total ?: 0L), persian)
                        sparkView.push(down + up)
                    }
                }
                delay(1000L)
            }
        }
    }

    private fun buildServiceNotification(): android.app.Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_OVERLAY,
                    "Speed overlay",
                    NotificationManager.IMPORTANCE_MIN,
                ).apply { description = "Keeps the live speed bubble running" },
            )
        }
        val open = PendingIntent.getActivity(
            this,
            90,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this,
            91,
            Intent(this, SpeedOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_OVERLAY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .addAction(R.drawable.ic_notif_action_stop, getString(R.string.overlay_stop), stop)
            .build()
    }

    private fun androidApp(): MarbleDuApp = applicationContext as MarbleDuApp

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        loop?.cancel()
        scope.cancel()
        view?.let { v -> runCatching { windowManager?.removeView(v) } }
        view = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_OVERLAY = "marbledu_overlay"
        private const val NOTIFICATION_ID = 1301
        private const val ACTION_STOP = "ir.marbledu.datausage.action.STOP_OVERLAY"

        fun start(context: Context) {
            runCatching {
                context.startForegroundService(Intent(context, SpeedOverlayService::class.java))
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, SpeedOverlayService::class.java)) }
        }
    }
}

/** نمودار میله‌ای کوچک سرعت برای حباب شناور (بدون Compose، سبک و سریع). */
private class MiniSpark(context: Context) : View(context) {

    var accent: Int = Color.CYAN
    private val points = ArrayDeque<Float>()

    fun push(value: Double) {
        points.addLast(value.toFloat())
        while (points.size > 28) points.removeFirst()
        invalidate()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return
        val maxValue = points.max().coerceAtLeast(1f)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            style = android.graphics.Paint.Style.FILL
        }
        val barW = width.toFloat() / points.size
        points.forEachIndexed { index, value ->
            val normalized = (value / maxValue).toDouble().pow(0.55).toFloat().coerceIn(0.04f, 1f)
            val h = height * normalized
            canvas.drawRoundRect(
                index * barW, height - h, index * barW + barW * 0.62f, height.toFloat(),
                barW * 0.31f, barW * 0.31f, paint,
            )
        }
    }
}
