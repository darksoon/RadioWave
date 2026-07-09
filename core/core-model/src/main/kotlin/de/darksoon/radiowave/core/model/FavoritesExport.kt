// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.model

import kotlinx.serialization.Serializable

/**
 * On-disk JSON shape for a user-triggered favorites backup (see "Favorites export/import"
 * in the roadmap). Wrapping the station list in an envelope (rather than serializing a bare
 * `List<Station>`) gives us [version] as headroom for future format changes without breaking
 * older exports — see [FavoritesImportOutcome.UnsupportedVersion].
 */
@Serializable
data class FavoritesExport(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long,
    val stations: List<Station>,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/**
 * Result of merging an imported [FavoritesExport] into the local favorites — reported back
 * to the user so they know exactly what happened, without exposing DB/JSON internals.
 */
sealed interface FavoritesImportOutcome {
    data class Success(
        val imported: Int,
        val skippedExisting: Int,
        val skippedInvalid: Int,
    ) : FavoritesImportOutcome

    /** The file didn't parse as a [FavoritesExport] at all (corrupt/foreign JSON). */
    data object ParseError : FavoritesImportOutcome

    /** Parsed fine, but declares a newer format version than this app understands. */
    data class UnsupportedVersion(val version: Int) : FavoritesImportOutcome
}
