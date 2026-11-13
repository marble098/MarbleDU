package ir.marbledu.datausage.data

/**
 * تشخیص جهش غیرعادی مصرف اپ‌ها:
 * مصرف «امروز» هر برنامه را با میانگین روزانه‌ی همان برنامه در روزهای اخیر
 * مقایسه می‌کند تا سریع معلوم شود کدام برنامه ناگهان بیشتر از معمول اینترنت مصرف کرده است.
 */
object AppSurgeDetector {

    data class Input(
        val uid: Int,
        val packageName: String,
        val label: String,
        val todayMobileBytes: Long,
        val todayWifiBytes: Long,
        val baselineMobileBytes: Long,
        val baselineWifiBytes: Long,
    ) {
        val todayTotalBytes: Long get() = todayMobileBytes + todayWifiBytes
        val baselineTotalBytes: Long get() = baselineMobileBytes + baselineWifiBytes
    }

    data class SurgeItem(
        val uid: Int,
        val packageName: String,
        val label: String,
        val todayTotalBytes: Long,
        val todayMobileBytes: Long,
        val todayWifiBytes: Long,
        val baselineDailyBytes: Long,
        val extraBytes: Long,
        /** نسبت مصرف امروز به میانگین روزانه (برای جهش تازه و بی‌سابقه ۰ است). */
        val multiplier: Double,
        val isNewSpike: Boolean,
    ) {
        val isRestrictableApp: Boolean
            get() = !packageName.startsWith("uid:") && uid >= 10_000
    }

    fun buildInputs(
        todayMobile: List<AppUsage>,
        todayWifi: List<AppUsage>,
        baselineMobile: List<AppUsage>,
        baselineWifi: List<AppUsage>,
    ): List<Input> {
        data class Acc(
            val uid: Int,
            val packageName: String,
            val label: String,
            var tMob: Long = 0L,
            var tWifi: Long = 0L,
            var bMob: Long = 0L,
            var bWifi: Long = 0L,
        )
        val map = LinkedHashMap<String, Acc>()
        fun getOrCreate(app: AppUsage): Acc = map.getOrPut(app.packageName) {
            Acc(uid = app.uid, packageName = app.packageName, label = app.label)
        }
        todayMobile.forEach { getOrCreate(it).tMob += it.bytes.coerceAtLeast(0L) }
        todayWifi.forEach { getOrCreate(it).tWifi += it.bytes.coerceAtLeast(0L) }
        baselineMobile.forEach { getOrCreate(it).bMob += it.bytes.coerceAtLeast(0L) }
        baselineWifi.forEach { getOrCreate(it).bWifi += it.bytes.coerceAtLeast(0L) }
        return map.values.map {
            Input(
                uid = it.uid,
                packageName = it.packageName,
                label = it.label,
                todayMobileBytes = it.tMob,
                todayWifiBytes = it.tWifi,
                baselineMobileBytes = it.bMob,
                baselineWifiBytes = it.bWifi,
            )
        }
    }

    fun detect(
        items: List<Input>,
        baselineDays: Int = 7,
        minTodayBytes: Long = 20L * 1024L * 1024L,
        minExtraBytes: Long = 15L * 1024L * 1024L,
        minMultiplier: Double = 1.7,
        newAppThreshold: Long = 50L * 1024L * 1024L,
        limit: Int = 8,
    ): List<SurgeItem> {
        val days = baselineDays.coerceAtLeast(1)
        val out = ArrayList<SurgeItem>()
        for (item in items) {
            if (item.todayTotalBytes < minTodayBytes) continue
            val avgDaily = item.baselineTotalBytes / days
            if (avgDaily <= 0L) {
                if (item.todayTotalBytes >= newAppThreshold) {
                    out.add(
                        SurgeItem(
                            uid = item.uid,
                            packageName = item.packageName,
                            label = item.label,
                            todayTotalBytes = item.todayTotalBytes,
                            todayMobileBytes = item.todayMobileBytes,
                            todayWifiBytes = item.todayWifiBytes,
                            baselineDailyBytes = 0L,
                            extraBytes = item.todayTotalBytes,
                            multiplier = 0.0,
                            isNewSpike = true,
                        ),
                    )
                }
                continue
            }
            val extra = item.todayTotalBytes - avgDaily
            val ratio = item.todayTotalBytes.toDouble() / avgDaily.toDouble()
            if (extra >= minExtraBytes && ratio >= minMultiplier) {
                out.add(
                    SurgeItem(
                        uid = item.uid,
                        packageName = item.packageName,
                        label = item.label,
                        todayTotalBytes = item.todayTotalBytes,
                        todayMobileBytes = item.todayMobileBytes,
                        todayWifiBytes = item.todayWifiBytes,
                        baselineDailyBytes = avgDaily,
                        extraBytes = extra,
                        multiplier = ratio,
                        isNewSpike = false,
                    ),
                )
            }
        }
        return out
            .sortedWith(
                compareByDescending<SurgeItem> { it.extraBytes }
                    .thenByDescending { it.multiplier },
            )
            .take(limit.coerceAtLeast(0))
    }
}
