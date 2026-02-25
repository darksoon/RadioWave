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
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstStationRepository @Inject constructor(
    private val api: RadioBrowserApi,
    private val stationDao: StationDao,
) : StationRepository {

    override fun searchStations(query: String): Flow<List<Station>> = flow {
        emit(api.searchByName(query).map { it.toDomain() })
    }

    override fun getTopStations(): Flow<List<Station>> = flow {
        emit(api.getTopStations(100).map { it.toDomain() })
    }

    override fun getStationsByCountry(countryCode: String): Flow<List<Station>> = flow {
        emit(api.searchByCountry(countryCode).map { it.toDomain() })
    }

    override fun getStationsByTag(tag: String): Flow<List<Station>> = flow {
        emit(api.searchByTag(tag).map { it.toDomain() })
    }

    override fun getTags(): Flow<List<Genre>> = flow {
        emit(api.getTags().map { it.toDomain() })
    }

    override fun getCountries(): Flow<List<Country>> = flow {
        emit(api.getCountries().map { it.toDomain() })
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
