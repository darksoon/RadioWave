package de.radiowave.core.data.repository

import de.radiowave.core.network.RadioBrowserApi
import de.radiowave.core.network.dto.RadioBrowserCountry
import de.radiowave.core.network.dto.RadioBrowserStation
import de.radiowave.core.network.dto.RadioBrowserTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StationRepositoryImplTest {

    @Test
    fun `searchStations maps dto fields to domain`() = runBlocking {
        val api = FakeRadioBrowserApi(
            searchByNameResult = listOf(
                RadioBrowserStation(
                    uuid = "uuid-1",
                    name = "Station One",
                    urlResolved = "https://stream.resolved",
                    url = "https://stream.raw",
                    tags = "rock, pop",
                ),
            ),
        )
        val repository = StationRepositoryImpl(api)

        val stations = repository.searchStations("station").first()

        assertEquals(1, stations.size)
        assertEquals("uuid-1", stations.first().uuid)
        assertEquals("Station One", stations.first().name)
        assertEquals("https://stream.resolved", stations.first().streamUrl)
        assertEquals(listOf("rock", "pop"), stations.first().tags)
    }

    @Test
    fun `searchStations falls back to url when urlResolved missing`() = runBlocking {
        val api = FakeRadioBrowserApi(
            searchByNameResult = listOf(
                RadioBrowserStation(
                    uuid = "uuid-2",
                    name = "Fallback",
                    urlResolved = null,
                    url = "https://fallback.stream",
                ),
            ),
        )
        val repository = StationRepositoryImpl(api)

        val station = repository.searchStations("fallback").first().first()

        assertEquals("https://fallback.stream", station.streamUrl)
    }

    @Test
    fun `getTags and getCountries map api responses`() = runBlocking {
        val api = FakeRadioBrowserApi(
            tagsResult = listOf(RadioBrowserTag(name = "rock", stationCount = 12)),
            countriesResult = listOf(RadioBrowserCountry(name = "Germany", code = "DE", stationCount = 42)),
        )
        val repository = StationRepositoryImpl(api)

        val tags = repository.getTags().first()
        val countries = repository.getCountries().first()

        assertEquals("rock", tags.first().name)
        assertEquals(12, tags.first().stationCount)
        assertEquals("DE", countries.first().code)
        assertEquals(42, countries.first().stationCount)
    }

    @Test
    fun `registerClick and reportBrokenStream swallow api exceptions`() = runBlocking {
        val api = FakeRadioBrowserApi(
            throwOnRegisterClick = true,
            throwOnReportBroken = true,
        )
        val repository = StationRepositoryImpl(api)

        repository.registerClick("uuid")
        repository.reportBrokenStream("uuid")

        assertTrue(api.registerClickCalled)
        assertTrue(api.reportBrokenCalled)
    }

    @Test
    fun `getTopStations returns empty list when api throws http error`() = runBlocking {
        val api = FakeRadioBrowserApi(throwOnGetTopStations = true)
        val repository = StationRepositoryImpl(api)

        val stations = repository.getTopStations().first()

        assertTrue(stations.isEmpty())
    }
}

private class FakeRadioBrowserApi(
    private val searchByNameExactResult: List<RadioBrowserStation> = emptyList(),
    private val searchByNameResult: List<RadioBrowserStation> = emptyList(),
    private val searchByTagResult: List<RadioBrowserStation> = emptyList(),
    private val searchByCountryResult: List<RadioBrowserStation> = emptyList(),
    private val topStationsResult: List<RadioBrowserStation> = emptyList(),
    private val tagsResult: List<RadioBrowserTag> = emptyList(),
    private val countriesResult: List<RadioBrowserCountry> = emptyList(),
    private val throwOnRegisterClick: Boolean = false,
    private val throwOnReportBroken: Boolean = false,
    private val throwOnGetTopStations: Boolean = false,
) : RadioBrowserApi {
    var registerClickCalled: Boolean = false
    var reportBrokenCalled: Boolean = false

    override suspend fun searchByNameExact(
        name: String,
        limit: Int,
        order: String,
        reverse: Boolean,
        hideBroken: Boolean,
    ): List<RadioBrowserStation> = searchByNameExactResult

    override suspend fun searchByName(
        name: String,
        limit: Int,
        order: String,
        reverse: Boolean,
        hideBroken: Boolean,
    ): List<RadioBrowserStation> = searchByNameResult

    override suspend fun searchByTag(
        tag: String,
        limit: Int,
        order: String,
        reverse: Boolean,
        hideBroken: Boolean,
    ): List<RadioBrowserStation> = searchByTagResult

    override suspend fun searchByCountry(
        countryCode: String,
        limit: Int,
        order: String,
        reverse: Boolean,
        hideBroken: Boolean,
    ): List<RadioBrowserStation> = searchByCountryResult

    override suspend fun getTopStations(count: Int): List<RadioBrowserStation> {
        if (throwOnGetTopStations) throw IllegalStateException("HTTP 502 Bad Gateway")
        return topStationsResult
    }

    override suspend fun getTags(order: String, reverse: Boolean, limit: Int): List<RadioBrowserTag> = tagsResult

    override suspend fun getCountries(order: String, reverse: Boolean): List<RadioBrowserCountry> = countriesResult

    override suspend fun registerClick(uuid: String) {
        registerClickCalled = true
        if (throwOnRegisterClick) throw IllegalStateException("register failed")
    }

    override suspend fun reportBrokenStream(uuid: String) {
        reportBrokenCalled = true
        if (throwOnReportBroken) throw IllegalStateException("report failed")
    }
}
