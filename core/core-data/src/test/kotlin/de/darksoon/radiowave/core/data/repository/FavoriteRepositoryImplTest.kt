// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import de.darksoon.radiowave.core.database.dao.CustomStationDao
import de.darksoon.radiowave.core.database.dao.FavoriteDao
import de.darksoon.radiowave.core.database.entity.CustomStationEntity
import de.darksoon.radiowave.core.database.entity.FavoriteEntity
import de.darksoon.radiowave.core.model.FavoritesExport
import de.darksoon.radiowave.core.model.FavoritesImportOutcome
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val testJson = Json { ignoreUnknownKeys = true; coerceInputValues = true }

class FavoriteRepositoryImplTest {

    @Test
    fun `getFavorites prefers custom station details and sets favorite flag`() = runBlocking {
        val favoriteDao = FakeFavoriteDao(
            initialFavorites = listOf(
                FavoriteEntity(
                    stationUuid = "uuid-1",
                    stationName = "Favorite Name",
                    streamUrl = "https://favorite.stream",
                    tags = "rock,pop",
                ),
            ),
        )
        val customStationDao = FakeCustomStationDao(
            initialCustomStations = listOf(
                CustomStationEntity(
                    uuid = "uuid-1",
                    name = "Custom Name",
                    streamUrl = "https://custom.stream",
                    genre = "indie",
                ),
            ),
        )
        val repository = FavoriteRepositoryImpl(favoriteDao, customStationDao, testJson)

        val favorites = repository.getFavorites().first()

        assertEquals(1, favorites.size)
        assertEquals("Custom Name", favorites.first().name)
        assertEquals("https://custom.stream", favorites.first().streamUrl)
        assertTrue(favorites.first().isFavorite)
        assertTrue(favorites.first().isCustom)
    }

    @Test
    fun `getFavorites builds station from favorite entity with parsed tags`() = runBlocking {
        val favoriteDao = FakeFavoriteDao(
            initialFavorites = listOf(
                FavoriteEntity(
                    stationUuid = "uuid-2",
                    stationName = "Entity Station",
                    streamUrl = "https://entity.stream",
                    tags = " rock, pop , ,jazz ",
                ),
            ),
        )
        val repository = FavoriteRepositoryImpl(favoriteDao, FakeCustomStationDao(), testJson)

        val station = repository.getFavorites().first().first()

        assertEquals("Entity Station", station.name)
        assertEquals(listOf("rock", "pop", "jazz"), station.tags)
        assertTrue(station.isFavorite)
    }

    @Test
    fun `toggleFavorite adds and removes station`() = runBlocking {
        val favoriteDao = FakeFavoriteDao()
        val repository = FavoriteRepositoryImpl(favoriteDao, FakeCustomStationDao(), testJson)
        val station = Station(
            uuid = "uuid-toggle",
            name = "Toggle",
            streamUrl = "https://toggle.stream",
        )

        repository.toggleFavorite(station)
        assertEquals(1, favoriteDao.currentFavorites().size)
        assertTrue(favoriteDao.currentFavorites().any { it.stationUuid == "uuid-toggle" })

        repository.toggleFavorite(station)
        assertEquals(0, favoriteDao.currentFavorites().size)
    }

    @Test
    fun `reorderFavorites updates sort order by given ids`() = runBlocking {
        val favoriteDao = FakeFavoriteDao(
            initialFavorites = listOf(
                FavoriteEntity(stationUuid = "a", stationName = "A", streamUrl = "https://a", sortOrder = 0),
                FavoriteEntity(stationUuid = "b", stationName = "B", streamUrl = "https://b", sortOrder = 1),
            ),
        )
        val repository = FavoriteRepositoryImpl(favoriteDao, FakeCustomStationDao(), testJson)

        repository.reorderFavorites(listOf("b", "a"))

        val sorted = favoriteDao.currentFavorites().sortedBy { it.sortOrder }
        assertEquals(listOf("b", "a"), sorted.map { it.stationUuid })
        assertEquals(listOf(0, 1), sorted.map { it.sortOrder })
    }

    @Test
    fun `exportFavoritesJson serializes current favorites in display order`() = runBlocking {
        val favoriteDao = FakeFavoriteDao(
            initialFavorites = listOf(
                FavoriteEntity(stationUuid = "a", stationName = "A", streamUrl = "https://a", sortOrder = 0),
                FavoriteEntity(stationUuid = "b", stationName = "B", streamUrl = "https://b", sortOrder = 1),
            ),
        )
        val repository = FavoriteRepositoryImpl(favoriteDao, FakeCustomStationDao(), testJson)

        val exportedJson = repository.exportFavoritesJson()
        val export = testJson.decodeFromString(FavoritesExport.serializer(), exportedJson)

        assertEquals(FavoritesExport.CURRENT_VERSION, export.version)
        assertEquals(listOf("a", "b"), export.stations.map { it.uuid })
    }

    @Test
    fun `importFavoritesJson adds only stations not already favorited`() = runBlocking {
        val favoriteDao = FakeFavoriteDao(
            initialFavorites = listOf(
                FavoriteEntity(stationUuid = "existing", stationName = "Existing", streamUrl = "https://existing"),
            ),
        )
        val repository = FavoriteRepositoryImpl(favoriteDao, FakeCustomStationDao(), testJson)
        val export = FavoritesExport(
            exportedAt = 0L,
            stations = listOf(
                Station(uuid = "existing", name = "Existing (renamed)", streamUrl = "https://existing-renamed"),
                Station(uuid = "new", name = "New", streamUrl = "https://new"),
            ),
        )

        val outcome = repository.importFavoritesJson(testJson.encodeToString(FavoritesExport.serializer(), export))

        assertTrue(outcome is FavoritesImportOutcome.Success)
        val success = outcome as FavoritesImportOutcome.Success
        assertEquals(1, success.imported)
        assertEquals(1, success.skippedExisting)
        assertEquals(0, success.skippedInvalid)
        // The pre-existing favorite must be untouched, not overwritten by the import.
        assertEquals("Existing", favoriteDao.currentFavorites().first { it.stationUuid == "existing" }.stationName)
        assertTrue(favoriteDao.currentFavorites().any { it.stationUuid == "new" })
    }

    @Test
    fun `importFavoritesJson skips stations with blank streamUrl`() = runBlocking {
        val favoriteDao = FakeFavoriteDao()
        val repository = FavoriteRepositoryImpl(favoriteDao, FakeCustomStationDao(), testJson)
        val export = FavoritesExport(
            exportedAt = 0L,
            stations = listOf(
                Station(uuid = "valid", name = "Valid", streamUrl = "https://valid"),
                Station(uuid = "invalid", name = "Invalid", streamUrl = ""),
            ),
        )

        val outcome = repository.importFavoritesJson(testJson.encodeToString(FavoritesExport.serializer(), export))

        assertTrue(outcome is FavoritesImportOutcome.Success)
        val success = outcome as FavoritesImportOutcome.Success
        assertEquals(1, success.imported)
        assertEquals(0, success.skippedExisting)
        assertEquals(1, success.skippedInvalid)
        assertEquals(1, favoriteDao.currentFavorites().size)
    }

    @Test
    fun `importFavoritesJson returns ParseError for malformed json`() = runBlocking {
        val repository = FavoriteRepositoryImpl(FakeFavoriteDao(), FakeCustomStationDao(), testJson)

        val outcome = repository.importFavoritesJson("{ not valid json ")

        assertEquals(FavoritesImportOutcome.ParseError, outcome)
    }

    @Test
    fun `importFavoritesJson returns UnsupportedVersion for a newer format`() = runBlocking {
        val repository = FavoriteRepositoryImpl(FakeFavoriteDao(), FakeCustomStationDao(), testJson)
        val futureJson = """{"version":999,"exportedAt":0,"stations":[]}"""

        val outcome = repository.importFavoritesJson(futureJson)

        assertEquals(FavoritesImportOutcome.UnsupportedVersion(999), outcome)
    }
}

private class FakeFavoriteDao(
    initialFavorites: List<FavoriteEntity> = emptyList(),
) : FavoriteDao {
    private val favoritesFlow = MutableStateFlow(initialFavorites)

    override fun getAllFavorites(): Flow<List<FavoriteEntity>> = favoritesFlow

    override suspend fun addFavorite(favorite: FavoriteEntity) {
        val existing = favoritesFlow.value.filterNot { it.stationUuid == favorite.stationUuid }
        favoritesFlow.value = existing + favorite
    }

    override suspend fun removeFavorite(favorite: FavoriteEntity) {
        favoritesFlow.value = favoritesFlow.value.filterNot { it.stationUuid == favorite.stationUuid }
    }

    override fun isFavorite(uuid: String): Flow<Boolean> {
        return MutableStateFlow(favoritesFlow.value.any { it.stationUuid == uuid })
    }

    override suspend fun isFavoriteOnce(uuid: String): Boolean {
        return favoritesFlow.value.any { it.stationUuid == uuid }
    }

    override suspend fun deleteByUuid(uuid: String) {
        favoritesFlow.value = favoritesFlow.value.filterNot { it.stationUuid == uuid }
    }

    override suspend fun getMaxSortOrder(): Int? {
        return favoritesFlow.value.maxOfOrNull { it.sortOrder }
    }

    fun currentFavorites(): List<FavoriteEntity> = favoritesFlow.value
}

private class FakeCustomStationDao(
    initialCustomStations: List<CustomStationEntity> = emptyList(),
) : CustomStationDao {
    private val stationsFlow = MutableStateFlow(initialCustomStations)

    override fun getAllCustomStations(): Flow<List<CustomStationEntity>> = stationsFlow

    override suspend fun insertCustomStation(station: CustomStationEntity) {
        val existing = stationsFlow.value.filterNot { it.uuid == station.uuid }
        stationsFlow.value = existing + station
    }

    override suspend fun deleteCustomStation(station: CustomStationEntity) {
        stationsFlow.value = stationsFlow.value.filterNot { it.uuid == station.uuid }
    }

    override suspend fun deleteByUuid(uuid: String) {
        stationsFlow.value = stationsFlow.value.filterNot { it.uuid == uuid }
    }

    override suspend fun getByUuid(uuid: String): CustomStationEntity? {
        return stationsFlow.value.firstOrNull { it.uuid == uuid }
    }
}

