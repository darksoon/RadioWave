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

        // containsKey, not `cache[cacheKey]?.let { ... }` — a prior "no artwork found" lookup
        // is cached as `null`, which Map.get() can't distinguish from "never looked up". Using
        // only the null-check let every no-artwork track re-hit the network on every play,
        // defeating the whole point of caching that (most common) case.
        synchronized(cacheLock) {
            if (cache.containsKey(cacheKey)) return cache.getValue(cacheKey)
        }

        return try {
            val searchTerm = "${artist.trim()} ${title.trim()}"
            val response = iTunesSearchApi.search(term = searchTerm)

            val artworkUrl = response.results
                .firstOrNull()
                ?.let { it.artworkUrl600 ?: it.artworkUrl100?.replace("100x100", "600x600") }

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
