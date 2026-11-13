package ir.marbledu.datausage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.marbledu.datausage.data.AppLanguage
import ir.marbledu.datausage.data.AppSettings
import ir.marbledu.datausage.data.ThemeMode
import ir.marbledu.datausage.ui.AppRoot
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.service.MonitorService
import ir.marbledu.datausage.ui.theme.MarbleDuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        setContent {
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsState()
            MarbleDuTheme(
                language = settings.language,
                darkTheme = when (settings.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
                expressive = settings.expressiveDesign,
                dynamicColor = settings.dynamicColor,
            ) {
                ApplyLocale(settings.language)
                AppRoot(vm = vm, settings = settings)
            }
        }
    }

    /** اجازه‌ی اعلان (اندروید ۱۳+) تا سرعت در نوار اعلان دیده شود. */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onStart() {
        super.onStart()
        MonitorService.start(this)
    }
}

/** زبان انتخابی کاربر را روی منابع اعمال می‌کند تا تاریخ‌ها/اعداد سیستم هم هماهنگ شوند. */
@Composable
private fun ApplyLocale(language: AppLanguage) {
    val context = LocalContext.current
    val target = when (language) {
        AppLanguage.PERSIAN -> "fa"
        AppLanguage.ENGLISH -> "en"
        AppLanguage.SYSTEM -> null
    }
    if (target != null) {
        val config = Configuration(context.resources.configuration)
        if (config.locales[0].language != target) {
            config.setLocale(java.util.Locale(target))
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}

fun Context.app(): MarbleDuApp = applicationContext as MarbleDuApp
