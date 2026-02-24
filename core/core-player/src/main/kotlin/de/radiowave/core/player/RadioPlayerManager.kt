package de.radiowave.core.player

import de.radiowave.core.model.PlayerState
import de.radiowave.core.model.Station
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class that serves as interface between UI and Player Service.
 */
@Singleton
class RadioPlayerManager @Inject constructor(
    private val playerController: PlayerController,
) {
    val playerState: StateFlow<PlayerState> = playerController.playerState

    suspend fun playStation(station: Station) {
        playerController.playStation(station)
    }

    fun playStation(url: String) {
        // For simple URL playback without full station data
        val tempStation = Station(
            uuid = "temp-${System.currentTimeMillis()}",
            name = "Custom Stream",
            streamUrl = url,
        )
        // Note: This would need to be called from a coroutine in real usage
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
