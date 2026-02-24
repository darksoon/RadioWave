package de.radiowave.core.data.repository

import de.radiowave.core.data.mapper.toDomain
import de.radiowave.core.data.mapper.toRecentEntity
import de.radiowave.core.database.dao.CustomStationDao
import de.radiowave.core.database.dao.RecentDao
import de.radiowave.core.database.entity.RecentEntity
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
                recentList.map { recent ->
                    customStationDao.getByUuid(recent.stationUuid)
                        ?.toDomain()
                        ?.copy(lastPlayedAt = recent.lastPlayedAt)
                        ?: recent.toStation()
                }
            }
    }

    override suspend fun addRecentStation(station: Station) {
        val existing = recentDao.getByUuid(station.uuid)
        val merged = station.toRecentEntity().copy(
            playCount = (existing?.playCount ?: 0) + 1,
        )
        recentDao.upsertRecent(merged)
    }

    override suspend fun clearHistory() {
        recentDao.clearHistory()
    }
}

private fun RecentEntity.toStation(): Station = Station(
    uuid = stationUuid,
    name = stationName,
    streamUrl = streamUrl,
    homepageUrl = homepageUrl,
    faviconUrl = faviconUrl,
    country = country,
    countryCode = countryCode,
    language = language,
    tags = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
    codec = codec,
    bitrate = bitrate,
    lastPlayedAt = lastPlayedAt,
)
