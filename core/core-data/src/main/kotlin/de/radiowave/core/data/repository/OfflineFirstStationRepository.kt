package de.radiowave.core.data.repository

import de.radiowave.core.data.mapper.toDomain
import de.radiowave.core.data.mapper.toEntity
import de.radiowave.core.database.dao.StationDao
import de.radiowave.core.model.Country
import de.radiowave.core.model.Genre
import de.radiowave.core.model.Station
import de.radiowave.core.network.RadioBrowserApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstStationRepository @Inject constructor(
    private val api: RadioBrowserApi,
    private val stationDao: StationDao,
) : StationRepository {

    override fun searchStations(query: String): Flow<List<Station>> = flow {
        val normalizedQuery = query.trim().lowercase()
        val now = System.currentTimeMillis()
        val cached = searchCache[normalizedQuery]
        if (cached != null && now - cached.timestampMs <= searchCacheTtlMs) {
            emit(cached.stations)
            return@flow
        }

        val fresh = runCatching { api.searchByName(query).map { it.toDomain() } }
            .getOrDefault(cached?.stations.orEmpty())
        searchCache[normalizedQuery] = SearchCacheEntry(
            timestampMs = now,
            stations = fresh,
        )
        emit(fresh)
    }

    override fun getTopStations(): Flow<List<Station>> = flow {
        emit(runCatching { api.getTopStations(100).map { it.toDomain() } }.getOrDefault(emptyList()))
    }

    override fun getStationsByCountry(countryCode: String): Flow<List<Station>> = flow {
        emit(runCatching { api.searchByCountry(countryCode).map { it.toDomain() } }.getOrDefault(emptyList()))
    }

    override fun getStationsByTag(tag: String): Flow<List<Station>> = flow {
        emit(runCatching { api.searchByTag(tag).map { it.toDomain() } }.getOrDefault(emptyList()))
    }

    override fun getTags(): Flow<List<Genre>> = flow {
        emit(runCatching { api.getTags().map { it.toDomain() } }.getOrDefault(emptyList()))
    }

    override fun getCountries(): Flow<List<Country>> = flow {
        emit(runCatching { api.getCountries().map { it.toDomain() } }.getOrDefault(emptyList()))
    }

    override suspend fun registerClick(stationUuid: String) {
        try {
            api.registerClick(stationUuid)
        } catch (e: Exception) {
            // Silently fail - not critical
        }
    }

    override suspend fun reportBrokenStream(stationUuid: String) {
        try {
            api.reportBrokenStream(stationUuid)
        } catch (e: Exception) {
            // Silently fail - not critical
        }
    }

    private data class SearchCacheEntry(
        val timestampMs: Long,
        val stations: List<Station>,
    )

    private companion object {
        const val searchCacheTtlMs: Long = 15 * 60 * 1000L
        val searchCache: ConcurrentHashMap<String, SearchCacheEntry> = ConcurrentHashMap()
    }
}
