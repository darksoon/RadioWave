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
    fun setAutomotivePerformanceModeEnabled(enabled: Boolean)

    /**
     * Registers a listener invoked (on the player's application thread) whenever the
     * underlying ExoPlayer is (re)created — e.g. after a buffer-profile/thermal/automotive
     * change releases and rebuilds it. The MediaLibrarySession must re-bind to the new
     * player via this callback, otherwise it keeps forwarding transport commands to a
     * released player. Pass null to clear.
     */
    fun setOnSessionPlayerChangedListener(listener: ((Player) -> Unit)?)

    fun sessionPlayer(): Player?
    fun stop()
    fun release()
}

