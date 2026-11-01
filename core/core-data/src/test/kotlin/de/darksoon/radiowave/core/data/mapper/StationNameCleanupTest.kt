// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test

// REA 95fcb33: Tests zur rekonstruierten Namensbereinigung (tk2.m9638a).
class StationNameCleanupTest {

    @Test
    fun stripsCodecAndBitrateSuffix() {
        assertEquals("Radio X", cleanStationName("Radio X - MP3 128k"))
        assertEquals("Radio X", cleanStationName("Radio X | AAC+ 64 kbps"))
        assertEquals("Radio X", cleanStationName("Radio X AAC 96k"))
        assertEquals("Radio X", cleanStationName("Radio X – HLS"))
    }

    @Test
    fun keepsNamesWithoutSuffix() {
        assertEquals("1LIVE", cleanStationName("  1LIVE "))
        assertEquals("Radio Opus", cleanStationName("Radio Opus"))
    }

    @Test
    fun keepsNameThatIsOnlyACodec() {
        assertEquals("MP3 128k", cleanStationName("MP3 128k"))
        assertEquals("", cleanStationName(""))
    }
}
