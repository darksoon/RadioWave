// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import de.darksoon.radiowave.core.player.visualizer.AudioVisualizerTap
import androidx.media3.common.MediaMetadata
import androidx.test.core.app.ApplicationProvider
import de.darksoon.radiowave.core.data.repository.AppSettingsState
import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.Country
import de.darksoon.radiowave.core.model.Genre
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.model.StreamMetadata
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies [PlayerControllerImpl.buildNowPlayingMetadata] — the metadata written back onto the
 * session MediaItem so Android Auto / the Media3 notification show the live song instead of only
 * the static station name + logo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerControllerImplMetadataTest {
    private val controllers = mutableListOf<PlayerControllerImpl>()

    @org.junit.After fun releaseControllers() = controllers.forEach { it.release() }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** MediaItem metadata as [PlayerControllerImpl.createMediaItem] builds it: station name + favicon. */
    private fun stationBaseMetadata(name: String, favicon: String?): MediaMetadata =
        MediaMetadata.Builder()
            .setTitle(name)
            .apply { favicon?.let { setArtworkUri(android.net.Uri.parse(it)) } }
            .build()

    @Test
    fun `live song title overrides the station name that would otherwise mask it`() {
        val controller = createController()
        val base = stationBaseMetadata("Radio Example", "https://cdn.example/logo.png")

        val result = controller.buildNowPlayingMetadata(
            base = base,
            station = station("s1", favicon = "https://cdn.example/logo.png"),
            meta = StreamMetadata(title = "Yesterday", artist = "The Beatles"),
        )

        assertEquals("Yesterday", result.title)
        assertEquals("The Beatles", result.artist)
        // Station name moves to the third line (albumTitle) while a song is showing.
        assertEquals("Radio Example", result.albumTitle)
        // No stream album art → keep the station favicon as artwork.
        assertEquals("https://cdn.example/logo.png", result.artworkUri.toString())
    }

    @Test
    fun `no live metadata falls back to station name, clears artist, keeps favicon`() {
        val controller = createController()
        val base = stationBaseMetadata("Radio Example", "https://cdn.example/logo.png")

        val result = controller.buildNowPlayingMetadata(
            base = base,
            station = station("s1", favicon = "https://cdn.example/logo.png"),
            meta = null,
        )

        assertEquals("Radio Example", result.title)
        assertNull(result.artist)
        // Title already IS the station name — no duplicate third line.
        assertNull(result.albumTitle)
        assertEquals("https://cdn.example/logo.png", result.artworkUri.toString())
    }

    @Test
    fun `stream album art wins over the station favicon when present`() {
        val controller = createController()
        val base = stationBaseMetadata("Radio Example", "https://cdn.example/logo.png")

        val result = controller.buildNowPlayingMetadata(
            base = base,
            station = station("s1", favicon = "https://cdn.example/logo.png"),
            meta = StreamMetadata(title = "Song", artist = "Artist", albumArtUrl = "https://cdn.example/cover.jpg"),
        )

        assertEquals("https://cdn.example/cover.jpg", result.artworkUri.toString())
    }

    @Test
    fun `non-http favicon is dropped by the scheme guard`() {
        val controller = createController()
        val base = stationBaseMetadata("Radio Example", null)

        val result = controller.buildNowPlayingMetadata(
            base = base,
            station = station("s1", favicon = "file:///data/local/evil.png"),
            meta = StreamMetadata(title = "Song"),
        )

        assertNull(result.artworkUri)
    }

    @Test
    fun `blank live title is treated as absent and falls back to station name`() {
        val controller = createController()
        val base = stationBaseMetadata("Radio Example", null)

        val result = controller.buildNowPlayingMetadata(
            base = base,
            station = station("s1", favicon = null),
            meta = StreamMetadata(title = "   ", artist = "   "),
        )

        assertEquals("Radio Example", result.title)
        assertNull(result.artist)
    }

    // --- harness (mirrors PlayerControllerImplRecoveryTest) ---

    private fun createController(): PlayerControllerImpl {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return PlayerControllerImpl(context, FakeStationRepository(), FakeSettingsRepository(), FakeCoverArtRepository(), AudioVisualizerTap()).also(controllers::add)
    }

    private fun station(uuid: String, favicon: String?): Station =
        Station(
            uuid = uuid,
            name = "Radio Example",
            streamUrl = "https://example.com/$uuid",
            faviconUrl = favicon,
        )

    private class FakeSettingsRepository : SettingsRepository(
        ApplicationProvider.getApplicationContext(),
    ) {
        private val state = MutableStateFlow(AppSettingsState.DEFAULTS)
        override val data = state
        override fun initialSnapshotBlocking(): AppSettingsState = state.value
    }

    private class FakeStationRepository : StationRepository {
        override fun searchStations(query: String) = flowOf(emptyList<Station>())
        override fun getTopStations() = flowOf(emptyList<Station>())
        override fun getStationsByCountry(countryCode: String) = flowOf(emptyList<Station>())
        override fun getStationsByTag(tag: String) = flowOf(emptyList<Station>())
        override suspend fun getStationVariants(station: Station): List<Station> = listOf(station)
        override suspend fun getSimilarStations(station: Station): List<Station> = emptyList()
        override fun getTags() = flowOf(emptyList<Genre>())
        override fun getCountries() = flowOf(emptyList<Country>())
        override suspend fun registerClick(stationUuid: String) = Unit
    }
}
