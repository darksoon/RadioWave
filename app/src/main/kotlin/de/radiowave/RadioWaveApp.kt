// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave

import android.app.Application
import de.radiowave.core.data.update.LocalIssueReporter
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RadioWaveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalIssueReporter.install(this)
        AppLanguageManager.applyFromPrefs(this)
        AppShortcuts.sync(this)
    }
}

