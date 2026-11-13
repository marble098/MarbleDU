package ir.marbledu.datausage.data

/**
 * تحلیل خالص مصرف «پیش‌زمینه» در برابر «پس‌زمینه» برای هر برنامه.
 *
 * داده‌ها مستقیماً از bucket.state در NetworkStatsManager می‌آیند
 * (STATE_FOREGROUND در برابر STATE_DEFAULT) و هیچ وابستگی به اندروید در این
 * کلاس وجود ندارد تا با تست واحد قابل بررسی باشد.
 */
object BackgroundDrainAnalyzer {

    data class AppEntry(
        val uid: Int,
        val packageName: String,
        val label: String,
        val mobileForegroundBytes: Long,
        val mobileBackgroundBytes: Long,
        val mobileTotalBytes: Long,
        val wifiForegroundBytes: Long,
        val wifiBackgroundBytes: Long,
        val wifiTotalBytes: Long,
    ) {
        val totalBytes: Long get() = mobileTotalBytes + wifiTotalBytes
        val foregroundBytes: Long get() = mobileForegroundBytes + wifiForegroundBytes
        val backgroundBytes: Long get() = mobileBackgroundBytes + wifiBackgroundBytes
        val knownStateBytes: Long get() = foregroundBytes + backgroundBytes

        val backgroundSharePercent: Int
            get() = if (knownStateBytes <= 0L) 0
            else ((backgroundBytes * 100.0) / knownStateBytes).toInt().coerceIn(0, 100)

        val mobileBackgroundSharePercent: Int
            get() {
                val knownMobile = mobileForegroundBytes + mobileBackgroundBytes
                return if (knownMobile <= 0L) 0
                else ((mobileBackgroundBytes * 100.0) / knownMobile).toInt().coerceIn(0, 100)
            }

        /** سرویس‌های هسته و شناسه‌های خاص سیستم صفحه‌ی تنظیمات اپ مستقل ندارند. */
        val isRestrictableApp: Boolean
            get() = !packageName.startsWith("uid:") && uid >= 10_000
    }

    data class Summary(
        val totalBytes: Long,
        val foregroundBytes: Long,
        val backgroundBytes: Long,
        val mobileBackgroundBytes: Long,
        val wifiBackgroundBytes: Long,
        val backgroundSharePercent: Int,
        val entriesByPackage: Map<String, AppEntry>,
        val topDrainers: List<AppEntry>,
    )

    fun summarize(
        mobile: List<AppUsage>,
        wifi: List<AppUsage>,
        limit: Int = 10,
        minBackgroundBytes: Long = 1024L * 1024L,
    ): Summary {
        val map = LinkedHashMap<String, AppEntry>()
        mobile.forEach { app ->
            val current = map[app.packageName]
            map[app.packageName] = if (current == null) {
                AppEntry(
                    uid = app.uid,
                    packageName = app.packageName,
                    label = app.label,
                    mobileForegroundBytes = app.foregroundBytes.coerceAtLeast(0L),
                    mobileBackgroundBytes = app.backgroundBytes.coerceAtLeast(0L),
                    mobileTotalBytes = app.bytes.coerceAtLeast(0L),
                    wifiForegroundBytes = 0L,
                    wifiBackgroundBytes = 0L,
                    wifiTotalBytes = 0L,
                )
            } else {
                current.copy(
                    mobileForegroundBytes = current.mobileForegroundBytes + app.foregroundBytes.coerceAtLeast(0L),
                    mobileBackgroundBytes = current.mobileBackgroundBytes + app.backgroundBytes.coerceAtLeast(0L),
                    mobileTotalBytes = current.mobileTotalBytes + app.bytes.coerceAtLeast(0L),
                )
            }
        }
        wifi.forEach { app ->
            val current = map[app.packageName]
            map[app.packageName] = if (current == null) {
                AppEntry(
                    uid = app.uid,
                    packageName = app.packageName,
                    label = app.label,
                    mobileForegroundBytes = 0L,
                    mobileBackgroundBytes = 0L,
                    mobileTotalBytes = 0L,
                    wifiForegroundBytes = app.foregroundBytes.coerceAtLeast(0L),
                    wifiBackgroundBytes = app.backgroundBytes.coerceAtLeast(0L),
                    wifiTotalBytes = app.bytes.coerceAtLeast(0L),
                )
            } else {
                current.copy(
                    wifiForegroundBytes = current.wifiForegroundBytes + app.foregroundBytes.coerceAtLeast(0L),
                    wifiBackgroundBytes = current.wifiBackgroundBytes + app.backgroundBytes.coerceAtLeast(0L),
                    wifiTotalBytes = current.wifiTotalBytes + app.bytes.coerceAtLeast(0L),
                )
            }
        }

        val all = map.values
        val total = all.sumOf { it.totalBytes }
        val fg = all.sumOf { it.foregroundBytes }
        val bg = all.sumOf { it.backgroundBytes }
        val mobileBg = all.sumOf { it.mobileBackgroundBytes }
        val wifiBg = all.sumOf { it.wifiBackgroundBytes }
        val known = fg + bg
        val share = if (known <= 0L) 0 else ((bg * 100.0) / known).toInt().coerceIn(0, 100)

        val candidates = all.filter { it.backgroundBytes >= minBackgroundBytes }
            .ifEmpty { all.filter { it.backgroundBytes > 0L } }
        val top = candidates
            .sortedWith(
                compareByDescending<AppEntry> { it.backgroundBytes }
                    .thenByDescending { it.mobileBackgroundBytes }
                    .thenByDescending { it.totalBytes },
            )
            .take(limit.coerceAtLeast(0))

        return Summary(
            totalBytes = total,
            foregroundBytes = fg,
            backgroundBytes = bg,
            mobileBackgroundBytes = mobileBg,
            wifiBackgroundBytes = wifiBg,
            backgroundSharePercent = share,
            entriesByPackage = map,
            topDrainers = top,
        )
    }
}
