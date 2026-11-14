package ir.marbledu.datausage.widget

import ir.marbledu.datausage.data.OperatorBrandGradient

/**
 * تمام داده‌ای که ویجت «مصرف امروز» برای رسم لازم دارد.
 *
 * این کلاس عمداً هیچ وابستگی‌ای به اندروید ندارد تا منطق ویجت (اندازه، رنگ،
 * متن‌ها) با تست واحد روی JVM قابل بررسی باشد. سازنده‌ی [WidgetSnapshot] در
 * `UsageWidgetProvider.buildSnapshot` مقدارها را از تنظیمات و آمار دستگاه پر می‌کند.
 */
data class WidgetSnapshot(
    /** زبان رابط: فارسی (RTL) یا انگلیسی (LTR). */
    val persian: Boolean = true,
    /** مصرف امروز روی داده‌ی موبایل. */
    val mobileBytes: Long = 0L,
    /** مصرف امروز روی وای‌فای. */
    val wifiBytes: Long = 0L,
    /** مجموع دانلود امروز (موبایل + وای‌فای). */
    val rxBytes: Long = 0L,
    /** مجموع آپلود امروز (موبایل + وای‌فای). */
    val txBytes: Long = 0L,
    /** نام اپراتور یا شبکه‌ی فعال؛ خالی یعنی نمایش داده نشود. */
    val operatorName: String = "",
    /** مصرف موبایل از ابتدای دوره‌ی بسته. */
    val capUsedBytes: Long = 0L,
    /** حجم کل بسته؛ ۰ یعنی کاربر سقفی تعیین نکرده است. */
    val capTotalBytes: Long = 0L,
    /** آیا پایش پس‌زمینه روشن است؟ */
    val monitoring: Boolean = false,
    /** آیا اجازه‌ی «دسترسی به مصرف» به برنامه داده شده است؟ */
    val hasUsageAccess: Boolean = true,
    /** زمان آخرین به‌روزرسانی داده (میلی‌ثانیه). */
    val updatedAtMillis: Long = 0L,
    val showOperator: Boolean = true,
    val showCap: Boolean = true,
    val detailed: Boolean = false,
    val bigText: Boolean = false,
    val quickActions: Boolean = true,
    /** آیا رنگ‌های ویجت از پالت تم برنامه گرفته شود؟ */
    val followTheme: Boolean = true,
    /** رنگ اصلی پالت (ARGB)؛ نال یعنی پالت پیش‌فرض ویجت. */
    val accentPrimary: Int? = null,
    /** رنگ دوم پالت (ARGB) برای ستون وای‌فای. */
    val accentSecondary: Int? = null,
    /** پس‌زمینه‌ی تیره (تم تاریک/شب) یا روشن. */
    val darkSurface: Boolean = true,
    /** تم سیاه مطلق (AMOLED): پس‌زمینه‌ی کاملاً مشکی. */
    val amoled: Boolean = false,
    /**
     * پالت گرادیان اپراتور فعال؛ وقتی پر است، کارت ویجت همان گرادیان کارت صفحه‌ی خانه
     * و اعلان را می‌گیرد. نال یعنی سطح معمولی تم (ویجت هم‌رنگ برنامه نیست یا اتصالی نیست).
     */
    val brand: OperatorBrandGradient? = null,
) {
    /** مجموع مصرف امروز. */
    val totalBytes: Long get() = mobileBytes + wifiBytes

    /** آیا سقف بسته تعریف شده است؟ */
    val hasCap: Boolean get() = capTotalBytes > 0L

    /** باقی‌مانده‌ی بسته (هرگز منفی نمی‌شود). */
    val capRemainingBytes: Long get() = (capTotalBytes - capUsedBytes).coerceAtLeast(0L)

    /** آیا داده‌ی قابل نمایش وجود دارد؟ (بدون دسترسی، اعداد «—» می‌شوند) */
    val hasNumbers: Boolean get() = hasUsageAccess
}
