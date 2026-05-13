// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.auto

import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaItem.RequestMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.SessionError
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.AndroidEntryPoint
import de.darksoon.radiowave.core.data.repository.FavoriteRepository
import de.darksoon.radiowave.core.data.repository.RecentRepository
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.model.AppSettings
import de.darksoon.radiowave.core.model.Genre
import de.darksoon.radiowave.core.player.PlayerController
import de.darksoon.radiowave.core.player.StreamQualityResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.Normalizer
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

    @Inject
    lateinit var streamQualityResolver: StreamQualityResolver

    private val isDebuggable: Boolean by lazy {
        (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    // Prevents concurrent startStationPlayback calls from racing against each other
    // when rapid Next/Prev taps arrive before the previous playback-start completes.
    private val playbackStartMutex = Mutex()
    private val stationCache = ConcurrentHashMap<String, Station>()
    private val searchCache = ConcurrentHashMap<String, List<Station>>()
    private var lastAutoResumeAttemptAtMs = 0L
    private var mediaLibrarySession: MediaLibrarySession? = null
    private var currentAutoQueue: List<Station> = emptyList()

    override fun onCreate() {
        super.onCreate()
        playerController.setAutomotivePerformanceModeEnabled(true)
        val callback = RadioWaveLibraryCallback()
        playerController.setPlaybackNotificationEnabled(false)
        val player = playerController.ensureSessionPlayer()
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, callback).build().also { session ->
            session.setSessionExtras(buildAutoSessionExtras())
            session.setMediaButtonPreferences(buildAutoMediaButtons())
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        tryAutoResumeOnConnect()
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.release()
        mediaLibrarySession = null
        playerController.setPlaybackNotificationEnabled(true)
        playerController.setAutomotivePerformanceModeEnabled(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    private inner class RadioWaveLibraryCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ConnectionResult {
            // Only accept connections from trusted system/media packages.
            // This prevents rogue apps from binding to the exported MediaLibraryService
            // and injecting arbitrary playback commands.
            val pkg = controller.packageName
            if (pkg != packageName && !isTrustedController(pkg)) {
                return ConnectionResult.reject()
            }
            val base = super.onConnect(session, controller)
            val sessionCommands = base.availableSessionCommands
                .buildUpon()
                .add(CUSTOM_COMMAND_PREVIOUS)
                .add(CUSTOM_COMMAND_NEXT)
                .build()
            val playerCommands = base.availablePlayerCommands
                .buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()
            return ConnectionResult.accept(
                sessionCommands,
                playerCommands,
            )
        }

        override fun onPostConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ) {
            super.onPostConnect(session, controller)
            session.setSessionExtras(controller, buildAutoSessionExtras())
            session.setMediaButtonPreferences(controller, buildAutoMediaButtons())
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = browsableItem(ROOT_ID, getString(R.string.auto_root_title))
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return serviceFuture {
                val item = resolveBrowsableItem(mediaId) ?: resolvePlayableItem(mediaId)
                    ?: return@serviceFuture LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                LibraryResult.ofItem(item, null)
            }
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return serviceFuture {
                val children = when (parentId) {
                    ROOT_ID -> listOf(
                        browsableItem(FAVORITES_ID, getString(R.string.auto_favorites)),
                        // Some Android Auto surfaces prioritize this slot.
                        // Offer a compact mix of favorites and recents on those surfaces.
                        browsableItem(RECENTS_ID, getString(R.string.auto_quick_access)),
                        browsableItem(TOP_STATIONS_ID, getString(R.string.auto_top_stations)),
                        browsableItem(GENRES_ID, getString(R.string.auto_genres)),
                    )

                    FAVORITES_ID -> asStationChildren(loadFavorites())
                    RECENTS_ID -> asStationChildren(loadQuickAccess())
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
                LibraryResult.ofItemList(ImmutableList.copyOf(paged), params)
            }
        }

        @UnstableApi
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            return serviceFuture {
                val resolvedStartIndex = when {
                    startIndex >= 0 && startIndex < mediaItems.size -> startIndex
                    mediaItems.isNotEmpty() -> 0
                    else -> -1
                }
                val selected = mediaItems.getOrNull(resolvedStartIndex)
                val station = selected?.let { resolveOrCreateStation(it) }
                if (station != null) {
                    startStationPlayback(station)
                    val resolvedItem = stationItem(station)
                    @UnstableApi
                    MediaSession.MediaItemsWithStartPosition(
                        mutableListOf(resolvedItem),
                        0,
                        0L,
                    )
                } else {
                    logInfo("Auto onSetMediaItems unresolved; size=${mediaItems.size}, startIndex=$startIndex")
                    @UnstableApi
                    MediaSession.MediaItemsWithStartPosition(
                        mediaItems,
                        resolvedStartIndex,
                        startPositionMs,
                    )
                }
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            return serviceFuture {
                val mapped = mediaItems.mapNotNull { item ->
                    resolvePlayableItem(
                        mediaId = item.mediaId,
                        mediaUri = item.localConfiguration?.uri?.toString(),
                    ) ?: resolveOrCreateStation(item)?.let(::stationItem)
                }
                logInfo("Auto onAddMediaItems mapped=${mapped.size}/${mediaItems.size}")
                mapped
            }
        }

        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int,
        ): Int {
            return when (playerCommand) {
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                    if (playAdjacentFromCurrentQueue(+1)) {
                        SessionResult.RESULT_SUCCESS
                    } else {
                        SessionResult.RESULT_ERROR_NOT_SUPPORTED
                    }
                }

                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                    if (playAdjacentFromCurrentQueue(-1)) {
                        SessionResult.RESULT_SUCCESS
                    } else {
                        SessionResult.RESULT_ERROR_NOT_SUPPORTED
                    }
                }

                else -> SessionResult.RESULT_SUCCESS
            }
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            return serviceFuture {
                val resultCode = when (customCommand.customAction) {
                    CUSTOM_ACTION_PREVIOUS -> {
                        if (playAdjacentStation(-1)) SessionResult.RESULT_SUCCESS
                        else SessionResult.RESULT_ERROR_NOT_SUPPORTED
                    }

                    CUSTOM_ACTION_NEXT -> {
                        if (playAdjacentStation(+1)) SessionResult.RESULT_SUCCESS
                        else SessionResult.RESULT_ERROR_NOT_SUPPORTED
                    }

                    else -> SessionResult.RESULT_ERROR_NOT_SUPPORTED
                }
                SessionResult(resultCode)
            }
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            val normalized = sanitizeSearchQuery(query)
            if (normalized.isBlank()) return Futures.immediateFuture(LibraryResult.ofVoid(params))
            return serviceFuture {
                val results = searchStations(normalized)
                searchCache[normalized.lowercase(Locale.ROOT)] = results
                session.notifySearchResultChanged(browser, normalized, results.size, params)
                logInfo("Auto search '$normalized' -> ${results.size} results")
                LibraryResult.ofVoid(params)
            }
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return serviceFuture {
                val normalized = sanitizeSearchQuery(query).lowercase(Locale.ROOT)
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
                            title = getString(R.string.auto_no_results_for, query),
                        ),
                    )
                } else {
                    candidates.map(::stationItem)
                }
                val items = paginate(resolvedItems, page, pageSize)
                LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
            }
        }
    }

    private fun startStationPlayback(station: Station) {
        logInfo("Auto start playback '${station.name}' (${station.streamUrl})")
        serviceScope.launch {
            // Mutex prevents concurrent start attempts from racing (e.g. rapid Next/Prev taps).
            // If a previous attempt is still running it completes first; the next one then
            // re-checks the current station and short-circuits if already playing the right station.
            playbackStartMutex.withLock {
                repeat(AUTO_RESUME_MAX_ATTEMPTS) { attemptIndex ->
                    // Another attempt may have started a different station while we were waiting.
                    val currentUuid = playerController.playerState.value.currentStation?.uuid
                    if (currentUuid != null && currentUuid != station.uuid &&
                        playerController.playerState.value.isPlaying
                    ) {
                        return@withLock
                    }
                    val started = performPlaybackStart(station)
                    delay(AUTO_RESUME_VERIFY_DELAY_MS)
                    val shouldRetry = !started || shouldRetryPlayback(station)
                    if (!shouldRetry) return@withLock
                    if (attemptIndex < AUTO_RESUME_MAX_ATTEMPTS - 1) {
                        logInfo(
                            "Auto playback verify failed, retrying '${station.name}' " +
                                "(${attemptIndex + 2}/$AUTO_RESUME_MAX_ATTEMPTS)",
                        )
                    }
                }
            }
        }
    }

    private fun <T> serviceFuture(block: suspend () -> T): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        serviceScope.launch {
            runCatching { block() }
                .onSuccess(future::set)
                .onFailure(future::setException)
        }
        return future
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

    private suspend fun performPlaybackStart(station: Station): Boolean {
        val selectedStation = streamQualityResolver.resolve(
            station = station,
            automotiveMode = true,
        )
        playerController.playStation(selectedStation)

        val stateAfterStart = playerController.playerState.value
        val sameStation = stateAfterStart.currentStation?.streamUrl == selectedStation.streamUrl
        val blockedBeforePlayerStart =
            sameStation &&
                stateAfterStart.error != null &&
                !stateAfterStart.isLoading &&
                !stateAfterStart.isBuffering &&
                !stateAfterStart.isPlaying
        if (blockedBeforePlayerStart) {
            logInfo("Auto playback start blocked for '${selectedStation.name}', will retry")
            return false
        }

        recentRepository.addRecentStation(selectedStation)
        val activePlayer = playerController.ensureSessionPlayer()
        applyAutoQueue(activePlayer, selectedStation)
        mediaLibrarySession?.setPlayer(activePlayer)
        refreshConnectedAutoControllers()
        activePlayer.playWhenReady = true
        activePlayer.play()
        return true
    }

    private suspend fun applyAutoQueue(player: Player, selectedStation: Station) {
        val queue = buildAutoQueue(selectedStation)
        currentAutoQueue = queue
        if (queue.size <= 1) return

        val startIndex = queue.indexOfFirst { candidate ->
            candidate.uuid == selectedStation.uuid || candidate.streamUrl == selectedStation.streamUrl
        }
        if (startIndex == -1) return

        val mediaItems = queue.map(::stationItem)
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
    }

    private suspend fun buildAutoQueue(selectedStation: Station): List<Station> {
        val favorites = loadFavorites()
        val quickAccess = loadQuickAccess()
        val source = when {
            favorites.any { it.uuid == selectedStation.uuid || it.streamUrl == selectedStation.streamUrl } -> favorites
            quickAccess.any { it.uuid == selectedStation.uuid || it.streamUrl == selectedStation.streamUrl } -> quickAccess
            else -> listOf(selectedStation)
        }
        return source
            .map { station ->
                if (station.uuid == selectedStation.uuid || station.streamUrl == selectedStation.streamUrl) {
                    selectedStation
                } else {
                    station
                }
            }
            .distinctBy { it.uuid }
    }

    private suspend fun loadTopStations(): List<Station> = withContext(Dispatchers.IO) {
        runCatching { stationRepository.getTopStations().first().take(MAX_CHILDREN) }
            .getOrDefault(emptyList())
    }

    private suspend fun loadGenres(): List<Genre> = withContext(Dispatchers.IO) {
        runCatching { stationRepository.getTags().first().take(MAX_GENRES) }
            .getOrDefault(emptyList())
    }

    private suspend fun loadStationsByGenre(tag: String): List<Station> = withContext(Dispatchers.IO) {
        runCatching { stationRepository.getStationsByTag(tag).first().take(MAX_CHILDREN) }
            .getOrDefault(emptyList())
    }

    private suspend fun loadFavorites(): List<Station> = withContext(Dispatchers.IO) {
        runCatching { favoriteRepository.getFavorites().first().take(MAX_CHILDREN) }
            .getOrDefault(emptyList())
    }

    private suspend fun loadRecents(): List<Station> = withContext(Dispatchers.IO) {
        runCatching { recentRepository.getRecentStations(limit = MAX_CHILDREN).first() }
            .getOrDefault(emptyList())
    }

    private suspend fun loadQuickAccess(): List<Station> {
        return (loadFavorites() + loadRecents())
            .distinctBy { it.uuid }
            .take(MAX_CHILDREN)
    }

    private suspend fun searchStations(query: String): List<Station> {
        val needle = sanitizeSearchQuery(query)
        if (needle.isBlank()) return emptyList()
        val localMatches = rankStationsByQuery(loadQuickAccess(), needle)
        val remote = withContext(Dispatchers.IO) {
            runCatching {
                withTimeoutOrNull(SEARCH_REMOTE_TIMEOUT_MS) {
                    stationRepository.searchStations(needle).first()
                }.orEmpty()
            }
                .getOrDefault(emptyList())
        }
        val remoteMatches = rankStationsByQuery(remote, needle)
        return (localMatches + remoteMatches)
            .distinctBy { it.uuid }
            .take(MAX_SEARCH_RESULTS)
    }

    private fun rankStationsByQuery(stations: List<Station>, query: String): List<Station> {
        val normalizedQuery = normalizeForMatch(query)
        val queryTokens = normalizedQuery.split(" ").filter { it.isNotBlank() }
        return stations
            .asSequence()
            .filter { it.streamUrl.isNotBlank() }
            .map { station ->
                val name = normalizeForMatch(station.name)
                val country = normalizeForMatch(station.country.orEmpty())
                val tags = normalizeForMatch(station.tags.joinToString(" "))
                val haystack = listOf(name, country, tags)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                val score = when {
                    name == normalizedQuery -> 0
                    name.startsWith(normalizedQuery) -> 1
                    name.contains(normalizedQuery) -> 2
                    queryTokens.isNotEmpty() && queryTokens.all { token -> haystack.contains(token) } -> 3
                    queryTokens.isNotEmpty() && queryTokens.any { token -> name.contains(token) } -> 4
                    haystack.contains(normalizedQuery) -> 5
                    else -> 99
                }
                station to score
            }
            .filter { (_, score) -> score < 99 }
            .sortedWith(compareBy<Pair<Station, Int>> { it.second }.thenBy { it.first.name })
            .map { it.first }
            .toList()
    }

    private fun normalizeForMatch(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("\u00E4", "ae", ignoreCase = true)
            .replace("\u00F6", "oe", ignoreCase = true)
            .replace("\u00FC", "ue", ignoreCase = true)
            .replace("\u00DF", "ss", ignoreCase = true)
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
    }

    private fun sanitizeSearchQuery(value: String): String {
        return value
            .replace("\u201E", "\"")
            .replace("\u201C", "\"")
            .trim()
            .trim('"', '\'', '\u201E', '\u201C', '\u201A', '\u2018', '\u2019', '\u00AB', '\u00BB')
            .trim()
    }

    private suspend fun resolveStation(
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

    private fun resolveBrowsableItem(mediaId: String): MediaItem? {
        return when (mediaId) {
            ROOT_ID -> browsableItem(ROOT_ID, getString(R.string.auto_root_title))
            FAVORITES_ID -> browsableItem(FAVORITES_ID, getString(R.string.auto_favorites))
            RECENTS_ID -> browsableItem(RECENTS_ID, getString(R.string.auto_quick_access))
            TOP_STATIONS_ID -> browsableItem(TOP_STATIONS_ID, getString(R.string.auto_top_stations))
            GENRES_ID -> browsableItem(GENRES_ID, getString(R.string.auto_genres))
            else -> mediaId
                .removePrefix(GENRE_PREFIX)
                .takeIf { mediaId.startsWith(GENRE_PREFIX) && it.isNotBlank() }
                ?.let { genre -> browsableItem(mediaId, genre) }
        }
    }

    private suspend fun resolvePlayableItem(
        mediaId: String?,
        mediaUri: String? = null,
    ): MediaItem? {
        val station = resolveStation(mediaId = mediaId, mediaUri = mediaUri) ?: return null
        return stationItem(station)
    }

    private suspend fun resolveOrCreateStation(item: MediaItem): Station? {
        val mediaUri = item.localConfiguration?.uri?.toString()
            ?: item.requestMetadata.mediaUri?.toString()
        val resolved = resolveStation(mediaId = item.mediaId, mediaUri = mediaUri)
        if (resolved != null) return resolved

        if (mediaUri.isNullOrBlank()) return null

        // Security: only allow http/https URIs to prevent playing local file:// or
        // content:// URIs that a rogue controller could inject via MediaItem.
        val scheme = Uri.parse(mediaUri).scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            Log.w(LOG_TAG, "resolveOrCreateStation: rejected non-http URI scheme '$scheme'")
            return null
        }

        val fallbackUuid = item.mediaId
            ?.removePrefix(STATION_PREFIX)
            ?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: mediaUri
        val fallbackTitle = item.mediaMetadata.title?.toString()?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: getString(R.string.auto_live_stream)
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
                    .setTitle(sanitizeAutoDisplayText(station.name))
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
        val bitrate = station.bitrate?.takeIf { it > 0 }?.let { "${it} kbps" }
        val codec = station.codec?.trim()?.uppercase(Locale.ROOT).takeUnless { it.isNullOrBlank() }
        val language = sanitizeAutoDisplayText(station.language).takeUnless { it.isBlank() }
        val parts = listOfNotNull(bitrate, codec, language)
        return parts.joinToString(" • ").takeIf { it.isNotBlank() }
    }

    private fun buildStationArtist(station: Station): String? {
        val country = sanitizeAutoDisplayText(station.country).takeUnless { it.isBlank() }
        return country?.takeIf { it.isNotBlank() }
    }

    private fun sanitizeAutoDisplayText(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return value
            .trim()
            .replace("â€ž", "\"")
            .replace("â€œ", "\"")
            .replace("â€\u009c", "\"")
            .replace("â€\u009d", "\"")
            .replace("â€™", "'")
            .replace("â€˜", "'")
            .replace("â€“", "-")
            .replace("â€”", "-")
            .replace("â€¢", "•")
            .replace("Ã¤", "ä", ignoreCase = true)
            .replace("Ã¶", "ö", ignoreCase = true)
            .replace("Ã¼", "ü", ignoreCase = true)
            .replace("Ã„", "Ä")
            .replace("Ã–", "Ö")
            .replace("Ãœ", "Ü")
            .replace("ÃŸ", "ß")
            .trim()
            .trim('"', '\'', '„', '“', '”', '‚', '‘', '’', '«', '»', '€')
            .replace("\\s+".toRegex(), " ")
            .trim()
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
                    title = getString(R.string.auto_no_stations_available),
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
                    title = getString(R.string.auto_no_genres_available),
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

    private fun playAdjacentFromCurrentQueue(step: Int): Boolean {
        val current = playerController.playerState.value.currentStation ?: return false
        val queue = currentAutoQueue
            .takeIf { candidates ->
                candidates.size > 1 && candidates.any { it.matches(current) }
            }
            ?: return false
        return playAdjacentFromQueue(queue, current, step)
    }

    private suspend fun playAdjacentStation(step: Int): Boolean {
        val current = playerController.playerState.value.currentStation ?: return false
        val queue = currentAutoQueue
            .takeIf { candidates ->
                candidates.size > 1 && candidates.any { it.matches(current) }
            }
            ?: loadFavorites().takeIf { favorites ->
                favorites.size > 1 && favorites.any { it.matches(current) }
            }
            ?: loadQuickAccess().takeIf { quickAccess ->
                quickAccess.size > 1 && quickAccess.any { it.matches(current) }
            }
            ?: return false
        return playAdjacentFromQueue(queue, current, step)
    }

    private fun playAdjacentFromQueue(
        queue: List<Station>,
        current: Station,
        step: Int,
    ): Boolean {
        val currentIndex = queue.indexOfFirst { candidate ->
            candidate.matches(current)
        }
        if (currentIndex == -1) return false
        val targetIndex = (currentIndex + step).mod(queue.size)
        val target = queue[targetIndex]
        startStationPlayback(target)
        return true
    }

    private fun Station.matches(other: Station): Boolean {
        return uuid == other.uuid || streamUrl == other.streamUrl
    }

    /**
     * Trusted controller packages that are allowed to bind to this MediaLibraryService.
     * Includes Android Auto, Google Assistant, Google Play, and media notification systems.
     * Any app not in this list or matching our own package will be rejected in [onConnect].
     */
    private fun isTrustedController(packageName: String): Boolean {
        return packageName in TRUSTED_CONTROLLER_PACKAGES
    }

    private companion object {
        const val LOG_TAG = "RadioWaveAuto"

        /** Packages allowed to bind to our exported MediaLibraryService. */
        val TRUSTED_CONTROLLER_PACKAGES = setOf(
            "com.google.android.projection.gearhead",   // Android Auto
            "com.google.android.googlequicksearchbox",  // Google Assistant
            "com.google.android.apps.automotive.media", // Automotive OS
            "com.google.android.mediahome.castmanager", // Cast
            "com.google.android.music",                 // Google Play Music (legacy)
            "com.google.android.youtube.music",         // YTM (shares session protocol)
            "android",                                  // System media session manager
            "com.android.bluetooth",                    // Bluetooth media controls
            "com.android.systemui",                     // Media notification controls
        )
        const val AUTO_RESUME_VERIFY_DELAY_MS = 1800L
        const val AUTO_RESUME_CONNECT_COOLDOWN_MS = 3500L
        const val AUTO_RESUME_MAX_ATTEMPTS = 3
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
        const val CUSTOM_ACTION_PREVIOUS = "de.darksoon.radiowave.auto.action.PREVIOUS"
        const val CUSTOM_ACTION_NEXT = "de.darksoon.radiowave.auto.action.NEXT"

        val CUSTOM_COMMAND_PREVIOUS = SessionCommand(CUSTOM_ACTION_PREVIOUS, Bundle.EMPTY)
        val CUSTOM_COMMAND_NEXT = SessionCommand(CUSTOM_ACTION_NEXT, Bundle.EMPTY)
    }

    private fun logInfo(message: String) {
        if (isDebuggable) Log.i(LOG_TAG, message)
    }

    private fun buildAutoSessionExtras(): Bundle {
        return Bundle().apply {
            putBoolean(MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_PREV, true)
            putBoolean(MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_NEXT, true)
        }
    }

    private fun buildAutoMediaButtons(): List<CommandButton> {
        return listOf(
            CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                .setSessionCommand(CUSTOM_COMMAND_PREVIOUS)
                .setDisplayName(getString(R.string.auto_previous))
                .setEnabled(true)
                .setSlots(CommandButton.SLOT_BACK)
                .build(),
            CommandButton.Builder(CommandButton.ICON_NEXT)
                .setSessionCommand(CUSTOM_COMMAND_NEXT)
                .setDisplayName(getString(R.string.auto_next))
                .setEnabled(true)
                .setSlots(CommandButton.SLOT_FORWARD)
                .build(),
        )
    }

    private fun refreshConnectedAutoControllers() {
        val session = mediaLibrarySession ?: return
        val extras = buildAutoSessionExtras()
        val buttons = buildAutoMediaButtons()
        session.connectedControllers.forEach { controller ->
            session.setSessionExtras(controller, extras)
            session.setMediaButtonPreferences(controller, buttons)
        }
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

        // Don't auto-resume during an active or incoming call — the audio focus handoff
        // after the call/ringtone ends would otherwise cause the radio to start unexpectedly.
        val audioManager = getSystemService(AudioManager::class.java)
        if (audioManager?.mode in setOf(
                AudioManager.MODE_IN_CALL,
                AudioManager.MODE_IN_COMMUNICATION,
                AudioManager.MODE_RINGTONE,
            )
        ) return

        val streamUrl = prefs.getString(AppSettings.KEY_LAST_STATION_STREAM_URL, null)
            ?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: return
        val name = prefs.getString(AppSettings.KEY_LAST_STATION_NAME, null)
            ?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: getString(R.string.auto_last_station)
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

