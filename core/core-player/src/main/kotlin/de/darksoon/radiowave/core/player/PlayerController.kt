// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import de.darksoon.radiowave.core.model.PlayerError
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.model.Station
import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for the audio player controller.
 */
interface PlayerController {
    val playerState: StateFlow<PlayerState>

    suspend fun playStation(station: Station)
    fun playPreviousStation()
    fun playNextStation()
    fun togglePlayPause()
    fun pauseForExternalPlayback()
    fun toggleMute()
    fun ensureSessionPlayer(): Player
    fun setPlaybackNotificationEnabled(enabled: Boolean)
    fun setAutomotivePerformanceModeEnabled(enabled: Boolean)
    fun sessionPlayer(): Player?
    fun stop()
    fun release()
}

