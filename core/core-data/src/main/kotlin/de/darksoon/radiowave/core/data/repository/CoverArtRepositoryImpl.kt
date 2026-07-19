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

    // Bounded, access-ordered LRU — a @Singleton in a radio app that streams for
    // hours would otherwise grow by one entry per unique song forever (incl. the
    // negative-cache nulls). 300 entries ≈ many hours of listening across stations
    // while keeping re-lookups for recently repeated tracks free.
    private val cache = object : LinkedHashMap<String, String?>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String?>): Boolean =
            size > MAX_CACHE_ENTRIES
    }
    private val cacheLock = Any()

    private companion object {
        const val MAX_CACHE_ENTRIES = 300
    }

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
