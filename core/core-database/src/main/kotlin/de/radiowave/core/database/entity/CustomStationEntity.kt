package de.radiowave.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for custom stations added by the user.
 */
@Entity(tableName = "custom_stations")
data class CustomStationEntity(
    @PrimaryKey
    val uuid: String,
    val name: String,
    val streamUrl: String,
    val homepageUrl: String? = null,
    val faviconUrl: String? = null,
    val genre: String? = null,
    val country: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
)
