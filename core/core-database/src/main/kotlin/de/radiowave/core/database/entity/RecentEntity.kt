package de.radiowave.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for recently played stations.
 */
@Entity(tableName = "recent_stations")
data class RecentEntity(
    @PrimaryKey
    val stationUuid: String,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1,
)
