package ir.marbledu.datausage

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import ir.marbledu.datausage.ui.theme.MarbleDuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
