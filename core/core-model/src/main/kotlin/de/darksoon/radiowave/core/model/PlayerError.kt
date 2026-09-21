// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.model

/**
 * Sealed class representing player errors.
 */
sealed class PlayerError {
    data object NetworkError : PlayerError()
    data object StreamBroken : PlayerError()
    /** Playback refused because mobile data is off in settings and no Wi-Fi is active. */
    data object MobileDataBlocked : PlayerError()
    data class Unknown(val message: String) : PlayerError()
}

