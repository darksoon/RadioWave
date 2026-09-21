// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import android.os.SystemClock
import de.darksoon.radiowave.core.network.ITunesSearchApi
import de.darksoon.radiowave.core.network.dto.ITunesSearchResult
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Implementation of [CoverArtRepository] that uses the iTunes Search API.
 * Includes an in-memory cache to avoid redundant API calls.
 *
 * Privacy: every lookup sends the song's artist and title to Apple. Returns null without
 * any request when the user turned off [AppSettingsState.onlineCoverLookup], so callers
 * (player controller, player screen) need no guard of their own.
 */
@Singleton
class CoverArtRepositoryImpl internal constructor(
    private val iTunesSearchApi: ITunesSearchApi,
    private val settingsRepository: SettingsRepository,
    private val elapsedRealtimeMs: () -> Long,
) : CoverArtRepository {

    @Inject constructor(
        iTunesSearchApi: ITunesSearchApi,
        settingsRepository: SettingsRepository,
    ) : this(iTunesSearchApi, settingsRepository, SystemClock::elapsedRealtime)

    /** [artworkUrl] null = iTunes answered but had no matching artwork ("no match"). */
    private class CacheEntry(val artworkUrl: String?, val storedAtMs: Long)

    // Bounded, access-ordered LRU — a @Singleton in a radio app that streams for
    // hours would otherwise grow by one entry per unique song forever (incl. the
    // negative-cache entries). 300 entries ≈ many hours of listening across stations
    // while keeping re-lookups for recently repeated tracks free.
    private val cache = object : LinkedHashMap<String, CacheEntry>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>): Boolean =
            size > MAX_CACHE_ENTRIES
    }
    private val cacheLock = Any()

    internal companion object {
        const val MAX_CACHE_ENTRIES = 300

        // A "no match" is only remembered for a while — iTunes' catalogue and our matching
        // can change, and a song that repeats hours later deserves another try.
        const val NO_MATCH_TTL_MS = 30 * 60 * 1000L

        // A few candidates so a wrong-artist first hit doesn't win by default.
        const val SEARCH_RESULT_LIMIT = 5
    }

    override suspend fun fetchCoverArt(artist: String?, title: String?): String? {
        if (artist.isNullOrBlank() || title.isNullOrBlank()) {
            return null
        }
        if (!settingsRepository.data.first().onlineCoverLookup) return null

        val cacheKey = "${artist.trim().lowercase()}|${title.trim().lowercase()}"

        synchronized(cacheLock) {
            val cached = cache[cacheKey]
            if (cached != null) {
                val expired = cached.artworkUrl == null &&
                    elapsedRealtimeMs() - cached.storedAtMs >= NO_MATCH_TTL_MS
                if (!expired) return cached.artworkUrl
                cache.remove(cacheKey)
            }
        }

        val response = try {
            iTunesSearchApi.search(
                term = "${artist.trim()} ${title.trim()}",
                limit = SEARCH_RESULT_LIMIT,
            )
        } catch (e: CancellationException) {
            // Song/station changed while the lookup ran — not an answer about this song.
            throw e
        } catch (e: Exception) {
            // Offline, timeout, HTTP or parse error: not cached, so the next play retries.
            return null
        }

        val artworkUrl = CoverArtMatcher.pickArtworkUrl(artist, response.results)
        synchronized(cacheLock) {
            cache[cacheKey] = CacheEntry(artworkUrl, elapsedRealtimeMs())
        }
        return artworkUrl
    }

    override fun clearCache() {
        synchronized(cacheLock) {
            cache.clear()
        }
    }
}

/**
 * Decides whether an iTunes search hit actually belongs to the song we asked for. The
 * search is fuzzy full-text, so its first hit is often a cover version, a karaoke track or
 * an unrelated artist that merely shares a word with the title — showing that artwork is
 * worse than falling back to the station logo.
 */
internal object CoverArtMatcher {

    private val combiningMarks = Regex("\\p{Mn}+")
    private val nonAlphanumeric = Regex("[^\\p{L}\\p{N}]+")
    // Featured artists: "A feat. B", "A ft B", "A (featuring B)" -> "A".
    private val featuredArtists = Regex("[(\\[]?\\s*\\b(feat|ft|featuring)\\b.*$", RegexOption.IGNORE_CASE)

    /** First candidate by [artist] that carries artwork, upscaled to 600px, or null. */
    fun pickArtworkUrl(artist: String, results: List<ITunesSearchResult>): String? =
        results.asSequence()
            .filter { artistMatches(artist, it.artistName) }
            .mapNotNull { it.artworkUrl600 ?: it.artworkUrl100?.replace("100x100", "600x600") }
            .firstOrNull { it.isNotBlank() }

    /**
     * Case/diacritics/punctuation-insensitive; featured artists are ignored on both sides;
     * either name may contain the other as whole words ("Beatles" ~ "The Beatles",
     * "David Guetta" ~ "David Guetta & Sia").
     */
    fun artistMatches(queryArtist: String, resultArtist: String?): Boolean {
        if (resultArtist.isNullOrBlank()) return false
        val query = normalize(queryArtist.replace(featuredArtists, ""))
        val result = normalize(resultArtist.replace(featuredArtists, ""))
        if (query.isEmpty() || result.isEmpty()) return false
        return query == result || " $query ".contains(" $result ") || " $result ".contains(" $query ")
    }

    fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace("+", " and ")
            .replace(nonAlphanumeric, " ")
            .trim()
}
