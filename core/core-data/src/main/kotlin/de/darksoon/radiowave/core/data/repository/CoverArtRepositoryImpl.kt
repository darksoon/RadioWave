// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import de.darksoon.radiowave.core.network.ITunesSearchApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [CoverArtRepository] that uses the iTunes Search API.
 * Includes an in-memory cache to avoid redundant API calls.
 */
@Singleton
class CoverArtRepositoryImpl @Inject constructor(
    private val iTunesSearchApi: ITunesSearchApi,
) : CoverArtRepository {

    private val cache = mutableMapOf<String, String?>()
    private val cacheLock = Any()

    override suspend fun fetchCoverArt(artist: String?, title: String?): String? {
        if (artist.isNullOrBlank() || title.isNullOrBlank()) {
            return null
        }

        val cacheKey = "${artist.trim().lowercase()}|${title.trim().lowercase()}"

        synchronized(cacheLock) {
            cache[cacheKey]?.let { return it }
        }

        return try {
            val searchTerm = "${artist.trim()} ${title.trim()}"
            val response = iTunesSearchApi.search(term = searchTerm)

            val artworkUrl = response.results
                .firstOrNull()
                ?.artworkUrl100
                ?.replace("100x100", "600x600")

            synchronized(cacheLock) {
                cache[cacheKey] = artworkUrl
            }

            artworkUrl
        } catch (e: Exception) {
            synchronized(cacheLock) {
                cache[cacheKey] = null
            }
            null
        }
    }

    override fun clearCache() {
        synchronized(cacheLock) {
            cache.clear()
        }
    }
}
