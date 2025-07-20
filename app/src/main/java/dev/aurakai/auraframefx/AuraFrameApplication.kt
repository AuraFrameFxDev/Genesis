package dev.aurakai.auraframefx

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.aurakai.auraframefx.security.IntegrityMonitorService

@HiltAndroidApp
class AuraFrameApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        startService(Intent(this, IntegrityMonitorService::class.java))
    }
}