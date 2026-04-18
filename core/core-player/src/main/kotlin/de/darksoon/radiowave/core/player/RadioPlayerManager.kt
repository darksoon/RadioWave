// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.StateFlow
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
        playerController.stop()
    }

    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    fun toggleMute() {
        playerController.toggleMute()
    }

    fun release() {
        playerController.release()
    }
}

