package de.radiowave.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for favorite stations.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val stationUuid: String,
    val addedAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
)
