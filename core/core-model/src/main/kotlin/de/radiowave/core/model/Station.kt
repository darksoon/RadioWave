package de.radiowave.core.model

import kotlinx.serialization.Serializable

/**
 * Represents a radio station.
 */
@Serializable
data class Station(
    val uuid: String,
    val name: String,
    val streamUrl: String,
    val homepageUrl: String? = null,
    val faviconUrl: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val language: String? = null,
    val tags: List<String> = emptyList(),
    val codec: String? = null,
    val bitrate: Int? = null,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long? = null,
    val addedAt: Long = System.currentTimeMillis(),
)
