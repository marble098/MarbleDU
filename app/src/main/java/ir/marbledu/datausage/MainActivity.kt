package ir.marbledu.datausage

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.marbledu.datausage.data.LocaleHelper
import ir.marbledu.datausage.data.ThemeMode
import ir.marbledu.datausage.service.MonitorService
import ir.marbledu.datausage.ui.AppRoot
import ir.marbledu.datausage.ui.MainViewModel
import ir.marbledu.datausage.ui.theme.MarbleDuTheme
import ir.marbledu.datausage.ui.theme.ThemePalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var requestedTab by mutableStateOf<String?>(null)

    /** ثبت قبل از STARTED و به‌صورت همیشگی (نه شرطی داخل onCreate). */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results.entries.filter { it.value }.map { it.key }.toSet()
        MarbleDuApp.instance.permissionState.value = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRuntimePermissions()
        requestedTab = intent?.getStringExtra(EXTRA_START_TAB)

        setContent {
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsState()

            // زبان، جهت چیدمان و منابع همگی از یک تصمیم واحد می‌آیند.
            val languageTag = remember(settings.language) {
                LocaleHelper.resolveLanguageTag(settings.language)
            }
            val persian = languageTag == "fa"
            val localized = remember(languageTag) {
                LocaleHelper.localizedContext(this@MainActivity, languageTag)
            }

            CompositionLocalProvider(LocalContext provides localized) {
                MarbleDuTheme(
                    persian = persian,
                    darkTheme = when (settings.themeMode) {
                        ThemeMode.SYSTEM -> isSystemInDarkTheme()
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                    },
                    dynamicColor = settings.dynamicColor,
                    palette = ThemePalette.of(settings.palette),
                    textScale = settings.textSize.scale,
                ) {
                    AppRoot(
                        vm = vm,
                        settings = settings,
                        persian = persian,
                        requestedTab = requestedTab,
                        onConsumeRequestedTab = { requestedTab = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_START_TAB)?.let { requestedTab = it }
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
        lifecycleScope.launch {
            val settings = app().settingsRepository.settings.first()
            if (settings.onboarded && settings.monitorInBackground) {
                MonitorService.start(this@MainActivity)
            } else {
                MonitorService.stop(this@MainActivity)
            }
        }
    }

    companion object {
        const val EXTRA_START_TAB = "start_tab"
        const val TAB_REPORTS = "reports"
    }
}

fun Context.app(): MarbleDuApp = applicationContext as MarbleDuApp
