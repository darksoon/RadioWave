package de.radiowave.core.model

/**
 * Represents the current state of the audio player.
 */
data class PlayerState(
    val currentStation: Station? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLoading: Boolean = false,
    val sessionStartedAtElapsedMs: Long? = null,
    val error: PlayerError? = null,
    val metadata: StreamMetadata? = null,
)
