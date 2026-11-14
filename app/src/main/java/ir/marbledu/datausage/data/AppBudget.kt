package ir.marbledu.datausage.data

import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * «سهم هر اپ از بسته» — برنامه‌ریزی بسته بر اساس رفتار واقعی، نه حدس.
 *
 * مسئله‌ای که حل می‌کند: کاربر می‌داند بسته‌اش چند گیگ است و چند روز از دوره
 * مانده، ولی نمی‌داند *کدام اپ* دارد بسته را می‌خورد و اگر یکی از آن‌ها را
 * محدود کند چقدر عقب نمی‌افتد. اینجا برای هر اپ سه عدد حساب می‌شود:
 *
 *  - [AppAllowance.sharePercent]   : چند درصد از مصرف دوره را همین اپ ساخته،
 *  - [AppAllowance.keepBytes]      : با همین روند تا پایان دوره چقدر مصرف می‌کند،
 *  - [AppAllowance.safeLimitBytes] : سهم این اپ از باقی‌مانده‌ی بسته، به همان
 *    نسبتی که تا امروز مصرف کرده،
 *  - [AppAllowance.overPace]       : این اپ به‌تنهایی بیشتر از «سهم روزانه‌ی کل
 *    بسته» مصرف می‌کند؛ یعنی همان اپی است که بسته را زودتر تمام می‌کند.
 *
 * نکته‌ی مهم درباره‌ی [AppAllowance.overPace]: مقایسه‌ی ساده‌ی «مصرف پیشِ رو» با
 * «سهم متناسب» هیچ‌وقت اپ‌ها را از هم جدا نمی‌کند، چون نسبت‌ها ساده می‌شوند و
 * نتیجه برای همه‌ی اپ‌ها یکی می‌شود. برای همین معیار «سرعت» است: آهنگ روزانه‌ی
 * همین اپ با سهم روزانه‌ی کل بسته سنجیده می‌شود، نه با سهم متناسب خودش.
 *
 * کلاس کاملاً خالص است (بدون اندروید) تا با تست واحد سنجیده شود.
 */
object AppBudget {

    /** مصرف یک اپ در دوره‌ی جاری. */
    data class AppSpend(val key: String, val label: String, val bytes: Long)

    /** سهم یک اپ از بسته‌ی باقی‌مانده. */
    data class AppAllowance(
        val key: String,
        val label: String,
        /** مصرف ثبت‌شده در دوره‌ی جاری. */
        val usedBytes: Long,
        /** درصد از کل مصرف دوره (۰..۱۰۰). */
        val sharePercent: Int,
        /** با همین روند تا پایان دوره مصرف می‌شود (تخمین). */
        val keepBytes: Long,
        /** سهم این اپ از باقی‌مانده‌ی بسته (به نسبت مصرفش از کل دوره). */
        val safeLimitBytes: Long,
        /** آهنگ مصرف روزانه‌ی همین اپ در دوره‌ی جاری. */
        val dailyPaceBytes: Long,
        /**
         * همین اپ به‌تنهایی بیشتر از سهم روزانه‌ی کل بسته مصرف می‌کند؛
         * یعنی اگر محدود نشود، بسته پیش از پایان دوره تمام می‌شود.
         */
        val overPace: Boolean,
    )

    data class Plan(
        val capBytes: Long,
        val usedBytes: Long,
        val remainingBytes: Long,
        val daysLeftInCycle: Int,
        val dailyBudgetBytes: Long,
        /** سهم روزانه‌ی هر اپ اگر همه‌ی برنامه‌ها همان‌قدر که امروز مصرف می‌کنند ادامه دهند. */
        val rows: List<AppAllowance>,
        /** جمع تخمینی مصرف *پیشِ رو* تا پایان دوره (بدون مصرف گذشته). */
        val projectedBytes: Long,
        /** با این روند، بسته چند روز زودتر تمام می‌شود (۰ = سر وقت یا دیرتر). */
        val shortfallDays: Int,
    ) {
        val willRunOutEarly: Boolean get() = shortfallDays > 0
        val overPaceApps: List<AppAllowance> get() = rows.filter { it.overPace }
    }

    /**
     * ساخت نقشه‌ی سهم اپ‌ها.
     *
     * @param spends مصرف هر اپ در دوره‌ی جاری (هر ترتیبی).
     * @param capBytes سقف بسته؛ اگر صفر باشد فقط درصدها محاسبه می‌شوند.
     * @param usedBytes مصرف کل موبایلِ دوره؛ اگر با جمع [spends] فرق داشت،
     *   تفاوت به‌عنوان «سایر برنامه‌ها» در نظر گرفته می‌شود تا درصدها به ۱۰۰ برسند.
     * @param daysLeftInCycle روزهای باقی‌مانده (حداقل ۱).
     * @param elapsedDays روزهای سپری‌شده از دوره (حداقل ۱) برای محاسبه‌ی نرخ روزانه.
     */
    fun plan(
        spends: List<AppSpend>,
        capBytes: Long,
        usedBytes: Long,
        daysLeftInCycle: Int,
        elapsedDays: Int,
    ): Plan {
        val daysLeft = daysLeftInCycle.coerceAtLeast(1)
        val elapsed = elapsedDays.coerceAtLeast(1)
        val cap = capBytes.coerceAtLeast(0L)
        val remaining = (cap - usedBytes).coerceAtLeast(0L)

        val clean = spends.filter { it.bytes > 0L && it.key.isNotBlank() }
            .sortedByDescending { it.bytes }
        val appSum = clean.sumOf { it.bytes }
        // «سایر برنامه‌ها» = مصرف موبایلی که به اپ مشخصی نسبت داده نشده است.
        val others = (usedBytes - appSum).coerceAtLeast(0L)
        val total = usedBytes.coerceAtLeast(appSum).coerceAtLeast(0L)

        fun percent(value: Long): Int =
            if (total <= 0L) 0 else ((value * 100.0) / total).roundToInt().coerceIn(0, 100)

        // سهم روزانه‌ی کل بسته: اگر هر روز بیشتر از این مصرف شود، بسته نمی‌رسد.
        val dailyBudget = if (cap <= 0L) 0L else (remaining / daysLeft).coerceAtLeast(0L)
        val rows = clean.map { spend ->
            // مصرف روزانه‌ی همین اپ، ضرب در روزهای مانده = تخمین مصرف پیشِ رو.
            val dailyPace = spend.bytes.toDouble() / elapsed
            val keep = (dailyPace * daysLeft).roundToLong()
            // سهم این اپ از باقی‌مانده‌ی بسته، به همان نسبتی که تا امروز مصرف کرده.
            val shareOfTotal = if (total <= 0L) 0.0 else spend.bytes.toDouble() / total
            val safe = (remaining * shareOfTotal).toLong()
            val pace = dailyPace.roundToLong()
            AppAllowance(
                key = spend.key,
                label = spend.label,
                usedBytes = spend.bytes,
                sharePercent = percent(spend.bytes),
                keepBytes = keep,
                safeLimitBytes = if (cap <= 0L) 0L else safe,
                dailyPaceBytes = pace,
                // بدون سقف، «سهم روزانه» معنا ندارد و هیچ اپی ناهنجار شمرده نمی‌شود.
                overPace = cap > 0L && dailyBudget > 0L && pace > dailyBudget,
            )
        }

        val projected = rows.sumOf { it.keepBytes } +
            (if (total > 0L && others > 0L) (others.toDouble() / elapsed * daysLeft).roundToLong() else 0L)
        // چند روز زودتر تمام می‌شود؟ با نرخ روزانه‌ی فعلی نسبت به بودجه‌ی روزانه.
        val shortfall = when {
            cap <= 0L -> 0
            remaining <= 0L -> daysLeft
            dailyBudget <= 0L -> 0
            projected <= remaining -> 0
            else -> {
                val dailyPace = projected.toDouble() / daysLeft
                val extraDays = (projected - remaining).toDouble() / dailyPace.coerceAtLeast(1.0)
                extraDays.roundToInt().coerceIn(0, daysLeft)
            }
        }
        // اگر مصرف دوره از سقف گذشته باشد، همین حالا «تمام شده» حساب می‌شود.
        val finalShortfall = if (cap > 0L && usedBytes > cap) daysLeft else shortfall

        return Plan(
            capBytes = cap,
            usedBytes = usedBytes,
            remainingBytes = remaining,
            daysLeftInCycle = daysLeft,
            dailyBudgetBytes = dailyBudget,
            rows = rows,
            projectedBytes = projected,
            shortfallDays = finalShortfall,
        )
    }

    /**
     * کاربر می‌گوید «این اپ را محدود می‌کنم»؛ این تابع می‌گوید این تصمیم چند
     * روز به عمر بسته اضافه می‌کند (یا اگر اپ کم‌مصرف باشد، تقریباً هیچ).
     *
     * منطق ساده است: حجم آزادشده تقسیم بر مصرف روزانه‌ی قبلی همان اپ، تعداد
     * روزهایی است که بسته بیشتر دوام می‌آورد؛ و منطقی نیست بیشتر از روزهای
     * باقی‌مانده‌ی دوره بشود.
     *
     * @param capBytes سقف بسته
     * @param usedBytes مصرف فعلی دوره
     * @param daysLeftInCycle روزهای باقی‌مانده
     * @param freedBytesPerDay حجمی که روزانه با محدودکردن این اپ آزاد می‌شود
     */
    fun daysGained(
        capBytes: Long,
        usedBytes: Long,
        daysLeftInCycle: Int,
        freedBytesPerDay: Long,
    ): Int {
        if (capBytes <= 0L || freedBytesPerDay <= 0L) return 0
        val daysLeft = daysLeftInCycle.coerceAtLeast(1)
        val remaining = (capBytes - usedBytes).coerceAtLeast(0L)
        if (remaining <= 0L) return 0
        // هر روزی که این اپ محدود بماند، همین حجم آزاد می‌شود.
        val extraDays = (remaining.toDouble() / freedBytesPerDay).roundToInt()
        return extraDays.coerceIn(0, daysLeft)
    }
}
