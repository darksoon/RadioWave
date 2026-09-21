// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import android.os.SystemClock
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class that serves as interface between UI and Player Service.
 */
@Singleton
class RadioPlayerManager @Inject constructor(
    private val playerController: PlayerController,
    private val streamQualityResolver: StreamQualityResolver,
) {
    val playerState: StateFlow<PlayerState> = playerController.playerState

    // Lives for the process's lifetime (this class is a Hilt @Singleton), same lifecycle as
    // playback itself — unlike a sleep timer implemented as Activity/Compose state, this
    // survives the hosting Activity being finished (e.g. swiped away from Recents) while the
    // foreground playback service keeps running, which is exactly when a sleep timer matters.
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sleepTimerJob: Job? = null
    private var externalPlaybackStopper: (() -> Unit)? = null

    private val _sleepTimerEndsAtElapsedMs = MutableStateFlow<Long?>(null)
    val sleepTimerEndsAtElapsedMs: StateFlow<Long?> = _sleepTimerEndsAtElapsedMs.asStateFlow()

    /** Pass `null` to cancel an active timer. */
    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _sleepTimerEndsAtElapsedMs.value = null
            return
        }
        val endsAt = SystemClock.elapsedRealtime() + minutes * 60_000L
        _sleepTimerEndsAtElapsedMs.value = endsAt
        sleepTimerJob = managerScope.launch {
            delay((endsAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
            stop()
        }
    }

    suspend fun playStation(
        station: Station,
        automotiveMode: Boolean = false,
    ) {
        val selectedStation = streamQualityResolver.resolve(
            station = station,
            automotiveMode = automotiveMode,
        )
        playerController.playStation(selectedStation)
    }

    fun playPreviousStation() {
        playerController.playPreviousStation()
    }

    fun playNextStation() {
        playerController.playNextStation()
    }


    fun stop() {
        setSleepTimer(null)
        try {
            externalPlaybackStopper?.invoke()
        } finally {
            playerController.stop()
        }
    }

    /** The active remote output registers its stop command without a Cast dependency here. */
    fun setExternalPlaybackStopper(stopper: (() -> Unit)?) {
        externalPlaybackStopper = stopper
    }

    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    fun pauseForExternalPlayback() {
        playerController.pauseForExternalPlayback()
    }

    fun toggleMute() {
        playerController.toggleMute()
    }

    fun release() {
        managerScope.cancel()
        playerController.release()
    }
}
