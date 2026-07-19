// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave

import android.app.Application
import de.darksoon.radiowave.core.cast.CastManager
import de.darksoon.radiowave.core.data.repository.StationMirrorRepository
import de.darksoon.radiowave.core.data.update.LocalIssueReporter
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class RadioWaveApp : Application() {
    @Inject
    lateinit var castManager: CastManager

    @Inject
    lateinit var appLanguageManager: AppLanguageManager

    @Inject
    lateinit var stationMirrorRepository: StationMirrorRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        LocalIssueReporter.install(this)
        appLanguageManager.applyPersistedLanguage()
        AppShortcuts.sync(this)
        castManager.initialize()
        // Refresh the local station-directory mirror when it's enabled and due
        // (interval + Wi-Fi-only constraints checked inside; cheap no-op otherwise).
        applicationScope.launch {
            stationMirrorRepository.syncIfDue()
        }
    }
}

