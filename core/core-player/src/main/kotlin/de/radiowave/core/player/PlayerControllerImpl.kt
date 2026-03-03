package de.radiowave.core.player

import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import de.radiowave.core.data.repository.StationRepository
import de.radiowave.core.model.AppSettings
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
@UnstableApi
class PlayerControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stationRepository: StationRepository,
) : PlayerController {

    private var exoPlayer: ExoPlayer? = null
    private var reconnectJob: Job? = null
    private var bufferingWatchdogJob: Job? = null
    private var playbackLostRecoveryJob: Job? = null
    private var restartGuardJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var reconnectAttempts = 0
    private var playbackLostRecoveryAttempts = 0
    private var userPausedPlayback = false
    private var isStopping = false
    private val maxReconnectAttempts = 8
    private val maxPlaybackLostRecoveryAttempts = 3
    private val initialReconnectDelayMs = 1_500L
    private val maxReconnectDelayMs = 25_000L
    private val networkRecoveryDelayMs = 350L
    private val networkRecoveryCooldownMs = 1_500L
    private val playbackLostRecoveryDelayMs = 450L
    private val bufferingStallThresholdMs = 18_000L
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(ConnectivityManager::class.java)
    private val settingsPrefs by lazy {
        context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)
    }
    private var isNetworkCallbackRegistered = false
    private var networkLossObserved = false
    private var lastNetworkRecoveryAt = 0L
    private var isForegroundServiceRunning = false
    private var isInternalRestartInProgress = false
    private var shouldResumeAfterAudioFocusGain = false
    private var wasDuckedForAudioFocus = false
    private var activeBufferProfile: String? = null
    private val playbackBackStack = ArrayDeque<Station>()
    private val playbackForwardStack = ArrayDeque<Station>()
    private var stationPool: List<Station> = emptyList()
    private var stationPoolJob: Job? = null
    private val maxPlaybackHistorySize = 40
    private val audioManager: AudioManager? = context.getSystemService(AudioManager::class.java)
    private val isDebuggableApp: Boolean by lazy {
        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        controllerScope.launch {
            handleAudioFocusChange(focusChange)
        }
    }
    private val audioFocusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                PlatformAudioAttributes.Builder()
                    .setContentType(PlatformAudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(PlatformAudioAttributes.USAGE_MEDIA)
                    .build(),
            )
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
    }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            controllerScope.launch {
                attemptRecoveryAfterNetworkReturn()
            }
        }

        override fun onLost(network: Network) {
            networkLossObserved = true
        }
    }

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        warmUpStationPool()
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: createPlayer(getSelectedBufferProfile()).also { player ->
            setupPlayerListeners(player)
            registerNetworkCallbackIfNeeded()
            initializePlaybackLocks()
            player.volume = if (_playerState.value.isMuted) 0f else 1f
            exoPlayer = player
        }
    }

    private fun initializePlaybackLocks() {
        if (wakeLock == null) {
            val powerManager = context.getSystemService(PowerManager::class.java)
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RadioWave:PlayerWakeLock",
            )?.apply {
                setReferenceCounted(false)
            }
        }

        if (wifiLock == null) {
            val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
            wifiLock = wifiManager?.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "RadioWave:PlayerWifiLock",
            )?.apply {
                setReferenceCounted(false)
            }
        }
    }

    private fun updatePlaybackLocks(player: ExoPlayer?) {
        val currentPlayer = player ?: return
        val keepAwake = currentPlayer.playWhenReady &&
            (currentPlayer.isPlaying || currentPlayer.playbackState == Player.STATE_BUFFERING)

        if (keepAwake) {
            try {
                if (wakeLock?.isHeld != true) {
                    wakeLock?.acquire(30 * 60 * 1000L)
                }
            } catch (error: SecurityException) {
                logWarning("WakeLock acquire failed: ${error.message}")
            }

            try {
                if (wifiLock?.isHeld != true) {
                    wifiLock?.acquire()
                }
            } catch (error: SecurityException) {
                logWarning("WifiLock acquire failed: ${error.message}")
            }
        } else {
            releasePlaybackLocks()
        }
    }

    private fun releasePlaybackLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (error: RuntimeException) {
            logWarning("WakeLock release failed: ${error.message}")
        }

        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (error: RuntimeException) {
            logWarning("WifiLock release failed: ${error.message}")
        }
    }

    private fun createPlayer(bufferProfile: String): ExoPlayer {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("RadioWave/1.0")
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)
        val bufferDurations = resolveBufferDurations(bufferProfile)

        return ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                false,
            )
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        bufferDurations.minBufferMs,
                        bufferDurations.maxBufferMs,
                        bufferDurations.bufferForPlaybackMs,
                        bufferDurations.bufferForPlaybackAfterRebufferMs,
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
            .also { activeBufferProfile = bufferProfile }
    }

    private fun getSelectedBufferProfile(): String {
        val value = settingsPrefs.getString(
            AppSettings.KEY_BUFFER_PROFILE,
            AppSettings.BUFFER_MEDIUM,
        )
        return when (value) {
            AppSettings.BUFFER_SMALL,
            AppSettings.BUFFER_MEDIUM,
            AppSettings.BUFFER_LARGE,
            -> value

            else -> AppSettings.BUFFER_MEDIUM
        }
    }

    private fun isPlaybackBlockedByMobileDataPolicy(): Boolean {
        val allowMobileData = settingsPrefs.getBoolean(AppSettings.KEY_ALLOW_MOBILE_DATA, true)
        if (allowMobileData) return false

        val manager = connectivityManager ?: return false
        val activeNetwork = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(activeNetwork) ?: return false

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return false
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return false
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return true
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return false

        return true
    }

    private fun resolveBufferDurations(profile: String): BufferDurations {
        return when (profile) {
            AppSettings.BUFFER_SMALL -> BufferDurations(
                minBufferMs = 20_000,
                maxBufferMs = 90_000,
                bufferForPlaybackMs = 1_500,
                bufferForPlaybackAfterRebufferMs = 3_000,
            )

            AppSettings.BUFFER_LARGE -> BufferDurations(
                minBufferMs = 60_000,
                maxBufferMs = 240_000,
                bufferForPlaybackMs = 3_000,
                bufferForPlaybackAfterRebufferMs = 6_500,
            )

            else -> BufferDurations(
                minBufferMs = 45_000,
                maxBufferMs = 180_000,
                bufferForPlaybackMs = 2_500,
                bufferForPlaybackAfterRebufferMs = 5_000,
            )
        }
    }

    private fun recreatePlayerForBufferProfileIfNeeded() {
        val selectedProfile = getSelectedBufferProfile()
        if (activeBufferProfile == null || activeBufferProfile == selectedProfile) return

        exoPlayer?.release()
        exoPlayer = null
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
                if (isPlaying) {
                    userPausedPlayback = false
                    playbackLostRecoveryAttempts = 0
                    playbackLostRecoveryJob?.cancel()
                    ensureForegroundPlaybackServiceRunning()
                } else if (
                    !userPausedPlayback &&
                    !isStopping &&
                    !isInternalRestartInProgress &&
                    _playerState.value.currentStation != null &&
                    player.playWhenReady &&
                    player.playbackState == Player.STATE_READY
                ) {
                    triggerPlaybackLostRecovery(
                        station = _playerState.value.currentStation ?: return,
                        reason = "unexpected-not-playing",
                    )
                } else if (isStopping) {
                    stopForegroundPlaybackServiceIfRunning()
                } else {
                    val stationName = _playerState.value.currentStation?.name.orEmpty()
                    updateForegroundPlaybackNotification(
                        stationName = stationName,
                        subtitle = resolveNotificationSubtitle(isPlaying = false),
                    )
                }
                updatePlaybackLocks(player)
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        isInternalRestartInProgress = false
                        restartGuardJob?.cancel()
                        logDebug("Player: STATE_BUFFERING")
                        startBufferingWatchdog(player)
                    }

                    Player.STATE_READY -> {
                        isInternalRestartInProgress = false
                        restartGuardJob?.cancel()
                        logDebug("Player: STATE_READY")
                        reconnectAttempts = 0
                        playbackLostRecoveryAttempts = 0
                        reconnectJob?.cancel()
                        bufferingWatchdogJob?.cancel()
                        playbackLostRecoveryJob?.cancel()
                    }

                    Player.STATE_ENDED -> {
                        logDebug("Player: STATE_ENDED")
                        bufferingWatchdogJob?.cancel()
                        maybeRecoverFromLostState(player, reason = "state-ended")
                    }

                    Player.STATE_IDLE -> {
                        logDebug("Player: STATE_IDLE")
                        bufferingWatchdogJob?.cancel()
                        maybeRecoverFromLostState(player, reason = "state-idle")
                    }
                }

                _playerState.update { current ->
                    current.copy(
                        isBuffering = state == Player.STATE_BUFFERING,
                        isLoading = state == Player.STATE_BUFFERING,
                    )
                }
                updatePlaybackLocks(player)
            }

            override fun onPlayerError(error: PlaybackException) {
                val station = _playerState.value.currentStation
                logError("Player error ${error.errorCode}: ${error.message}", error)
                bufferingWatchdogJob?.cancel()

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
                val stationName = _playerState.value.currentStation?.name?.trim()
                val title = metadata.title?.toString()?.trim()
                val artist = metadata.artist?.toString()?.trim()
                val albumArtUrl = metadata.artworkUri?.toString()
                val hasUsefulMetadata = (!title.isNullOrBlank() && !title.equals(stationName, ignoreCase = true)) ||
                    !artist.isNullOrBlank() ||
                    !albumArtUrl.isNullOrBlank()

                if (!hasUsefulMetadata) {
                    return
                }

                _playerState.update {
                    val previous = it.metadata ?: StreamMetadata()
                    it.copy(
                        metadata = previous.copy(
                            title = title ?: previous.title,
                            artist = artist ?: previous.artist,
                            albumArtUrl = albumArtUrl ?: previous.albumArtUrl,
                        ),
                    )
                }

                val currentStationName = _playerState.value.currentStation?.name.orEmpty()
                updateForegroundPlaybackNotification(
                    stationName = currentStationName,
                    subtitle = listOfNotNull(artist, title).joinToString(" - ").ifBlank {
                        resolveNotificationSubtitle(isPlaying = _playerState.value.isPlaying)
                    },
                )
            }

            override fun onMetadata(metadata: Metadata) {
                val streamTitle = extractStreamTitle(metadata) ?: return
                applyStreamTitle(streamTitle)
            }
        })
    }

    private fun maybeRecoverFromLostState(player: ExoPlayer, reason: String) {
        if (isStopping || userPausedPlayback) return
        if (isInternalRestartInProgress) return
        if (!player.playWhenReady) return
        val station = _playerState.value.currentStation ?: return
        triggerPlaybackLostRecovery(station, reason)
    }

    private fun startBufferingWatchdog(player: ExoPlayer) {
        bufferingWatchdogJob?.cancel()
        val stationUuid = _playerState.value.currentStation?.uuid ?: return

        bufferingWatchdogJob = controllerScope.launch {
            delay(bufferingStallThresholdMs)

            val currentStation = _playerState.value.currentStation ?: return@launch
            if (currentStation.uuid != stationUuid) return@launch
            if (isStopping || userPausedPlayback) return@launch
            if (!player.playWhenReady) return@launch
            if (player.playbackState != Player.STATE_BUFFERING) return@launch

            logWarning("Buffering stall detected after ${bufferingStallThresholdMs}ms")
            triggerPlaybackLostRecovery(currentStation, reason = "buffer-stall")
        }
    }

    private fun triggerPlaybackLostRecovery(station: Station, reason: String) {
        if (playbackLostRecoveryJob?.isActive == true) return
        if (reconnectJob?.isActive == true) return
        if (isStopping || userPausedPlayback) return
        if (isInternalRestartInProgress) return

        if (playbackLostRecoveryAttempts >= maxPlaybackLostRecoveryAttempts) {
            logError("Playback lost recovery exhausted: $reason")
            _playerState.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    isLoading = false,
                    error = PlayerError.NetworkError,
                )
            }
            return
        }

        playbackLostRecoveryAttempts++
        logWarning(
            "Playback lost recovery $playbackLostRecoveryAttempts/$maxPlaybackLostRecoveryAttempts ($reason)",
        )

        playbackLostRecoveryJob = controllerScope.launch {
            _playerState.update {
                it.copy(
                    error = null,
                    isBuffering = true,
                    isLoading = true,
                )
            }

            delay(playbackLostRecoveryDelayMs)

            val player = exoPlayer ?: return@launch
            val currentStation = _playerState.value.currentStation ?: return@launch
            if (currentStation.uuid != station.uuid) return@launch
            if (isStopping || userPausedPlayback) return@launch

            restartStream(player, station)
        }
    }

    private fun extractStreamTitle(metadata: Metadata): String? {
        for (index in 0 until metadata.length()) {
            val entry = metadata[index]

            if (entry is IcyInfo) {
                val title = entry.title?.trim()
                if (!title.isNullOrBlank()) {
                    return title
                }
                val fromRaw = extractStreamTitleFromMetadataText(entry.toString())
                if (!fromRaw.isNullOrBlank()) {
                    return fromRaw
                }
            }

            val fallback = extractStreamTitleFromMetadataText(entry.toString())
            if (!fallback.isNullOrBlank()) {
                return fallback
            }
        }
        return null
    }

    private fun extractStreamTitleFromMetadataText(text: String): String? {
        val streamTitleRegex = Regex("(?i)StreamTitle='([^']*)'")
        val streamTitleMatch = streamTitleRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
        if (!streamTitleMatch.isNullOrBlank()) {
            return streamTitleMatch
        }

        val titleRegex = Regex("(?i)title=([^,}]+)")
        val titleMatch = titleRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
        return titleMatch?.trim('"', '\'')
    }

    private fun applyStreamTitle(rawTitle: String) {
        val titleText = rawTitle.trim().takeUnless { it.isBlank() } ?: return
        val stationName = _playerState.value.currentStation?.name?.trim()
        if (stationName != null && titleText.equals(stationName, ignoreCase = true)) {
            return
        }

        val splitPattern = Regex("\\s[-–|]\\s")
        val parts = splitPattern.split(titleText, limit = 2)
        val parsedArtist = parts.getOrNull(0)?.trim().takeUnless { it.isNullOrBlank() }
        val parsedTitle = when {
            parts.size >= 2 -> parts[1].trim()
            else -> titleText
        }.takeUnless { it.isBlank() }

        _playerState.update {
            val previous = it.metadata ?: StreamMetadata()
            it.copy(
                metadata = previous.copy(
                    title = parsedTitle ?: previous.title,
                    artist = parsedArtist ?: previous.artist,
                ),
            )
        }
    }

    private fun registerNetworkCallbackIfNeeded() {
        if (isNetworkCallbackRegistered) return
        val manager = connectivityManager ?: return

        try {
            manager.registerDefaultNetworkCallback(networkCallback)
            isNetworkCallbackRegistered = true
        } catch (error: Exception) {
            logWarning("Unable to register network callback: ${error.message}")
        }
    }

    private fun unregisterNetworkCallbackIfNeeded() {
        if (!isNetworkCallbackRegistered) return
        val manager = connectivityManager ?: return

        try {
            manager.unregisterNetworkCallback(networkCallback)
        } catch (error: Exception) {
            logWarning("Unable to unregister network callback: ${error.message}")
        } finally {
            isNetworkCallbackRegistered = false
        }
    }

    private suspend fun attemptRecoveryAfterNetworkReturn() {
        if (!networkLossObserved) return

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

        networkLossObserved = false
        logDebug("Network restored. Triggering fast stream recovery.")
        reconnectAttempts = 0
        scheduleReconnect(station, delayOverrideMs = networkRecoveryDelayMs, countAttempt = false)
    }

    private fun scheduleReconnect(
        station: Station,
        delayOverrideMs: Long? = null,
        countAttempt: Boolean = true,
    ) {
        bufferingWatchdogJob?.cancel()
        if (countAttempt) {
            reconnectAttempts++
        }
        val effectiveAttempt = if (reconnectAttempts == 0) 1 else reconnectAttempts
        val delayMs = delayOverrideMs ?: computeReconnectDelayMs(effectiveAttempt)
        reconnectJob?.cancel()

        logDebug("Reconnect $effectiveAttempt/$maxReconnectAttempts in ${delayMs}ms")

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
        isInternalRestartInProgress = true
        restartGuardJob?.cancel()
        restartGuardJob = controllerScope.launch {
            delay(2_000L)
            isInternalRestartInProgress = false
        }
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

    private suspend fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pausePlaybackForAudioFocus(resumeWhenFocusReturns = false)
                abandonAudioFocus()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            ->
                duckPlaybackForAudioFocus()

            AudioManager.AUDIOFOCUS_GAIN -> resumePlaybackAfterAudioFocusGainIfNeeded()
        }
    }

    private fun pausePlaybackForAudioFocus(resumeWhenFocusReturns: Boolean) {
        val player = exoPlayer ?: return
        val shouldResume = resumeWhenFocusReturns &&
            !userPausedPlayback &&
            _playerState.value.currentStation != null &&
            (player.isPlaying || player.playWhenReady)
        reconnectJob?.cancel()
        bufferingWatchdogJob?.cancel()
        playbackLostRecoveryJob?.cancel()
        player.pause()
        unregisterNetworkCallbackIfNeeded()
        shouldResumeAfterAudioFocusGain = shouldResume
        wasDuckedForAudioFocus = false
    }

    private fun duckPlaybackForAudioFocus() {
        val player = exoPlayer ?: return
        if (_playerState.value.isMuted) return
        player.volume = 0.25f
        wasDuckedForAudioFocus = true
    }

    private fun resumePlaybackAfterAudioFocusGainIfNeeded() {
        val player = exoPlayer ?: return
        if (wasDuckedForAudioFocus) {
            player.volume = if (_playerState.value.isMuted) 0f else 1f
            wasDuckedForAudioFocus = false
        }
        if (!shouldResumeAfterAudioFocusGain) return
        shouldResumeAfterAudioFocusGain = false

        if (_playerState.value.currentStation == null) return
        if (userPausedPlayback) return

        registerNetworkCallbackIfNeeded()
        player.playWhenReady = true
        player.play()
    }

    private fun requestAudioFocus(): Boolean {
        val manager = audioManager ?: return true
        val result = manager.requestAudioFocus(audioFocusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        shouldResumeAfterAudioFocusGain = false
        wasDuckedForAudioFocus = false
        manager.abandonAudioFocusRequest(audioFocusRequest)
    }

    private fun warmUpStationPool() {
        if (stationPoolJob?.isActive == true) return
        stationPoolJob = controllerScope.launch(Dispatchers.IO) {
            val loaded = stationRepository.getTopStations().firstOrNull().orEmpty()
                .filter { it.streamUrl.isNotBlank() }
                .distinctBy { it.uuid }
            if (loaded.isNotEmpty()) {
                stationPool = loaded
            }
        }
    }

    private fun rememberCurrentStationForBackNavigation(nextStation: Station) {
        val currentStation = _playerState.value.currentStation ?: return
        if (currentStation.uuid == nextStation.uuid) return
        if (playbackBackStack.lastOrNull()?.uuid == currentStation.uuid) return

        playbackBackStack.addLast(currentStation)
        while (playbackBackStack.size > maxPlaybackHistorySize) {
            playbackBackStack.removeFirst()
        }
    }

    private fun playStationFromNavigation(target: Station) {
        controllerScope.launch {
            playStation(target, addCurrentToBackStack = false, clearForwardStack = false)
        }
    }

    private suspend fun pickNextStationCandidate(): Station? {
        val currentStationUuid = _playerState.value.currentStation?.uuid
        val currentPool = if (stationPool.isNotEmpty()) {
            stationPool
        } else {
            stationRepository.getTopStations().firstOrNull().orEmpty()
                .filter { it.streamUrl.isNotBlank() }
                .distinctBy { it.uuid }
                .also { loaded ->
                    if (loaded.isNotEmpty()) {
                        stationPool = loaded
                    }
                }
        }

        if (currentPool.isEmpty()) return null

        val candidates = currentPool.filterNot { it.uuid == currentStationUuid }
        return if (candidates.isNotEmpty()) {
            candidates.random()
        } else {
            currentPool.random()
        }
    }

    private suspend fun playStation(
        station: Station,
        addCurrentToBackStack: Boolean,
        clearForwardStack: Boolean,
    ) {
        if (addCurrentToBackStack) {
            rememberCurrentStationForBackNavigation(station)
        }
        if (clearForwardStack) {
            playbackForwardStack.clear()
        }
        startPlayback(station)
    }

    override suspend fun playStation(station: Station) {
        playStation(
            station = station,
            addCurrentToBackStack = true,
            clearForwardStack = true,
        )
    }

    override fun playPreviousStation() {
        val currentStation = _playerState.value.currentStation ?: return
        val previousStation = playbackBackStack.removeLastOrNull() ?: return
        playbackForwardStack.addLast(currentStation)
        while (playbackForwardStack.size > maxPlaybackHistorySize) {
            playbackForwardStack.removeFirst()
        }
        playStationFromNavigation(previousStation)
    }

    override fun playNextStation() {
        val currentStation = _playerState.value.currentStation
        val forwardStation = playbackForwardStack.removeLastOrNull()
        if (forwardStation != null) {
            if (currentStation != null) {
                playbackBackStack.addLast(currentStation)
                while (playbackBackStack.size > maxPlaybackHistorySize) {
                    playbackBackStack.removeFirst()
                }
            }
            playStationFromNavigation(forwardStation)
            return
        }

        controllerScope.launch {
            val nextStation = pickNextStationCandidate() ?: return@launch
            playStation(
                station = nextStation,
                addCurrentToBackStack = true,
                clearForwardStack = true,
            )
        }
    }

    private suspend fun startPlayback(station: Station) {
        reconnectJob?.cancel()
        bufferingWatchdogJob?.cancel()
        playbackLostRecoveryJob?.cancel()
        restartGuardJob?.cancel()
        reconnectAttempts = 0
        playbackLostRecoveryAttempts = 0
        networkLossObserved = false
        userPausedPlayback = false
        isStopping = false

        if (isPlaybackBlockedByMobileDataPolicy()) {
            _playerState.update {
                it.copy(
                    currentStation = station,
                    error = PlayerError.Unknown("WLAN erforderlich (Mobile Daten deaktiviert)"),
                    isPlaying = false,
                    isLoading = false,
                    isBuffering = false,
                )
            }
            return
        }

        if (!requestAudioFocus()) {
            _playerState.update {
                it.copy(
                    currentStation = station,
                    error = PlayerError.Unknown("Audio focus not available"),
                    isPlaying = false,
                    isLoading = false,
                    isBuffering = false,
                )
            }
            return
        }
        recreatePlayerForBufferProfileIfNeeded()

        _playerState.update {
            it.copy(
                currentStation = station,
                error = null,
                isLoading = true,
                isBuffering = true,
                sessionStartedAtElapsedMs = SystemClock.elapsedRealtime(),
                metadata = null,
            )
        }
        persistLastStation(station)

        val player = getOrCreatePlayer()
        ensureForegroundPlaybackServiceRunning(
            stationName = station.name,
            subtitle = "Buffering...",
        )
        restartStream(player, station)
    }

    private fun persistLastStation(station: Station) {
        settingsPrefs.edit()
            .putString(AppSettings.KEY_LAST_STATION_UUID, station.uuid)
            .putString(AppSettings.KEY_LAST_STATION_NAME, station.name)
            .putString(AppSettings.KEY_LAST_STATION_STREAM_URL, station.streamUrl)
            .putString(AppSettings.KEY_LAST_STATION_FAVICON_URL, station.faviconUrl)
            .putString(AppSettings.KEY_LAST_STATION_COUNTRY, station.country)
            .apply()
    }

    override fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                userPausedPlayback = true
                shouldResumeAfterAudioFocusGain = false
                wasDuckedForAudioFocus = false
                reconnectJob?.cancel()
                bufferingWatchdogJob?.cancel()
                playbackLostRecoveryJob?.cancel()
                player.pause()
                unregisterNetworkCallbackIfNeeded()
                abandonAudioFocus()
            } else {
                if (!requestAudioFocus()) {
                    _playerState.update {
                        it.copy(
                            error = PlayerError.Unknown("Audio focus not available"),
                            isPlaying = false,
                            isLoading = false,
                            isBuffering = false,
                        )
                    }
                    return
                }
                userPausedPlayback = false
                registerNetworkCallbackIfNeeded()
                player.playWhenReady = true
                player.play()
            }
        }
    }

    override fun toggleMute() {
        val player = exoPlayer ?: return
        val shouldMute = !_playerState.value.isMuted
        player.volume = if (shouldMute) 0f else 1f
        _playerState.update { it.copy(isMuted = shouldMute) }
    }

    override fun sessionPlayer(): Player? = exoPlayer

    override fun stop() {
        isStopping = true
        reconnectJob?.cancel()
        bufferingWatchdogJob?.cancel()
        playbackLostRecoveryJob?.cancel()
        restartGuardJob?.cancel()
        reconnectAttempts = 0
        playbackLostRecoveryAttempts = 0
        networkLossObserved = false
        userPausedPlayback = false
        shouldResumeAfterAudioFocusGain = false
        wasDuckedForAudioFocus = false
        exoPlayer?.stop()
        playbackBackStack.clear()
        playbackForwardStack.clear()
        unregisterNetworkCallbackIfNeeded()
        stopForegroundPlaybackServiceIfRunning()
        releasePlaybackLocks()
        abandonAudioFocus()
        _playerState.update { PlayerState() }
        isStopping = false
    }

    override fun release() {
        isStopping = true
        controllerScope.cancel()
        reconnectJob?.cancel()
        bufferingWatchdogJob?.cancel()
        playbackLostRecoveryJob?.cancel()
        reconnectAttempts = 0
        playbackLostRecoveryAttempts = 0
        networkLossObserved = false
        userPausedPlayback = false
        shouldResumeAfterAudioFocusGain = false
        wasDuckedForAudioFocus = false
        stationPoolJob?.cancel()
        playbackBackStack.clear()
        playbackForwardStack.clear()
        stationPool = emptyList()
        unregisterNetworkCallbackIfNeeded()
        stopForegroundPlaybackServiceIfRunning()
        releasePlaybackLocks()
        abandonAudioFocus()
        exoPlayer?.release()
        exoPlayer = null
        activeBufferProfile = null
        wakeLock = null
        wifiLock = null
        _playerState.update { PlayerState() }
        isStopping = false
    }

    private fun ensureForegroundPlaybackServiceRunning(
        stationName: String = _playerState.value.currentStation?.name.orEmpty(),
        subtitle: String = resolveNotificationSubtitle(isPlaying = _playerState.value.isPlaying),
    ) {
        try {
            PlaybackForegroundService.start(
                context = context,
                stationName = stationName,
                subtitle = subtitle,
                isPlaying = _playerState.value.isPlaying,
            )
            isForegroundServiceRunning = true
        } catch (error: Exception) {
            logWarning("Unable to start playback foreground service: ${error.message}")
        }
    }

    private fun updateForegroundPlaybackNotification(
        stationName: String,
        subtitle: String,
    ) {
        if (!isForegroundServiceRunning) return
        ensureForegroundPlaybackServiceRunning(
            stationName = stationName,
            subtitle = subtitle,
        )
    }

    private fun resolveNotificationSubtitle(isPlaying: Boolean): String {
        val metadata = _playerState.value.metadata
        val metadataText = listOfNotNull(
            metadata?.artist?.trim().takeUnless { it.isNullOrBlank() },
            metadata?.title?.trim().takeUnless { it.isNullOrBlank() },
        ).joinToString(" - ")
        if (metadataText.isNotBlank()) return metadataText
        return if (isPlaying) "Live stream playing" else "Playback paused"
    }

    private fun stopForegroundPlaybackServiceIfRunning() {
        if (!isForegroundServiceRunning) return
        try {
            PlaybackForegroundService.stop(context)
        } catch (error: Exception) {
            logWarning("Unable to stop playback foreground service: ${error.message}")
        } finally {
            isForegroundServiceRunning = false
            isInternalRestartInProgress = false
        }
    }

    private fun logDebug(message: String) {
        if (isDebuggableApp) {
            Log.d(APP_LOG_TAG, message)
        }
    }

    private fun logWarning(message: String) {
        if (isDebuggableApp) {
            Log.w(APP_LOG_TAG, message)
        }
    }

    private fun logError(message: String, throwable: Throwable? = null) {
        if (!isDebuggableApp) return
        if (throwable == null) {
            Log.e(APP_LOG_TAG, message)
        } else {
            Log.e(APP_LOG_TAG, message, throwable)
        }
    }

    private companion object {
        const val APP_LOG_TAG = "RadioWave"
    }
}

private data class BufferDurations(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int,
)

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
