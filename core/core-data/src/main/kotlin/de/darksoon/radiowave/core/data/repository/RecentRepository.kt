// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for recent stations.
 */
interface RecentRepository {
    fun getRecentStations(limit: Int = 50): Flow<List<Station>>
    suspend fun addRecentStation(station: Station)
    suspend fun clearHistory()
}

