// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.data.repository

import de.radiowave.core.model.Country
import de.radiowave.core.model.Genre
import de.radiowave.core.model.Station
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
    fun getTags(): Flow<List<Genre>>
    fun getCountries(): Flow<List<Country>>
    suspend fun registerClick(stationUuid: String)
    suspend fun reportBrokenStream(stationUuid: String)
}

