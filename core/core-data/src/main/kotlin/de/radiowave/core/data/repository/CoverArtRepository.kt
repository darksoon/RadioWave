// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.data.repository

/**
 * Repository interface for fetching cover art from external APIs.
 */
interface CoverArtRepository {
    /**
     * Fetches cover art URL for the given artist and title.
     * Returns null if no cover art is found.
     * Results are cached in memory.
     */
    suspend fun fetchCoverArt(artist: String?, title: String?): String?

    /**
     * Clears the in-memory cache.
     */
    fun clearCache()
}
