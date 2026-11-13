package ir.marbledu.datausage.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** باز کردن صفحه‌ی «دسترسی به داده‌های مصرف» در تنظیمات سیستم. */
object UsageAccess {

    /**
     * تلاش می‌کند اول صفحه‌ی مخصوص همین اپ را باز کند (اندروید ۱۰+) و اگر
     * دستگاه پشتیبانی نکرد، به فهرست کلی دسترسی‌ها برمی‌گردد. قبلاً Intent
     * با uri برابر null ساخته می‌شد که روی بعضی ROM ها صفحه‌ی اشتباهی باز می‌کرد.
     */
    fun openSettings(context: Context) {
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(
                    Intent(
                        Settings.ACTION_USAGE_ACCESS_SETTINGS,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            }
            add(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            add(Intent(Settings.ACTION_SETTINGS))
        }
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
    }

    fun openNotificationSettings(context: Context) {
        val candidates = buildList {
            add(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
            add(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:${context.packageName}")),
            )
        }
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
    }

    /** آیا اجازه‌ی «نمایش روی برنامه‌های دیگر» داده شده است؟ */
    fun canDrawOverlays(context: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
    }.getOrDefault(true)

    /** صفحه‌ی اجازه‌ی «نمایش روی برنامه‌های دیگر» (برای حباب شناور سرعت). */
    fun openOverlaySettings(context: Context) {
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                add(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
                add(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }
            add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}")))
        }
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
    }

    /** باز کردن صفحه‌ی جزئیات اپ در تنظیمات سیستم (مجوزها و اعلان‌ها از این‌جا قابل تغییرند). */
    fun openAppDetails(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
