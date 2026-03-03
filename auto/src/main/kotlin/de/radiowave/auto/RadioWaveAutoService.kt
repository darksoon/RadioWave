package de.radiowave.auto

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaItem.RequestMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import de.radiowave.core.data.repository.FavoriteRepository
import de.radiowave.core.data.repository.RecentRepository
import de.radiowave.core.data.repository.StationRepository
import de.radiowave.core.model.Station
import de.radiowave.core.model.AppSettings
import de.radiowave.core.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
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
    private var lastAutoResumeAttemptAtMs = 0L
    private lateinit var fallbackPlayer: ExoPlayer
    private var mediaLibrarySession: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        fallbackPlayer = ExoPlayer.Builder(this).build()
        val callback = RadioWaveLibraryCallback()
        val player = playerController.sessionPlayer() ?: fallbackPlayer
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, callback).build()

        serviceScope.launch {
            playerController.playerState.collect {
                val activePlayer = playerController.sessionPlayer() ?: fallbackPlayer
                mediaLibrarySession?.player?.let { current ->
                    if (current !== activePlayer) {
                        mediaLibrarySession?.setPlayer(activePlayer)
                    }
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        tryAutoResumeOnConnect()
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.release()
        mediaLibrarySession = null
        fallbackPlayer.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private inner class RadioWaveLibraryCallback : MediaLibrarySession.Callback {
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
                )

                FAVORITES_ID -> loadFavorites().map { stationItem(it) }
                RECENTS_ID -> loadRecents().map { stationItem(it) }
                else -> emptyList()
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
    }

    private fun startStationPlayback(station: Station) {
        logInfo("Auto start playback '${station.name}' (${station.streamUrl})")
        serviceScope.launch {
            performPlaybackStart(station)
            delay(AUTO_RESUME_VERIFY_DELAY_MS)
            val state = playerController.playerState.value
            if (!state.isPlaying && state.currentStation?.streamUrl == station.streamUrl) {
                logInfo("Auto playback verify failed, retrying '${station.name}'")
                performPlaybackStart(station)
            }
        }
    }

    private suspend fun performPlaybackStart(station: Station) {
        playerController.playStation(station)
        recentRepository.addRecentStation(station)
        val activePlayer = playerController.sessionPlayer() ?: fallbackPlayer
        mediaLibrarySession?.setPlayer(activePlayer)
        activePlayer.playWhenReady = true
        activePlayer.play()
    }

    private fun loadTopStations(): List<Station> = runBlocking {
        runCatching { stationRepository.getTopStations().first().take(MAX_CHILDREN) }
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
                    .setArtist(station.country)
                    .setArtworkUri(artworkUri)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build(),
            )
            .build()
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

    private fun <T> paginate(items: List<T>, page: Int, pageSize: Int): List<T> {
        if (pageSize <= 0 || page < 0) return items
        val from = page * pageSize
        if (from >= items.size) return emptyList()
        val to = kotlin.math.min(from + pageSize, items.size)
        return items.subList(from, to)
    }

    private companion object {
        const val LOG_TAG = "RadioWaveAuto"
        const val AUTO_RESUME_VERIFY_DELAY_MS = 1800L
        const val AUTO_RESUME_CONNECT_COOLDOWN_MS = 3500L
        const val ROOT_ID = "root"
        const val FAVORITES_ID = "favorites"
        const val RECENTS_ID = "recents"
        const val STATION_PREFIX = "station:"
        const val MAX_CHILDREN = 50
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
