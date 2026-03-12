// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.darksoon.radiowave.core.database.entity.CustomStationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for custom stations.
 */
@Dao
interface CustomStationDao {
    @Query("SELECT * FROM custom_stations ORDER BY addedAt DESC")
    fun getAllCustomStations(): Flow<List<CustomStationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomStation(station: CustomStationEntity)

    @Delete
    suspend fun deleteCustomStation(station: CustomStationEntity)

    @Query("DELETE FROM custom_stations WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)

    @Query("SELECT * FROM custom_stations WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): CustomStationEntity?
}

