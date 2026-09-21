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

    @Query("SELECT * FROM stations WHERE name = :name COLLATE NOCASE LIMIT 100")
    suspend fun getStationsByExactName(name: String): List<StationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: StationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<StationEntity>)

    @Query("DELETE FROM stations WHERE uuid = :uuid")
    suspend fun deleteStation(uuid: String)

    @Query("DELETE FROM stations")
    suspend fun deleteAllStations()

    @Query("SELECT * FROM stations WHERE name LIKE '%' || :query || '%' ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun searchStations(query: String, limit: Int = 50): List<StationEntity>

    @Query("SELECT * FROM stations ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getLatestStations(limit: Int = 200): List<StationEntity>

    @Query("SELECT * FROM stations WHERE LOWER(countryCode) = LOWER(:countryCode) ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getStationsByCountryCode(countryCode: String, limit: Int = 200): List<StationEntity>

    @Query("SELECT * FROM stations WHERE tags LIKE '%' || :tag || '%' ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getStationsByTag(tag: String, limit: Int = 200): List<StationEntity>

    /** Lightweight uuid+name projection for the in-memory fuzzy-search index. */
    @Query("SELECT uuid, name FROM stations")
    suspend fun getNameIndex(): List<StationNameRow>

    @Query("SELECT * FROM stations WHERE uuid IN (:uuids)")
    suspend fun getStationsByUuids(uuids: List<String>): List<StationEntity>

    @Query("SELECT COUNT(*) FROM stations")
    suspend fun countStations(): Int
}

/** Projection row for [StationDao.getNameIndex]. */
data class StationNameRow(
    val uuid: String,
    val name: String,
)
