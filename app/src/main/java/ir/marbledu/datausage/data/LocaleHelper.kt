package ir.marbledu.datausage.data

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/**
 * مدیریت یکپارچه‌ی زبان اپ در Activity، سرویس اعلان و خواندن آمار شبکه
 * تا وقتی کاربر «فارسی» را انتخاب می‌کند همه‌چیز (حتی با زبان انگلیسی سیستم)
 * صددرصد فارسی بماند.
 */
object LocaleHelper {

    fun resolveLanguageTag(
        language: AppLanguage,
        systemLanguage: String = systemLocaleLanguage(),
    ): String = when (language) {
        AppLanguage.PERSIAN -> "fa"
        AppLanguage.ENGLISH -> "en"
        AppLanguage.SYSTEM -> if (systemLanguage.equals("fa", ignoreCase = true)) "fa" else "en"
    }

    fun isPersian(
        language: AppLanguage,
        systemLanguage: String = systemLocaleLanguage(),
    ): Boolean = resolveLanguageTag(language, systemLanguage) == "fa"

    private fun systemLocaleLanguage(): String =
        runCatching { Resources.getSystem().configuration.locales[0].language }
            .getOrElse { Locale.getDefault().language }

    fun localizedContext(base: Context, language: AppLanguage): Context =
        localizedContext(base, resolveLanguageTag(language))

    /**
     * منابع بومی‌شده بدون دستکاری سراسری Resources.updateConfiguration().
     * ContextWrapper همچنان به Activity/Service پایه متصل می‌ماند.
     */
    fun localizedContext(base: Context, tag: String): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        val localized = base.createConfigurationContext(config)
        return object : ContextWrapper(base) {
            private val localizedResources: Resources = localized.resources
            override fun getResources(): Resources = localizedResources
        }
    }
}
