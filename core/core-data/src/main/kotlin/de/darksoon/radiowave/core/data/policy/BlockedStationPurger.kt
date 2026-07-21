// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.policy

import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.database.dao.FavoriteDao
import de.darksoon.radiowave.core.database.dao.RecentDao
import de.darksoon.radiowave.core.database.dao.StationDao
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Removes [StationDirectoryBlocklist.blockedUuids] entries from data that reached
 * users' devices BEFORE the blocklist existed: the cached `stations` table,
 * favorites, and listening history. Read-path filtering keeps unknown future
 * entries out of the UI; this purge closes the gap for known ones that are
 * already stored locally. Runs on every app start — cheap deletes by UUID.
 */
@Singleton
class BlockedStationPurger @Inject constructor(
    private val stationDao: StationDao,
    private val favoriteDao: FavoriteDao,
    private val recentDao: RecentDao,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun purge() {
        for (uuid in StationDirectoryBlocklist.blockedUuids) {
            runCatching {
                stationDao.deleteStation(uuid)
                favoriteDao.deleteByUuid(uuid)
                recentDao.deleteByUuid(uuid)
            }.onFailure {
                android.util.Log.w("RadioWave", "Blocked-station purge failed: ${it.message}")
            }
        }
        clearBlockedLastStation()
    }

    /**
     * The "continue where you left off" restore (phone + Android Auto) rebuilds a
     * Station from DataStore, bypassing the directory read paths — so a blocked
     * station stored there pre-blocklist must be cleared too. Full rule match, not
     * just UUIDs, because the stored copy carries name/URLs/country.
     */
    private suspend fun clearBlockedLastStation() {
        runCatching {
            val settings = settingsRepository.data.first()
            val streamUrl = settings.lastStationStreamUrl ?: return
            val lastStation = Station(
                uuid = settings.lastStationUuid.orEmpty(),
                name = settings.lastStationName.orEmpty(),
                streamUrl = streamUrl,
                faviconUrl = settings.lastStationFaviconUrl,
                country = settings.lastStationCountry,
            )
            if (StationDirectoryBlocklist.isBlocked(lastStation)) {
                settingsRepository.setLastStation(
                    uuid = null,
                    name = null,
                    streamUrl = null,
                    faviconUrl = null,
                    country = null,
                )
            }
        }.onFailure {
            android.util.Log.w("RadioWave", "Blocked last-station check failed: ${it.message}")
        }
    }
}
