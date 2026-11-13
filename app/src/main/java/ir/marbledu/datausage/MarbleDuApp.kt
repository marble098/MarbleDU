package ir.marbledu.datausage

import android.app.Application
import ir.marbledu.datausage.data.AppUsageRepository
import ir.marbledu.datausage.data.LiveMonitor
import ir.marbledu.datausage.data.SettingsRepository

class MarbleDuApp : Application() {

    val settingsRepository by lazy { SettingsRepository(this) }
    val appUsageRepository by lazy { AppUsageRepository(this) }
    val liveMonitor by lazy { LiveMonitor(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MarbleDuApp
            private set
    }
}
