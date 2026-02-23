package de.radiowave.core.model

/**
 * Sealed class representing player errors.
 */
sealed class PlayerError {
    data object NetworkError : PlayerError()
    data object StreamBroken : PlayerError()
    data class Unknown(val message: String) : PlayerError()
}
