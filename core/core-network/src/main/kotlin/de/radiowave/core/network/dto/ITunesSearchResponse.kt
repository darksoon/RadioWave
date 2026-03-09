// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for iTunes Search API response.
 */
@Serializable
data class ITunesSearchResponse(
    @SerialName("resultCount")
    val resultCount: Int = 0,
    val results: List<ITunesSearchResult> = emptyList(),
)
