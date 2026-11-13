package ir.marbledu.datausage.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/** باز کردن صفحه‌ی «دسترسی به داده‌های مصرف» در تنظیمات سیستم. */
object UsageAccess {

    fun openSettings(context: Context) {
        val intents = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, null))
            }
            add(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val ok = runCatching { context.startActivity(intent) }.isSuccess
            if (ok) return
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
