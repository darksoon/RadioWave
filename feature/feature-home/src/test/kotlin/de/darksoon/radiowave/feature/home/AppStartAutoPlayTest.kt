// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.feature.home

import de.darksoon.radiowave.core.data.repository.AppSettingsState
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStartAutoPlayTest {
    private val remembered = AppSettingsState(
        autoPlayOnAppStart = true,
        lastStationUuid = "uuid-1",
        lastStationName = "Radio One",
        lastStationStreamUrl = "https://example.test/one",
    )

    @Test fun `disabled setting never auto-plays`() {
        assertNull(stationForAppStartAutoPlay(remembered.copy(autoPlayOnAppStart = false), PlayerState(), emptyList()))
    }

    @Test fun `nothing remembered means nothing to play`() {
        assertNull(stationForAppStartAutoPlay(remembered.copy(lastStationStreamUrl = " "), PlayerState(), emptyList()))
    }

    @Test fun `running playback is left alone`() {
        val playing = PlayerState(currentStation = Station(uuid = "x", name = "X", streamUrl = "https://example.test/x"), isPlaying = true)
        assertNull(stationForAppStartAutoPlay(remembered, playing, emptyList()))
    }

    @Test fun `prefers the full history entry of the remembered station`() {
        val recent = Station(uuid = "uuid-1", name = "Radio One", streamUrl = "https://example.test/one", isFavorite = true)
        assertEquals(recent, stationForAppStartAutoPlay(remembered, PlayerState(), listOf(recent)))
    }

    @Test fun `falls back to the persisted fields as a direct stream`() {
        val station = stationForAppStartAutoPlay(remembered, PlayerState(), emptyList())!!
        assertEquals("uuid-1", station.uuid)
        assertEquals("https://example.test/one", station.streamUrl)
        assertTrue(station.isCustom)
    }
}
