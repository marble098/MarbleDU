package ir.marbledu.datausage.data

/**
 * تبدیل تأخیر، نوسان و افت بسته به یک نمره‌ی ۰ تا ۱۰۰ و اینکه اتصال برای
 * بازی آنلاین، تماس تصویری، تماشای ویدیو و وب‌گردی چقدر مناسب است.
 */
object ConnectionQuality {

    enum class Grade { EXCELLENT, GOOD, FAIR, POOR }

    data class Verdict(
        val score: Int,
        val overall: Grade,
        val gaming: Grade,
        val videoCall: Grade,
        val streaming: Grade,
        val browsing: Grade,
    )

    fun score(averageMs: Double, jitterMs: Double, lossPercent: Int): Int {
        if (averageMs <= 0.0) return 0
        val latencyPenalty = ((averageMs - 40.0) / 360.0 * 45.0).coerceIn(0.0, 45.0)
        val jitterPenalty = (jitterMs / 100.0 * 25.0).coerceIn(0.0, 25.0)
        val lossPenalty = (lossPercent * 1.5).coerceIn(0.0, 30.0)
        return (100.0 - latencyPenalty - jitterPenalty - lossPenalty).toInt().coerceIn(0, 100)
    }

    fun evaluate(averageMs: Double, jitterMs: Double, lossPercent: Int): Verdict {
        val s = score(averageMs, jitterMs, lossPercent)
        val valid = averageMs > 0.0
        fun grade(excellent: Boolean, good: Boolean, fair: Boolean): Grade = when {
            !valid -> Grade.POOR
            excellent -> Grade.EXCELLENT
            good -> Grade.GOOD
            fair -> Grade.FAIR
            else -> Grade.POOR
        }
        return Verdict(
            score = s,
            overall = when {
                s >= 85 -> Grade.EXCELLENT
                s >= 65 -> Grade.GOOD
                s >= 40 -> Grade.FAIR
                else -> Grade.POOR
            },
            gaming = grade(
                averageMs <= 80 && jitterMs <= 20 && lossPercent == 0,
                averageMs <= 130 && jitterMs <= 40 && lossPercent <= 2,
                averageMs <= 220 && lossPercent <= 5,
            ),
            videoCall = grade(
                averageMs <= 150 && jitterMs <= 30 && lossPercent <= 1,
                averageMs <= 250 && jitterMs <= 50 && lossPercent <= 3,
                averageMs <= 400 && lossPercent <= 8,
            ),
            streaming = grade(
                averageMs <= 300 && lossPercent <= 2,
                averageMs <= 600 && lossPercent <= 5,
                lossPercent <= 15,
            ),
            browsing = grade(
                averageMs <= 200 && lossPercent <= 5,
                averageMs <= 450 && lossPercent <= 10,
                averageMs <= 900,
            ),
        )
    }

    /** جمع‌بندی چند مقصد: فقط مقصدهایی که پاسخ داده‌اند حساب می‌شوند. */
    fun evaluate(results: List<PingResult>): Verdict? {
        val ok = results.filter { it.ok }
        if (ok.isEmpty()) return null
        return evaluate(
            averageMs = ok.map { it.averageMs }.average(),
            jitterMs = ok.map { it.jitterMs }.average(),
            lossPercent = ok.map { it.lossPercent }.average().toInt(),
        )
    }
}
