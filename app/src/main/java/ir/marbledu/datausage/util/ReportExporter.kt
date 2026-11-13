package ir.marbledu.datausage.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppUsage
import ir.marbledu.datausage.data.DayReport
import ir.marbledu.datausage.data.DaySummary
import ir.marbledu.datausage.data.JalaliDate
import ir.marbledu.datausage.data.OperatorCatalog
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.LiveMonitor
import ir.marbledu.datausage.data.TimeRange
import ir.marbledu.datausage.ui.AppUsageUi
import ir.marbledu.datausage.ui.DashboardUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** ساخت گزارش CSV و متن خلاصه برای اشتراک‌گذاری. */
object ReportExporter {

    suspend fun exportCsv(
        context: Context,
        range: TimeRange,
        mobile: List<AppUsage>,
        wifi: List<AppUsage>,
        persian: Boolean,
        operatorName: String,
    ): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "MarbleDU-${range.name}-${System.currentTimeMillis()}.csv")
        val header = "app,package,connection,bytes,human,first_seen,last_seen\n"
        val builder = StringBuilder(header)
        fun line(app: AppUsage, type: String) {
            builder.append(
                listOf(
                    csvEscape(app.label),
                    csvEscape(app.packageName),
                    type,
                    app.bytes.toString(),
                    Formats.bytes(app.bytes, false),
                    LiveMonitor.formatDate(app.firstTimestamp) + " " + LiveMonitor.formatHourMinute(app.firstTimestamp),
                    LiveMonitor.formatDate(app.lastTimestamp) + " " + LiveMonitor.formatHourMinute(app.lastTimestamp),
                ).joinToString(","),
            ).append('\n')
        }
        mobile.forEach { line(it, "mobile") }
        wifi.forEach { line(it, "wifi") }
        builder.append("# carrier,${csvEscape(operatorName)}\n")
        builder.append("# generated,${LiveMonitor.formatDate(System.currentTimeMillis())}\n")
        file.writeText(builder.toString())
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * CSV escape استاندارد: اگر فیلد کاما، کوتیشن یا newline داشته باشد،
     * داخل کوتیشن می‌گذاریم و کوتیشن‌های داخلی را دوبرابر می‌کنیم.
     * (نسخه‌ی قبلی فقط کاما را با فاصله عوض می‌کرد و داده را خراب می‌کرد.)
     */
    private fun csvEscape(value: String): String {
        val needsQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuote) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    /** CSV خلاصه‌ی روزانه‌ی ۳۰ روز: تاریخ شمسی و میلادی، دانلود/آپلود موبایل و وای‌فای. */
    suspend fun exportDailyCsv(context: Context, days: List<DaySummary>): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "MarbleDU-30days-${System.currentTimeMillis()}.csv")
        val builder = StringBuilder(
            "date_jalali,date_gregorian,weekday,mobile_download,mobile_upload,wifi_download,wifi_upload," +
                "total_download,total_upload,total_bytes,total_human\n",
        )
        days.forEach { d ->
            builder.append(
                listOf(
                    JalaliDate.fromMillis(d.startMillis).numeric(false),
                    JalaliDate.gregorianNumeric(d.startMillis, false),
                    JalaliDate.weekday(d.startMillis, false),
                    d.mobile.rx, d.mobile.tx, d.wifi.rx, d.wifi.tx,
                    d.all.rx, d.all.tx, d.total,
                    Formats.bytes(d.total, false),
                ).joinToString(","),
            ).append('\n')
        }
        file.writeText(builder.toString())
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** CSV گزارش کامل یک روز: اپراتورها، ۲۴ ساعت و اپ‌ها. */
    suspend fun exportDayCsv(context: Context, report: DayReport): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val start = report.summary.startMillis
        val jalali = JalaliDate.fromMillis(start).numeric(false)
        val file = File(dir, "MarbleDU-day-${jalali.replace('/', '-')}.csv")
        val b = StringBuilder()
        b.append("# date_jalali,").append(jalali).append('\n')
        b.append("# date_gregorian,").append(JalaliDate.gregorianNumeric(start, false)).append('\n')
        b.append("section,name,connection,download,upload,total\n")
        b.append("day,total,all,${report.summary.all.rx},${report.summary.all.tx},${report.summary.total}\n")
        b.append("day,total,mobile,${report.summary.mobile.rx},${report.summary.mobile.tx},${report.summary.mobile.total}\n")
        b.append("day,total,wifi,${report.summary.wifi.rx},${report.summary.wifi.tx},${report.summary.wifi.total}\n")
        b.append("audit,Android system total,mobile,${report.summary.mobile.rx},${report.summary.mobile.tx},${report.summary.mobile.total}\n")
        b.append("audit,TrafficStats observed,mobile,${report.observedMobile.rx},${report.observedMobile.tx},${report.observedMobile.total}\n")
        val notObservedRx = (report.summary.mobile.rx - report.observedMobile.rx).coerceAtLeast(0L)
        val notObservedTx = (report.summary.mobile.tx - report.observedMobile.tx).coerceAtLeast(0L)
        val observedOverRx = (report.observedMobile.rx - report.summary.mobile.rx).coerceAtLeast(0L)
        val observedOverTx = (report.observedMobile.tx - report.summary.mobile.tx).coerceAtLeast(0L)
        b.append("audit,Android not observed,mobile,$notObservedRx,$notObservedTx,${notObservedRx + notObservedTx}\n")
        b.append("audit,Monitor above Android,mobile,$observedOverRx,$observedOverTx,${observedOverRx + observedOverTx}\n")
        report.operators.forEach { o ->
            val simLabel = o.sim?.let { sim ->
                val slot = sim.slotIndex?.let { "SIM ${it + 1}" }
                listOfNotNull(slot, sim.displayName.takeIf { it.isNotBlank() }).joinToString(" · ")
            }.orEmpty()
            val name = listOfNotNull(
                OperatorCatalog.byId(o.operatorId).en.takeIf { o.operatorId != OperatorCatalog.ID_UNKNOWN },
                simLabel.takeIf { it.isNotBlank() },
                if (o.estimated) "unassigned / estimate" else null,
            ).joinToString(" · ").ifBlank { "unassigned / unknown" }
            b.append("operator,${csvEscape(name)},${o.transport},${o.traffic.rx},${o.traffic.tx},${o.traffic.total}\n")
        }
        report.operatorHours.forEach { hour ->
            val hourLabel = "%02d:00".format(java.util.Locale.US, hour.hour)
            val simLabel = if (hour.transport == "mobile") {
                hour.sim?.let { sim ->
                    val slot = sim.slotIndex?.let { "SIM ${it + 1}" }
                    listOfNotNull(slot, sim.displayName.takeIf { it.isNotBlank() })
                        .joinToString(" · ")
                        .ifBlank { "SIM not identified" }
                } ?: "SIM not identified"
            } else ""
            val name = listOfNotNull(
                hourLabel,
                OperatorCatalog.byId(hour.operatorId).en,
                simLabel.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            b.append(
                "operator_hour,${csvEscape(name)},${hour.transport},${hour.traffic.rx},${hour.traffic.tx},${hour.traffic.total}\n",
            )
        }
        report.hours.forEach { h ->
            val name = "%02d:00".format(java.util.Locale.US, h.hour)
            b.append("hour,$name,mobile,${h.mobile.rx},${h.mobile.tx},${h.mobile.total}\n")
            b.append("hour,$name,wifi,${h.wifi.rx},${h.wifi.tx},${h.wifi.total}\n")
        }
        report.apps.forEach { a ->
            val name = csvEscape(a.label + " [" + a.packageName + "]")
            if (a.mobile.total > 0) b.append("app,$name,mobile,${a.mobile.rx},${a.mobile.tx},${a.mobile.total}\n")
            if (a.wifi.total > 0) b.append("app,$name,wifi,${a.wifi.rx},${a.wifi.tx},${a.wifi.total}\n")
        }
        b.append("session_start,session_end,sim_slot,sim_name,operator,download,upload,total,start_estimated,end_estimated,sampling_gaps\n")
        report.sessions.forEach { session ->
            val sim = session.sim
            val start = LiveMonitor.formatDate(session.startMillis) + " " + LiveMonitor.formatHourMinute(session.startMillis)
            val end = session.endMillis?.let {
                LiveMonitor.formatDate(it) + " " + LiveMonitor.formatHourMinute(it)
            } ?: "open"
            val slot = sim?.slotIndex?.let { "SIM ${it + 1}" }.orEmpty()
            val operator = OperatorCatalog.byId(session.operatorId).en
            b.append(
                listOf(
                    start,
                    end,
                    csvEscape(slot),
                    csvEscape(sim?.displayName.orEmpty()),
                    csvEscape(operator),
                    session.rxBytes,
                    session.txBytes,
                    session.totalBytes,
                    session.startEstimated,
                    session.endEstimated,
                    session.samplingGaps,
                ).joinToString(","),
            ).append('\n')
        }
        file.writeText(b.toString())
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareIntent(context: Context, uri: Uri): Intent {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_csv_title))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return intent
    }

    fun shareSummary(
        context: Context,
        usage: AppUsageUi,
        dashboard: DashboardUi,
        persian: Boolean,
    ) {
        val mobile = usage.mobile.sumOf { it.bytes }
        val wifi = usage.wifi.sumOf { it.bytes }
        val body = buildString {
            appendLine(context.getString(R.string.app_name) + " – " + context.getString(R.string.share_csv_title))
            appendLine("• " + context.getString(R.string.cellular) + ": " + Formats.bytes(mobile, persian))
            appendLine("• " + context.getString(R.string.wifi) + ": " + Formats.bytes(wifi, persian))
            if (dashboard.averagePingMs > 0) {
                appendLine("• " + context.getString(R.string.average_ping) + ": " +
                    Formats.number(dashboard.averagePingMs, persian, 0) + " " + context.getString(R.string.ms))
            }
            appendLine()
            usage.mobile.take(10).forEach { app ->
                val label = OperatorCatalog.specialUidLabel(app.uid, persian) ?: app.label
                appendLine("• $label: ${Formats.bytes(app.bytes, persian)}")
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
            // اگر از context غیر-Activity صدا زده شود، بدون این فلگ کرش می‌کند.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_report)))
    }
}
