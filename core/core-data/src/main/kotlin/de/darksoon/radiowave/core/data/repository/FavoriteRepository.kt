// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import de.darksoon.radiowave.core.model.FavoritesImportOutcome
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for favorite operations.
 */
interface FavoriteRepository {
    fun getFavorites(): Flow<List<Station>>
    fun isFavorite(uuid: String): Flow<Boolean>
    suspend fun toggleFavorite(station: Station)

    /**
     * Removes the favorite with [uuid] (no-op when it is not a favorite).
     *
     * REA 95fcb33 (p000/l00.java Fall 0 → c91.m1540c = FavoriteDao.deleteByUuid): Browse entfernt
     * damit alle Favoriten-Varianten desselben Senders. REA-UNSICHER: Name geraten, R8 hat die
     * Methode in den Aufrufer inline'd.
     */
    suspend fun removeFavorite(uuid: String)
    suspend fun reorderFavorites(stationIds: List<String>)

    /** Serializes the current favorites (in display order) to a FavoritesExport JSON string. */
    suspend fun exportFavoritesJson(): String

    /**
     * Parses [jsonString] as a favorites export and merges it into the local favorites: existing
     * favorites (matched by [Station.uuid]) are left untouched, stations missing required
     * fields are skipped individually, and everything else is added. Never throws — parse
     * failures are reported via [FavoritesImportOutcome.ParseError].
     */
    suspend fun importFavoritesJson(jsonString: String): FavoritesImportOutcome
}

