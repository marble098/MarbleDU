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
                    val pkg = if (uid >= 0) runCatching { pm.getNameForUid(uid) }.getOrNull() else null
                    val cleanPkg = pkg?.substringBefore(':')
                    AppUsage(
                        uid = uid,
                        packageName = cleanPkg ?: "uid:$uid",
                        label = specialLabel(uid) ?: labelFor(cleanPkg),
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

    // ------------------------------------------------------------------
    // گزارش ۳۰ روزه
    // ------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun deviceTraffic(
        manager: NetworkStatsManager,
        networkType: Int,
        subscriber: String?,
        start: Long,
        end: Long,
    ): Traffic {
        if (end <= start) return Traffic()
        return runCatching {
            val bucket = manager.querySummaryForDevice(networkType, subscriber, start, end)
            if (bucket == null) Traffic()
            else Traffic(bucket.rxBytes.coerceAtLeast(0), bucket.txBytes.coerceAtLeast(0))
        }.getOrDefault(Traffic())
    }

    /** خلاصه‌ی هر روز از [days] روز اخیر (قدیمی‌ترین اول): دانلود/آپلودِ موبایل و وای‌فای. */
    suspend fun daySummaries(days: Int = 30): List<DaySummary> = withContext(Dispatchers.IO) {
        val manager = nsm ?: return@withContext emptyList()
        val subscriber = subscriberId()
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, -(days - 1).coerceAtLeast(0))
        }
        val result = ArrayList<DaySummary>(days.coerceAtLeast(0))
        repeat(days.coerceAtLeast(0)) {
            val start = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val end = minOf(calendar.timeInMillis, now)
            result.add(
                DaySummary(
                    startMillis = start,
                    endMillis = calendar.timeInMillis,
                    mobile = deviceTraffic(manager, ConnectivityManager.TYPE_MOBILE, subscriber, start, end),
                    wifi = deviceTraffic(manager, ConnectivityManager.TYPE_WIFI, null, start, end),
                ),
            )
        }
        result
    }

    /** ۲۴ ساعتِ یک روز: هر ساعت چقدر دانلود/آپلود روی موبایل و وای‌فای. */
    suspend fun dayHours(dayStart: Long, dayEnd: Long): List<HourTraffic> = withContext(Dispatchers.IO) {
        val manager = nsm ?: return@withContext emptyList()
        val subscriber = subscriberId()
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = dayStart }
        val result = ArrayList<HourTraffic>(24)
        var guard = 0
        while (calendar.timeInMillis < dayEnd && guard < 26) {
            guard++
            val start = calendar.timeInMillis
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            calendar.add(Calendar.HOUR_OF_DAY, 1)
            val end = minOf(calendar.timeInMillis, dayEnd, now)
            val mobile = deviceTraffic(manager, ConnectivityManager.TYPE_MOBILE, subscriber, start, end)
            val wifi = deviceTraffic(manager, ConnectivityManager.TYPE_WIFI, null, start, end)
            val existing = result.indexOfFirst { it.hour == hour }
            if (existing >= 0) {
                // روز تغییر ساعت رسمی: همان ساعت دوبار می‌آید.
                val old = result[existing]
                result[existing] = old.copy(mobile = old.mobile + mobile, wifi = old.wifi + wifi)
            } else {
                result.add(HourTraffic(hour, mobile, wifi))
            }
        }
        // همیشه ۲۴ ستون، حتی اگر روز هنوز تمام نشده باشد.
        (0..23).map { h -> result.firstOrNull { it.hour == h } ?: HourTraffic(h, Traffic(), Traffic()) }
    }

    /** مصرف هر اپ در یک بازه، با تفکیک دانلود/آپلود و موبایل/وای‌فای. */
    @SuppressLint("MissingPermission")
    suspend fun appTraffic(fromMillis: Long, toMillis: Long): List<AppTraffic> = withContext(Dispatchers.IO) {
        val manager = nsm ?: return@withContext emptyList()
        val map = LinkedHashMap<Int, AppTraffic>()
        fun collect(networkType: Int, subscriber: String?, wifi: Boolean) {
            val stats = runCatching {
                manager.querySummary(networkType, subscriber, fromMillis, toMillis)
            }.getOrNull() ?: return
            try {
                val bucket = NetworkStats.Bucket()
                while (stats.hasNextBucket() && stats.getNextBucket(bucket)) {
                    val traffic = Traffic(bucket.rxBytes.coerceAtLeast(0), bucket.txBytes.coerceAtLeast(0))
                    if (traffic.total <= 0) continue
                    val uid = bucket.uid
                    val existing = map[uid] ?: run {
                        val pkg = if (uid >= 0) runCatching { pm.getNameForUid(uid) }.getOrNull() else null
                        AppTraffic(
                            uid = uid,
                            packageName = pkg?.substringBefore(':') ?: "uid:$uid",
                            label = specialLabel(uid) ?: labelFor(pkg?.substringBefore(':')),
                            mobile = Traffic(),
                            wifi = Traffic(),
                        )
                    }
                    map[uid] = if (wifi) existing.copy(wifi = existing.wifi + traffic)
                    else existing.copy(mobile = existing.mobile + traffic)
                }
            } catch (ignored: Exception) {
                // آمار ناقص بهتر از کرش است.
            } finally {
                runCatching { stats.close() }
            }
        }
        collect(ConnectivityManager.TYPE_MOBILE, subscriberId(), wifi = false)
        collect(ConnectivityManager.TYPE_WIFI, null, wifi = true)
        map.values.sortedByDescending { it.total.total }
    }

    /** شناسه‌های ویژه‌ی سیستم که اپ واقعی نیستند (رابط کاربری نام بومی می‌گذارد). */
    private fun specialLabel(uid: Int): String? = when (uid) {
        NetworkStats.Bucket.UID_REMOVED -> "Removed apps"
        NetworkStats.Bucket.UID_TETHERING -> "Tethering"
        0 -> "root"
        1000 -> "Android system"
        1001 -> "Phone / Radio"
        1013 -> "Media server"
        else -> null
    }

    /** مجموع مصرف امروز (موبایل و وای‌فای) با حداقل سربار برای سرویس اعلان. */
    suspend fun todayDeviceTraffic(): Pair<Traffic, Traffic> = withContext(Dispatchers.IO) {
        val manager = nsm ?: return@withContext Traffic() to Traffic()
        val now = System.currentTimeMillis()
        val start = TimeMath.dayStart(now)
        val subscriber = subscriberId()
        val mobile = deviceTraffic(manager, ConnectivityManager.TYPE_MOBILE, subscriber, start, now)
        val wifi = deviceTraffic(manager, ConnectivityManager.TYPE_WIFI, null, start, now)
        mobile to wifi
    }

    /** مجموع مصرف ۳۰ روزه‌ی موبایل برای محاسبه‌ی باقی‌مانده‌ی سقف بسته. */
    suspend fun monthMobileTotal(): Long = withContext(Dispatchers.IO) {
        val manager = nsm ?: return@withContext 0L
        val now = System.currentTimeMillis()
        val start = now - TimeRange.MONTH.millis()
        val fromApps = usageByApp(start, now, ConnectionType.CELLULAR).sumOf { it.bytes }
        if (fromApps > 0L) return@withContext fromApps
        deviceTraffic(manager, ConnectivityManager.TYPE_MOBILE, subscriberId(), start, now).total
    }

    /** پرمصرف‌ترین اپ امروز (برای نمایش اختیاری در نوار اعلان). */
    suspend fun topAppToday(persian: Boolean): Pair<String, Long>? = withContext(Dispatchers.IO) {
        if (!hasUsageAccess()) return@withContext null
        val now = System.currentTimeMillis()
        val apps = appTraffic(TimeMath.dayStart(now), now)
            .filter { it.uid != Process.myUid() && it.total.total > 0 }
        val top = apps.firstOrNull() ?: return@withContext null
        val name = OperatorCatalog.specialUidLabel(top.uid, persian) ?: top.label
        name to top.total.total
    }

    /** ترافیک ساعتی یک روز به تفکیک یک اپراتور (برای گزارش ۲۴ ساعت). */
    suspend fun hourlyOperatorTraffic(
        dayStart: Long,
        operatorId: String,
    ): List<HourOperatorTraffic> = withContext(Dispatchers.IO) {
        UsageLedger.hourlyForDay(context, dayStart).filter { it.operatorId == operatorId }
    }

    /** گزارش کامل یک روز. */
    suspend fun dayReport(summary: DaySummary): DayReport {
        val now = System.currentTimeMillis()
        val end = minOf(summary.endMillis, now)
        val hours = dayHours(summary.startMillis, summary.endMillis)
        val apps = appTraffic(summary.startMillis, end)
        val sim = NetworkStatsReader.simOperator(context)
        val operators = UsageLedger.reconcile(
            recorded = UsageLedger.forDay(context, summary.startMillis),
            systemMobile = summary.mobile,
            systemWifi = summary.wifi,
            defaultMobileId = sim?.id ?: OperatorCatalog.ID_MOBILE,
        )
        return DayReport(summary, hours, apps, operators)
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

/** خلاصه‌ی یک روز تقویمی. */
data class DaySummary(
    val startMillis: Long,
    val endMillis: Long,
    val mobile: Traffic,
    val wifi: Traffic,
) {
    val all: Traffic get() = mobile + wifi
    val total: Long get() = mobile.total + wifi.total
}

/** مصرف یک ساعت از روز (۰ تا ۲۳). */
data class HourTraffic(val hour: Int, val mobile: Traffic, val wifi: Traffic) {
    val total: Long get() = mobile.total + wifi.total
    val rx: Long get() = mobile.rx + wifi.rx
    val tx: Long get() = mobile.tx + wifi.tx
}

data class AppTraffic(
    val uid: Int,
    val packageName: String,
    val label: String,
    val mobile: Traffic,
    val wifi: Traffic,
) {
    val total: Traffic get() = mobile + wifi
}

data class DayReport(
    val summary: DaySummary,
    val hours: List<HourTraffic>,
    val apps: List<AppTraffic>,
    val operators: List<OperatorTraffic>,
)

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
