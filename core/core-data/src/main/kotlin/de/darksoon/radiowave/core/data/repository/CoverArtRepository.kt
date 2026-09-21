// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

/**
 * Repository interface for fetching cover art from external APIs.
 */
interface CoverArtRepository {
    /**
     * Fetches cover art URL for the given artist and title.
     * Returns null if no matching cover art is found, the lookup failed, or online cover
     * lookup is disabled in the settings.
     * Hits are cached in memory; "no match" answers expire, failures are never cached.
     */
    suspend fun fetchCoverArt(artist: String?, title: String?): String?

    /**
     * Clears the in-memory cache.
     */
    fun clearCache()
}
