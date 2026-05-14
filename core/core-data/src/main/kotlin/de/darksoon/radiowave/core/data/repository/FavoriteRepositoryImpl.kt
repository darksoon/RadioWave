// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import de.darksoon.radiowave.core.data.mapper.toDomain
import de.darksoon.radiowave.core.data.mapper.toFavoriteEntity
import de.darksoon.radiowave.core.database.dao.CustomStationDao
import de.darksoon.radiowave.core.database.dao.FavoriteDao
import de.darksoon.radiowave.core.database.entity.FavoriteEntity
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val customStationDao: CustomStationDao,
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
                        Station(
                            uuid = favorite.stationUuid,
                            name = favorite.stationName,
                            streamUrl = favorite.streamUrl,
                            homepageUrl = favorite.homepageUrl,
                            faviconUrl = favorite.faviconUrl,
                            country = favorite.country,
                            countryCode = favorite.countryCode,
                            language = favorite.language,
                            tags = favorite.tags
                                ?.split(",")
                                ?.map { tag -> tag.trim() }
                                ?.filter { tag -> tag.isNotEmpty() }
                                ?: emptyList(),
                            codec = favorite.codec,
                            bitrate = favorite.bitrate,
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
            // Use a dedicated MAX query instead of loading the full favorites list —
            // avoids opening/closing a Flow subscription and scanning all rows.
            val nextSortOrder = (favoriteDao.getMaxSortOrder() ?: -1) + 1
            favoriteDao.addFavorite(
                station.toFavoriteEntity().copy(sortOrder = nextSortOrder),
            )
        }
    }

    override suspend fun reorderFavorites(stationIds: List<String>) {
        val currentFavorites = favoriteDao.getAllFavorites().firstOrNull().orEmpty()
        val favoriteByUuid = currentFavorites.associateBy { favorite -> favorite.stationUuid }
        stationIds.forEachIndexed { index, uuid ->
            val currentFavorite = favoriteByUuid[uuid] ?: return@forEachIndexed
            favoriteDao.addFavorite(
                FavoriteEntity(
                    stationUuid = uuid,
                    stationName = currentFavorite.stationName,
                    streamUrl = currentFavorite.streamUrl,
                    homepageUrl = currentFavorite.homepageUrl,
                    faviconUrl = currentFavorite.faviconUrl,
                    country = currentFavorite.country,
                    countryCode = currentFavorite.countryCode,
                    language = currentFavorite.language,
                    tags = currentFavorite.tags,
                    codec = currentFavorite.codec,
                    bitrate = currentFavorite.bitrate,
                    addedAt = currentFavorite.addedAt,
                    sortOrder = index,
                ),
            )
        }
    }
}

