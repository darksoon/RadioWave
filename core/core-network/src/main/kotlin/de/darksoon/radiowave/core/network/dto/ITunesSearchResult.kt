// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for a single iTunes search result.
 */
@Serializable
data class ITunesSearchResult(
    @SerialName("artworkUrl600")
    val artworkUrl600: String? = null,
    @SerialName("artworkUrl100")
    val artworkUrl100: String? = null,
    @SerialName("trackName")
    val trackName: String? = null,
    @SerialName("artistName")
    val artistName: String? = null,
    @SerialName("collectionName")
    val collectionName: String? = null,
)
