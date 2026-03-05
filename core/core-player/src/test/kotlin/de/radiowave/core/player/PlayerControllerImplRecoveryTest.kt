package de.radiowave.core.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import de.radiowave.core.data.repository.StationRepository
import de.radiowave.core.model.Country
import de.radiowave.core.model.Genre
import de.radiowave.core.model.PlayerError
import de.radiowave.core.model.PlayerState
import de.radiowave.core.model.Station
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
        controller.testSetPlaybackLostRecoveryAttempts(3)
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
        val controller = createController()
        val station = station(uuid = "s4")
        controller.testSetPlayerState(PlayerState(currentStation = station))
        val player = createBufferingPlayerProxy()

        controller.testStartBufferingWatchdog(player)
        runCurrent()
        assertEquals(0, controller.testPlaybackLostRecoveryAttempts())

        advanceTimeBy(18_000L)
        runCurrent()

        assertEquals(1, controller.testPlaybackLostRecoveryAttempts())
        assertTrue(controller.playerState.value.isLoading)
        assertTrue(controller.playerState.value.isBuffering)
    }

    private fun createController(): PlayerControllerImpl {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return PlayerControllerImpl(context, FakeStationRepository())
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

private class FakeStationRepository : StationRepository {
    override fun searchStations(query: String) = flowOf(emptyList<Station>())
    override fun getTopStations() = flowOf(emptyList<Station>())
    override fun getStationsByCountry(countryCode: String) = flowOf(emptyList<Station>())
    override fun getStationsByTag(tag: String) = flowOf(emptyList<Station>())
    override suspend fun getStationVariants(station: Station): List<Station> = listOf(station)
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
