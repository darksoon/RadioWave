// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import de.darksoon.radiowave.core.data.mapper.toDomain
import de.darksoon.radiowave.core.data.mapper.toEntity
import de.darksoon.radiowave.core.data.policy.StationDirectoryBlocklist
import de.darksoon.radiowave.core.database.dao.StationDao
import de.darksoon.radiowave.core.model.Country
import de.darksoon.radiowave.core.model.Genre
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.network.RadioBrowserApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstStationRepository @Inject constructor(
    private val api: RadioBrowserApi,
    private val stationDao: StationDao,
    mirrorRepository: StationMirrorRepository,
) : StationRepository {

    // ─── Directory availability (drives the "Senderverzeichnis nicht erreichbar"
    // banner in Browse). Flips false when a directory API call fails, back to true
    // on the next success. Starts optimistic.
    private val _directoryAvailable = MutableStateFlow(true)
    val directoryAvailable: StateFlow<Boolean> = _directoryAvailable.asStateFlow()

    init {
        // Rebuild the fuzzy name index right after a mirror sync instead of waiting
        // for its TTL to lapse.
        mirrorRepository.onMirrorUpdated = { invalidateNameIndex() }
    }

    override fun searchStations(query: String): Flow<List<Station>> = flow {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) {
            emit(emptyList())
            return@flow
        }

        val now = System.currentTimeMillis()
        var hasEmitted = false

        val exactLocal = runCatching {
            // suspend variant — no Flow subscription churn on every keystroke
            stationDao.searchStations(normalizedQuery, limit = searchResultLimit)
                .map { it.toDomain() }
                .scrubbed()
        }.getOrDefault(emptyList())
        // Fuzzy pass over the local name index — catches typos ("texhnobase" →
        // "TechnoBase") and diacritic variants the SQL LIKE above can't see. With the
        // full mirror enabled this covers the whole directory; without it, whatever
        // the station cache has accumulated.
        val fuzzyLocal = runCatching { fuzzySearchLocal(normalizedQuery) }.getOrDefault(emptyList())
        val local = mergeByUuid(primary = exactLocal, secondary = fuzzyLocal)
        if (local.isNotEmpty()) {
            emit(local)
            hasEmitted = true
        }

        val cached = searchCache[normalizedQuery]
        if (cached != null && now - cached.timestampMs <= searchCacheTtlMs) {
            val merged = mergeByUuid(primary = cached.stations, secondary = local)
            if (merged.isNotEmpty() || !hasEmitted) {
                emit(merged)
            }
            return@flow
        }

        val fresh = runCatching { api.searchByName(query).map { it.toDomain() }.scrubbed() }
            .onFailure {
                android.util.Log.w("RadioWave", "searchByName failed: ${it.message}")
                _directoryAvailable.value = false
            }
            .onSuccess { stations ->
                _directoryAvailable.value = true
                cacheStations(stations)
                // Refresh the in-memory search cache ONLY on success. Writing a failed
                // (empty or stale) result here with a fresh timestamp would make the entry
                // look valid for the whole TTL and suppress network retries — a transient
                // failure must not poison the cache.
                searchCache[normalizedQuery] = SearchCacheEntry(timestampMs = now, stations = stations)
            }
            .getOrDefault(cached?.stations.orEmpty())
        val merged = mergeByUuid(primary = fresh, secondary = local)
        if (merged.isNotEmpty() || !hasEmitted) {
            emit(merged)
        }
    }

    override fun getTopStations(): Flow<List<Station>> = listWithTtl(
        cacheKey = "top",
        loadLocal = { stationDao.getLatestStations(limit = topStationsLimit).map { it.toDomain() } },
        fetchRemote = { api.getTopStations(100).map { it.toDomain() } },
    )

    override fun getStationsByCountry(countryCode: String): Flow<List<Station>> = listWithTtl(
        cacheKey = "country:${countryCode.trim().uppercase()}",
        loadLocal = {
            stationDao.getStationsByCountryCode(
                countryCode = countryCode.trim().uppercase(),
                limit = countryStationsLimit,
            ).map { it.toDomain() }
        },
        fetchRemote = { api.searchByCountry(countryCode).map { it.toDomain() } },
    )

    override fun getStationsByTag(tag: String): Flow<List<Station>> = listWithTtl(
        cacheKey = "tag:${tag.trim().lowercase()}",
        loadLocal = {
            stationDao.getStationsByTag(
                tag = tag.trim().lowercase(),
                limit = tagStationsLimit,
            ).map { it.toDomain() }
        },
        fetchRemote = { api.searchByTag(tag).map { it.toDomain() } },
    )

    /**
     * Shared shape of every browse list: emit the local Room copy immediately, then
     * either serve the in-memory TTL cache (no network on tab-hopping within
     * [listCacheTtlMs]) or fetch fresh. A fetch failure keeps whatever we have,
     * never poisons the cache, and flips [directoryAvailable] for the UI banner.
     */
    private fun listWithTtl(
        cacheKey: String,
        loadLocal: suspend () -> List<Station>,
        fetchRemote: suspend () -> List<Station>,
    ): Flow<List<Station>> = flow {
        val now = System.currentTimeMillis()
        val local = runCatching { loadLocal().scrubbed() }.getOrDefault(emptyList())
        if (local.isNotEmpty()) {
            emit(local)
        }

        val cached = listCache[cacheKey]
        if (cached != null && now - cached.timestampMs <= listCacheTtlMs) {
            emit(mergeByUuid(primary = cached.stations, secondary = local))
            return@flow
        }

        val fresh = runCatching { fetchRemote().scrubbed() }
            .onFailure {
                android.util.Log.w("RadioWave", "list fetch '$cacheKey' failed: ${it.message}")
                _directoryAvailable.value = false
            }
            .onSuccess { stations ->
                _directoryAvailable.value = true
                cacheStations(stations)
                listCache[cacheKey] = SearchCacheEntry(timestampMs = now, stations = stations)
            }
            .getOrDefault(cached?.stations ?: local)
        emit(mergeByUuid(primary = fresh, secondary = local))
    }

    override suspend fun getStationVariants(station: Station): List<Station> {
        val stationName = station.name.trim()
        if (stationName.isBlank()) return listOf(station)
        val normalizedName = normalizeName(stationName)

        val remoteCandidates = runCatching {
            api.searchByNameExact(
                name = stationName,
                limit = 100,
            ).map { it.toDomain() }.scrubbed()
        }.getOrDefault(emptyList())

        val exactNameCandidates = remoteCandidates.filter {
            normalizeName(it.name) == normalizedName
        }
        val withCountryPreference = station.countryCode
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { country ->
                val sameCountry = exactNameCandidates.filter { candidate ->
                    candidate.countryCode.equals(country, ignoreCase = true)
                }
                if (sameCountry.isNotEmpty()) sameCountry else exactNameCandidates
            }
            ?: exactNameCandidates

        return (listOf(station) + withCountryPreference)
            .filter { it.streamUrl.isNotBlank() }
            .distinctBy { it.streamUrl }
            .ifEmpty { listOf(station) }
    }

    override suspend fun getSimilarStations(station: Station): List<Station> {
        val tags = station.tags.filter { it.isNotBlank() }.take(2)
        val candidates = mutableListOf<Station>()
        for (tag in tags) {
            val results = runCatching {
                api.searchByTag(tag, limit = 30).map { it.toDomain() }
            }.getOrDefault(emptyList())
            candidates += results
            if (candidates.size >= 20) break
        }
        if (candidates.isEmpty() && !station.countryCode.isNullOrBlank()) {
            candidates += runCatching {
                api.searchByCountry(station.countryCode!!, limit = 30).map { it.toDomain() }
            }.getOrDefault(emptyList())
        }
        return candidates
            .scrubbed()
            .filter { it.uuid != station.uuid }
            .distinctBy { it.uuid }
            .take(15)
    }

    // Tags and countries are quasi-static for hours — cache them in memory
    // with a 6h TTL to avoid re-fetching on every Browse navigation.
    @Volatile private var cachedTags: List<Genre>? = null
    @Volatile private var cachedTagsAtMs: Long = 0L
    @Volatile private var cachedCountries: List<Country>? = null
    @Volatile private var cachedCountriesAtMs: Long = 0L
    private val metadataTtlMs = 6 * 60 * 60 * 1000L

    override fun getTags(): Flow<List<Genre>> = flow {
        val now = System.currentTimeMillis()
        val cached = cachedTags
        if (cached != null && now - cachedTagsAtMs < metadataTtlMs) {
            emit(cached)
            return@flow
        }
        val fresh = runCatching { api.getTags().map { it.toDomain() } }
            .onFailure {
                android.util.Log.w("RadioWave", "getTags failed: ${it.message}")
                _directoryAvailable.value = false
            }
            .onSuccess { _directoryAvailable.value = true }
            .getOrDefault(emptyList())
        if (fresh.isNotEmpty()) {
            cachedTags = fresh
            cachedTagsAtMs = now
        }
        emit(fresh.ifEmpty { cached.orEmpty() })
    }

    override fun getCountries(): Flow<List<Country>> = flow {
        val now = System.currentTimeMillis()
        val cached = cachedCountries
        if (cached != null && now - cachedCountriesAtMs < metadataTtlMs) {
            emit(cached)
            return@flow
        }
        val fresh = runCatching { api.getCountries().map { it.toDomain() } }
            .onFailure {
                android.util.Log.w("RadioWave", "getCountries failed: ${it.message}")
                _directoryAvailable.value = false
            }
            .onSuccess { _directoryAvailable.value = true }
            .getOrDefault(emptyList())
        if (fresh.isNotEmpty()) {
            cachedCountries = fresh
            cachedCountriesAtMs = now
        }
        emit(fresh.ifEmpty { cached.orEmpty() })
    }

    override suspend fun registerClick(stationUuid: String) {
        try {
            api.registerClick(stationUuid)
        } catch (e: Exception) {
            // Silently fail - not critical
        }
    }

    override suspend fun reportBrokenStream(stationUuid: String) {
        try {
            api.reportBrokenStream(stationUuid)
        } catch (e: Exception) {
            // Silently fail - not critical
        }
    }

    private data class SearchCacheEntry(
        val timestampMs: Long,
        val stations: List<Station>,
    )

    private fun normalizeName(value: String): String {
        return value.trim().lowercase()
    }

    private suspend fun cacheStations(stations: List<Station>) {
        if (stations.isEmpty()) return
        runCatching { stationDao.insertStations(stations.map { it.toEntity() }) }
    }

    // Content-policy filter (see StationDirectoryBlocklist) — applied to every list
    // that reaches the UI, the caches, or Room, so blocked stations can't surface
    // through any directory path.
    private fun List<Station>.scrubbed(): List<Station> =
        filterNot(StationDirectoryBlocklist::isBlocked)

    private fun mergeByUuid(primary: List<Station>, secondary: List<Station>): List<Station> {
        return (primary + secondary)
            .filter { it.uuid.isNotBlank() }
            .distinctBy { it.uuid }
            .take(mergeResultLimit)
    }

    // Instance-level LRU cache (max 64 entries) so:
    // 1. "Clear station cache" in Settings actually clears in-memory search results.
    // 2. Memory use is bounded even after long sessions with many searches.
    private val searchCache = object : java.util.LinkedHashMap<String, SearchCacheEntry>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, SearchCacheEntry>?) = size > 64
    }

    // Browse lists (top / by-country / by-tag) — same entry shape as the search
    // cache, 30-min TTL, bounded LRU.
    private val listCache = object : java.util.LinkedHashMap<String, SearchCacheEntry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, SearchCacheEntry>?) = size > 32
    }

    /** Clears the in-memory search cache. Called from SettingsViewModel on user request. */
    fun clearSearchCache() {
        searchCache.clear()
        listCache.clear()
        invalidateNameIndex()
    }

    // ─── Fuzzy search ────────────────────────────────────────────────────────────
    //
    // In-memory (uuid, normalizedName) index over the whole Room stations table —
    // with the full mirror enabled that's the complete directory (~50k rows, a few
    // MB). Built lazily off the query path's dispatcher, rebuilt after a mirror
    // sync or when its TTL lapses so freshly cached stations become findable.

    private data class NameIndexEntry(val uuid: String, val normalizedName: String)

    @Volatile private var nameIndex: List<NameIndexEntry>? = null
    @Volatile private var nameIndexBuiltAtMs = 0L

    private fun invalidateNameIndex() {
        nameIndex = null
    }

    private suspend fun ensureNameIndex(): List<NameIndexEntry> {
        val existing = nameIndex
        val now = System.currentTimeMillis()
        if (existing != null && now - nameIndexBuiltAtMs < nameIndexTtlMs) return existing
        val built = stationDao.getNameIndex()
            .mapNotNull { row ->
                val normalized = normalizeForFuzzy(row.name)
                if (normalized.isBlank()) null else NameIndexEntry(row.uuid, normalized)
            }
        nameIndex = built
        nameIndexBuiltAtMs = now
        return built
    }

    /**
     * Typo-tolerant match of [normalizedQuery] against the local name index.
     * Scoring: normalized-substring hit = 0 (catches diacritics/punctuation variants
     * SQL LIKE misses); otherwise the best token-wise Levenshtein distance, allowed
     * up to 1 edit for 4–5 letter queries and 2 edits from 6 letters. Queries under
     * 4 letters skip fuzzy entirely — too ambiguous.
     */
    private suspend fun fuzzySearchLocal(normalizedQuery: String): List<Station> {
        val query = normalizeForFuzzy(normalizedQuery)
        if (query.length < fuzzyMinQueryLength) return emptyList()
        val maxEdits = if (query.length >= 6) 2 else 1

        val scored = ArrayList<Pair<NameIndexEntry, Int>>()
        for (entry in ensureNameIndex()) {
            val name = entry.normalizedName
            val score = if (name.contains(query)) {
                0
            } else {
                var best = Int.MAX_VALUE
                for (token in name.split(' ')) {
                    if (kotlin.math.abs(token.length - query.length) > maxEdits) continue
                    val d = boundedLevenshtein(token, query, maxEdits)
                    if (d < best) best = d
                    if (best == 0) break
                }
                best
            }
            if (score <= maxEdits) {
                scored += entry to score
            }
        }
        if (scored.isEmpty()) return emptyList()

        val topUuids = scored
            .sortedBy { it.second }
            .take(fuzzyResultLimit)
            .map { it.first.uuid }
        val byUuid = stationDao.getStationsByUuids(topUuids)
            .associateBy { it.uuid }
        // Preserve score order — Room's IN() gives no ordering guarantee.
        return topUuids.mapNotNull { byUuid[it]?.toDomain() }.scrubbed()
    }

    /** lowercase, strip diacritics, collapse everything non-alphanumeric to single spaces. */
    private fun normalizeForFuzzy(value: String): String {
        val decomposed = java.text.Normalizer.normalize(value.lowercase(), java.text.Normalizer.Form.NFD)
        val sb = StringBuilder(decomposed.length)
        var lastWasSpace = true
        for (c in decomposed) {
            when {
                c.isLetterOrDigit() && c.code < 128 -> {
                    sb.append(c); lastWasSpace = false
                }
                c.isLetterOrDigit() -> {
                    // Non-ASCII letter that NFD didn't decompose (e.g. ø, ß) — keep as-is.
                    sb.append(c); lastWasSpace = false
                }
                Character.getType(c) == Character.NON_SPACING_MARK.toInt() -> Unit // dropped diacritic
                else -> {
                    if (!lastWasSpace) { sb.append(' '); lastWasSpace = true }
                }
            }
        }
        return sb.toString().trim()
    }

    /**
     * Levenshtein distance with early exit: returns a value > [max] as soon as the
     * distance provably exceeds it, so the 50k-row scan stays cheap.
     */
    private fun boundedLevenshtein(a: String, b: String, max: Int): Int {
        if (a == b) return 0
        if (kotlin.math.abs(a.length - b.length) > max) return max + 1
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            var rowMin = current[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + cost,
                )
                if (current[j] < rowMin) rowMin = current[j]
            }
            if (rowMin > max) return max + 1
            val tmp = previous
            previous = current
            current = tmp
        }
        return previous[b.length]
    }

    private companion object {
        const val searchCacheTtlMs: Long = 15 * 60 * 1000L
        const val listCacheTtlMs: Long = 30 * 60 * 1000L
        const val nameIndexTtlMs: Long = 15 * 60 * 1000L
        const val searchResultLimit = 120
        const val topStationsLimit = 100
        const val countryStationsLimit = 160
        const val tagStationsLimit = 160
        const val mergeResultLimit = 180
        const val fuzzyMinQueryLength = 4
        const val fuzzyResultLimit = 30
    }
}

