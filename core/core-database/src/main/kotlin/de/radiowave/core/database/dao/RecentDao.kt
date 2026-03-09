// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import de.radiowave.core.database.entity.RecentEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for recently played stations.
 */
@Dao
interface RecentDao {
    @Query("SELECT * FROM recent_stations ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<RecentEntity>>

    @Query("SELECT * FROM recent_stations WHERE stationUuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): RecentEntity?

    @Upsert
    suspend fun upsertRecent(recent: RecentEntity)

    @Query("DELETE FROM recent_stations")
    suspend fun clearHistory()

    @Query("DELETE FROM recent_stations WHERE stationUuid = :uuid")
    suspend fun deleteByUuid(uuid: String)
}

