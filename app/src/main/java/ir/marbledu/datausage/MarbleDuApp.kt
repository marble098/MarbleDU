package ir.marbledu.datausage

import android.app.Application
import ir.marbledu.datausage.data.AppUsageRepository
import ir.marbledu.datausage.data.LiveMonitor
import ir.marbledu.datausage.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MarbleDuApp : Application() {

    val settingsRepository by lazy { SettingsRepository(this) }
    val appUsageRepository by lazy { AppUsageRepository(this) }
    val liveMonitor by lazy { LiveMonitor(this) }

    /** مجوزهای رن‌تایم که کاربر تا اینجا اجازه داده است (POST_NOTIFICATIONS, READ_PHONE_STATE و …). */
    val permissionState: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val permissionStateFlow: StateFlow<Set<String>> = permissionState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MarbleDuApp
            private set
    }
}
