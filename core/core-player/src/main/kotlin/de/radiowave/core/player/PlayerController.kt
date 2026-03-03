package de.radiowave.core.player

import de.radiowave.core.model.PlayerError
import de.radiowave.core.model.PlayerState
import de.radiowave.core.model.Station
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
    fun toggleMute()
    fun sessionPlayer(): Player?
    fun stop()
    fun release()
}
