// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.core.player

import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.player.visualizer.AudioVisualizerTap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

// REA 95fcb33: neue Tests für die rekonstruierte Spektrum-Analyse und die Stream-Ausweichwahl.
class VisualizerAndFailoverTest {

    @Test fun `silence yields sixteen empty bands`() {
        val bands = AudioVisualizerTap.analyze(FloatArray(512), 11_025)
        assertEquals(16, bands.size)
        assertTrue(bands.all { it == 0f })
    }

    @Test fun `a 60 Hz tone peaks in the lowest band`() {
        val rate = 11_025
        val samples = FloatArray(512) { i -> sin(2 * PI * 60.0 * i / rate).toFloat() }
        val bands = AudioVisualizerTap.analyze(samples, rate)
        assertEquals(0, bands.indices.maxBy { bands[it] })
    }

    @Test fun `failover prefers https and never upgrades bitrate`() {
        val station = Station(uuid = "a", name = "Radio", streamUrl = "https://a.example/128", bitrate = 128, homepageUrl = "https://radio.example")
        val variants = listOf(
            station,
            station.copy(uuid = "b", streamUrl = "http://b.example/96", bitrate = 96),
            station.copy(uuid = "c", streamUrl = "https://c.example/64", bitrate = 64),
            station.copy(uuid = "d", streamUrl = "https://d.example/320", bitrate = 320),
        )
        assertEquals("https://c.example/64", selectFailoverStream(station, variants, emptySet())?.streamUrl)
        assertNull(selectFailoverStream(station, variants, setOf("https://c.example/64")))
    }
}
