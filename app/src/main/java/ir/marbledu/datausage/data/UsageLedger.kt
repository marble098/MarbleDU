package ir.marbledu.datausage.data

import android.content.Context
import java.io.File
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.max

/** حجم دریافت/ارسال. */
data class Traffic(val rx: Long = 0L, val tx: Long = 0L) {
    val total: Long get() = rx + tx
    operator fun plus(other: Traffic): Traffic = Traffic(rx + other.rx, tx + other.tx)
}

/** مصرف یک اپراتور روی یک نوع اتصال در یک روز. */
data class OperatorTraffic(
    val operatorId: String,
    /** [UsageLedger.MOBILE] یا [UsageLedger.WIFI] */
    val transport: String,
    val traffic: Traffic,
    /** true یعنی این ردیف از روی ثبت دقیق نیست و تخمینی/باقی‌مانده است. */
    val estimated: Boolean = false,
)

/** مصرف یک اپراتور در یک ساعت خاص از یک روز (۰ تا ۲۳). */
data class HourOperatorTraffic(
    val hour: Int,
    val operatorId: String,
    val transport: String,
    val traffic: Traffic,
) {
    val total: Long get() = traffic.total
}

/**
 * دفتر مصرف به تفکیک اپراتور.
 *
 * اندروید تاریخچه‌ی مصرف را فقط به تفکیک «موبایل / وای‌فای» نگه می‌دارد، نه به
 * تفکیک اپراتور. برای این‌که بگوییم «دیروز روی وای‌فای چقدر از ایرانسل و چقدر
 * از مخابرات مصرف شد» باید خودمان همان لحظه یادداشت کنیم که اینترنت مال کدام
 * اپراتور بود. این کلاس همان یادداشت‌هاست: یک فایل متنی ساده روی خود گوشی.
 *
 * فرمت هر خط فایل:
 *   روز \t نوع اتصال \t اپراتور \t ساعت(-1=جمع روزانه) \r \t tx
 */
object UsageLedger {

    const val MOBILE = "mobile"
    const val WIFI = "wifi"
    private const val KEEP_DAYS = 40
    private const val FLUSH_INTERVAL_MS = 20_000L

    private val lock = Any()
    /** روزانه (ساعت=-1) و ساعتی، کلید: روز\tنوع\tاپراتور\sاعت. */
    private val entries = HashMap<String, LongArray>()
    private var file: File? = null
    private var loaded = false
    private var dirty = false
    private var lastFlush = 0L
    private var lastRx = -1L
    private var lastTx = -1L

    fun dayKey(millis: Long, zone: TimeZone = TimeZone.getDefault()): Int {
        val cal = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 +
            cal.get(Calendar.DAY_OF_MONTH)
    }

    private fun key(day: Int, transport: String, operatorId: String, hour: Int) =
        if (hour == -1) "$day\t$transport\t$operatorId" else "$day\t$transport\t$operatorId\t$hour"

    private fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val f = File(context.applicationContext.filesDir, "operator_ledger.tsv")
        file = f
        runCatching { parse(f.readText()).forEach { (k, v) -> entries[k] = v } }
    }

    /** متن فایل → نقشه. خط‌های خراب نادیده گرفته می‌شوند. فرمت ۵ قسمتی (قدیم) و ۶ قسمتی جدید. */
    fun parse(text: String): Map<String, LongArray> {
        val out = HashMap<String, LongArray>()
        text.lineSequence().forEach { line ->
            val parts = line.split('\t')
            if (parts.size < 5) return@forEach
            val day = parts[0].toIntOrNull() ?: return@forEach
            val transport = parts[1]
            val operatorId = parts[2]
            val hour = if (parts.size >= 6) parts[3].toIntOrNull() else -1
            if (hour == null) return@forEach
            val rx = parts[parts.size - 2].toLongOrNull() ?: return@forEach
            val tx = parts[parts.size - 1].toLongOrNull() ?: return@forEach
            out[key(day, transport, operatorId, hour)] = longArrayOf(rx, tx)
        }
        return out
    }

    fun serialize(map: Map<String, LongArray>): String = buildString {
        map.forEach { (k, v) ->
            val parts = k.split('\t')
            val hour = parts.getOrNull(3)?.toIntOrNull()
            // divisão: ان_entries روزانه (ساعت=-۱) بدون ساعت ذخیره می‌شوند تا با فرمت قدیمی سازگار باشند
            val keyOut = if (hour == -1) parts.take(3).joinToString("\t") else k
            append(keyOut).append('\t').append(v[0]).append('\t').append(v[1]).append('\n')
        }
    }

    /**
     * یک نمونه‌برداری: اختلاف شمارنده‌ها با نمونه‌ی قبلی به اپراتور فعلی نسبت
     * داده می‌شود. هر ثبت هم در شمارنده‌ی روزانه و هم در شمارنده‌ی ساعتی آن روز
     * ثبت می‌شود تا گزارش «۲۴ ساعت» دقیق باشد.
     */
    fun sample(context: Context, rx: Long, tx: Long, snapshot: NetworkSnapshot, now: Long = System.currentTimeMillis()) {
        synchronized(lock) {
            ensureLoaded(context)
            val previousRx = lastRx
            val previousTx = lastTx
            lastRx = rx
            lastTx = tx
            if (previousRx < 0 || previousTx < 0) return
            val dRx = max(0L, rx - previousRx)
            val dTx = max(0L, tx - previousTx)
            if (dRx + dTx > 0) {
                val transport = when (snapshot.type) {
                    ConnectionType.CELLULAR -> MOBILE
                    ConnectionType.WIFI, ConnectionType.ETHERNET -> WIFI
                    else -> null
                }
                if (transport != null) {
                    val day = dayKey(now)
                    val operatorId = snapshot.operator.id
                    val daily = key(day, transport, operatorId, -1)
                    val slot = entries.getOrPut(daily) { LongArray(2) }
                    slot[0] += dRx
                    slot[1] += dTx

                    val cal = Calendar.getInstance(TimeZone.getDefault())
                    cal.timeInMillis = now
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val hourlyKey = key(day, transport, operatorId, hour)
                    val hSlot = entries.getOrPut(hourlyKey) { LongArray(2) }
                    hSlot[0] += dRx
                    hSlot[1] += dTx
                    dirty = true
                }
            }
            if (dirty && now - lastFlush >= FLUSH_INTERVAL_MS) flushLocked(now)
        }
    }

    private fun flushLocked(now: Long) {
        val f = file ?: return
        val cutoff = dayKey(now - KEEP_DAYS * 24L * 60 * 60 * 1000)
        entries.keys.filter {
            val parts = it.split('\t')
            val dayNum = parts.firstOrNull()?.toIntOrNull() ?: Int.MAX_VALUE
            dayNum < cutoff
        }.forEach { entries.remove(it) }
        runCatching {
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeText(serialize(entries))
            if (!tmp.renameTo(f)) {
                f.writeText(serialize(entries))
                tmp.delete()
            }
        }
        dirty = false
        lastFlush = now
    }

    /**
     * یادداشت‌های یک روز (جمع روزانه، ساعت=-۱).
     *
     * کلید روزانه ۳ قسمتی است (`روز\ttype\toperator`) و کلید ساعتی ۴ قسمتی
     * (`روز\ttype\toperator\thour`). قبلاً شرط `getOrNull(3) == "-1"` برای کلیدهای
     * ۳قسمتی `null == "-1"` باز می‌گرفت (false)، یعنی همه‌ی ردیف‌های روزانه حذف
     * می‌شدند و گزارش اپراتورهای روز خالی می‌ماند. اکنون بر اساس تعداد Sections
     * فیلتر می‌شود.
     */
    fun forDay(context: Context, dayStartMillis: Long): List<OperatorTraffic> = synchronized(lock) {
        ensureLoaded(context)
        val prefix = "${dayKey(dayStartMillis)}\t"
        entries.entries.asSequence().filter { (k, _) ->
            // کلید روزانه ۳ قسمتی (۲ تاب)، ساعتی ۴ قسمتی (۳ تاب). collapse کردن split تکراری در فیلتر.
            k.startsWith(prefix) && k.count { it == '\t' } == 2
        }.map { (k, v) ->
            val parts = k.split('\t')
            OperatorTraffic(parts[2], parts[1], Traffic(v[0], v[1]))
        }.toList()
    }

    /** یادداشت‌های ساعتی یک روز به تفکیک اپراتور و نوع اتصال. */
    fun hourlyForDay(context: Context, dayStartMillis: Long): List<HourOperatorTraffic> = synchronized(lock) {
        ensureLoaded(context)
        val prefix = "${dayKey(dayStartMillis)}\t"
        entries.entries.asSequence().filter { (k, _) ->
            // فقط ردیف‌های ساعتی (۴ قسمت = ۳ تاب و قسمت چهارم معادل "-۱" نباشد)
            k.startsWith(prefix) && k.count { it == '\t' } == 3
        }.map { (k, v) ->
            val parts = k.split('\t')
            HourOperatorTraffic(parts[3].toIntOrNull() ?: -1, parts[2], parts[1], Traffic(v[0], v[1]))
        }.sortedBy { it.hour }.toList()
    }

    /**
     * یادداشت‌های خودمان را با عدد واقعی سیستم تطبیق می‌دهد.
     *
     *  - اگر یادداشت‌ها از عدد سیستم بیشتر شد (مثلاً VPN ترافیک را دوبار
     *    شمرده)، به نسبت کوچک می‌شوند تا جمعشان دقیقاً عدد سیستم شود.
     *  - اگر کمتر شد (اپ مدتی بسته بوده)، باقی‌مانده به اپراتور پیش‌فرض همان
     *    اتصال می‌رسد و «تخمینی» علامت می‌خورد.
     */
    fun reconcile(
        recorded: List<OperatorTraffic>,
        systemMobile: Traffic,
        systemWifi: Traffic,
        defaultMobileId: String,
        defaultWifiId: String = OperatorCatalog.ID_WIFI,
    ): List<OperatorTraffic> {
        val out = ArrayList<OperatorTraffic>()
        fun one(transport: String, system: Traffic, defaultId: String) {
            val rows = recorded.filter { it.transport == transport && it.traffic.total > 0 }
            val sumRx = rows.sumOf { it.traffic.rx }
            val sumTx = rows.sumOf { it.traffic.tx }
            val scaleRx = if (sumRx > system.rx && sumRx > 0) system.rx.toDouble() / sumRx else 1.0
            val scaleTx = if (sumTx > system.tx && sumTx > 0) system.tx.toDouble() / sumTx else 1.0
            val merged = LinkedHashMap<String, Traffic>()
            rows.forEach { row ->
                val scaled = Traffic((row.traffic.rx * scaleRx).toLong(), (row.traffic.tx * scaleTx).toLong())
                merged[row.operatorId] = (merged[row.operatorId] ?: Traffic()) + scaled
            }
            val usedRx = merged.values.sumOf { it.rx }
            val usedTx = merged.values.sumOf { it.tx }
            val rest = Traffic(max(0L, system.rx - usedRx), max(0L, system.tx - usedTx))
            var restAdded = false
            merged.forEach { (id, traffic) ->
                if (id == defaultId) {
                    // باقی‌مانده با ردیفِ اپراتور پیش‌فرضِ همین اتصال جمع می‌شود.
                    out.add(OperatorTraffic(id, transport, traffic + rest, estimated = transport == WIFI))
                    restAdded = true
                } else if (traffic.total > 0) {
                    out.add(OperatorTraffic(id, transport, traffic, estimated = false))
                }
            }
            if (!restAdded && rest.total > 0) {
                out.add(
                    OperatorTraffic(
                        defaultId, transport, rest,
                        estimated = transport == WIFI || defaultId == OperatorCatalog.ID_MOBILE,
                    ),
                )
            }
        }
        one(MOBILE, systemMobile, defaultMobileId)
        one(WIFI, systemWifi, defaultWifiId)
        return out.sortedByDescending { it.traffic.total }
    }
}
