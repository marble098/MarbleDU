package ir.marbledu.datausage.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import android.telephony.TelephonyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * تفکیک مصرف به تفکیک اپ (مثل NetworkStatsManager xray/neko) به‌همراه نام و
 * آیکون اپ و «آخرین زمان استفاده» برای گزارش «از چه ساعتی تا چه ساعتی».
 */
class AppUsageRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val nsm: NetworkStatsManager? =
        context.getSystemService(NetworkStatsManager::class.java)

    /**
     * آیا «دسترسی به داده‌های مصرف» (PACKAGE_USAGE_STATS / AppOps) فعال است؟
     *
     * unsafeCheckOpNoThrow فقط از API 29 به بعد وجود دارد؛ minSdk اپ 26 است،
     * پس روی اندروید ۸/۹ باید از checkOpNoThrow قدیمی استفاده شود وگرنه
     * NoSuchMethodError می‌دهد و کل صفحه خالی می‌ماند.
     */
    @Suppress("DEPRECATION")
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * روی اندروید ۹ و پایین‌تر querySummary(TYPE_MOBILE, ...) به subscriberId
     * نیاز دارد؛ از اندروید ۱۰ به بعد null قابل قبول است.
     */
    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission", "HardwareIds")
    private fun subscriberId(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) return null
        return runCatching {
            context.getSystemService(TelephonyManager::class.java)?.subscriberId
        }.getOrNull()
    }

    /** مصرف هر اپ در بازه‌ی زمانی مشخص. */
    @SuppressLint("MissingPermission")
    suspend fun usageByApp(
        fromMillis: Long,
        toMillis: Long,
        type: ConnectionType,
    ): List<AppUsage> = withContext(Dispatchers.IO) {
        val manager = nsm ?: return@withContext emptyList()
        val networkType = when (type) {
            ConnectionType.WIFI -> ConnectivityManager.TYPE_WIFI
            else -> ConnectivityManager.TYPE_MOBILE
        }
        val stats = runCatching {
            manager.querySummary(networkType, subscriberId(), fromMillis, toMillis)
        }.getOrNull() ?: return@withContext emptyList()

        val result = mutableMapOf<Int, AppUsage>()
        try {
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket() && stats.getNextBucket(bucket)) {
                val uid = bucket.uid
                if (uid == Process.myUid() || uid < 0) continue
                val bytes = bucket.rxBytes + bucket.txBytes
                if (bytes <= 0) continue
                // گاهی startTimeStamp/endTimeStamp صفر است؛ نباید 1970 وارد گزارش شود.
                val first = bucket.startTimeStamp.takeIf { it > 0 } ?: fromMillis
                val last = bucket.endTimeStamp.takeIf { it > 0 } ?: toMillis
                val existing = result[uid]
                result[uid] = if (existing == null) {
                    val pkg = runCatching { pm.getNameForUid(uid) }.getOrNull()
                    AppUsage(
                        uid = uid,
                        packageName = pkg ?: "uid:$uid",
                        label = labelFor(pkg),
                        bytes = bytes,
                        firstTimestamp = first,
                        lastTimestamp = last,
                    )
                } else {
                    existing.copy(
                        bytes = existing.bytes + bytes,
                        firstTimestamp = minOf(existing.firstTimestamp, first),
                        lastTimestamp = maxOf(existing.lastTimestamp, last),
                    )
                }
            }
        } finally {
            stats.close()
        }
        result.values.sortedByDescending { it.bytes }
    }

    /** نوع اتصال فعلی (برای این‌که «مصرف امروز» همان چیزی باشد که کاربر انتظار دارد). */
    fun currentType(): ConnectionType =
        NetworkStatsReader.currentNetwork(context).type.let {
            if (it == ConnectionType.WIFI) ConnectionType.WIFI else ConnectionType.CELLULAR
        }

    /**
     * مصرف کل دستگاه در هر روز، برای نمودار روند.
     * مستقیماً از تاریخچه‌ی NetworkStatsManager خوانده می‌شود؛ نیازی به ذخیره‌سازی نیست.
     */
    @SuppressLint("MissingPermission")
    suspend fun dailyTotals(days: Int, type: ConnectionType): List<DayUsage> =
        withContext(Dispatchers.IO) {
            val manager = nsm ?: return@withContext emptyList()
            val networkType = if (type == ConnectionType.WIFI) ConnectivityManager.TYPE_WIFI
            else ConnectivityManager.TYPE_MOBILE
            val subscriber = subscriberId()
            // مرز روزها را با Calendar جلو می‌بریم تا تغییر ساعت تابستانی جابه‌جا نشود.
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, -(days - 1).coerceAtLeast(0))
            }
            val result = mutableListOf<DayUsage>()
            repeat(days.coerceAtLeast(0)) {
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val end = calendar.timeInMillis
                val bytes = runCatching {
                    val bucket =
                        manager.querySummaryForDevice(networkType, subscriber, start, end)
                    bucket.rxBytes + bucket.txBytes
                }.getOrDefault(0L)
                result.add(DayUsage(start, bytes))
            }
            result
        }

    /**
     * مصرف کل دستگاه در هر «ساعت شبانه‌روز» تا ساعت‌های پرمصرف واقعی پیدا شود.
     *
     * قبلاً ReportsScreen تمام بایت‌های یک اپ را به ساعتِ «آخرین استفاده»
     * نسبت می‌داد که کاملاً گمراه‌کننده بود. اینجا بازه را ساعت‌به‌ساعت
     * می‌شکنیم؛ تعداد پرس‌وجوها به [maxBuckets] محدود است تا برای بازه‌های
     * طولانی هم سریع بماند.
     */
    @SuppressLint("MissingPermission")
    suspend fun hourlyTotals(
        fromMillis: Long,
        toMillis: Long,
        type: ConnectionType,
        maxBuckets: Int = 24 * 7,
    ): List<HourUsage> = withContext(Dispatchers.IO) {
        val manager = nsm ?: return@withContext emptyList()
        val networkType = if (type == ConnectionType.WIFI) ConnectivityManager.TYPE_WIFI
        else ConnectivityManager.TYPE_MOBILE
        val subscriber = subscriberId()
        val hourMillis = 60L * 60 * 1000
        // اگر بازه بیشتر از سقف بود، فقط انتهای بازه (داده‌ی تازه‌تر) را می‌بینیم.
        val start = maxOf(fromMillis, toMillis - maxBuckets * hourMillis)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = start
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val buckets = LinkedHashMap<Int, Long>()
        while (calendar.timeInMillis < toMillis) {
            val sliceStart = calendar.timeInMillis
            calendar.add(Calendar.HOUR_OF_DAY, 1)
            val sliceEnd = minOf(calendar.timeInMillis, toMillis)
            if (sliceEnd <= sliceStart) continue
            val hourOfDay = Calendar.getInstance().apply {
                timeInMillis = sliceStart
            }.get(Calendar.HOUR_OF_DAY)
            val bytes = runCatching {
                val bucket = manager.querySummaryForDevice(networkType, subscriber, sliceStart, sliceEnd)
                bucket.rxBytes + bucket.txBytes
            }.getOrDefault(0L)
            buckets[hourOfDay] = (buckets[hourOfDay] ?: 0L) + bytes
        }
        buckets.map { HourUsage(it.key, it.value) }.sortedByDescending { it.bytes }
    }

    /** مصرف تک‌تک اپ‌ها به تفکیک وای‌فای و موبایل (نمای جزئیات). */
    suspend fun detailedBreakdown(from: Long, to: Long): Pair<List<AppUsage>, List<AppUsage>> {
        val mobile = usageByApp(from, to, ConnectionType.CELLULAR)
        val wifi = usageByApp(from, to, ConnectionType.WIFI)
        return mobile to wifi
    }

    private fun labelFor(pkg: String?): String {
        if (pkg == null) return "—"
        return runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg.substringAfterLast('.'))
    }

    fun iconFor(pkg: String?): android.graphics.drawable.Drawable? = runCatching {
        pkg?.let { pm.getApplicationIcon(it) }
    }.getOrNull()
}

data class DayUsage(val startMillis: Long, val bytes: Long)

/** مصرف تجمیعی در یک ساعت شبانه‌روز (۰ تا ۲۳). */
data class HourUsage(val hour: Int, val bytes: Long)

data class AppUsage(
    val uid: Int,
    val packageName: String,
    val label: String,
    val bytes: Long,
    val firstTimestamp: Long,
    val lastTimestamp: Long,
)

/** بازه‌های زمانی گزارش‌گیری. */
enum class TimeRange(val labelFa: String, val labelEn: String) {
    TODAY("امروز", "Today"),
    WEEK("۷ روز اخیر", "Last 7 days"),
    MONTH("۳۰ روز اخیر", "Last 30 days"),
    CYCLE("دوره‌ی صورتحساب", "Billing cycle");

    fun millis(): Long = when (this) {
        TODAY -> 24L * 60 * 60 * 1000
        WEEK -> 7L * 24 * 60 * 60 * 1000
        MONTH -> 30L * 24 * 60 * 60 * 1000
        CYCLE -> 30L * 24 * 60 * 60 * 1000
    }
}
