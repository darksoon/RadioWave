// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.cast

import android.content.Context
import android.net.Uri
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage
import dagger.hilt.android.qualifiers.ApplicationContext
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.player.RadioPlayerManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Singleton
class CastManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val radioPlayerManager: RadioPlayerManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var castContext: CastContext? = null
    private var didRegisterListener = false
    private var didStartObservingPlayerState = false
    private var lastLoadedStationUuid: String? = null

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) = Unit

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            lastLoadedStationUuid = null
            loadCurrentStationIntoSession(session)
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) = Unit

        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            lastLoadedStationUuid = null
            loadCurrentStationIntoSession(session)
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) = Unit

        override fun onSessionSuspended(session: CastSession, reason: Int) = Unit

        override fun onSessionEnding(session: CastSession) = Unit

        override fun onSessionEnded(session: CastSession, error: Int) {
            lastLoadedStationUuid = null
        }
    }

    fun initialize() {
        val sharedCastContext = runCatching {
            CastContext.getSharedInstance(context)
        }.getOrNull() ?: return

        castContext = sharedCastContext
        if (!didRegisterListener) {
            sharedCastContext.sessionManager.addSessionManagerListener(
                sessionListener,
                CastSession::class.java,
            )
            didRegisterListener = true
        }
        if (!didStartObservingPlayerState) {
            observePlayerState()
            didStartObservingPlayerState = true
        }
    }

    private fun observePlayerState() {
        scope.launch {
            radioPlayerManager.playerState.collectLatest { playerState ->
                val session = castContext?.sessionManager?.currentCastSession ?: return@collectLatest
                loadStationIntoSessionIfNeeded(session, playerState)
            }
        }
    }

    private fun loadCurrentStationIntoSession(session: CastSession) {
        loadStationIntoSessionIfNeeded(session, radioPlayerManager.playerState.value, force = true)
    }

    private fun loadStationIntoSessionIfNeeded(
        session: CastSession,
        playerState: PlayerState,
        force: Boolean = false,
    ) {
        val station = playerState.currentStation ?: return
        if (!force && lastLoadedStationUuid == station.uuid) return

        val remoteMediaClient = session.remoteMediaClient ?: return
        val mediaInfo = buildMediaInfo(station)
        remoteMediaClient.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build(),
        )
        lastLoadedStationUuid = station.uuid
        if (playerState.isPlaying) {
            radioPlayerManager.togglePlayPause()
        }
    }

    private fun buildMediaInfo(station: Station): MediaInfo {
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, station.name)
            station.country?.takeIf { it.isNotBlank() }?.let {
                putString(MediaMetadata.KEY_SUBTITLE, it)
            }
            station.faviconUrl
                ?.takeIf { it.isNotBlank() }
                ?.let(Uri::parse)
                ?.let(::WebImage)
                ?.let(::addImage)
        }

        return MediaInfo.Builder(station.streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setContentType(inferContentType(station))
            .setMetadata(metadata)
            .build()
    }

    private fun inferContentType(station: Station): String {
        val codec = station.codec?.lowercase().orEmpty()
        val url = station.streamUrl.lowercase()
        return when {
            url.contains(".m3u8") -> "application/x-mpegURL"
            codec.contains("aac") || codec.contains("aac+") || codec.contains("aacp") -> "audio/aac"
            codec.contains("ogg") || codec.contains("vorbis") -> "audio/ogg"
            codec.contains("opus") -> "audio/ogg"
            codec.contains("flac") -> "audio/flac"
            else -> "audio/mpeg"
        }
    }
}
