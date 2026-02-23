package de.radiowave.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import de.radiowave.core.database.dao.CustomStationDao
import de.radiowave.core.database.dao.FavoriteDao
import de.radiowave.core.database.dao.RecentDao
import de.radiowave.core.database.entity.CustomStationEntity
import de.radiowave.core.database.entity.FavoriteEntity
import de.radiowave.core.database.entity.RecentEntity
import de.radiowave.core.database.entity.StationEntity

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
    version = 1,
    exportSchema = true,
)
abstract class RadioWaveDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
    abstract fun customStationDao(): CustomStationDao
}
