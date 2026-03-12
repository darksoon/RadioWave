// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.darksoon.radiowave.core.database.entity.StationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for cached stations.
 */
@Dao
interface StationDao {
    @Query("SELECT * FROM stations ORDER BY cachedAt DESC")
    fun getAllStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE uuid = :uuid LIMIT 1")
    suspend fun getStationByUuid(uuid: String): StationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: StationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<StationEntity>)

    @Query("DELETE FROM stations WHERE uuid = :uuid")
    suspend fun deleteStation(uuid: String)

    @Query("DELETE FROM stations")
    suspend fun deleteAllStations()

    @Query("SELECT * FROM stations WHERE name LIKE '%' || :query || '%' ORDER BY cachedAt DESC LIMIT :limit")
    fun searchStations(query: String, limit: Int = 50): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getLatestStations(limit: Int = 200): List<StationEntity>

    @Query("SELECT * FROM stations WHERE LOWER(countryCode) = LOWER(:countryCode) ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getStationsByCountryCode(countryCode: String, limit: Int = 200): List<StationEntity>

    @Query("SELECT * FROM stations WHERE tags LIKE '%' || :tag || '%' ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getStationsByTag(tag: String, limit: Int = 200): List<StationEntity>
}

