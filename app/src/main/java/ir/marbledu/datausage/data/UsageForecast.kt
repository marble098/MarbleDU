package ir.marbledu.datausage.data

import kotlin.math.roundToInt

/**
 * «تخمین مصرف پیشِ رو» — پیش‌بینی این‌که تا پایان امروز و تا پایان دوره چقدر
 * اینترنت مصرف می‌شود، بر اساس همان روزهای کاری/تعطیلیِ خودِ کاربر.
 *
 * چرا این سوال برای کاربر ایرانی مهم است؟ چون بسته‌ها حجمی‌اند و قیمت هر گیگ
 * بالاست؛ کاربر می‌خواهد بداند «اگر همین‌طوری پیش بروم، امروز چند مگ دیگر
 * مصرف می‌کنم و بسته تا آخر ماه می‌رسد یا نه؟». میانگین ساده گمراه‌کننده است
 * (روز کاری با روز تعطیل فرق دارد)، پس اینجا وزن روز هفته لحاظ می‌شود.
 *
 * کلاس کاملاً خالص است (بدون اندروید) و با تست واحد بررسی می‌شود.
 */
object UsageForecast {

    /** یک روز تاریخی با مصرف کل. */
    data class Day(val dayStart: Long, val bytes: Long)

    /**
     * @param projectedTodayBytes برآورد مصرف کل امروز
     * @param remainingTodayBytes برآورد مصرف باقی‌مانده‌ی امروز
     * @param weekdayFactor ضریب روز هفته (۱ = روز عادی)
     * @param confidence ۰..۱؛ هرچه روزهای بیشتری در تاریخچه باشد بالاتر
     * @param basisDays تعداد روزهایی که تخمین روی آن‌ها ساخته شده
     */
    data class Estimate(
        val projectedTodayBytes: Long,
        val remainingTodayBytes: Long,
        val weekdayFactor: Float,
        val confidence: Int,
        val basisDays: Int,
    )

    /**
     * برآورد مصرف امروز بر اساس ساعتِ همین لحظه و الگوی روزهای اخیر.
     *
     * منطق: هر روز یک «سرعت مصرف در دقیقه» دارد. برای امروز، مصرف دیده‌شده را
     * با سرعت روزهای مشابه (همان روز هفته) مقایسه می‌کنیم و باقی روز را با
     * همان سرعت پیش‌بینی می‌کنیم. اگر داده‌ی همان روز هفته نبود، میانگین کل
     * روزها (با وزن کمتر برای آخر هفته) استفاده می‌شود.
     *
     * @param history روزهای اخیر (قدیمی‌ترین اول)؛ امروز نباید داخلش باشد.
     * @param todayStart شروع امروز (نیمه‌شب محلی)
     * @param nowMillis همین لحظه
     * @param weekdayOf تبدیل میلی‌ثانیه به روز هفته (۰ = یکشنبه)
     */
    fun estimateToday(
        history: List<Day>,
        todayStart: Long,
        nowMillis: Long,
        todayBytes: Long,
        weekdayOf: (Long) -> Int = { millis ->
            java.util.Calendar.getInstance().apply { timeInMillis = millis }
                .get(java.util.Calendar.DAY_OF_WEEK) - 1
        },
    ): Estimate {
        val useable = history.filter { it.bytes > 0L }
        val elapsedFraction = (((nowMillis - todayStart).toDouble()) / DAY_MILLIS)
            .coerceIn(0.02, 1.0)
        if (useable.isEmpty()) {
            // بدون تاریخچه، همین مصرف امروز را تا آخر روز خطی گسترش می‌دهیم.
            val projected = (todayBytes / elapsedFraction).toLong()
            return Estimate(projected, (projected - todayBytes).coerceAtLeast(0L), 1f, 0, 0)
        }
        val weekday = weekdayOf(nowMillis)
        val sameWeekday = useable.filter { weekdayOf(it.dayStart) == weekday }
        val isWeekend = weekday == 4 || weekday == 5 // پنجشنبه/جمعه در ایران
        val reference = when {
            sameWeekday.size >= 2 -> sameWeekday
            else -> useable.takeLast(14)
        }
        val referenceAvg = reference.sumOf { it.bytes }.toDouble() / reference.size
        val allAvg = useable.sumOf { it.bytes }.toDouble() / useable.size
        val base = if (sameWeekday.size >= 2) referenceAvg else allAvg *
            (if (isWeekend) WEEKEND_FACTOR else 1f)
        // نرخ مصرف در دقیقه‌ی روزهای مرجع، برای پیش‌بینی ادامه‌ی امروز.
        val todayPace = todayBytes / elapsedFraction
        // اگر امروز خیلی کندتر یا تندتر از معمول است، ۵۰-۵۰ بین «سرعت امروز» و
        // «میانگین روز مرجع» میانگین می‌گیریم تا پیش‌بینی نه خیلی خوش‌بین باشد نه بدبین.
        val projected = if (todayBytes <= 0L) {
            base.toLong()
        } else {
            ((todayPace * 0.55) + (base * 0.45)).toLong().coerceAtLeast(todayBytes)
        }
        val confidence = (useable.size * 100 / 14).coerceIn(10, 100)
        val factor = if (base <= 0.0) 1f else (base / allAvg.coerceAtLeast(1.0)).toFloat()
            .coerceIn(0.4f, 2.2f)
        return Estimate(
            projectedTodayBytes = projected,
            remainingTodayBytes = (projected - todayBytes).coerceAtLeast(0L),
            weekdayFactor = factor,
            confidence = confidence,
            basisDays = useable.size,
        )
    }

    /** پیش‌بینی مصرف کل تا پایان دوره با احتساب تفاوت روزهای هفته و تعطیل. */
    data class CycleEstimate(
        val projectedBytes: Long,
        val capBytes: Long,
        val remainingBytes: Long,
        /** با این روند، بسته چند روز زودتر تمام می‌شود (۰ = می‌رسد). */
        val shortfallDays: Int,
    )

    fun estimateCycle(
        history: List<Day>,
        capBytes: Long,
        usedBytes: Long,
        daysLeftInCycle: Int,
        weekdayOf: (Long) -> Int = { millis ->
            java.util.Calendar.getInstance().apply { timeInMillis = millis }
                .get(java.util.Calendar.DAY_OF_WEEK) - 1
        },
        todayMillis: Long = System.currentTimeMillis(),
    ): CycleEstimate {
        val daysLeft = daysLeftInCycle.coerceAtLeast(0)
        val remaining = (capBytes - usedBytes).coerceAtLeast(0L)
        val useable = history.filter { it.bytes > 0L }
        if (useable.isEmpty() || daysLeft == 0) {
            return CycleEstimate(
                projectedBytes = usedBytes,
                capBytes = capBytes,
                remainingBytes = remaining,
                shortfallDays = if (capBytes > 0L && remaining <= 0L) daysLeft else 0,
            )
        }
        val avg = useable.sumOf { it.bytes }.toDouble() / useable.size
        // برای هر روز باقی‌مانده، ضریب روز هفته‌ی همان روز را از تاریخچه می‌گیریم.
        val factors = (1..daysLeft).map { offset ->
            val dayMillis = todayMillis + offset * DAY_MILLIS
            val weekday = weekdayOf(dayMillis)
            val sameDay = useable.filter { weekdayOf(it.dayStart) == weekday }
            val dayAvg = if (sameDay.isNotEmpty()) {
                sameDay.sumOf { it.bytes }.toDouble() / sameDay.size
            } else if (weekday == 4 || weekday == 5) avg * WEEKEND_FACTOR else avg
            (dayAvg / avg.coerceAtLeast(1.0)).coerceIn(0.3, 2.5)
        }
        val projected = usedBytes + factors.sumOf { (it * avg) }.toLong()
        val shortfall = when {
            capBytes <= 0L -> 0
            remaining <= 0L -> daysLeft
            projected <= capBytes -> 0
            else -> {
                val perDay = (projected - usedBytes).toDouble() / daysLeft
                val extra = (projected - capBytes).toDouble() / perDay.coerceAtLeast(1.0)
                extra.roundToInt().coerceIn(0, daysLeft)
            }
        }
        return CycleEstimate(
            projectedBytes = projected,
            capBytes = capBytes,
            remainingBytes = remaining,
            shortfallDays = shortfall,
        )
    }

    private const val DAY_MILLIS = 86_400_000.0
    /** پنجشنبه/جمعه در ایران پرمصرف‌تر است (وقت آزاد بیشتر). */
    private const val WEEKEND_FACTOR = 1.18f
}
