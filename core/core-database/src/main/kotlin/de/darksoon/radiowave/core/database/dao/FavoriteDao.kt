// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.darksoon.radiowave.core.database.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for favorite stations.
 */
@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY sortOrder ASC, addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun removeFavorite(favorite: FavoriteEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE stationUuid = :uuid)")
    fun isFavorite(uuid: String): Flow<Boolean>

    @Query("DELETE FROM favorites WHERE stationUuid = :uuid")
    suspend fun deleteByUuid(uuid: String)
}

