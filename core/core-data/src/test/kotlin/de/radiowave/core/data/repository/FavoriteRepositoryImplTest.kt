// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.data.repository

import de.radiowave.core.database.dao.CustomStationDao
import de.radiowave.core.database.dao.FavoriteDao
import de.radiowave.core.database.entity.CustomStationEntity
import de.radiowave.core.database.entity.FavoriteEntity
import de.radiowave.core.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
        val repository = FavoriteRepositoryImpl(favoriteDao, customStationDao)

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
        val repository = FavoriteRepositoryImpl(favoriteDao, FakeCustomStationDao())

        val station = repository.getFavorites().first().first()

        assertEquals("Entity Station", station.name)
        assertEquals(listOf("rock", "pop", "jazz"), station.tags)
        assertTrue(station.isFavorite)
    }

    @Test
    fun `toggleFavorite adds and removes station`() = runBlocking {
        val favoriteDao = FakeFavoriteDao()
        val repository = FavoriteRepositoryImpl(favoriteDao, FakeCustomStationDao())
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
        val repository = FavoriteRepositoryImpl(favoriteDao, FakeCustomStationDao())

        repository.reorderFavorites(listOf("b", "a"))

        val sorted = favoriteDao.currentFavorites().sortedBy { it.sortOrder }
        assertEquals(listOf("b", "a"), sorted.map { it.stationUuid })
        assertEquals(listOf(0, 1), sorted.map { it.sortOrder })
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

    override suspend fun deleteByUuid(uuid: String) {
        favoritesFlow.value = favoritesFlow.value.filterNot { it.stationUuid == uuid }
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

