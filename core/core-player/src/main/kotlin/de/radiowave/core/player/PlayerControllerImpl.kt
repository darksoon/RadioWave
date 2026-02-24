package de.radiowave.core.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import de.radiowave.core.model.PlayerError
import de.radiowave.core.model.PlayerState
import de.radiowave.core.model.Station
import de.radiowave.core.model.StreamMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
@UnstableApi
class PlayerControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlayerController {

    private var exoPlayer: ExoPlayer? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 8
    private val initialReconnectDelayMs = 1_500L
    private val maxReconnectDelayMs = 25_000L
    private val networkRecoveryDelayMs = 350L
    private val networkRecoveryCooldownMs = 1_500L
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(ConnectivityManager::class.java)
    private var isNetworkCallbackRegistered = false
    private var lastNetworkRecoveryAt = 0L
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            controllerScope.launch {
                attemptRecoveryAfterNetworkReturn()
            }
        }
    }

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: createPlayer().also { player ->
            setupPlayerListeners(player)
            registerNetworkCallbackIfNeeded()
            exoPlayer = player
        }
    }

    private fun createPlayer(): ExoPlayer {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("RadioWave/1.0")
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)

        return ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        45_000,  // minBufferMs
                        180_000, // maxBufferMs
                        2_500,   // bufferForPlaybackMs
                        5_000,   // bufferForPlaybackAfterRebufferMs
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build(),
            )
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(httpDataSourceFactory)
                    .setLoadErrorHandlingPolicy(RadioLoadErrorHandlingPolicy()),
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    private fun setupPlayerListeners(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update {
                    it.copy(
                        isPlaying = isPlaying,
                        sessionStartedAtElapsedMs = when {
                            it.sessionStartedAtElapsedMs != null -> it.sessionStartedAtElapsedMs
                            isPlaying -> SystemClock.elapsedRealtime()
                            else -> null
                        },
                        error = if (isPlaying) null else it.error,
                    )
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        Log.d("RadioWave", "Player: STATE_BUFFERING")
                    }

                    Player.STATE_READY -> {
                        Log.d("RadioWave", "Player: STATE_READY")
                        reconnectAttempts = 0
                        reconnectJob?.cancel()
                    }

                    Player.STATE_ENDED -> {
                        Log.d("RadioWave", "Player: STATE_ENDED")
                    }

                    Player.STATE_IDLE -> {
                        Log.d("RadioWave", "Player: STATE_IDLE")
                    }
                }

                _playerState.update { current ->
                    current.copy(
                        isBuffering = state == Player.STATE_BUFFERING,
                        isLoading = state == Player.STATE_BUFFERING,
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val station = _playerState.value.currentStation
                Log.e(
                    "RadioWave",
                    "Player error ${error.errorCode}: ${error.message}",
                )

                if (station != null && reconnectAttempts < maxReconnectAttempts) {
                    scheduleReconnect(station)
                    return
                }

                _playerState.update {
                    it.copy(
                        error = mapPlayerError(error),
                        isPlaying = false,
                        isBuffering = false,
                        isLoading = false,
                    )
                }
                reconnectAttempts = 0
            }

            override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                _playerState.update {
                    it.copy(
                        metadata = StreamMetadata(
                            title = metadata.title?.toString(),
                            artist = metadata.artist?.toString(),
                            albumArtUrl = metadata.artworkUri?.toString(),
                        ),
                    )
                }
            }
        })
    }

    private fun registerNetworkCallbackIfNeeded() {
        if (isNetworkCallbackRegistered) return
        val manager = connectivityManager ?: return

        try {
            manager.registerDefaultNetworkCallback(networkCallback)
            isNetworkCallbackRegistered = true
        } catch (error: Exception) {
            Log.w("RadioWave", "Unable to register network callback: ${error.message}")
        }
    }

    private fun unregisterNetworkCallbackIfNeeded() {
        if (!isNetworkCallbackRegistered) return
        val manager = connectivityManager ?: return

        try {
            manager.unregisterNetworkCallback(networkCallback)
        } catch (error: Exception) {
            Log.w("RadioWave", "Unable to unregister network callback: ${error.message}")
        } finally {
            isNetworkCallbackRegistered = false
        }
    }

    private suspend fun attemptRecoveryAfterNetworkReturn() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNetworkRecoveryAt < networkRecoveryCooldownMs) return
        lastNetworkRecoveryAt = now

        val station = _playerState.value.currentStation ?: return
        val player = exoPlayer ?: return
        if (player.isPlaying) return
        if (reconnectJob?.isActive == true) return

        val state = _playerState.value
        val shouldRecover = state.error is PlayerError.NetworkError || state.isLoading || state.isBuffering
        if (!shouldRecover) return

        Log.d("RadioWave", "Network restored. Triggering fast stream recovery.")
        reconnectAttempts = 0
        scheduleReconnect(station, delayOverrideMs = networkRecoveryDelayMs, countAttempt = false)
    }

    private fun scheduleReconnect(
        station: Station,
        delayOverrideMs: Long? = null,
        countAttempt: Boolean = true,
    ) {
        if (countAttempt) {
            reconnectAttempts++
        }
        val effectiveAttempt = if (reconnectAttempts == 0) 1 else reconnectAttempts
        val delayMs = delayOverrideMs ?: computeReconnectDelayMs(effectiveAttempt)
        reconnectJob?.cancel()

        Log.d(
            "RadioWave",
            "Reconnect $effectiveAttempt/$maxReconnectAttempts in ${delayMs}ms",
        )

        reconnectJob = controllerScope.launch {
            _playerState.update {
                it.copy(
                    error = null,
                    isBuffering = true,
                    isLoading = true,
                )
            }

            delay(delayMs)

            val player = exoPlayer ?: return@launch
            if (_playerState.value.currentStation?.uuid != station.uuid) return@launch

            restartStream(player, station)
        }
    }

    private fun computeReconnectDelayMs(attempt: Int): Long {
        val exponential = initialReconnectDelayMs * (1 shl (attempt - 1))
        return min(exponential, maxReconnectDelayMs)
    }

    private fun createMediaItem(station: Station): MediaItem {
        return MediaItem.Builder()
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtworkUri(station.faviconUrl?.let(Uri::parse))
                    .build(),
            )
            .build()
    }

    private fun restartStream(player: ExoPlayer, station: Station) {
        player.stop()
        player.setMediaItem(createMediaItem(station))
        player.prepare()
        player.playWhenReady = true
    }

    private fun mapPlayerError(error: PlaybackException): PlayerError {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
            -> PlayerError.NetworkError

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            -> PlayerError.StreamBroken

            else -> PlayerError.Unknown(error.message ?: "Unknown Error")
        }
    }

    override suspend fun playStation(station: Station) {
        reconnectJob?.cancel()
        reconnectAttempts = 0

        _playerState.update {
            it.copy(
                currentStation = station,
                error = null,
                isLoading = true,
                isBuffering = true,
                sessionStartedAtElapsedMs = SystemClock.elapsedRealtime(),
            )
        }

        val player = getOrCreatePlayer()
        restartStream(player, station)
    }

    override fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                reconnectJob?.cancel()
                player.pause()
            } else {
                player.playWhenReady = true
                player.play()
            }
        }
    }

    override fun stop() {
        reconnectJob?.cancel()
        reconnectAttempts = 0
        exoPlayer?.stop()
        _playerState.update { PlayerState() }
    }

    override fun release() {
        reconnectJob?.cancel()
        reconnectAttempts = 0
        unregisterNetworkCallbackIfNeeded()
        exoPlayer?.release()
        exoPlayer = null
        _playerState.update { PlayerState() }
    }
}

@UnstableApi
private class RadioLoadErrorHandlingPolicy : DefaultLoadErrorHandlingPolicy() {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val exception = loadErrorInfo.exception
        val errorCount = loadErrorInfo.errorCount

        if (exception is HttpDataSource.InvalidResponseCodeException) {
            if (exception.responseCode == 404 || exception.responseCode == 410) {
                return C.TIME_UNSET
            }
        }

        val baseDelayMs = min(1_000L * errorCount, 8_000L)
        return baseDelayMs
    }

    override fun getMinimumLoadableRetryCount(dataType: Int): Int {
        return if (dataType == C.DATA_TYPE_MEDIA) 12 else 6
    }
}
