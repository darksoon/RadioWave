package de.radiowave.core.data.repository

import de.radiowave.core.data.mapper.toDomain
import de.radiowave.core.model.Country
import de.radiowave.core.model.Genre
import de.radiowave.core.model.Station
import de.radiowave.core.network.RadioBrowserApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationRepositoryImpl @Inject constructor(
    private val api: RadioBrowserApi,
) : StationRepository {

    override fun searchStations(query: String): Flow<List<Station>> = flow {
        emit(runCatching { api.searchByName(query).map { it.toDomain() } }.getOrDefault(emptyList()))
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

    private fun normalizeName(value: String): String {
        return value.trim().lowercase()
    }
}
