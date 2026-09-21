// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.feature.player

import de.darksoon.radiowave.core.data.repository.CoverArtRepository
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.Country
import de.darksoon.radiowave.core.model.Genre
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimilarStationsTest {

    private val repository = FakeStationRepository()
    private val source = station("src", "Jazz FM", tags = listOf("jazz", "smooth"), country = "Germany")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `drops the source, its re-uploads, streamless entries and name duplicates`() {
        val result = buildSimilarStations(
            source,
            listOf(
                station("src", "Jazz FM", tags = listOf("jazz")),
                station("copy", " jazz fm ", tags = listOf("jazz")),
                station("nostream", "Blue Radio", tags = listOf("jazz"), streamUrl = ""),
                station("a", "Smooth Jazz 24", tags = listOf("smooth jazz")),
                station("b", "Smooth Jazz 24", tags = listOf("jazz")),
                station("c", "Swing Radio", tags = listOf("swing")),
            ),
        )
        assertEquals(listOf("a", "c"), result.stations.map { it.uuid })
        assertEquals(listOf("jazz", "smooth"), result.matchedTags)
        assertNull(result.matchedCountry)
    }

    @Test
    fun `falls back to the country as reason when no tag matches`() {
        val result = buildSimilarStations(
            source,
            listOf(station("a", "Radio Eins", tags = listOf("news"), country = "Germany")),
        )
        assertTrue(result.matchedTags.isEmpty())
        assertEquals("Germany", result.matchedCountry)
    }

    @Test
    fun `loads once per station and retries after an empty result`() {
        val viewModel = PlayerViewModel(NoCoverArt, repository)

        repository.result = emptyList()
        viewModel.loadSimilarStations(source)
        assertEquals(1, repository.calls)
        assertTrue(viewModel.similarStations.value.stations.isEmpty())

        repository.result = listOf(station("a", "Smooth Jazz 24", tags = listOf("jazz")))
        viewModel.loadSimilarStations(source)
        assertEquals(2, repository.calls)
        assertEquals(listOf("a"), viewModel.similarStations.value.stations.map { it.uuid })

        viewModel.loadSimilarStations(source)
        assertEquals(2, repository.calls)

        viewModel.loadSimilarStations(station("other", "Other"))
        assertEquals(3, repository.calls)
        assertEquals("other", viewModel.similarStations.value.sourceStationUuid)
    }

    private fun station(
        uuid: String,
        name: String,
        tags: List<String> = emptyList(),
        country: String? = null,
        streamUrl: String = "https://example.org/$uuid",
    ) = Station(uuid = uuid, name = name, streamUrl = streamUrl, tags = tags, country = country)

    private object NoCoverArt : CoverArtRepository {
        override suspend fun fetchCoverArt(artist: String?, title: String?): String? = null
        override fun clearCache() = Unit
    }

    private class FakeStationRepository : StationRepository {
        var result: List<Station> = emptyList()
        var calls = 0

        override suspend fun getSimilarStations(station: Station): List<Station> {
            calls++
            return result
        }

        override fun searchStations(query: String): Flow<List<Station>> = emptyFlow()
        override fun getTopStations(): Flow<List<Station>> = emptyFlow()
        override fun getStationsByCountry(countryCode: String): Flow<List<Station>> = emptyFlow()
        override fun getStationsByTag(tag: String): Flow<List<Station>> = emptyFlow()
        override suspend fun getStationVariants(station: Station): List<Station> = listOf(station)
        override fun getTags(): Flow<List<Genre>> = emptyFlow()
        override fun getCountries(): Flow<List<Country>> = emptyFlow()
        override suspend fun registerClick(stationUuid: String) = Unit
    }
}
