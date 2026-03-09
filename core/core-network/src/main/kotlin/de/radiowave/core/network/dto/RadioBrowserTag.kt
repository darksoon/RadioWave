// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for genre/tag from Radio Browser API.
 */
@Serializable
data class RadioBrowserTag(
    val name: String,
    @SerialName("stationcount")
    val stationCount: Int,
)

