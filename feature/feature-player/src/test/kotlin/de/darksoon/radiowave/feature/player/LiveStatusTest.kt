// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.feature.player

import de.darksoon.radiowave.core.model.PlayerError
import de.darksoon.radiowave.core.model.PlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveStatusTest {

    @Test
    fun `playing stream is live`() {
        val state = PlayerState(isPlaying = true, sessionStartedAtElapsedMs = 1_000L)
        assertEquals(LiveStatus.Live, resolveLiveStatus(state, isWifiRequiredError = false))
    }

    @Test
    fun `buffering before the first playing frame is connecting`() {
        val state = PlayerState(isBuffering = true, isLoading = true, sessionStartedAtElapsedMs = null)
        assertEquals(LiveStatus.Connecting, resolveLiveStatus(state, isWifiRequiredError = false))
    }

    @Test
    fun `buffering after the station was live is reconnecting`() {
        val state = PlayerState(isBuffering = true, isLoading = true, sessionStartedAtElapsedMs = 1_000L)
        assertEquals(LiveStatus.Reconnecting, resolveLiveStatus(state, isWifiRequiredError = false))
    }

    @Test
    fun `mobile data block wins over the generic error`() {
        val state = PlayerState(error = PlayerError.Unknown("Wi-Fi required"))
        assertEquals(LiveStatus.WifiRequired, resolveLiveStatus(state, isWifiRequiredError = true))
        assertEquals(LiveStatus.Disconnected, resolveLiveStatus(state, isWifiRequiredError = false))
    }

    @Test
    fun `stopped without error is paused`() {
        assertEquals(LiveStatus.Paused, resolveLiveStatus(PlayerState(), isWifiRequiredError = false))
    }

    @Test
    fun `connected duration counts from the session start while playing`() {
        val playing = PlayerState(isPlaying = true, sessionStartedAtElapsedMs = 10_000L, playedDurationMs = 5_000L)
        assertEquals(65_000L, connectedDurationMs(playing, nowElapsedMs = 75_000L))
        assertNull(connectedDurationMs(playing.copy(isPlaying = false), nowElapsedMs = 75_000L))
        assertNull(connectedDurationMs(playing.copy(sessionStartedAtElapsedMs = null), nowElapsedMs = 75_000L))
    }
}
