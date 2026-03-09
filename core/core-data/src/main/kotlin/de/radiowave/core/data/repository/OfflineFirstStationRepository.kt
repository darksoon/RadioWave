// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.data.repository

import de.radiowave.core.data.mapper.toDomain
import de.radiowave.core.data.mapper.toEntity
import de.radiowave.core.database.dao.StationDao
import de.radiowave.core.model.Country
import de.radiowave.core.model.Genre
import de.radiowave.core.model.Station
import de.radiowave.core.network.RadioBrowserApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        if (normalizedQuery.isBlank()) {
            emit(emptyList())
            return@flow
        }

        val now = System.currentTimeMillis()
        var hasEmitted = false

        val local = runCatching {
            stationDao.searchStations(normalizedQuery, limit = searchResultLimit)
                .first()
                .map { it.toDomain() }
        }.getOrDefault(emptyList())
        if (local.isNotEmpty()) {
            emit(local)
            hasEmitted = true
        }

        val cached = searchCache[normalizedQuery]
        if (cached != null && now - cached.timestampMs <= searchCacheTtlMs) {
            val merged = mergeByUuid(primary = cached.stations, secondary = local)
            if (merged.isNotEmpty() || !hasEmitted) {
                emit(merged)
            }
            return@flow
        }

        val fresh = runCatching { api.searchByName(query).map { it.toDomain() } }
            .onSuccess { stations ->
                cacheStations(stations)
            }
            .getOrDefault(cached?.stations.orEmpty())
        searchCache[normalizedQuery] = SearchCacheEntry(
            timestampMs = now,
            stations = fresh,
        )
        val merged = mergeByUuid(primary = fresh, secondary = local)
        if (merged.isNotEmpty() || !hasEmitted) {
            emit(merged)
        }
    }

    override fun getTopStations(): Flow<List<Station>> = flow {
        val local = runCatching {
            stationDao.getLatestStations(limit = topStationsLimit).map { it.toDomain() }
        }.getOrDefault(emptyList())
        if (local.isNotEmpty()) {
            emit(local)
        }

        val fresh = runCatching { api.getTopStations(100).map { it.toDomain() } }
            .onSuccess { stations ->
                cacheStations(stations)
            }
            .getOrDefault(local)
        emit(mergeByUuid(primary = fresh, secondary = local))
    }

    override fun getStationsByCountry(countryCode: String): Flow<List<Station>> = flow {
        val normalizedCountry = countryCode.trim().uppercase()
        val local = runCatching {
            stationDao.getStationsByCountryCode(
                countryCode = normalizedCountry,
                limit = countryStationsLimit,
            ).map { it.toDomain() }
        }.getOrDefault(emptyList())
        if (local.isNotEmpty()) {
            emit(local)
        }

        val fresh = runCatching { api.searchByCountry(countryCode).map { it.toDomain() } }
            .onSuccess { stations ->
                cacheStations(stations)
            }
            .getOrDefault(local)
        emit(mergeByUuid(primary = fresh, secondary = local))
    }

    override fun getStationsByTag(tag: String): Flow<List<Station>> = flow {
        val normalizedTag = tag.trim().lowercase()
        val local = runCatching {
            stationDao.getStationsByTag(
                tag = normalizedTag,
                limit = tagStationsLimit,
            ).map { it.toDomain() }
        }.getOrDefault(emptyList())
        if (local.isNotEmpty()) {
            emit(local)
        }

        val fresh = runCatching { api.searchByTag(tag).map { it.toDomain() } }
            .onSuccess { stations ->
                cacheStations(stations)
            }
            .getOrDefault(local)
        emit(mergeByUuid(primary = fresh, secondary = local))
    }

    override suspend fun getStationVariants(station: Station): List<Station> {
        val stationName = station.name.trim()
        if (stationName.isBlank()) return listOf(station)
        val normalizedName = normalizeName(stationName)

        val remoteCandidates = runCatching {
            api.searchByNameExact(
                name = stationName,
                limit = 100,
            ).map { it.toDomain() }
        }.getOrDefault(emptyList())

        val exactNameCandidates = remoteCandidates.filter {
            normalizeName(it.name) == normalizedName
        }
        val withCountryPreference = station.countryCode
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { country ->
                val sameCountry = exactNameCandidates.filter { candidate ->
                    candidate.countryCode.equals(country, ignoreCase = true)
                }
                if (sameCountry.isNotEmpty()) sameCountry else exactNameCandidates
            }
            ?: exactNameCandidates

        return (listOf(station) + withCountryPreference)
            .filter { it.streamUrl.isNotBlank() }
            .distinctBy { it.streamUrl }
            .ifEmpty { listOf(station) }
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

    private fun normalizeName(value: String): String {
        return value.trim().lowercase()
    }

    private suspend fun cacheStations(stations: List<Station>) {
        if (stations.isEmpty()) return
        runCatching { stationDao.insertStations(stations.map { it.toEntity() }) }
    }

    private fun mergeByUuid(primary: List<Station>, secondary: List<Station>): List<Station> {
        return (primary + secondary)
            .filter { it.uuid.isNotBlank() }
            .distinctBy { it.uuid }
            .take(mergeResultLimit)
    }

    private companion object {
        const val searchCacheTtlMs: Long = 15 * 60 * 1000L
        const val searchResultLimit = 120
        const val topStationsLimit = 100
        const val countryStationsLimit = 160
        const val tagStationsLimit = 160
        const val mergeResultLimit = 180
        val searchCache: ConcurrentHashMap<String, SearchCacheEntry> = ConcurrentHashMap()
    }
}

