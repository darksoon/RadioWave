// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.radiowave.core.database.DatabaseMigrations
import de.radiowave.core.database.RadioWaveDatabase
import de.radiowave.core.database.dao.CustomStationDao
import de.radiowave.core.database.dao.FavoriteDao
import de.radiowave.core.database.dao.RecentDao
import de.radiowave.core.database.dao.StationDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): RadioWaveDatabase {
        return Room.databaseBuilder(
            context,
            RadioWaveDatabase::class.java,
            "radiowave_database",
        )
            .addMigrations(
                DatabaseMigrations.MIGRATION_1_2,
                DatabaseMigrations.MIGRATION_2_3,
            )
            .build()
    }

    @Provides
    fun provideStationDao(database: RadioWaveDatabase): StationDao {
        return database.stationDao()
    }

    @Provides
    fun provideFavoriteDao(database: RadioWaveDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideRecentDao(database: RadioWaveDatabase): RecentDao {
        return database.recentDao()
    }

    @Provides
    fun provideCustomStationDao(database: RadioWaveDatabase): CustomStationDao {
        return database.customStationDao()
    }
}

