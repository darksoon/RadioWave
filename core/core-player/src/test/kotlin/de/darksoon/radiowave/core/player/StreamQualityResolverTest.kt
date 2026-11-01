// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.core.player

import de.darksoon.radiowave.core.model.AppSettings
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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

    @Test fun `without cached variants the directory is asked once per station`() = runTest {
        val low = original.copy(uuid = "two", streamUrl = "https://example.test/low", bitrate = 64)
        var lookups = 0
        val repository = object : PlaybackTestStations() {
            override suspend fun getStationVariants(station: Station): List<Station> { lookups++; return listOf(original, low) }
            override suspend fun getCachedStationVariants(station: Station) = listOf(original)
        }
        val settings = PlaybackTestSettings().apply { update { it.copy(defaultAudioQuality = AppSettings.QUALITY_LOW) } }
        val resolver = StreamQualityResolver(repository, settings)
        assertEquals(low.streamUrl, resolver.resolve(original, false).streamUrl)
        resolver.resolve(original, false)
        assertEquals(1, lookups)
    }

    @Test fun `hanging directory does not block a known stream`() = runTest {
        val repository = object : PlaybackTestStations() {
            override suspend fun getStationVariants(station: Station): List<Station> { delay(60_000); error("unreachable") }
            override suspend fun getCachedStationVariants(station: Station) = listOf(original)
        }
        assertEquals(original, StreamQualityResolver(repository, PlaybackTestSettings()).resolve(original, false))
        assertTrue("lookup must give up after its short timeout", testScheduler.currentTime < 5_000)
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

    // REA 95fcb33: neue Auto-Logik — getaktetes Netz nimmt mobileAudioQuality, sonst wifiAudioQuality.
    @Test fun `auto quality uses mobile preference on metered network`() = runTest {
        val low = original.copy(uuid = "two", streamUrl = "https://example.test/low", bitrate = 64)
        val repository = object : PlaybackTestStations() {
            override suspend fun getCachedStationVariants(station: Station) = listOf(original, low)
        }
        val settings = PlaybackTestSettings().apply {
            update { it.copy(defaultAudioQuality = AppSettings.QUALITY_AUTO, wifiAudioQuality = AppSettings.QUALITY_HIGH, mobileAudioQuality = AppSettings.QUALITY_LOW) }
        }
        assertEquals(low.streamUrl, StreamQualityResolver(repository, settings) { true }.resolve(original, false).streamUrl)
        assertEquals(original.streamUrl, StreamQualityResolver(repository, settings) { false }.resolve(original, false).streamUrl)
    }

    @Test fun `https station is never switched to an http variant`() = runTest {
        val insecure = original.copy(uuid = "two", streamUrl = "http://example.test/low", bitrate = 64)
        val repository = object : PlaybackTestStations() {
            override suspend fun getCachedStationVariants(station: Station) = listOf(original, insecure)
        }
        val settings = PlaybackTestSettings().apply { update { it.copy(defaultAudioQuality = AppSettings.QUALITY_LOW) } }
        assertEquals(original.streamUrl, StreamQualityResolver(repository, settings).resolve(original, false).streamUrl)
    }

    @Test fun `selected variant keeps identity of the played station`() = runTest {
        val low = Station(uuid = "mirror", name = "Mirror name", streamUrl = "https://example.test/low", bitrate = 64, codec = "AAC")
        val repository = object : PlaybackTestStations() {
            override suspend fun getCachedStationVariants(station: Station) = listOf(original, low)
        }
        val settings = PlaybackTestSettings().apply { update { it.copy(defaultAudioQuality = AppSettings.QUALITY_LOW) } }
        val resolved = StreamQualityResolver(repository, settings).resolve(original, false)
        assertEquals(original.copy(streamUrl = low.streamUrl, codec = "AAC", bitrate = 64), resolved)
    }
}
