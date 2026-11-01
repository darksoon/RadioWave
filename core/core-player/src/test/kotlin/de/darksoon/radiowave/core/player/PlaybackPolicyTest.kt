// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.core.player

import de.darksoon.radiowave.core.player.visualizer.AudioVisualizerTap
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Looper
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import de.darksoon.radiowave.core.data.repository.AppSettingsState
import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.PlayerError
import de.darksoon.radiowave.core.model.Country
import de.darksoon.radiowave.core.model.Genre
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkInfo

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaybackPolicyTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val settings = PlaybackTestSettings()
    private val stations = PlaybackTestStations()
    private val station = Station(uuid = "test", name = "Test", streamUrl = "https://example.test/radio")

    private fun controller() = PlayerControllerImpl(context, stations, settings, FakeCoverArtRepository(), AudioVisualizerTap())

    @Test fun `timer stops real player on its application thread and remote output`() = runTest {
        val controller = controller()
        val player = controller.ensureSessionPlayer()
        controller.testSetPlayerState(PlayerState(currentStation = station))
        val manager = RadioPlayerManager(controller, StreamQualityResolver(stations, settings))
        var remoteStops = 0
        manager.setExternalPlaybackStopper {
            assertEquals(player.applicationLooper, Looper.myLooper())
            remoteStops++
        }
        try {
            manager.setSleepTimer(1)
            runCurrent()
            advanceTimeBy(59_000)
            runCurrent()
            assertNotNull(manager.playerState.value.currentStation)
            advanceTimeBy(1_001)
            runCurrent()
            assertNull(manager.playerState.value.currentStation)
            assertNull(manager.sleepTimerEndsAtElapsedMs.value)
            assertEquals(1, remoteStops)
            assertEquals(Player.STATE_IDLE, player.playbackState)
        } finally { manager.release() }
    }

    @Test fun `manual stop cancels timer instead of stopping a later listening session`() = runTest {
        val controller = controller()
        val manager = RadioPlayerManager(controller, StreamQualityResolver(stations, settings))
        try {
            manager.setSleepTimer(1)
            runCurrent()
            manager.stop()
            controller.testSetPlayerState(PlayerState(currentStation = station))
            advanceTimeBy(61_000)
            runCurrent()
            assertEquals(station, manager.playerState.value.currentStation)
            assertNull(manager.sleepTimerEndsAtElapsedMs.value)
        } finally { manager.release() }
    }

    @Test fun `wifi to cellular stops buffering and does not autoplay when wifi returns`() = runTest {
        setNetwork(NetworkCapabilities.TRANSPORT_WIFI)
        settings.update { it.copy(allowMobileData = false) }
        val controller = controller()
        try {
            val player = controller.ensureSessionPlayer()
            controller.testSetPlayerState(PlayerState(currentStation = station, isLoading = true))
            player.playWhenReady = true
            runCurrent()
            setNetwork(NetworkCapabilities.TRANSPORT_CELLULAR)
            notifyCapabilities()
            runCurrent()
            assertFalse(player.playWhenReady)
            assertFalse(controller.playerState.value.isLoading)
            assertEquals(PlayerError.MobileDataBlocked, controller.playerState.value.error)
            setNetwork(NetworkCapabilities.TRANSPORT_WIFI)
            shadowOf(connectivity).networkCallbacks.toList().forEach { it.onAvailable(connectivity.activeNetwork!!) }
            runCurrent()
            assertFalse(player.playWhenReady)
        } finally { controller.release() }
    }

    @Test fun `disabling mobile data stops an already active session`() = runTest {
        setNetwork(NetworkCapabilities.TRANSPORT_CELLULAR)
        val controller = controller()
        try {
            val player = controller.ensureSessionPlayer()
            controller.testSetPlayerState(PlayerState(currentStation = station, isLoading = true))
            player.playWhenReady = true
            runCurrent()
            settings.update { it.copy(allowMobileData = false) }
            runCurrent()
            assertFalse(player.playWhenReady)
            assertEquals(PlayerError.MobileDataBlocked, controller.playerState.value.error)
        } finally { controller.release() }
    }

    @Test fun `phone and media session resume cannot bypass mobile restriction`() = runTest {
        setNetwork(NetworkCapabilities.TRANSPORT_CELLULAR)
        settings.update { it.copy(allowMobileData = false) }
        val controller = controller()
        try {
            val player = controller.ensureSessionPlayer()
            controller.testSetPlayerState(PlayerState(currentStation = station))
            controller.togglePlayPause()
            assertFalse(player.playWhenReady)
            // Notification / Android Auto can issue this directly on the session player.
            player.playWhenReady = true
            shadowOf(Looper.getMainLooper()).idle()
            runCurrent()
            assertFalse(player.playWhenReady)
            assertEquals(PlayerError.MobileDataBlocked, controller.playerState.value.error)
        } finally { controller.release() }
    }

    private fun setNetwork(transport: Int) {
        val type = if (transport == NetworkCapabilities.TRANSPORT_WIFI) ConnectivityManager.TYPE_WIFI else ConnectivityManager.TYPE_MOBILE
        shadowOf(connectivity).setActiveNetworkInfo(
            ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.CONNECTED, type, 0, true, true),
        )
        shadowOf(connectivity).setNetworkCapabilities(
            connectivity.activeNetwork!!, NetworkCapabilities().also { shadowOf(it).addTransportType(transport) },
        )
    }

    @Test fun `queued reconnect rechecks policy before preparing a stream`() = runTest {
        setNetwork(NetworkCapabilities.TRANSPORT_WIFI)
        settings.update { it.copy(allowMobileData = false) }
        val controller = controller()
        try {
            val player = controller.ensureSessionPlayer()
            controller.testSetPlayerState(PlayerState(currentStation = station))
            controller.testScheduleReconnect(station, delayOverrideMs = 500)
            runCurrent()
            setNetwork(NetworkCapabilities.TRANSPORT_CELLULAR)
            // Do not deliver a network callback: the restart itself must enforce policy.
            advanceTimeBy(501)
            runCurrent()
            assertEquals(0, player.mediaItemCount)
            assertFalse(player.playWhenReady)
            assertEquals(PlayerError.MobileDataBlocked, controller.playerState.value.error)
        } finally { controller.release() }
    }

    private fun notifyCapabilities() {
        val network = connectivity.activeNetwork!!
        shadowOf(connectivity).networkCallbacks.toList().forEach {
            it.onCapabilitiesChanged(network, connectivity.getNetworkCapabilities(network)!!)
        }
    }
}

internal class PlaybackTestSettings : SettingsRepository(ApplicationProvider.getApplicationContext()) {
    private val state = MutableStateFlow(AppSettingsState.DEFAULTS)
    override val data = state
    override fun initialSnapshotBlocking() = state.value
    fun update(transform: (AppSettingsState) -> AppSettingsState) { state.value = transform(state.value) }
    override suspend fun setLastStation(uuid: String?, name: String?, streamUrl: String?, faviconUrl: String?, country: String?) = Unit
}

internal open class PlaybackTestStations : StationRepository {
    override fun searchStations(query: String) = flowOf(emptyList<Station>())
    override fun getTopStations() = flowOf(emptyList<Station>())
    override fun getStationsByCountry(countryCode: String) = flowOf(emptyList<Station>())
    override fun getStationsByTag(tag: String) = flowOf(emptyList<Station>())
    override suspend fun getStationVariants(station: Station) = listOf(station)
    override suspend fun getSimilarStations(station: Station) = emptyList<Station>()
    override fun getTags() = flowOf(emptyList<Genre>())
    override fun getCountries() = flowOf(emptyList<Country>())
    override suspend fun registerClick(stationUuid: String) = Unit
}
