// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import de.darksoon.radiowave.core.model.Country
import de.darksoon.radiowave.core.model.Genre
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for station operations.
 */
interface StationRepository {
    fun searchStations(query: String): Flow<List<Station>>
    fun getTopStations(): Flow<List<Station>>
    fun getStationsByCountry(countryCode: String): Flow<List<Station>>
    fun getStationsByTag(tag: String): Flow<List<Station>>
    suspend fun getStationVariants(station: Station): List<Station>
    suspend fun getSimilarStations(station: Station): List<Station>
    fun getTags(): Flow<List<Genre>>
    fun getCountries(): Flow<List<Country>>
    suspend fun registerClick(stationUuid: String)
    suspend fun reportBrokenStream(stationUuid: String)
}

