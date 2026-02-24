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
    val stationName: String,
    val streamUrl: String,
    val homepageUrl: String? = null,
    val faviconUrl: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val language: String? = null,
    val tags: String? = null,
    val codec: String? = null,
    val bitrate: Int? = null,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1,
)
