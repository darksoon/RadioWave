package de.radiowave

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RadioWaveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLanguageManager.applyFromPrefs(this)
    }
}
