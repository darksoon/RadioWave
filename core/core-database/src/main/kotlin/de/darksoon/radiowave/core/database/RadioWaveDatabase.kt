// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import de.darksoon.radiowave.core.database.dao.CustomStationDao
import de.darksoon.radiowave.core.database.dao.FavoriteDao
import de.darksoon.radiowave.core.database.dao.RecentDao
import de.darksoon.radiowave.core.database.dao.StationDao
import de.darksoon.radiowave.core.database.entity.CustomStationEntity
import de.darksoon.radiowave.core.database.entity.FavoriteEntity
import de.darksoon.radiowave.core.database.entity.RecentEntity
import de.darksoon.radiowave.core.database.entity.StationEntity

/**
 * Main database for RadioWave app.
 */
@Database(
    entities = [
        StationEntity::class,
        FavoriteEntity::class,
        RecentEntity::class,
        CustomStationEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class RadioWaveDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
    abstract fun customStationDao(): CustomStationDao
}

