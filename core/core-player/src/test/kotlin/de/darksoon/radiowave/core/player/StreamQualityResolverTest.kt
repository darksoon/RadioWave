// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.core.player

import de.darksoon.radiowave.core.model.AppSettings
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StreamQualityResolverTest {
    private val original = Station(uuid = "one", name = "Radio", streamUrl = "https://example.test/high", bitrate = 192)

    @Test fun `playback selects cached quality without contacting directory`() = runTest {
        val low = original.copy(uuid = "two", streamUrl = "https://example.test/low", bitrate = 64)
        val repository = object : PlaybackTestStations() {
            override suspend fun getStationVariants(station: Station): List<Station> = error("Network must not be used for playback")
            override suspend fun getCachedStationVariants(station: Station) = listOf(original, low)
        }
        val settings = PlaybackTestSettings().apply { update { it.copy(defaultAudioQuality = AppSettings.QUALITY_LOW) } }
        assertEquals(low.streamUrl, StreamQualityResolver(repository, settings).resolve(original, false).streamUrl)
    }

    @Test fun `custom station is never substituted by matching directory name`() = runTest {
        val custom = original.copy(isCustom = true)
        val repository = object : PlaybackTestStations() {
            override suspend fun getCachedStationVariants(station: Station): List<Station> = error("Custom URL must be used directly")
        }
        assertEquals(custom, StreamQualityResolver(repository, PlaybackTestSettings()).resolve(custom, false))
    }

    @Test fun `cache failure still allows known stream to start`() = runTest {
        val repository = object : PlaybackTestStations() {
            override suspend fun getCachedStationVariants(station: Station): List<Station> = error("Cache unavailable")
        }
        assertEquals(original, StreamQualityResolver(repository, PlaybackTestSettings()).resolve(original, false))
    }

    @Test(expected = CancellationException::class)
    fun `cancelled selection does not start an obsolete station`() = runTest {
        val repository = object : PlaybackTestStations() {
            override suspend fun getCachedStationVariants(station: Station): List<Station> = throw CancellationException()
        }
        StreamQualityResolver(repository, PlaybackTestSettings()).resolve(original, false)
        Unit
    }
}
