// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for cached stations from the API.
 */
@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey
    val uuid: String,
    val name: String,
    val streamUrl: String,
    val homepageUrl: String? = null,
    val faviconUrl: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val language: String? = null,
    val tags: String? = null, // Comma-separated
    val codec: String? = null,
    val bitrate: Int? = null,
    val cachedAt: Long = System.currentTimeMillis(),
)

