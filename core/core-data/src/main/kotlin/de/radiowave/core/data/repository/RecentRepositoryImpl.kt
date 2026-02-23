package de.radiowave.core.data.repository

import de.radiowave.core.data.mapper.toDomain
import de.radiowave.core.data.mapper.toRecentEntity
import de.radiowave.core.database.dao.CustomStationDao
import de.radiowave.core.database.dao.RecentDao
import de.radiowave.core.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentRepositoryImpl @Inject constructor(
    private val recentDao: RecentDao,
    private val customStationDao: CustomStationDao,
) : RecentRepository {

    override fun getRecentStations(limit: Int): Flow<List<Station>> {
        return recentDao.getRecent(limit)
            .map { recentList ->
                // In a real implementation, you'd join with station data
                // For now, creating placeholder stations
                recentList.map { recent ->
                    Station(
                        uuid = recent.stationUuid,
                        name = "Recent Station",
                        streamUrl = "",
                        lastPlayedAt = recent.lastPlayedAt,
                    )
                }
            }
    }

    override suspend fun clearHistory() {
        recentDao.clearHistory()
    }
}
