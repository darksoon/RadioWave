// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    /** One-shot (non-Flow) variant for use inside [toggleFavorite]'s transaction. */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE stationUuid = :uuid)")
    suspend fun isFavoriteOnce(uuid: String): Boolean

    @Query("DELETE FROM favorites WHERE stationUuid = :uuid")
    suspend fun deleteByUuid(uuid: String)

    /** Returns the current maximum sortOrder, or null if no favorites exist. */
    @Query("SELECT MAX(sortOrder) FROM favorites")
    suspend fun getMaxSortOrder(): Int?

    /**
     * Atomically checks the current favorite state and flips it. `@Transaction` wraps this
     * whole default body in one SQLite transaction, so two overlapping calls for the same
     * station (e.g. a rapid double-tap, or Android Auto and the phone UI toggling at nearly
     * the same moment) can no longer both read the same pre-toggle state and take the same
     * branch — the second call always sees the first call's committed write.
     */
    @Transaction
    suspend fun toggleFavorite(favorite: FavoriteEntity) {
        if (isFavoriteOnce(favorite.stationUuid)) {
            deleteByUuid(favorite.stationUuid)
        } else {
            val nextSortOrder = (getMaxSortOrder() ?: -1) + 1
            addFavorite(favorite.copy(sortOrder = nextSortOrder))
        }
    }
}

