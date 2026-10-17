// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.Country
import de.darksoon.radiowave.core.model.Genre
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerControllerImplBehaviorTest {
    private val controllers = mutableListOf<PlayerControllerImpl>()

    @org.junit.After fun releaseControllers() = controllers.forEach { it.release() }

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `togglePlayPause without initialized player keeps default state`() = runTest {
        val controller = createController(stations = emptyList())

        controller.togglePlayPause()
        runCurrent()

        assertEquals(PlayerState(), controller.playerState.value)
    }

    @Test
    fun `playNextStation with empty pool keeps station null`() = runTest {
        val controller = createController(stations = emptyList())

        controller.playNextStation()
        runCurrent()

        assertNull(controller.playerState.value.currentStation)
    }

    @Test
    fun `pause on output removal when playing and last external route gone`() = runTest {
        val controller = createController(stations = emptyList())

        val shouldPause = controller.shouldPauseOnAudioOutputRemoval(
            wasPlaying = true,
            externalOutputRemoved = true,
            externalOutputStillConnected = false,
        )

        assertEquals(true, shouldPause)
    }

    @Test
    fun `keep playing when another external output is still connected`() = runTest {
        val controller = createController(stations = emptyList())

        val shouldPause = controller.shouldPauseOnAudioOutputRemoval(
            wasPlaying = true,
            externalOutputRemoved = true,
            externalOutputStillConnected = true,
        )

        assertEquals(false, shouldPause)
    }

    @Test
    fun `do not pause when nothing was playing`() = runTest {
        val controller = createController(stations = emptyList())

        val shouldPause = controller.shouldPauseOnAudioOutputRemoval(
            wasPlaying = false,
            externalOutputRemoved = true,
            externalOutputStillConnected = false,
        )

        assertEquals(false, shouldPause)
    }

    @Test
    fun `do not pause when removed device was not an external output`() = runTest {
        val controller = createController(stations = emptyList())

        val shouldPause = controller.shouldPauseOnAudioOutputRemoval(
            wasPlaying = true,
            externalOutputRemoved = false,
            externalOutputStillConnected = false,
        )

        assertEquals(false, shouldPause)
    }

    @Test
    fun `playNextStation with available pool selects station`() = runTest {
        val stations = listOf(
            Station(uuid = "a", name = "A", streamUrl = "https://example.com/a"),
            Station(uuid = "b", name = "B", streamUrl = "https://example.com/b"),
        )
        val controller = createController(stations = stations)

        controller.playNextStation()
        runCurrent()

        assertNotNull(controller.playerState.value.currentStation)
    }

    @Test
    fun `stop leaves the reused player audible after a mute`() = runTest {
        val controller = createController(stations = emptyList())
        controller.ensureSessionPlayer()

        controller.toggleMute()
        assertEquals(0f, controller.testPlayerVolume())
        assertEquals(true, controller.playerState.value.isMuted)

        controller.stop()

        assertEquals(false, controller.playerState.value.isMuted)
        assertEquals(1f, controller.testPlayerVolume())
    }

    @Test
    fun `immediately granted audio focus lifts an earlier duck`() = runTest {
        val controller = createController(stations = emptyList())
        controller.ensureSessionPlayer()

        controller.testHandleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        assertEquals(0.25f, controller.testPlayerVolume())

        assertTrue(controller.testRequestAudioFocusGranted())

        assertEquals(1f, controller.testPlayerVolume())
    }

    @Test
    fun `granted focus keeps a user mute while lifting the duck`() = runTest {
        val controller = createController(stations = emptyList())
        controller.ensureSessionPlayer()
        controller.toggleMute()

        // Muted players are never ducked, so the grant must leave volume at 0.
        controller.testHandleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        assertTrue(controller.testRequestAudioFocusGranted())

        assertEquals(0f, controller.testPlayerVolume())
    }

    @Test
    fun `flat preset keeps the equalizer out of the audio chain`() = runTest {
        val controller = createController(stations = emptyList())
        controller.ensureSessionPlayer()
        runCurrent()

        assertFalse(controller.testIsEqualizerAttached())
    }

    private fun createController(stations: List<Station>): PlayerControllerImpl {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return PlayerControllerImpl(
            context = context,
            stationRepository = LocalStationRepositoryFake(stations),
            settingsRepository = PlaybackTestSettings(),
            coverArtRepository = FakeCoverArtRepository(),
        ).also(controllers::add)
    }
}

private class LocalStationRepositoryFake(
    private val stations: List<Station>,
) : StationRepository {
    override fun searchStations(query: String): Flow<List<Station>> = flowOf(emptyList())
    override fun getTopStations(): Flow<List<Station>> = flowOf(stations)
    override fun getStationsByCountry(countryCode: String): Flow<List<Station>> = flowOf(emptyList())
    override fun getStationsByTag(tag: String): Flow<List<Station>> = flowOf(emptyList())
    override suspend fun getStationVariants(station: Station): List<Station> = listOf(station)
    override suspend fun getSimilarStations(station: Station): List<Station> = emptyList()
    override fun getTags(): Flow<List<Genre>> = flowOf(emptyList())
    override fun getCountries(): Flow<List<Country>> = flowOf(emptyList())
    override suspend fun registerClick(stationUuid: String) = Unit
}
