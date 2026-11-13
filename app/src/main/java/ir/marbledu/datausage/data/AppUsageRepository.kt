package ir.marbledu.datausage.data

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * تفکیک مصرف به تفکیک اپ (مثل NetworkStatsManager xray/neko) به‌همراه نام و
 * آیکون اپ و «آخرین زمان استفاده» برای گزارش «از چه ساعتی تا چه ساعتی».
 */
class AppUsageRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val nsm: NetworkStatsManager? =
        context.getSystemService(NetworkStatsManager::class.java)

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
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
        val result = mutableMapOf<Int, AppUsage>()
        runCatching {
            manager.querySummary(networkType, null, fromMillis, toMillis)
        }.onSuccess { stats ->
            while (stats.hasNextBucket()) {
                val bucket = NetworkStats.Bucket()
                stats.getNextBucket(bucket)
                val uid = bucket.uid
                if (uid == Process.myUid() || uid < 0) continue
                val bytes = bucket.rxBytes + bucket.txBytes
                if (bytes <= 0) continue
                val existing = result[uid]
                if (existing == null) {
                    val pkg = runCatching { pm.getNameForUid(uid) }.getOrNull()
                    result[uid] = AppUsage(
                        uid = uid,
                        packageName = pkg ?: "uid:$uid",
                        label = labelFor(pkg),
                        bytes = bytes,
                        firstTimestamp = bucket.startTimeStamp.takeIf { it > 0 } ?: fromMillis,
                        lastTimestamp = bucket.endTimeStamp.takeIf { it > 0 } ?: toMillis,
                    )
                } else {
                    result[uid] = existing.copy(
                        bytes = existing.bytes + bytes,
                        firstTimestamp = minOf(existing.firstTimestamp, bucket.startTimeStamp),
                        lastTimestamp = maxOf(existing.lastTimestamp, bucket.endTimeStamp),
                    )
                }
            }
        }
        result.values.sortedByDescending { it.bytes }
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
