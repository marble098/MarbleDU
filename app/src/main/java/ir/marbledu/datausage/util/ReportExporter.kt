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
        report.operators.forEach { o ->
            val name = OperatorCatalog.byId(o.operatorId).en + if (o.estimated) " (unattributed)" else ""
            b.append("operator,${csvEscape(name)},${o.transport},${o.traffic.rx},${o.traffic.tx},${o.traffic.total}\n")
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
                appendLine("• ${app.label}: ${Formats.bytes(app.bytes, persian)}")
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
