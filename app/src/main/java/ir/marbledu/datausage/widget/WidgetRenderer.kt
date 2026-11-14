package ir.marbledu.datausage.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import ir.marbledu.datausage.MainActivity
import ir.marbledu.datausage.R
import ir.marbledu.datausage.data.Formats
import ir.marbledu.datausage.data.LocaleHelper

/**
 * ساخت و رسم ظاهر ویجت.
 *
 * هر ویجت جداگانه و با اندازه‌ی واقعی خودش رسم می‌شود؛ یعنی اگر کاربر ویجت را
 * بکشد و بزرگ کند، بخش‌های بیشتری (سقف بسته، میان‌برها، ریزجزئیات) ظاهر
 * می‌شوند و در حالت کوچک فقط سربرگ و عدد بزرگ می‌ماند. هیچ متنی بریده نمی‌شود
 * و ظاهر روشن/تاریک/AMOLED هم از یک‌جا ([WidgetTheme]) رنگ می‌گیرد.
 */
internal object WidgetRenderer {

    private const val REQUEST_OPEN = 40
    private const val REQUEST_REFRESH = 41
    private const val REQUEST_TOGGLE = 42
    private const val REQUEST_REPORTS = 43

    private const val DEFAULT_WIDTH_DP = 250
    private const val DEFAULT_HEIGHT_DP = 150

    /** رسم همه‌ی ویجت‌های روی صفحه‌ی اصلی با یک تصویر از داده. */
    fun renderAll(context: Context, snapshot: WidgetSnapshot) {
        val manager = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return
        val ids = runCatching {
            manager.getAppWidgetIds(ComponentName(context, UsageWidgetProvider::class.java))
        }.getOrNull()
        if (ids.isNullOrEmpty()) return
        ids.forEach { id ->
            runCatching { manager.updateAppWidget(id, build(context, snapshot, manager, id)) }
        }
    }

    /** ساخت RemoteViews یک ویجت مشخص. */
    fun build(
        context: Context,
        snapshot: WidgetSnapshot,
        manager: AppWidgetManager?,
        appWidgetId: Int,
    ): RemoteViews {
        val (widthDp, heightDp) = measure(manager, appWidgetId)
        val visibility = WidgetLayout.visibility(snapshot, widthDp, heightDp)
        val colors = WidgetTheme.colors(
            dark = snapshot.darkSurface,
            amoled = snapshot.amoled,
            primary = snapshot.accentPrimary,
            secondary = snapshot.accentSecondary,
        )
        val strings = LocaleHelper.localizedContext(context, if (snapshot.persian) "fa" else "en")
        val persian = snapshot.persian
        val views = RemoteViews(context.packageName, R.layout.widget_usage)

        // ---- جهت چیدمان: فارسی راست‌به‌چپ، انگلیسی چپ‌به‌راست (مستقل از زبان گوشی)
        views.setInt(
            R.id.widget_root,
            "setLayoutDirection",
            if (persian) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR,
        )
        views.setInt(R.id.widget_surface, "setBackgroundResource", surfaceResource(snapshot))
        val horizontal = if (visibility.compact) 12 else 14
        val vertical = if (visibility.compact) 10 else 12
        views.setViewPadding(
            R.id.widget_surface,
            dp(context, horizontal),
            dp(context, vertical),
            dp(context, horizontal),
            dp(context, vertical),
        )

        // ---- سربرگ: نشان برنامه، عنوان، اپراتور، نشان وضعیت و دکمه‌ی تازه‌سازی
        views.setInt(R.id.widget_badge, "setColorFilter", colors.accent)
        backgroundTint(views, R.id.widget_badge, colors.badgeBg)
        views.setTextViewText(R.id.widget_title, strings.getString(R.string.widget_today))
        views.setTextColor(R.id.widget_title, colors.body)
        views.setTextViewTextSize(
            R.id.widget_title,
            TypedValue.COMPLEX_UNIT_SP,
            if (visibility.compact) 11f else 12f,
        )

        views.setViewVisibility(R.id.widget_operator, if (visibility.operator) View.VISIBLE else View.GONE)
        if (visibility.operator) {
            views.setTextViewText(R.id.widget_operator, snapshot.operatorName)
            views.setTextColor(R.id.widget_operator, colors.muted)
        }

        views.setViewVisibility(R.id.widget_status, if (visibility.statusChip) View.VISIBLE else View.GONE)
        if (visibility.statusChip) {
            views.setTextViewText(
                R.id.widget_status,
                strings.getString(
                    if (snapshot.monitoring) R.string.widget_status_on else R.string.widget_status_off,
                ),
            )
            backgroundTint(views, R.id.widget_status, colors.chipBg)
            views.setTextColor(
                R.id.widget_status,
                if (snapshot.monitoring) colors.accent else colors.muted,
            )
        }
        views.setInt(R.id.widget_refresh, "setColorFilter", colors.muted)
        views.setContentDescription(R.id.widget_refresh, strings.getString(R.string.widget_refresh_desc))

        // ---- عدد بزرگ مصرف امروز + درصد بسته
        val amount = WidgetFormat.amount(snapshot.totalBytes, persian, snapshot.hasNumbers)
        views.setTextViewText(R.id.widget_total, amount.value)
        views.setTextViewText(R.id.widget_unit, amount.unit)
        views.setTextColor(R.id.widget_total, colors.hero)
        views.setTextColor(R.id.widget_unit, colors.muted)
        views.setTextViewTextSize(
            R.id.widget_total,
            TypedValue.COMPLEX_UNIT_SP,
            visibility.heroTextSizeSp,
        )

        val percent = WidgetFormat.percent(snapshot.capUsedBytes, snapshot.capTotalBytes)
        val capAccent = WidgetTheme.capColor(WidgetFormat.capState(percent), colors)
        views.setViewVisibility(R.id.widget_percent, if (visibility.capPercent) View.VISIBLE else View.GONE)
        if (visibility.capPercent) {
            views.setTextViewText(R.id.widget_percent, WidgetFormat.percentText(percent, persian))
            backgroundTint(views, R.id.widget_percent, colors.chipBg)
            views.setTextColor(R.id.widget_percent, capAccent)
        }

        // ---- توضیح کوتاه: راهنمای دسترسی، وضعیت پایش یا ساعت به‌روزرسانی
        val caption = captionText(strings, snapshot, visibility, persian)
        views.setViewVisibility(R.id.widget_caption, if (caption.isEmpty()) View.GONE else View.VISIBLE)
        if (caption.isNotEmpty()) {
            views.setTextViewText(R.id.widget_caption, caption)
            views.setTextColor(R.id.widget_caption, colors.muted)
        }

        // ---- کارت‌های موبایل و وای‌فای
        views.setViewVisibility(R.id.widget_split_row, if (visibility.splitRow) View.VISIBLE else View.GONE)
        if (visibility.splitRow) {
            backgroundTint(views, R.id.widget_mobile_card, colors.cardBg)
            backgroundTint(views, R.id.widget_wifi_card, colors.cardBg)
            backgroundTint(views, R.id.widget_mobile_dot, colors.mobile)
            backgroundTint(views, R.id.widget_wifi_dot, colors.wifi)
            views.setTextViewText(R.id.widget_mobile_label, strings.getString(R.string.widget_mobile))
            views.setTextColor(R.id.widget_mobile_label, colors.muted)
            views.setTextViewText(R.id.widget_mobile, numbers(snapshot, snapshot.mobileBytes, persian))
            views.setTextColor(R.id.widget_mobile, colors.mobile)
            views.setTextViewText(R.id.widget_wifi_label, strings.getString(R.string.wifi))
            views.setTextColor(R.id.widget_wifi_label, colors.muted)
            views.setTextViewText(R.id.widget_wifi, numbers(snapshot, snapshot.wifiBytes, persian))
            views.setTextColor(R.id.widget_wifi, colors.wifi)
        }

        // ---- نوار سقف بسته
        views.setViewVisibility(R.id.widget_cap_row, if (visibility.capRow) View.VISIBLE else View.GONE)
        if (visibility.capRow) {
            views.setTextViewText(R.id.widget_cap_label, strings.getString(R.string.widget_remaining))
            views.setTextColor(R.id.widget_cap_label, colors.muted)
            views.setTextViewText(R.id.widget_cap_value, Formats.bytes(snapshot.capRemainingBytes, persian))
            views.setTextColor(R.id.widget_cap_value, capAccent)
            views.setProgressBar(R.id.widget_cap_progress, 100, percent, false)
            views.setColorStateList(
                R.id.widget_cap_progress,
                "setProgressTintList",
                ColorStateList.valueOf(capAccent),
            )
            views.setColorStateList(
                R.id.widget_cap_progress,
                "setProgressBackgroundTintList",
                ColorStateList.valueOf(colors.track),
            )
            views.setTextViewText(
                R.id.widget_cap_used,
                strings.getString(
                    R.string.widget_cap_of,
                    Formats.bytes(snapshot.capUsedBytes, persian),
                    Formats.bytes(snapshot.capTotalBytes, persian),
                ),
            )
            views.setTextColor(R.id.widget_cap_used, colors.muted)
        }

        // ---- ردیف دانلود/آپلود (حالت کامل)
        views.setViewVisibility(R.id.widget_detail_row, if (visibility.detailRow) View.VISIBLE else View.GONE)
        if (visibility.detailRow) {
            views.setTextViewText(
                R.id.widget_rx,
                strings.getString(R.string.download) + "  " + numbers(snapshot, snapshot.rxBytes, persian),
            )
            views.setTextViewText(
                R.id.widget_tx,
                strings.getString(R.string.upload) + "  " + numbers(snapshot, snapshot.txBytes, persian),
            )
            views.setTextColor(R.id.widget_rx, colors.muted)
            views.setTextColor(R.id.widget_tx, colors.muted)
        }

        // ---- میان‌برها: توقف/ادامه‌ی پایش و گزارش‌ها
        views.setViewVisibility(R.id.widget_actions_row, if (visibility.actionsRow) View.VISIBLE else View.GONE)
        if (visibility.actionsRow) {
            backgroundTint(views, R.id.widget_action_toggle, colors.actionBg)
            backgroundTint(views, R.id.widget_action_reports, colors.actionBg)
            views.setTextViewText(
                R.id.widget_action_toggle_text,
                strings.getString(
                    if (snapshot.monitoring) R.string.widget_pause else R.string.widget_resume,
                ),
            )
            views.setTextColor(R.id.widget_action_toggle_text, colors.actionText)
            views.setImageViewResource(
                R.id.widget_action_toggle_icon,
                if (snapshot.monitoring) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )
            views.setInt(R.id.widget_action_toggle_icon, "setColorFilter", colors.accent)
            views.setTextViewText(R.id.widget_action_reports_text, strings.getString(R.string.widget_reports))
            views.setTextColor(R.id.widget_action_reports_text, colors.actionText)
            views.setInt(R.id.widget_action_reports_icon, "setColorFilter", colors.accent)
            views.setOnClickPendingIntent(R.id.widget_action_toggle, toggleIntent(context))
            views.setOnClickPendingIntent(R.id.widget_action_reports, reportsIntent(context))
        }

        // ---- ضربه روی بقیه‌ی ویجت: باز کردن برنامه؛ ضربه روی آیکون: تازه‌سازی
        views.setOnClickPendingIntent(R.id.widget_root, openIntent(context))
        views.setOnClickPendingIntent(R.id.widget_refresh, refreshIntent(context))
        return views
    }

    private fun numbers(snapshot: WidgetSnapshot, bytes: Long, persian: Boolean): String =
        if (snapshot.hasNumbers) Formats.bytes(bytes, persian) else WidgetFormat.PLACEHOLDER

    private fun captionText(
        strings: Context,
        snapshot: WidgetSnapshot,
        visibility: WidgetVisibility,
        persian: Boolean,
    ): String = when {
        !snapshot.hasUsageAccess -> strings.getString(R.string.widget_need_access)
        !snapshot.monitoring -> strings.getString(R.string.widget_paused_hint)
        else -> {
            val clock = WidgetFormat.clockText(snapshot.updatedAtMillis, persian)
            val roomy = visibility.capRow || visibility.actionsRow || visibility.detailRow
            if (clock.isNotEmpty() && roomy) strings.getString(R.string.widget_updated_at, clock) else ""
        }
    }

    private fun surfaceResource(snapshot: WidgetSnapshot): Int = when {
        !snapshot.darkSurface -> R.drawable.widget_surface_light
        snapshot.amoled -> R.drawable.widget_surface_amoled
        else -> R.drawable.widget_surface_dark
    }

    private fun backgroundTint(views: RemoteViews, viewId: Int, color: Int) {
        views.setColorStateList(viewId, "setBackgroundTintList", ColorStateList.valueOf(color))
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    /**
     * اندازه‌ی واقعی ویجت به dp. عرض از «کمینه‌ی عرض» و ارتفاع از «بیشینه‌ی
     * ارتفاع» گرفته می‌شود که برای گوشی در حالت عمودی درست‌ترین تخمین است؛
     * اگر لانچر عددی نداد، اندازه‌ی پیش‌فرض ویجت (۴×۲) استفاده می‌شود.
     */
    private fun measure(manager: AppWidgetManager?, appWidgetId: Int): Pair<Int, Int> {
        val options = runCatching { manager?.getAppWidgetOptions(appWidgetId) }.getOrNull()
        val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
        val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
        val maxWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0) ?: 0
        val maxHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0
        val width = when {
            minWidth > 0 -> minWidth
            maxWidth > 0 -> maxWidth
            else -> DEFAULT_WIDTH_DP
        }
        val height = when {
            maxHeight > 0 -> maxHeight
            minHeight > 0 -> minHeight
            else -> DEFAULT_HEIGHT_DP
        }
        return width to height
    }

    private fun openIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_REFRESH,
        Intent(context, UsageWidgetProvider::class.java).setAction(UsageWidgetProvider.ACTION_REFRESH),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun toggleIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_TOGGLE,
        Intent(context, UsageWidgetProvider::class.java)
            .setAction(UsageWidgetProvider.ACTION_TOGGLE_MONITOR),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun reportsIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_REPORTS,
        Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_START_TAB, MainActivity.TAB_REPORTS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
