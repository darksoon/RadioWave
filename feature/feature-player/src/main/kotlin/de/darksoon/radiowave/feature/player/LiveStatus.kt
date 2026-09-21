// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.player

import de.darksoon.radiowave.core.model.PlayerState

/**
 * What the player shows instead of a position bar: radio streams are live and have no
 * seekable position, so the useful information is the connection state.
 */
internal enum class LiveStatus {
    /** Audio is playing from the live stream. */
    Live,

    /** First connection to a newly selected station. */
    Connecting,

    /** Was playing, stream stalled or dropped; the player is buffering or retrying. */
    Reconnecting,

    /** Stopped by the user. */
    Paused,

    /** Blocked because the user only allows streaming on Wi-Fi and the device is on mobile data. */
    WifiRequired,

    /** Playback failed and automatic retries have given up. */
    Disconnected,
}

internal fun resolveLiveStatus(
    state: PlayerState,
    isWifiRequiredError: Boolean,
): LiveStatus = when {
    isWifiRequiredError -> LiveStatus.WifiRequired
    state.error != null -> LiveStatus.Disconnected
    state.isBuffering || state.isLoading -> {
        // The controller sets sessionStartedAtElapsedMs on the first playing frame of a
        // station and keeps it across stalls, so a set value means "this was live before".
        if (state.sessionStartedAtElapsedMs != null) LiveStatus.Reconnecting else LiveStatus.Connecting
    }
    state.isPlaying -> LiveStatus.Live
    else -> LiveStatus.Paused
}

/**
 * Time since this station first went live in the current session, or null when not
 * playing. Pauses count, as the station session is kept across them.
 */
internal fun connectedDurationMs(state: PlayerState, nowElapsedMs: Long): Long? {
    if (!state.isPlaying) return null
    val startedAt = state.sessionStartedAtElapsedMs ?: return null
    return (nowElapsedMs - startedAt).coerceAtLeast(0L)
}
