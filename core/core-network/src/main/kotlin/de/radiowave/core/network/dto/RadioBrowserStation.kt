package de.radiowave.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for radio station from Radio Browser API.
 */
@Serializable
data class RadioBrowserStation(
    @SerialName("stationuuid")
    val uuid: String,
    val name: String,
    @SerialName("url_resolved")
    val urlResolved: String,
    val url: String,
    val homepage: String? = null,
    val favicon: String? = null,
    val country: String? = null,
    @SerialName("countrycode")
    val countryCode: String? = null,
    val language: String? = null,
    val tags: String? = null,
    val codec: String? = null,
    val bitrate: Int? = null,
    @SerialName("clickcount")
    val clickCount: Int? = null,
    val votes: Int? = null,
)
