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
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import dagger.hilt.android.qualifiers.ApplicationContext
import de.darksoon.radiowave.core.player.RadioPlayerManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CastManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val radioPlayerManager: RadioPlayerManager,
) {
    private var castContext: CastContext? = null
    private var didRegisterListener = false

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) = Unit

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            loadCurrentStationIntoSession(session)
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) = Unit

        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            loadCurrentStationIntoSession(session)
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) = Unit

        override fun onSessionSuspended(session: CastSession, reason: Int) = Unit

        override fun onSessionEnding(session: CastSession) = Unit

        override fun onSessionEnded(session: CastSession, error: Int) = Unit
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
    }

    private fun loadCurrentStationIntoSession(session: CastSession) {
        val playerState = radioPlayerManager.playerState.value
        val station = playerState.currentStation ?: return
        val remoteMediaClient = session.remoteMediaClient ?: return
        val mediaInfo = buildMediaInfo(station)
        remoteMediaClient.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build(),
        )
        if (playerState.isPlaying) {
            radioPlayerManager.togglePlayPause()
        }
    }

    private fun buildMediaInfo(station: de.darksoon.radiowave.core.model.Station): MediaInfo {
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

    private fun inferContentType(station: de.darksoon.radiowave.core.model.Station): String {
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
