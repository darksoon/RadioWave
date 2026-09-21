// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import de.darksoon.radiowave.core.data.mapper.toDomain
import de.darksoon.radiowave.core.data.mapper.toFavoriteEntity
import de.darksoon.radiowave.core.data.policy.StationInputValidator
import de.darksoon.radiowave.core.database.dao.CustomStationDao
import de.darksoon.radiowave.core.database.dao.FavoriteDao
import de.darksoon.radiowave.core.database.entity.FavoriteEntity
import de.darksoon.radiowave.core.model.FavoritesExport
import de.darksoon.radiowave.core.model.FavoritesImportOutcome
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val customStationDao: CustomStationDao,
    private val json: Json,
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
                            addedAt = favorite.addedAt,
                        )
                    }
            }
        }
    }

    override fun isFavorite(uuid: String): Flow<Boolean> {
        return favoriteDao.isFavorite(uuid)
    }

    override suspend fun toggleFavorite(station: Station) {
        // Delegated to a single @Transaction-wrapped DAO call so the check-then-act can't
        // race with a concurrent toggle of the same station (see FavoriteDao.toggleFavorite).
        favoriteDao.toggleFavorite(station.toFavoriteEntity())
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

    override suspend fun exportFavoritesJson(): String {
        val favorites = getFavorites().firstOrNull().orEmpty()
        val export = FavoritesExport(
            exportedAt = System.currentTimeMillis(),
            stations = favorites,
        )
        return json.encodeToString(FavoritesExport.serializer(), export)
    }

    override suspend fun importFavoritesJson(jsonString: String): FavoritesImportOutcome {
        // Callers read the file with StationInputValidator.readBoundedText; this is the
        // backstop for any other caller (UTF-8 is at least one byte per char).
        if (jsonString.length > StationInputValidator.MAX_IMPORT_BYTES) {
            return FavoritesImportOutcome.ParseError
        }
        val export = try {
            json.decodeFromString(FavoritesExport.serializer(), jsonString)
        } catch (_: SerializationException) {
            return FavoritesImportOutcome.ParseError
        } catch (_: IllegalArgumentException) {
            return FavoritesImportOutcome.ParseError
        }

        if (export.version > FavoritesExport.CURRENT_VERSION) {
            return FavoritesImportOutcome.UnsupportedVersion(export.version)
        }

        var imported = 0
        var skippedExisting = 0
        // Entries beyond the cap count as skipped so the result still adds up.
        var skippedInvalid = (export.stations.size - StationInputValidator.MAX_IMPORT_ENTRIES).coerceAtLeast(0)
        var skippedBlocked = 0
        for (station in export.stations.take(StationInputValidator.MAX_IMPORT_ENTRIES)) {
            // Same gate as the manual "add custom station" flow and the playlist import:
            // http(s) URL with a host, length limits, content-policy blocklist. Bad
            // entries are skipped individually rather than failing the whole import.
            val valid = when (val result = StationInputValidator.validate(station)) {
                is StationInputValidator.Result.Valid -> result.station
                StationInputValidator.Result.Invalid -> {
                    skippedInvalid++
                    continue
                }
                StationInputValidator.Result.Blocked -> {
                    skippedInvalid++
                    skippedBlocked++
                    continue
                }
            }
            val added = favoriteDao.addFavoriteIfAbsent(valid.toFavoriteEntity())
            if (added) imported++ else skippedExisting++
        }
        return FavoritesImportOutcome.Success(
            imported = imported,
            skippedExisting = skippedExisting,
            skippedInvalid = skippedInvalid,
            skippedBlocked = skippedBlocked,
        )
    }
}
