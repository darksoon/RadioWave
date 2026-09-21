// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.cast

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val loadTracker = CastLoadTracker()
    private var attachedRemoteMediaClient: RemoteMediaClient? = null
    private val _isCasting = MutableStateFlow(false)
    /**
     * True while a Cast session is connected and the receiver accepted the current station.
     * Goes false again after a failed load so the in-app controls drive the local player,
     * which keeps playing in that case.
     */
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()
    private val _loadFailures = MutableSharedFlow<Station>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    /** One event per station the receiver refused to load; playback stayed on the phone. */
    val loadFailures: SharedFlow<Station> = _loadFailures.asSharedFlow()
    private val _isRemotePlaying = MutableStateFlow(false)
    val isRemotePlaying: StateFlow<Boolean> = _isRemotePlaying.asStateFlow()

    private val remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            updateRemotePlaybackState(attachedRemoteMediaClient)
        }
    }

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) = Unit

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            loadTracker.reset()
            attachRemoteMediaClient(session.remoteMediaClient)
            loadCurrentStationIntoSession(session)
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) = Unit

        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            loadTracker.reset()
            attachRemoteMediaClient(session.remoteMediaClient)
            loadCurrentStationIntoSession(session)
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) = Unit

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            detachRemoteMediaClient()
        }

        override fun onSessionEnding(session: CastSession) = Unit

        override fun onSessionEnded(session: CastSession, error: Int) {
            loadTracker.reset()
            detachRemoteMediaClient()
        }
    }

    fun initialize() {
        val sharedCastContext = runCatching {
            CastContext.getSharedInstance(context)
        }.getOrNull() ?: return

        castContext = sharedCastContext
        attachRemoteMediaClient(sharedCastContext.sessionManager.currentCastSession?.remoteMediaClient)
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

    /**
     * Tear down the Cast integration: cancels the observer coroutine scope,
     * removes the session listener from Google Play Services, and detaches
     * the remote media client. Idempotent — safe to call multiple times.
     *
     * Intended for app shutdown / Process lifecycle ON_DESTROY.
     */
    fun release() {
        scope.cancel()
        castContext?.sessionManager?.let { sm ->
            if (didRegisterListener) {
                runCatching { sm.removeSessionManagerListener(sessionListener, CastSession::class.java) }
                didRegisterListener = false
            }
        }
        detachRemoteMediaClient()
        didStartObservingPlayerState = false
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
        if (!loadTracker.shouldLoad(station.uuid, force, isLocalLoading = playerState.isLoading)) return

        val remoteMediaClient = session.remoteMediaClient ?: return
        val mediaInfo = buildMediaInfo(station)
        val token = loadTracker.onLoadRequested(station.uuid)
        // Local playback keeps running until the receiver confirmed the load — pausing it
        // up front left the user in silence whenever the receiver rejected the stream
        // (unsupported codec, HTTP-only URL, receiver app crash, ...).
        val pendingResult = try {
            remoteMediaClient.load(
                MediaLoadRequestData.Builder()
                    .setMediaInfo(mediaInfo)
                    .setAutoplay(true)
                    .build(),
            )
        } catch (e: RuntimeException) {
            Log.w(LOG_TAG, "Cast load request for ${station.uuid} threw", e)
            onLoadResult(remoteMediaClient, station, token, success = false)
            return
        }
        // Delivered on the main looper, like every Cast SDK callback.
        pendingResult.setResultCallback { result ->
            val success = result.status.isSuccess
            if (!success) {
                Log.w(LOG_TAG, "Cast load for ${station.uuid} failed: ${result.status}")
            }
            onLoadResult(remoteMediaClient, station, token, success)
        }
    }

    private fun onLoadResult(
        remoteMediaClient: RemoteMediaClient,
        station: Station,
        token: Long,
        success: Boolean,
    ) {
        // Session ended/switched meanwhile — the answer belongs to a client we no longer use.
        if (attachedRemoteMediaClient !== remoteMediaClient) return
        if (!loadTracker.onLoadResult(token, success)) return
        publishCastingState()
        if (success) {
            radioPlayerManager.pauseForExternalPlayback()
            return
        }
        // Keep (or bring back) the phone as output. After a forced load on session
        // start/resume the local player may already be paused for the previous cast.
        val playerState = radioPlayerManager.playerState.value
        if (
            playerState.currentStation?.uuid == station.uuid &&
            !playerState.isPlaying &&
            !playerState.isLoading
        ) {
            radioPlayerManager.togglePlayPause()
        }
        _loadFailures.tryEmit(station)
    }

    private fun buildMediaInfo(station: Station): MediaInfo {
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, station.name)
            station.country?.takeIf { it.isNotBlank() }?.let {
                putString(MediaMetadata.KEY_SUBTITLE, it)
            }
            // Security: faviconUrl is community-editable API data (see PlayerControllerImpl's
            // matching guard on the same field) — only hand http/https URIs to the Cast SDK.
            station.faviconUrl
                ?.takeIf { it.isNotBlank() }
                ?.let(Uri::parse)
                ?.takeIf { it.scheme?.lowercase() in setOf("http", "https") }
                ?.let(::WebImage)
                ?.let(::addImage)
        }

        return MediaInfo.Builder(station.streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setContentType(inferContentType(station))
            .setMetadata(metadata)
            .build()
    }

    fun isCastSessionActive(): Boolean = _isCasting.value

    fun togglePlayback() {
        val remoteMediaClient = attachedRemoteMediaClient ?: return
        remoteMediaClient.togglePlayback()
        updateRemotePlaybackState(remoteMediaClient)
    }

    fun stopCasting() {
        attachedRemoteMediaClient?.stop()
        castContext?.sessionManager?.endCurrentSession(true)
        detachRemoteMediaClient()
        loadTracker.reset()
        radioPlayerManager.stop()
    }

    private fun attachRemoteMediaClient(remoteMediaClient: RemoteMediaClient?) {
        radioPlayerManager.setExternalPlaybackStopper(
            remoteMediaClient?.let { client -> { client.stop(); Unit } },
        )
        if (attachedRemoteMediaClient === remoteMediaClient) {
            updateRemotePlaybackState(remoteMediaClient)
            publishCastingState()
            return
        }
        attachedRemoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
        attachedRemoteMediaClient = remoteMediaClient
        remoteMediaClient?.registerCallback(remoteMediaClientCallback)
        publishCastingState()
        updateRemotePlaybackState(remoteMediaClient)
    }

    private fun publishCastingState() {
        _isCasting.value = attachedRemoteMediaClient != null && !loadTracker.lastLoadFailed
    }

    private fun detachRemoteMediaClient() {
        radioPlayerManager.setExternalPlaybackStopper(null)
        attachedRemoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
        attachedRemoteMediaClient = null
        _isCasting.value = false
        _isRemotePlaying.value = false
    }

    private fun updateRemotePlaybackState(remoteMediaClient: RemoteMediaClient?) {
        _isRemotePlaying.value = remoteMediaClient?.isPlaying == true || remoteMediaClient?.isBuffering == true
    }

    private companion object {
        const val LOG_TAG = "CastManager"
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
