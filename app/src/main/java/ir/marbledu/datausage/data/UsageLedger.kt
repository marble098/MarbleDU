package ir.marbledu.datausage.data

import android.content.Context
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.max

/** حجم دریافت/ارسال. */
data class Traffic(val rx: Long = 0L, val tx: Long = 0L) {
    val total: Long get() = rx + tx
    operator fun plus(other: Traffic): Traffic = Traffic(rx + other.rx, tx + other.tx)
}

/** شمارنده‌های تجمعی سیستم؛ mobileRx/mobileTx روی بعضی دستگاه‌های قدیمی در دسترس نیستند. */
data class NetworkByteCounters(
    val totalRxBytes: Long,
    val totalTxBytes: Long,
    val mobileRxBytes: Long = -1L,
    val mobileTxBytes: Long = -1L,
) {
    val hasMobileCounters: Boolean get() = mobileRxBytes >= 0L && mobileTxBytes >= 0L
}

/** شناسه‌ی محلی سیم‌کارت فعال برای داده؛ IMSI یا شماره تلفن ذخیره نمی‌شود. */
data class SimIdentity(
    val subscriptionId: Int? = null,
    val slotIndex: Int? = null,
    val displayName: String = "",
)

/** یک جلسه‌ی استفاده از مسیر داده‌ی موبایل. endMillis=null یعنی جلسه هنوز باز است. */
data class UsageSession(
    val id: Long,
    val startMillis: Long,
    val endMillis: Long?,
    val lastSampleMillis: Long,
    val rxBytes: Long,
    val txBytes: Long,
    val operatorId: String,
    val sim: SimIdentity? = null,
    /** مرزهای زمانی با نمونه‌برداری شبکه پیدا می‌شوند و ممکن است تا فاصله‌ی پایش خطا داشته باشند. */
    val startEstimated: Boolean = true,
    val endEstimated: Boolean = false,
    /** تعداد وقفه‌های بزرگ در نمونه‌برداری داخل این جلسه. */
    val samplingGaps: Int = 0,
) {
    val totalBytes: Long get() = rxBytes + txBytes
    val isOpen: Boolean get() = endMillis == null
}

/** مصرف یک اپراتور روی یک نوع اتصال در یک روز. */
data class OperatorTraffic(
    val operatorId: String,
    /** [UsageLedger.MOBILE] یا [UsageLedger.WIFI] */
    val transport: String,
    val traffic: Traffic,
    /** true یعنی حجم به سیم‌کارت/اپراتور قطعی نسبت داده نشده و فقط اختلاف سیستم است. */
    val estimated: Boolean = false,
    val sim: SimIdentity? = null,
)

/** مصرف یک اپراتور در یک ساعت خاص از یک روز (۰ تا ۲۳). */
data class HourOperatorTraffic(
    val hour: Int,
    val operatorId: String,
    val transport: String,
    val traffic: Traffic,
    val sim: SimIdentity? = null,
) {
    val total: Long get() = traffic.total
}

/**
 * دفتر محلی مصرف.
 *
 * اندروید مصرف تاریخی را عموماً در سطح دستگاه/نوع اتصال گزارش می‌کند؛ بنابراین
 * برای نسبت دادن آن به اپراتور و سیم‌کارت، شمارنده‌های رسمی TrafficStats به‌طور
 * دوره‌ای نمونه‌برداری می‌شوند. هر ردیف ثبت‌شده همان مقدار مشاهده‌شده است؛ در
 * صورت نبود پایش، اختلاف با آمار دستگاه به یک ردیف «تخصیص‌نیافته» می‌رود و هرگز
 * به‌صورت ساختگی به سیم‌کارت پیش‌فرض نسبت داده نمی‌شود.
 *
 * داده‌ها فقط روی خود دستگاه ذخیره می‌شوند. فایل ledger نسخه‌ی قدیمی (۵/۶ ستون)
 * همچنان خوانده می‌شود؛ فرمت جدید علاوه بر اپراتور، subscription/slot و نام سیم
 * را برای ردیف‌های موبایل نگه می‌دارد.
 */
object UsageLedger {

    const val MOBILE = "mobile"
    const val WIFI = "wifi"
    private const val KEEP_DAYS = 40
    private const val FLUSH_INTERVAL_MS = 15_000L
    private const val MIN_SAMPLE_INTERVAL_MS = 500L
    private const val MAX_CONTIGUOUS_SAMPLE_GAP_MS = 15_000L

    private val lock = Any()
    /** کلید هر ردیف شامل روز، اتصال، اپراتور، شناسه‌ی سیم و ساعت است. */
    private val entries = LinkedHashMap<String, LongArray>()
    private val sessions = ArrayList<UsageSession>()
    private var ledgerFile: File? = null
    private var sessionsFile: File? = null
    private var loaded = false
    private var dirty = false
    private var sessionsDirty = false
    private var lastFlush = 0L
    private var lastCounters: NetworkByteCounters? = null
    private var lastSampleAt = 0L
    private var nextSessionId = 1L

    fun dayKey(millis: Long, zone: TimeZone = TimeZone.getDefault()): Int {
        val cal = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 +
            cal.get(Calendar.DAY_OF_MONTH)
    }

    private data class LedgerKey(
        val day: Int,
        val transport: String,
        val operatorId: String,
        val hour: Int,
        val sim: SimIdentity? = null,
    )

    private fun encodeName(name: String): String =
        if (name.isBlank()) "-" else Base64.getUrlEncoder().withoutPadding()
            .encodeToString(name.toByteArray(StandardCharsets.UTF_8))

    private fun decodeName(encoded: String): String = runCatching {
        if (encoded == "-") "" else String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
    }.getOrDefault("")

    private fun encodeKey(key: LedgerKey): String {
        val sim = key.sim
        return listOf(
            key.day.toString(),
            key.transport,
            key.operatorId,
            sim?.subscriptionId?.toString() ?: "-",
            sim?.slotIndex?.toString() ?: "-",
            encodeName(sim?.displayName.orEmpty()),
            key.hour.toString(),
        ).joinToString("\t")
    }

    /** قدیمی: day/type/operator (روزانه) یا day/type/operator/hour (ساعتی); جدید: ۷ فیلد. */
    private fun decodeKey(encoded: String): LedgerKey? {
        val parts = encoded.split('\t')
        val day = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val transport = parts.getOrNull(1) ?: return null
        val operator = parts.getOrNull(2) ?: return null
        return when (parts.size) {
            3 -> LedgerKey(day, transport, operator, -1)
            4 -> LedgerKey(day, transport, operator, parts[3].toIntOrNull() ?: return null)
            in 7..Int.MAX_VALUE -> {
                val subscriptionId = parts[3].toIntOrNull()?.takeIf { it >= 0 }
                val slot = parts[4].toIntOrNull()?.takeIf { it >= 0 }
                val name = decodeName(parts[5])
                val sim = if (subscriptionId == null && slot == null && name.isBlank()) null
                else SimIdentity(subscriptionId, slot, name)
                LedgerKey(day, transport, operator, parts[6].toIntOrNull() ?: return null, sim)
            }
            else -> null
        }
    }

    private fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val filesDir = context.applicationContext.filesDir
        val ledger = File(filesDir, "operator_ledger.tsv")
        val sessionLog = File(filesDir, "mobile_sessions.tsv")
        ledgerFile = ledger
        sessionsFile = sessionLog
        runCatching { parse(ledger.readText()).forEach { (k, v) -> entries[k] = v } }
        runCatching {
            sessions.addAll(parseSessions(sessionLog.readText()))
        }
        // اگر فرایند قبلی ناگهانی متوقف شده باشد، آخرین مرز معلوم حفظ می‌شود و
        // اجرای بعدی به‌اشتباه ادامه‌ی یک جلسه‌ی پیوسته فرض نمی‌شود.
        for (index in sessions.indices) {
            val session = sessions[index]
            if (session.isOpen) {
                sessions[index] = session.copy(
                    endMillis = session.lastSampleMillis,
                    endEstimated = true,
                )
                sessionsDirty = true
            }
        }
        nextSessionId = (sessions.maxOfOrNull { it.id } ?: 0L) + 1L
    }

    /** متن فایل → نقشه. خط‌های خراب نادیده گرفته می‌شوند. */
    fun parse(text: String): Map<String, LongArray> {
        val out = LinkedHashMap<String, LongArray>()
        text.lineSequence().forEach { line ->
            val parts = line.split('\t')
            when (parts.size) {
                // فرمت قدیمی: day, transport, operator, rx, tx
                5 -> {
                    val day = parts[0].toIntOrNull() ?: return@forEach
                    val rx = parts[3].toLongOrNull() ?: return@forEach
                    val tx = parts[4].toLongOrNull() ?: return@forEach
                    out["$day\t${parts[1]}\t${parts[2]}"] = longArrayOf(rx, tx)
                }
                // فرمت قدیمی ساعتی: day, transport, operator, hour, rx, tx
                6 -> {
                    val day = parts[0].toIntOrNull() ?: return@forEach
                    val hour = parts[3].toIntOrNull() ?: return@forEach
                    val rx = parts[4].toLongOrNull() ?: return@forEach
                    val tx = parts[5].toLongOrNull() ?: return@forEach
                    out["$day\t${parts[1]}\t${parts[2]}\t$hour"] = longArrayOf(rx, tx)
                }
                // فرمت جدید: day, transport, operator, subscription, slot, name, hour, rx, tx
                9 -> {
                    val key = parts.take(7).joinToString("\t")
                    if (decodeKey(key) == null) return@forEach
                    val rx = parts[7].toLongOrNull() ?: return@forEach
                    val tx = parts[8].toLongOrNull() ?: return@forEach
                    out[key] = longArrayOf(rx, tx)
                }
            }
        }
        return out
    }

    fun serialize(map: Map<String, LongArray>): String = buildString {
        map.forEach { (key, value) ->
            if (value.size < 2) return@forEach
            val keyParts = key.split('\t')
            if (keyParts.size !in setOf(3, 4, 7)) return@forEach
            append(key).append('\t').append(value[0]).append('\t').append(value[1]).append('\n')
        }
    }

    /** فرمت sessions مستقل است تا نصب‌های قبلی بدون از دست رفتن ledger آپدیت شوند. */
    fun parseSessions(text: String): List<UsageSession> = text.lineSequence().mapNotNull { line ->
        val p = line.split('\t', limit = 13)
        if (p.size != 13) return@mapNotNull null
        val id = p[0].toLongOrNull() ?: return@mapNotNull null
        val start = p[1].toLongOrNull() ?: return@mapNotNull null
        val endRaw = p[2].toLongOrNull() ?: return@mapNotNull null
        val lastSample = p[3].toLongOrNull() ?: return@mapNotNull null
        val rx = p[4].toLongOrNull() ?: return@mapNotNull null
        val tx = p[5].toLongOrNull() ?: return@mapNotNull null
        val operatorId = p[6]
        val subscriptionId = p[7].toIntOrNull()?.takeIf { it >= 0 }
        val slotIndex = p[8].toIntOrNull()?.takeIf { it >= 0 }
        val name = decodeName(p[9])
        val sim = if (subscriptionId == null && slotIndex == null && name.isBlank()) null
        else SimIdentity(subscriptionId, slotIndex, name)
        UsageSession(
            id = id,
            startMillis = start,
            endMillis = endRaw.takeIf { it > 0L },
            lastSampleMillis = lastSample,
            rxBytes = rx,
            txBytes = tx,
            operatorId = operatorId,
            sim = sim,
            startEstimated = p[10].toBooleanStrictOrNull() ?: true,
            endEstimated = p[11].toBooleanStrictOrNull() ?: false,
            samplingGaps = p[12].toIntOrNull()?.coerceAtLeast(0) ?: 0,
        )
    }.toList()

    fun serializeSessions(value: List<UsageSession>): String = buildString {
        value.forEach { session ->
            val sim = session.sim
            append(
                listOf(
                    session.id.toString(),
                    session.startMillis.toString(),
                    (session.endMillis ?: 0L).toString(),
                    session.lastSampleMillis.toString(),
                    session.rxBytes.toString(),
                    session.txBytes.toString(),
                    session.operatorId.replace('\t', ' '),
                    sim?.subscriptionId?.toString() ?: "-1",
                    sim?.slotIndex?.toString() ?: "-1",
                    encodeName(sim?.displayName.orEmpty()),
                    session.startEstimated.toString(),
                    session.endEstimated.toString(),
                    session.samplingGaps.toString(),
                ).joinToString("\t"),
            ).append('\n')
        }
    }

    private fun addTraffic(
        day: Int,
        transport: String,
        operatorId: String,
        hour: Int,
        traffic: Traffic,
        sim: SimIdentity? = null,
    ) {
        if (traffic.rx <= 0L && traffic.tx <= 0L) return
        val key = encodeKey(LedgerKey(day, transport, operatorId, hour, sim))
        val slot = entries.getOrPut(key) { LongArray(2) }
        slot[0] += traffic.rx.coerceAtLeast(0L)
        slot[1] += traffic.tx.coerceAtLeast(0L)
        dirty = true
    }

    private fun recordTraffic(
        now: Long,
        transport: String,
        operatorId: String,
        traffic: Traffic,
        sim: SimIdentity? = null,
    ) {
        if (traffic.rx <= 0L && traffic.tx <= 0L) return
        val day = dayKey(now)
        addTraffic(day, transport, operatorId, -1, traffic, sim)
        val calendar = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = now }
        addTraffic(day, transport, operatorId, calendar.get(Calendar.HOUR_OF_DAY), traffic, sim)
    }

    private fun closeSession(id: Long, atMillis: Long, estimated: Boolean = true) {
        val index = sessions.indexOfLast { it.id == id && it.isOpen }
        if (index < 0) return
        val current = sessions[index]
        sessions[index] = current.copy(
            endMillis = atMillis.coerceAtLeast(current.startMillis),
            endEstimated = estimated,
        )
        sessionsDirty = true
    }

    private fun updateMobileSession(
        now: Long,
        previousSampleAt: Long,
        active: Boolean,
        operatorId: String,
        sim: SimIdentity?,
        delta: Traffic,
    ) {
        val activeSession = sessions.lastOrNull { it.isOpen }
        val today = dayKey(now)
        val crossingDay = activeSession != null && dayKey(activeSession.startMillis) != today
        val sameIdentity = activeSession != null && activeSession.operatorId == operatorId && activeSession.sim == sim

        if (!active || (activeSession != null && (!sameIdentity || crossingDay))) {
            if (activeSession != null) {
                val endAt = if (crossingDay && previousSampleAt > 0L) {
                    previousSampleAt.coerceAtLeast(activeSession.startMillis)
                } else now
                closeSession(activeSession.id, endAt)
            }
        }
        if (!active) return

        val latest = sessions.lastOrNull { it.isOpen }
        val canContinue = latest != null && sameIdentity && !crossingDay
        val gap = previousSampleAt > 0L && now - previousSampleAt > MAX_CONTIGUOUS_SAMPLE_GAP_MS
        if (canContinue) {
            val index = sessions.indexOfLast { it.id == latest!!.id }
            if (index >= 0) {
                val old = sessions[index]
                sessions[index] = old.copy(
                    lastSampleMillis = now,
                    rxBytes = old.rxBytes + delta.rx,
                    txBytes = old.txBytes + delta.tx,
                    samplingGaps = old.samplingGaps + if (gap) 1 else 0,
                )
                sessionsDirty = true
            }
        } else {
            sessions += UsageSession(
                id = nextSessionId++,
                startMillis = now,
                endMillis = null,
                lastSampleMillis = now,
                rxBytes = delta.rx,
                txBytes = delta.tx,
                operatorId = operatorId,
                sim = sim,
                startEstimated = true,
                endEstimated = false,
                samplingGaps = if (gap) 1 else 0,
            )
            sessionsDirty = true
        }
    }

    /**
     * یک نمونه‌برداری از شمارنده‌های سیستمی. بایت موبایل را با اپراتور و سیم
     * پیش‌فرضِ همان لحظه ثبت می‌کند؛ نشست‌ها فقط زمانی باز هستند که مسیر اصلی
     * اینترنت واقعاً سلولار باشد. مرز هر نشست به فاصله‌ی نمونه‌برداری محدود است.
     */
    fun sample(
        context: Context,
        counters: NetworkByteCounters,
        snapshot: NetworkSnapshot,
        now: Long = System.currentTimeMillis(),
    ): UsageSession? = synchronized(lock) {
        ensureLoaded(context)
        if (lastSampleAt > 0L && now - lastSampleAt < MIN_SAMPLE_INTERVAL_MS) {
            return@synchronized sessions.lastOrNull { it.isOpen }
        }

        val previous = lastCounters
        val previousSampleAt = lastSampleAt
        lastCounters = counters
        lastSampleAt = now

        val total = Traffic(
            delta(counters.totalRxBytes, previous?.totalRxBytes ?: -1L),
            delta(counters.totalTxBytes, previous?.totalTxBytes ?: -1L),
        )
        val mobile = if (counters.hasMobileCounters) {
            Traffic(
                delta(counters.mobileRxBytes, previous?.mobileRxBytes ?: -1L),
                delta(counters.mobileTxBytes, previous?.mobileTxBytes ?: -1L),
            )
        } else if (snapshot.type == ConnectionType.CELLULAR) {
            // fallback قدیمی: فقط نوع فعلی اتصال را می‌دانیم، پس ردیف تخمینی است.
            total
        } else Traffic()

        val isWifi = snapshot.type == ConnectionType.WIFI || snapshot.type == ConnectionType.ETHERNET
        val wifi = if (isWifi) {
            Traffic(
                (total.rx - mobile.rx).coerceAtLeast(0L),
                (total.tx - mobile.tx).coerceAtLeast(0L),
            )
        } else Traffic()

        val operatorId = snapshot.simOperator?.id
            ?: snapshot.operator.id.takeIf { snapshot.type == ConnectionType.CELLULAR && it != OperatorCatalog.ID_UNKNOWN }
            ?: OperatorCatalog.ID_UNKNOWN
        val sim = snapshot.simIdentity
        if (mobile.total > 0L) recordTraffic(now, MOBILE, operatorId, mobile, sim)
        if (wifi.total > 0L) {
            val wifiOperator = snapshot.operator.id.takeIf { snapshot.operator.id != OperatorCatalog.ID_UNKNOWN }
                ?: OperatorCatalog.ID_WIFI
            recordTraffic(now, WIFI, wifiOperator, wifi)
        }

        updateMobileSession(
            now = now,
            previousSampleAt = previousSampleAt,
            active = snapshot.type == ConnectionType.CELLULAR,
            operatorId = operatorId,
            sim = sim,
            delta = mobile,
        )

        val shouldFlush = dirty || sessionsDirty
        if (shouldFlush && now - lastFlush >= FLUSH_INTERVAL_MS) flushLocked(now)
        sessions.lastOrNull { it.isOpen }
    }

    private fun delta(current: Long, previous: Long): Long =
        if (current < 0L || previous < 0L || current < previous) 0L else current - previous

    /** نشست فعال را برای نمایش لحظه‌ای برمی‌گرداند. */
    fun activeSession(context: Context): UsageSession? = synchronized(lock) {
        ensureLoaded(context)
        sessions.lastOrNull { it.isOpen }
    }

    /** نشست‌های روز انتخابی، تازه‌ترین اول. */
    fun sessionsForDay(context: Context, dayStartMillis: Long): List<UsageSession> = synchronized(lock) {
        ensureLoaded(context)
        val targetDay = dayKey(dayStartMillis)
        sessions.asSequence()
            .filter { dayKey(it.startMillis) == targetDay }
            .sortedByDescending { it.startMillis }
            .toList()
    }

    private fun flushLocked(now: Long) {
        val ledger = ledgerFile ?: return
        val sessionLog = sessionsFile ?: return
        val cutoff = dayKey(now - KEEP_DAYS * 24L * 60 * 60 * 1000)
        entries.keys.removeAll { key -> (decodeKey(key)?.day ?: Int.MAX_VALUE) < cutoff }
        sessions.removeAll { session -> dayKey(session.startMillis) < cutoff }
        runCatching { writeAtomically(ledger, serialize(entries)) }
        runCatching { writeAtomically(sessionLog, serializeSessions(sessions)) }
        dirty = false
        sessionsDirty = false
        lastFlush = now
    }

    private fun writeAtomically(file: File, contents: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(contents)
        if (!tmp.renameTo(file)) {
            file.writeText(contents)
            tmp.delete()
        }
    }

    /**
     * با آمار کل دستگاه تطبیق می‌دهد، اما ثبت مشاهده‌شده را تغییر نمی‌دهد.
     * اختلافِ اندازه‌گیری به‌صورت «نامشخص/تخصیص‌نیافته» اضافه می‌شود تا حجم گمشده
     * به اپراتور یا سیم‌کارتی که در آن بازه حاضر نبوده، نسبت داده نشود.
     */
    fun reconcile(
        recorded: List<OperatorTraffic>,
        systemMobile: Traffic,
        systemWifi: Traffic,
        defaultMobileId: String,
        defaultWifiId: String = OperatorCatalog.ID_WIFI,
    ): List<OperatorTraffic> {
        @Suppress("UNUSED_VARIABLE") val legacyDefault = defaultMobileId
        val out = ArrayList<OperatorTraffic>()

        fun one(transport: String, system: Traffic, unknownId: String) {
            val rows = recorded.filter { it.transport == transport && it.traffic.total > 0L }
            // یکسان‌سازی فقط ردیف‌هایی که همان اپراتور و همان سیم هستند؛ مقدار هر ردیف دست‌نخورده می‌ماند.
            rows.groupBy { it.operatorId to it.sim }.forEach { (identity, matching) ->
                out += OperatorTraffic(
                    operatorId = identity.first,
                    transport = transport,
                    traffic = matching.fold(Traffic()) { acc, row -> acc + row.traffic },
                    estimated = matching.any { it.estimated },
                    sim = identity.second,
                )
            }
            val trackedRx = rows.sumOf { it.traffic.rx }
            val trackedTx = rows.sumOf { it.traffic.tx }
            val remaining = Traffic(
                max(0L, system.rx - trackedRx),
                max(0L, system.tx - trackedTx),
            )
            if (remaining.total > 0L) {
                val unknownIndex = out.indexOfLast {
                    it.transport == transport && it.operatorId == unknownId && it.sim == null
                }
                if (unknownIndex >= 0) {
                    val previous = out[unknownIndex]
                    out[unknownIndex] = previous.copy(
                        traffic = previous.traffic + remaining,
                        estimated = true,
                    )
                } else {
                    out += OperatorTraffic(
                        operatorId = unknownId,
                        transport = transport,
                        traffic = remaining,
                        estimated = true,
                    )
                }
            }
        }

        one(MOBILE, systemMobile, OperatorCatalog.ID_UNKNOWN)
        one(WIFI, systemWifi, defaultWifiId)
        return out.sortedByDescending { it.traffic.total }
    }

    /** دفترهای یک روز؛ اگر فرمت قدیمی بود، شناسه‌ی سیم‌کارت خالی می‌ماند. */
    fun forDay(context: Context, dayStartMillis: Long): List<OperatorTraffic> = synchronized(lock) {
        ensureLoaded(context)
        val targetDay = dayKey(dayStartMillis)
        entries.entries.asSequence().mapNotNull { (encoded, value) ->
            val key = decodeKey(encoded) ?: return@mapNotNull null
            if (key.day != targetDay || key.hour != -1) return@mapNotNull null
            OperatorTraffic(key.operatorId, key.transport, Traffic(value[0], value[1]), sim = key.sim)
        }.toList()
    }

    /** یادداشت‌های ساعتی یک روز به تفکیک اپراتور، اتصال و سیم‌کارت. */
    fun hourlyForDay(context: Context, dayStartMillis: Long): List<HourOperatorTraffic> = synchronized(lock) {
        ensureLoaded(context)
        val targetDay = dayKey(dayStartMillis)
        entries.entries.asSequence().mapNotNull { (encoded, value) ->
            val key = decodeKey(encoded) ?: return@mapNotNull null
            if (key.day != targetDay || key.hour !in 0..23) return@mapNotNull null
            HourOperatorTraffic(key.hour, key.operatorId, key.transport, Traffic(value[0], value[1]), key.sim)
        }.sortedWith(compareBy<HourOperatorTraffic> { it.hour }.thenBy { it.operatorId }.thenBy { it.sim?.slotIndex ?: -1 })
            .toList()
    }
}
