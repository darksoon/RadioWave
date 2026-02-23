package de.radiowave.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.radiowave.core.database.entity.StationEntity
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
}
