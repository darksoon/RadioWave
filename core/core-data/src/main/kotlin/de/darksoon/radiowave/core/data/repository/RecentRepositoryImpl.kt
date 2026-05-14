// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import de.darksoon.radiowave.core.data.mapper.toDomain
import de.darksoon.radiowave.core.data.mapper.toRecentEntity
import de.darksoon.radiowave.core.database.dao.CustomStationDao
import de.darksoon.radiowave.core.database.dao.RecentDao
import de.darksoon.radiowave.core.database.entity.RecentEntity
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentRepositoryImpl @Inject constructor(
    private val recentDao: RecentDao,
    private val customStationDao: CustomStationDao,
) : RecentRepository {

    override fun getRecentStations(limit: Int): Flow<List<Station>> {
        // Combine recents with all custom stations in a single Flow merge instead of
        // running N suspend queries (one per recent entry) on every emission.
        // This eliminates the N+1 DAO call pattern and reduces DB pressure.
        return recentDao.getRecent(limit).combine(
            customStationDao.getAllCustomStations(),
        ) { recentList, customs ->
            val byUuid = customs.associateBy { it.uuid }
            recentList.map { recent ->
                byUuid[recent.stationUuid]
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

