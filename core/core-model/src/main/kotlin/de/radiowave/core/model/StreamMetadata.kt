// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.model

/**
 * Represents metadata from the audio stream (e.g., ICY metadata).
 */
data class StreamMetadata(
    val title: String? = null,
    val artist: String? = null,
    val albumArtUrl: String? = null,
)

