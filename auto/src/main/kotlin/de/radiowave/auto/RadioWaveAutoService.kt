package de.radiowave.auto

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaItem.RequestMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import de.radiowave.core.data.repository.FavoriteRepository
import de.radiowave.core.data.repository.RecentRepository
import de.radiowave.core.data.repository.StationRepository
import de.radiowave.core.model.Station
import de.radiowave.core.model.AppSettings
import de.radiowave.core.model.Genre
import de.radiowave.core.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
@UnstableApi
class RadioWaveAutoService : MediaLibraryService() {

    @Inject
    lateinit var playerController: PlayerController

    @Inject
    lateinit var stationRepository: StationRepository

    @Inject
    lateinit var favoriteRepository: FavoriteRepository

    @Inject
    lateinit var recentRepository: RecentRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val stationCache = ConcurrentHashMap<String, Station>()
    private val searchCache = ConcurrentHashMap<String, List<Station>>()
    private var lastAutoResumeAttemptAtMs = 0L
    private var mediaLibrarySession: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        val callback = RadioWaveLibraryCallback()
        playerController.setPlaybackNotificationEnabled(false)
        val player = playerController.ensureSessionPlayer()
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, callback).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        tryAutoResumeOnConnect()
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.release()
        mediaLibrarySession = null
        playerController.setPlaybackNotificationEnabled(true)
        serviceScope.cancel()
        super.onDestroy()
    }

    private inner class RadioWaveLibraryCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ConnectionResult {
            val base = super.onConnect(session, controller)
            val playerCommands = base.availablePlayerCommands
                .buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()
            return ConnectionResult.accept(
                base.availableSessionCommands,
                playerCommands,
            )
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = browsableItem(ROOT_ID, "RadioWave")
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = resolvePlayableItem(mediaId)
                ?: return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            return Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val children = when (parentId) {
                ROOT_ID -> listOf(
                    browsableItem(FAVORITES_ID, "Favorites"),
                    browsableItem(RECENTS_ID, "Recents"),
                    browsableItem(TOP_STATIONS_ID, "Top Stations"),
                    browsableItem(GENRES_ID, "Genres"),
                )

                FAVORITES_ID -> asStationChildren(loadFavorites())
                RECENTS_ID -> asStationChildren(loadRecents())
                TOP_STATIONS_ID -> asStationChildren(loadTopStations())
                GENRES_ID -> asGenreChildren(loadGenres())
                else -> {
                    val genreTag = parentId
                        .removePrefix(GENRE_PREFIX)
                        .trim()
                        .takeUnless { it.isBlank() }
                    if (genreTag != null) {
                        asStationChildren(loadStationsByGenre(genreTag))
                    } else {
                        emptyList()
                    }
                }
            }
            val paged = paginate(children, page, pageSize)
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(paged), params))
        }

        @UnstableApi
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val resolvedStartIndex = when {
                startIndex >= 0 && startIndex < mediaItems.size -> startIndex
                mediaItems.isNotEmpty() -> 0
                else -> -1
            }
            val selected = mediaItems.getOrNull(resolvedStartIndex)
            val station = selected?.let(::resolveOrCreateStation)
            if (station != null) {
                startStationPlayback(station)
                val resolvedItem = stationItem(station)
                return Futures.immediateFuture(
                    @UnstableApi
                    MediaSession.MediaItemsWithStartPosition(
                        mutableListOf(resolvedItem),
                        0,
                        0L,
                    ),
                )
            }
            logInfo("Auto onSetMediaItems unresolved; size=${mediaItems.size}, startIndex=$startIndex")
            return Futures.immediateFuture(
                @UnstableApi
                MediaSession.MediaItemsWithStartPosition(
                    mediaItems,
                    resolvedStartIndex,
                    startPositionMs,
                ),
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            val mapped = mediaItems.mapNotNull { item ->
                resolvePlayableItem(
                    mediaId = item.mediaId,
                    mediaUri = item.localConfiguration?.uri?.toString(),
                ) ?: resolveOrCreateStation(item)?.let(::stationItem)
            }
            logInfo("Auto onAddMediaItems mapped=${mapped.size}/${mediaItems.size}")
            return Futures.immediateFuture(mapped)
        }

        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int,
        ): Int {
            return when (playerCommand) {
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                    if (playAdjacentFavorite(+1)) {
                        SessionResult.RESULT_SUCCESS
                    } else {
                        SessionResult.RESULT_ERROR_NOT_SUPPORTED
                    }
                }

                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                    if (playAdjacentFavorite(-1)) {
                        SessionResult.RESULT_SUCCESS
                    } else {
                        SessionResult.RESULT_ERROR_NOT_SUPPORTED
                    }
                }

                else -> SessionResult.RESULT_SUCCESS
            }
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            val normalized = normalizeSearchQuery(query)
            if (normalized.isBlank()) return Futures.immediateFuture(LibraryResult.ofVoid(params))
            val results = searchStations(normalized)
            searchCache[normalized.lowercase(Locale.ROOT)] = results
            session.notifySearchResultChanged(browser, normalized, results.size, params)
            logInfo("Auto search '$normalized' -> ${results.size} results")
            return Futures.immediateFuture(LibraryResult.ofVoid(params))
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val normalized = normalizeSearchQuery(query).lowercase(Locale.ROOT)
            val cached = searchCache[normalized].orEmpty()
            val candidates = if (cached.isNotEmpty()) {
                cached
            } else {
                searchStations(normalized).also { fresh ->
                    searchCache[normalized] = fresh
                }
            }
            val resolvedItems = if (candidates.isEmpty()) {
                listOf(
                    infoItem(
                        id = "info:no-search-results:${SystemClock.elapsedRealtime()}",
                        title = "No results for \"$query\"",
                    ),
                )
            } else {
                candidates.map(::stationItem)
            }
            val items = paginate(resolvedItems, page, pageSize)
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }
    }

    private fun startStationPlayback(station: Station) {
        logInfo("Auto start playback '${station.name}' (${station.streamUrl})")
        serviceScope.launch {
            performPlaybackStart(station)
            delay(AUTO_RESUME_VERIFY_DELAY_MS)
            if (shouldRetryPlayback(station)) {
                logInfo("Auto playback verify failed, retrying '${station.name}'")
                performPlaybackStart(station)
            }
        }
    }

    private fun shouldRetryPlayback(station: Station): Boolean {
        val state = playerController.playerState.value
        val activePlayer = playerController.ensureSessionPlayer()
        val sameStation = state.currentStation?.streamUrl == station.streamUrl
        if (!sameStation) return false

        val playerReadyOrBuffering =
            activePlayer.playbackState == Player.STATE_READY ||
                activePlayer.playbackState == Player.STATE_BUFFERING
        val audiblePlaybackLikely = state.isPlaying && activePlayer.isPlaying && playerReadyOrBuffering
        return !audiblePlaybackLikely
    }

    private suspend fun performPlaybackStart(station: Station) {
        playerController.playStation(station)
        recentRepository.addRecentStation(station)
        val activePlayer = playerController.ensureSessionPlayer()
        mediaLibrarySession?.setPlayer(activePlayer)
        activePlayer.playWhenReady = true
        activePlayer.play()
    }

    private fun loadTopStations(): List<Station> = runBlocking {
        runCatching { stationRepository.getTopStations().first().take(MAX_CHILDREN) }
            .getOrDefault(emptyList())
    }

    private fun loadGenres(): List<Genre> = runBlocking {
        runCatching { stationRepository.getTags().first().take(MAX_GENRES) }
            .getOrDefault(emptyList())
    }

    private fun loadStationsByGenre(tag: String): List<Station> = runBlocking {
        runCatching { stationRepository.getStationsByTag(tag).first().take(MAX_CHILDREN) }
            .getOrDefault(emptyList())
    }

    private fun loadFavorites(): List<Station> = runBlocking {
        runCatching { favoriteRepository.getFavorites().first().take(MAX_CHILDREN) }
            .getOrDefault(emptyList())
    }

    private fun loadRecents(): List<Station> = runBlocking {
        runCatching { recentRepository.getRecentStations(limit = MAX_CHILDREN).first() }
            .getOrDefault(emptyList())
    }

    private fun searchStations(query: String): List<Station> {
        val needle = normalizeSearchQuery(query)
        if (needle.isBlank()) return emptyList()
        val local = (loadFavorites() + loadRecents())
            .distinctBy { it.uuid }
        val localMatches = rankStationsByQuery(local, needle)
        if (localMatches.isNotEmpty()) return localMatches.take(MAX_SEARCH_RESULTS)

        val remote = runBlocking {
            runCatching {
                withTimeoutOrNull(SEARCH_REMOTE_TIMEOUT_MS) {
                    stationRepository.searchStations(needle).first()
                }.orEmpty()
            }
                .getOrDefault(emptyList())
        }
        return rankStationsByQuery(remote, needle).take(MAX_SEARCH_RESULTS)
    }

    private fun rankStationsByQuery(stations: List<Station>, query: String): List<Station> {
        val normalizedQuery = normalize(query)
        return stations
            .asSequence()
            .filter { it.streamUrl.isNotBlank() }
            .map { station ->
                val name = normalize(station.name)
                val score = when {
                    name == normalizedQuery -> 0
                    name.startsWith(normalizedQuery) -> 1
                    name.contains(normalizedQuery) -> 2
                    else -> 99
                }
                station to score
            }
            .filter { (_, score) -> score < 99 }
            .sortedWith(compareBy<Pair<Station, Int>> { it.second }.thenBy { it.first.name })
            .map { it.first }
            .toList()
    }

    private fun normalize(value: String): String {
        return value.trim().lowercase(Locale.ROOT)
    }

    private fun normalizeSearchQuery(value: String): String {
        return value
            .replace("„", "\"")
            .replace("“", "\"")
            .trim()
            .trim('"', '\'', '„', '“', '‚', '‘', '’', '«', '»')
            .trim()
    }

    private fun resolveStation(
        mediaId: String?,
        mediaUri: String? = null,
    ): Station? {
        val stationUuid = mediaId
            ?.removePrefix(STATION_PREFIX)
            ?.trim()
            .takeUnless { it.isNullOrBlank() }
        stationCache[stationUuid]?.let { return it }
        mediaId?.let(stationCache::get)?.let { return it }
        mediaUri?.let(stationCache::get)?.let { return it }

        val allCandidates = loadFavorites() + loadRecents()
        return allCandidates.firstOrNull { station ->
            (stationUuid != null && station.uuid == stationUuid) ||
                (!mediaId.isNullOrBlank() && station.uuid == mediaId) ||
                (!mediaUri.isNullOrBlank() && station.streamUrl == mediaUri)
        }
    }

    private fun resolvePlayableItem(
        mediaId: String?,
        mediaUri: String? = null,
    ): MediaItem? {
        val station = resolveStation(mediaId = mediaId, mediaUri = mediaUri) ?: return null
        return stationItem(station)
    }

    private fun resolveOrCreateStation(item: MediaItem): Station? {
        val mediaUri = item.localConfiguration?.uri?.toString()
            ?: item.requestMetadata.mediaUri?.toString()
        val resolved = resolveStation(mediaId = item.mediaId, mediaUri = mediaUri)
        if (resolved != null) return resolved

        if (mediaUri.isNullOrBlank()) return null
        val fallbackUuid = item.mediaId
            ?.removePrefix(STATION_PREFIX)
            ?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: mediaUri
        val fallbackTitle = item.mediaMetadata.title?.toString()?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: "Live Stream"
        val fallbackArtwork = item.mediaMetadata.artworkUri?.toString()
            ?.trim()
            .takeUnless { it.isNullOrBlank() }

        return Station(
            uuid = fallbackUuid,
            name = fallbackTitle,
            streamUrl = mediaUri,
            faviconUrl = fallbackArtwork,
            isCustom = true,
        )
    }

    private fun stationItem(station: Station): MediaItem {
        stationCache[station.uuid] = station
        stationCache["$STATION_PREFIX${station.uuid}"] = station
        stationCache[station.streamUrl] = station

        val artworkUri = station.faviconUrl
            ?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?.let(Uri::parse)

        return MediaItem.Builder()
            .setMediaId("$STATION_PREFIX${station.uuid}")
            .setUri(station.streamUrl)
            .setRequestMetadata(
                RequestMetadata.Builder()
                    .setMediaUri(Uri.parse(station.streamUrl))
                    .build(),
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(buildStationArtist(station))
                    .setSubtitle(buildStationSubtitle(station))
                    .setArtworkUri(artworkUri)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build(),
            )
            .build()
    }

    private fun buildStationSubtitle(station: Station): String? {
        val codec = station.codec?.trim()?.uppercase(Locale.ROOT).takeUnless { it.isNullOrBlank() }
        val bitrate = station.bitrate?.takeIf { it > 0 }?.let { "${it}kbps" }
        val language = station.language?.trim().takeUnless { it.isNullOrBlank() }
        val parts = listOfNotNull(codec, bitrate, language)
        return parts.joinToString(" • ").takeIf { it.isNotBlank() }
    }

    private fun buildStationArtist(station: Station): String? {
        val parts = mutableListOf<String>()
        val country = station.country?.trim().takeUnless { it.isNullOrBlank() }
        val codec = station.codec?.trim()?.uppercase(Locale.ROOT).takeUnless { it.isNullOrBlank() }
        val bitrate = station.bitrate?.takeIf { it > 0 }?.let { "${it}kbps" }

        if (country != null) parts += country
        if (codec != null) parts += codec
        if (bitrate != null) parts += bitrate

        return parts.joinToString(" • ").takeIf { it.isNotBlank() }
    }

    private fun browsableItem(id: String, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()
    }

    private fun asStationChildren(stations: List<Station>): List<MediaItem> {
        if (stations.isEmpty()) {
            return listOf(
                infoItem(
                    id = "info:empty:${SystemClock.elapsedRealtime()}",
                    title = "No stations available",
                ),
            )
        }
        return stations.map(::stationItem)
    }

    private fun asGenreChildren(genres: List<Genre>): List<MediaItem> {
        if (genres.isEmpty()) {
            return listOf(
                infoItem(
                    id = "info:empty-genres:${SystemClock.elapsedRealtime()}",
                    title = "No genres available",
                ),
            )
        }
        return genres.map { genre ->
            val tag = genre.name.trim()
            val id = "$GENRE_PREFIX$tag"
            browsableItem(id = id, title = "${genre.name} (${genre.stationCount})")
        }
    }

    private fun infoItem(id: String, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(false)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()
    }

    private fun <T> paginate(items: List<T>, page: Int, pageSize: Int): List<T> {
        if (pageSize <= 0 || page < 0) return items
        val from = page * pageSize
        if (from >= items.size) return emptyList()
        val to = kotlin.math.min(from + pageSize, items.size)
        return items.subList(from, to)
    }

    private fun playAdjacentFavorite(step: Int): Boolean {
        val favorites = loadFavorites()
        if (favorites.isEmpty()) return false
        val current = playerController.playerState.value.currentStation ?: return false
        val currentIndex = favorites.indexOfFirst { candidate ->
            candidate.uuid == current.uuid || candidate.streamUrl == current.streamUrl
        }
        if (currentIndex == -1) return false
        val targetIndex = (currentIndex + step).mod(favorites.size)
        val target = favorites[targetIndex]
        startStationPlayback(target)
        return true
    }

    private companion object {
        const val LOG_TAG = "RadioWaveAuto"
        const val AUTO_RESUME_VERIFY_DELAY_MS = 1800L
        const val AUTO_RESUME_CONNECT_COOLDOWN_MS = 3500L
        const val SEARCH_REMOTE_TIMEOUT_MS = 3500L
        const val MAX_SEARCH_RESULTS = 20
        const val ROOT_ID = "root"
        const val FAVORITES_ID = "favorites"
        const val RECENTS_ID = "recents"
        const val TOP_STATIONS_ID = "top_stations"
        const val GENRES_ID = "genres"
        const val GENRE_PREFIX = "genre:"
        const val STATION_PREFIX = "station:"
        const val MAX_CHILDREN = 50
        const val MAX_GENRES = 40
    }

    private fun logInfo(message: String) {
        Log.i(LOG_TAG, message)
    }

    private fun tryAutoResumeOnConnect() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAutoResumeAttemptAtMs < AUTO_RESUME_CONNECT_COOLDOWN_MS) return
        lastAutoResumeAttemptAtMs = now

        val prefs = getSharedPreferences(AppSettings.PREFS_NAME, MODE_PRIVATE)
        val autoPlayEnabled = prefs.getBoolean(
            AppSettings.KEY_AUTO_PLAY_ON_ANDROID_AUTO_CONNECT,
            true,
        )
        if (!autoPlayEnabled) return
        if (playerController.playerState.value.isPlaying) return

        val streamUrl = prefs.getString(AppSettings.KEY_LAST_STATION_STREAM_URL, null)
            ?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: return
        val name = prefs.getString(AppSettings.KEY_LAST_STATION_NAME, null)
            ?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: "Last Station"
        val station = Station(
            uuid = prefs.getString(AppSettings.KEY_LAST_STATION_UUID, null)
                ?.trim()
                .takeUnless { it.isNullOrBlank() }
                ?: streamUrl,
            name = name,
            streamUrl = streamUrl,
            faviconUrl = prefs.getString(AppSettings.KEY_LAST_STATION_FAVICON_URL, null),
            country = prefs.getString(AppSettings.KEY_LAST_STATION_COUNTRY, null),
            isCustom = true,
        )
        startStationPlayback(station)
    }
}
