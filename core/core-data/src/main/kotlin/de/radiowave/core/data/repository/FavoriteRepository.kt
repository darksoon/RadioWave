// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.data.repository

import de.radiowave.core.model.Station
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for favorite operations.
 */
interface FavoriteRepository {
    fun getFavorites(): Flow<List<Station>>
    fun isFavorite(uuid: String): Flow<Boolean>
    suspend fun toggleFavorite(station: Station)
    suspend fun reorderFavorites(stationIds: List<String>)
}

