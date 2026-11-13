package ir.marbledu.datausage.data

/** Pure transition logic: bytes spanning a change of route cannot safely be attributed. */
internal object SessionTracker {
    data class Route(val transport: String, val operatorId: String, val sim: SimIdentity? = null, val networkName: String = "")
    data class Result(val closed: UsageSession?, val open: UsageSession?)

    fun advance(current: UsageSession?, route: Route?, now: Long, delta: Traffic, nextId: Long): Result {
        val sameRoute = current != null && route != null && current.transport == route.transport &&
            current.sim == route.sim &&
            (route.transport != UsageLedger.MOBILE || current.operatorId == route.operatorId) &&
            (current.networkName.isBlank() || route.networkName.isBlank() || current.networkName == route.networkName)
        val sameDay = current != null && UsageLedger.dayKey(current.startMillis) == UsageLedger.dayKey(now)
        val continuing = sameRoute && sameDay
        val closed = if (current != null && !continuing) current.copy(
            endMillis = current.lastSampleMillis.coerceAtLeast(current.startMillis), endEstimated = true,
        ) else null
        if (route == null) return Result(closed, null)
        val gap = current != null && now - current.lastSampleMillis > 15_000L
        if (continuing) {
            return Result(null, current!!.copy(
                lastSampleMillis = now,
                rxBytes = current.rxBytes + delta.rx,
                txBytes = current.txBytes + delta.tx,
                operatorId = if (route.operatorId != OperatorCatalog.ID_WIFI && route.operatorId != OperatorCatalog.ID_UNKNOWN) route.operatorId else current.operatorId,
                networkName = route.networkName.ifBlank { current.networkName },
                samplingGaps = current.samplingGaps + if (gap) 1 else 0,
            ))
        }
        // A new route starts at this observation; the preceding interval is ambiguous.
        return Result(closed, UsageSession(
            id = nextId, startMillis = now, endMillis = null, lastSampleMillis = now,
            rxBytes = 0, txBytes = 0, operatorId = route.operatorId, sim = route.sim,
            transport = route.transport, networkName = route.networkName,
            samplingGaps = if (gap) 1 else 0,
        ))
    }
}
