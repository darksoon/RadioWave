// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import de.darksoon.radiowave.core.data.repository.AppSettingsState
import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.Country
import de.darksoon.radiowave.core.model.Genre
import de.darksoon.radiowave.core.model.PlayerError
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerControllerImplRecoveryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `scheduleReconnect updates state and increments attempts`() = runTest {
        val controller = createController()
        val station = station(uuid = "s1")

        controller.testScheduleReconnect(
            station = station,
            delayOverrideMs = 50L,
            countAttempt = true,
        )
        runCurrent()

        assertEquals(1, controller.testReconnectAttempts())
        assertTrue(controller.playerState.value.isLoading)
        assertTrue(controller.playerState.value.isBuffering)
    }

    @Test
    fun `triggerPlaybackLostRecovery sets network error when max attempts reached`() = runTest {
        val controller = createController()
        val station = station(uuid = "s2")
        controller.testSetPlaybackLostRecoveryAttempts(4)
        controller.testTriggerPlaybackLostRecovery(station = station, reason = "test")

        assertEquals(PlayerError.NetworkError, controller.playerState.value.error)
        assertEquals(false, controller.playerState.value.isLoading)
        assertEquals(false, controller.playerState.value.isBuffering)
    }

    @Test
    fun `triggerPlaybackLostRecovery schedules recovery and marks loading`() = runTest {
        val controller = createController()
        val station = station(uuid = "s3")
        controller.testSetPlayerState(PlayerState(currentStation = station))
        controller.testTriggerPlaybackLostRecovery(station = station, reason = "manual-trigger")
        runCurrent()

        assertEquals(1, controller.testPlaybackLostRecoveryAttempts())
        assertTrue(controller.playerState.value.isLoading)
        assertTrue(controller.playerState.value.isBuffering)
    }

    @Test
    fun `startBufferingWatchdog triggers lost playback recovery after threshold`() = runTest {
        setTimeshiftGuardEnabled(false)
        // initialSnapshotBlocking() picks up timeshiftGuard=false synchronously now,
        // so no extra runCurrent() is needed before arming the watchdog.
        val controller = createController()
        val station = station(uuid = "s4")
        controller.testSetPlayerState(PlayerState(currentStation = station))
        val player = createBufferingPlayerProxy()

        controller.testStartBufferingWatchdog(player)
        runCurrent()
        assertEquals(0, controller.testPlaybackLostRecoveryAttempts())

        advanceTimeBy(24_000L)
        runCurrent()

        assertEquals(1, controller.testPlaybackLostRecoveryAttempts())
        assertTrue(controller.playerState.value.isLoading)
        assertTrue(controller.playerState.value.isBuffering)
    }

    // The test now drives the timeshift-guard flag via the shared mutable fake settings
    // repository (instead of writing into the legacy SharedPreferences file).
    private val fakeSettingsRepository = FakeSettingsRepository()

    private fun setTimeshiftGuardEnabled(enabled: Boolean) {
        fakeSettingsRepository.update { it.copy(timeshiftGuard = enabled) }
    }

    private fun createController(): PlayerControllerImpl {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return PlayerControllerImpl(context, FakeStationRepository(), fakeSettingsRepository)
    }

    private fun createBufferingPlayerProxy(): ExoPlayer {
        return Proxy.newProxyInstance(
            ExoPlayer::class.java.classLoader,
            arrayOf(ExoPlayer::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getPlayWhenReady" -> true
                "getPlaybackState" -> Player.STATE_BUFFERING
                "isPlaying" -> false
                "toString" -> "ExoPlayerBufferingProxy"
                "hashCode" -> 0
                "equals" -> false
                else -> defaultReturnValue(method.returnType)
            }
        } as ExoPlayer
    }

    private fun defaultReturnValue(type: Class<*>): Any? {
        return when {
            !type.isPrimitive -> null
            type == Boolean::class.javaPrimitiveType -> false
            type == Int::class.javaPrimitiveType -> 0
            type == Long::class.javaPrimitiveType -> 0L
            type == Float::class.javaPrimitiveType -> 0f
            type == Double::class.javaPrimitiveType -> 0.0
            type == Short::class.javaPrimitiveType -> 0.toShort()
            type == Byte::class.javaPrimitiveType -> 0.toByte()
            type == Char::class.javaPrimitiveType -> '\u0000'
            else -> null
        }
    }

    private fun station(uuid: String): Station {
        return Station(
            uuid = uuid,
            name = "Station-$uuid",
            streamUrl = "https://example.com/$uuid",
        )
    }

}

/**
 * Test double for [SettingsRepository] backed by an in-memory [MutableStateFlow]
 * — avoids DataStore I/O and the legacy SharedPreferences migration in unit tests.
 */
private class FakeSettingsRepository : SettingsRepository(
    androidx.test.core.app.ApplicationProvider.getApplicationContext(),
) {
    private val state = MutableStateFlow(AppSettingsState.DEFAULTS)
    override val data = state

    // Synchronous read — for the fake, just return the current state.
    override fun initialSnapshotBlocking(): AppSettingsState = state.value

    fun update(transform: (AppSettingsState) -> AppSettingsState) {
        state.value = transform(state.value)
    }

    override suspend fun setLastStation(
        uuid: String?, name: String?, streamUrl: String?, faviconUrl: String?, country: String?,
    ) {
        state.value = state.value.copy(
            lastStationUuid = uuid,
            lastStationName = name,
            lastStationStreamUrl = streamUrl,
            lastStationFaviconUrl = faviconUrl,
            lastStationCountry = country,
        )
    }
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
    override suspend fun reportBrokenStream(stationUuid: String) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

