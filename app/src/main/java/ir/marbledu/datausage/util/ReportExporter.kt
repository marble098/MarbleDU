package ir.marbledu.datausage.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.AppUsage
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
                    app.label.replace(",", " "),
                    app.packageName,
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
        builder.append("# carrier,$operatorName\n")
        builder.append("# generated,${LiveMonitor.formatDate(System.currentTimeMillis())}\n")
        file.writeText(builder.toString())
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
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_report)))
    }
}
