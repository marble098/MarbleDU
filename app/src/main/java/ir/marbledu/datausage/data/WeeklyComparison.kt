package ir.marbledu.datausage.data

object WeeklyComparison {
    data class Result(val recent: Long, val previous: Long) {
        // No invented percentage when the previous period had no usage.
        val changePercent: Double? get() = if (previous == 0L) null else
            (recent.toDouble() - previous.toDouble()) / previous * 100.0
    }

    /** Excludes today's partial day. Calendar arithmetic respects DST and local midnight. */
    fun build(days: List<DaySummary>, todayStart: Long): Result? {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = todayStart }
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
        val recentStart = calendar.timeInMillis
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
        val previousStart = calendar.timeInMillis
        val complete = days.filter { it.startMillis >= previousStart && it.endMillis <= todayStart }
            .distinctBy { it.startMillis }
        if (complete.count { it.startMillis >= recentStart } != 7 ||
            complete.count { it.startMillis < recentStart } != 7) return null
        return Result(complete.filter { it.startMillis >= recentStart }.sumOf { it.total },
            complete.filter { it.startMillis < recentStart }.sumOf { it.total })
    }
}
