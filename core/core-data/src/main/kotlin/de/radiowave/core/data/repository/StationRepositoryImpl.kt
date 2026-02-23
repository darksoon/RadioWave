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
        try {
            val stations = api.searchByName(query).map { it.toDomain() }
            emit(stations)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getTopStations(): Flow<List<Station>> = flow {
        try {
            val stations = api.getTopStations(100).map { it.toDomain() }
            emit(stations)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getStationsByCountry(countryCode: String): Flow<List<Station>> = flow {
        try {
            val stations = api.searchByCountry(countryCode).map { it.toDomain() }
            emit(stations)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getStationsByTag(tag: String): Flow<List<Station>> = flow {
        try {
            val stations = api.searchByTag(tag).map { it.toDomain() }
            emit(stations)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getTags(): Flow<List<Genre>> = flow {
        try {
            val tags = api.getTags().map { it.toDomain() }
            emit(tags)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getCountries(): Flow<List<Country>> = flow {
        try {
            val countries = api.getCountries().map { it.toDomain() }
            emit(countries)
        } catch (e: Exception) {
            emit(emptyList())
        }
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
}
