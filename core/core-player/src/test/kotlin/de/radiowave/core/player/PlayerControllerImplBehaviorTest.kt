package de.radiowave.core.player

import androidx.test.core.app.ApplicationProvider
import de.radiowave.core.data.repository.StationRepository
import de.radiowave.core.model.Country
import de.radiowave.core.model.Genre
import de.radiowave.core.model.PlayerState
import de.radiowave.core.model.Station
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerControllerImplBehaviorTest {

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

    private fun createController(stations: List<Station>): PlayerControllerImpl {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return PlayerControllerImpl(
            context = context,
            stationRepository = LocalStationRepositoryFake(stations),
        )
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
    override fun getTags(): Flow<List<Genre>> = flowOf(emptyList())
    override fun getCountries(): Flow<List<Country>> = flowOf(emptyList())
    override suspend fun registerClick(stationUuid: String) = Unit
    override suspend fun reportBrokenStream(stationUuid: String) = Unit
}
