package ir.marbledu.datausage

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.marbledu.datausage.data.AppLanguage
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.ThemeMode
import ir.marbledu.datausage.service.MonitorService
import ir.marbledu.datausage.ui.AppRoot
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.theme.MarbleDuTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var viewModelRef: MainViewModel? = null

    /** ثبت قبل از STARTED و به‌صورت همیشگی (نه شرطی داخل onCreate). */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRuntimePermissions()
        setContent {
            val vm: MainViewModel = viewModel()
            viewModelRef = vm
            val settings by vm.settings.collectAsState()

            // زبان، جهت چیدمان و منابع باید همگی از یک تصمیم واحد بیایند.
            // حالت SYSTEM دیگر روی تنظیمات قبلی گیر نمی‌کند و زبان‌های دیگر
            // سیستم هم به انگلیسی (نه فارسیِ پیش‌فرض resources) سقوط می‌کنند.
            val languageTag = remember(settings.language) { resolveLanguageTag(settings.language) }
            val persian = languageTag == "fa"
            val localized = remember(languageTag) {
                localizedContext(this@MainActivity, languageTag)
            }

            CompositionLocalProvider(LocalContext provides localized) {
                MarbleDuTheme(
                    persian = persian,
                    darkTheme = when (settings.themeMode) {
                        ThemeMode.SYSTEM -> isSystemInDarkTheme()
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                    },
                    expressive = settings.expressiveDesign,
                    dynamicColor = settings.dynamicColor,
                ) {
                    AppRoot(vm = vm, settings = settings, persian = persian)
                }
            }
        }
    }

    /** اعلان (اندروید ۱۳+) و شناسه‌ی سیم‌کارت برای آمار موبایل روی اندروید قدیمی. */
    private fun requestRuntimePermissions() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.READ_PHONE_STATE)
            }
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }

    override fun onStart() {
        super.onStart()
        // اگر کاربر پایش پس‌زمینه را خاموش کرده، با باز کردن اپ دوباره روشن نشود.
        val enabled = viewModelRef?.settings?.value?.monitorInBackground ?: true
        if (enabled) MonitorService.start(this) else MonitorService.stop(this)
    }
}

private fun resolveLanguageTag(language: AppLanguage): String = when (language) {
    AppLanguage.PERSIAN -> "fa"
    AppLanguage.ENGLISH -> "en"
    AppLanguage.SYSTEM -> if (Locale.getDefault().language == "fa") "fa" else "en"
}

/**
 * منابعِ بومی‌شده بدون دستکاری سراسری Resources.updateConfiguration().
 * getString/getText در Context نهایی هستند و از getResources() می‌خوانند،
 * پس overriding آن کافی است؛ ضمن اینکه ContextWrapper همچنان به Activity
 * وصل می‌ماند و findActivity() کار می‌کند.
 */
private fun localizedContext(base: Context, tag: String): Context {
    val config = Configuration(base.resources.configuration)
    config.setLocale(Locale.forLanguageTag(tag))
    val localized = base.createConfigurationContext(config)
    return object : ContextWrapper(base) {
        private val localizedResources: Resources = localized.resources
        override fun getResources(): Resources = localizedResources
    }
}

fun Context.app(): MarbleDuApp = applicationContext as MarbleDuApp
