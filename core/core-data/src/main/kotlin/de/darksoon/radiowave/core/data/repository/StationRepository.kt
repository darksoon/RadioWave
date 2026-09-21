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

    // Explicit user refresh (pull-to-refresh): skips the in-memory TTL cache once,
    // still falling back to cached data when the directory fails. The defaults
    // keep simple implementations (test fakes) source-compatible.
    fun searchStations(query: String, forceRefresh: Boolean): Flow<List<Station>> = searchStations(query)
    fun getTopStations(forceRefresh: Boolean): Flow<List<Station>> = getTopStations()
    fun getStationsByCountry(countryCode: String, forceRefresh: Boolean): Flow<List<Station>> =
        getStationsByCountry(countryCode)
    fun getStationsByTag(tag: String, forceRefresh: Boolean): Flow<List<Station>> = getStationsByTag(tag)

    suspend fun getStationVariants(station: Station): List<Station>
    /** Playback must not wait for a directory request. */
    suspend fun getCachedStationVariants(station: Station): List<Station> = listOf(station)
    suspend fun getSimilarStations(station: Station): List<Station>
    fun getTags(): Flow<List<Genre>>
    fun getCountries(): Flow<List<Country>>
    suspend fun registerClick(stationUuid: String)
    suspend fun reportBrokenStream(stationUuid: String)
}
