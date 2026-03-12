// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for country from Radio Browser API.
 */
@Serializable
data class RadioBrowserCountry(
    val name: String,
    @SerialName("iso_3166_1")
    val code: String,
    @SerialName("stationcount")
    val stationCount: Int,
)

