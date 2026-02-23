package de.radiowave.core.data.repository

import de.radiowave.core.data.mapper.toDomain
import de.radiowave.core.data.mapper.toFavoriteEntity
import de.radiowave.core.database.dao.CustomStationDao
import de.radiowave.core.database.dao.FavoriteDao
import de.radiowave.core.database.entity.FavoriteEntity
import de.radiowave.core.model.Station
import de.radiowave.core.network.RadioBrowserApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val customStationDao: CustomStationDao,
    private val api: RadioBrowserApi,
) : FavoriteRepository {

    override fun getFavorites(): Flow<List<Station>> {
        return combine(
            favoriteDao.getAllFavorites(),
            customStationDao.getAllCustomStations(),
        ) { favorites, customStations ->
            favorites.mapNotNull { favorite ->
                // Try to find in custom stations first
                customStations.find { it.uuid == favorite.stationUuid }
                    ?.toDomain()
                    ?.copy(isFavorite = true)
                    ?: run {
                        // Otherwise fetch from API (simplified - in real app cache these)
                        Station(
                            uuid = favorite.stationUuid,
                            name = "Station ${favorite.stationUuid.take(8)}",
                            streamUrl = "",
                            isFavorite = true,
                        )
                    }
            }
        }
    }

    override fun isFavorite(uuid: String): Flow<Boolean> {
        return favoriteDao.isFavorite(uuid)
    }

    override suspend fun toggleFavorite(station: Station) {
        val isFav = favoriteDao.isFavorite(station.uuid).first()
        if (isFav) {
            favoriteDao.deleteByUuid(station.uuid)
        } else {
            favoriteDao.addFavorite(station.toFavoriteEntity())
        }
    }

    override suspend fun reorderFavorites(stationIds: List<String>) {
        stationIds.forEachIndexed { index, uuid ->
            favoriteDao.addFavorite(FavoriteEntity(uuid, sortOrder = index))
        }
    }
}
