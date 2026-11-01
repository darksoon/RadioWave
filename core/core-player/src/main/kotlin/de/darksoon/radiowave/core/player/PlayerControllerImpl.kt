// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
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
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import de.darksoon.radiowave.core.data.repository.StationRepository
import de.darksoon.radiowave.core.model.AppSettings
import de.darksoon.radiowave.core.model.PlayerError
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.model.StreamMetadata
import de.darksoon.radiowave.core.model.duplicateKey
import de.darksoon.radiowave.core.player.visualizer.AudioVisualizerTap
import de.darksoon.radiowave.core.player.visualizer.VisualizerAudioProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
    private val settingsRepository: de.darksoon.radiowave.core.data.repository.SettingsRepository,
    private val coverArtRepository: de.darksoon.radiowave.core.data.repository.CoverArtRepository,
    // REA 95fcb33: neuer fünfter Konstruktorparameter (cp3.f3405e = ll3), siehe
    // visualizer/AudioVisualizerTap.kt. Wird in den RenderersFactory gereicht.
    private val visualizerTap: AudioVisualizerTap,
) : PlayerController {

    private var exoPlayer: ExoPlayer? = null
    private var exoPlayerListener: Player.Listener? = null
    private val equalizerController = EqualizerController()
    private var reconnectJob: Job? = null
    private var bufferingWatchdogJob: Job? = null
    // Debounced iTunes cover lookup for the current ICY song — see scheduleCoverArtFetch.
    private var coverArtFetchJob: Job? = null
    private val coverArtFetchDebounceMs = 800L
    private var playbackLostRecoveryJob: Job? = null
    private var restartGuardJob: Job? = null
    private var reconnectAttempts = 0
    private var playbackLostRecoveryAttempts = 0
    // REA 95fcb33: Stream-Ausweichlogik bei Wiedergabefehlern (bp3.mo1238z, wi0, cp3.m1899h).
    // cp3.f3426s — fast retries of the same stream before looking for another stream
    // variant; cp3.f3422o — stream URLs that already failed in this session; cp3.f3423p —
    // set once an error is classified as permanent: no automatic recovery path may restart
    // the stream after that until the user starts/resumes playback again; cp3.f3424q —
    // bumped by every user/stop/pause action so a running failover lookup can tell that it
    // is stale; cp3.f3425r — the failover lookup job.
    // REA-UNSICHER: Namen geraten, Semantik aus den Zugriffen abgeleitet.
    private var quickRetryCount = 0
    private val maxQuickRetries = 2
    private val failedStreamUrls = LinkedHashSet<String>()
    private val maxFailedStreamUrls = 5
    private var isPermanentPlaybackFailure = false
    // FIX 2026-11-01: whether the CURRENT stream URL reached STATE_READY in this session.
    // Container/decoder errors after that are glitches of a live stream, not a broken URL.
    private var currentStreamReachedReady = false
    private var playbackGeneration = 0L
    private var streamFailoverJob: Job? = null
    private val failoverRestartDelayMs = 250L
    // REA 95fcb33: cp3.f3398a0 — re-checks the output route for the EQ every 3 s while
    // playing (ba2 Fall 12).
    private var equalizerRouteJob: Job? = null
    private var userPausedPlayback = false
    private var isStopping = false
    // Set by release(): late callbacks (Player.Listener, controller bind) must not
    // re-register callbacks or restart the service after controllerScope.cancel().
    @Volatile private var isReleased = false
    // Tracks whether registerClick was already called for the current station session.
    private var clickRegisteredForCurrentSession = false
    private val maxReconnectAttempts = 10
    private val maxPlaybackLostRecoveryAttempts = 4
    private val initialReconnectDelayMs = 1_000L
    private val maxReconnectDelayMs = 18_000L
    private val networkRecoveryDelayMs = 250L
    private val networkRecoveryCooldownMs = 2_500L
    private val playbackLostRecoveryDelayMs = 700L
    private val defaultBufferingStallThresholdMs = 24_000L
    private val timeshiftGuardBufferingStallThresholdMs = 60_000L
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(ConnectivityManager::class.java)
    // Cached snapshot of all settings from DataStore via SettingsRepository.
    // Stream-started Eagerly so synchronous reads (.value) always have a value
    // before the first stream is started.
    private val settingsState: kotlinx.coroutines.flow.StateFlow<de.darksoon.radiowave.core.data.repository.AppSettingsState> =
        settingsRepository.data.stateIn(
            scope = controllerScope,
            started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
            // Synchronous snapshot — avoids a window after construction where
            // settings reads (mobile data policy, buffer profile, thermal mode,
            // last station for auto-resume, ...) return DEFAULTS instead of the
            // user's saved values.
            initialValue = settingsRepository.initialSnapshotBlocking(),
        )
    private var isNetworkCallbackRegistered = false
    private var networkLossObserved = false
    private var lastNetworkRecoveryAt = 0L
    private var isForegroundServiceRunning = false
    private var isAutomotivePerformanceModeEnabled = false
    private var isInternalRestartInProgress = false
    private var shouldResumeAfterAudioFocusGain = false
    private var wasDuckedForAudioFocus = false
    private var activeBufferProfile: String? = null
    private var lastMetadataUpdateAtElapsedMs = 0L
    private var lastPlaybackResumedAtElapsedMs: Long? = null
    private val playbackBackStack = ArrayDeque<Station>()
    private val playbackForwardStack = ArrayDeque<Station>()
    private var stationPool: List<Station> = emptyList()
    private var stationPoolJob: Job? = null
    private val maxPlaybackHistorySize = 40
    private val audioManager: AudioManager? = context.getSystemService(AudioManager::class.java)
    private var audioDeviceCallback: AudioDeviceCallback? = null
    // REA 95fcb33: cp3.f3414i0 (lazy e52 Fall 21) — media attributes for
    // AudioManager.getAudioDevicesForAttributes (API 33+).
    private val mediaPlaybackAttributes: PlatformAudioAttributes by lazy {
        PlatformAudioAttributes.Builder()
            .setUsage(PlatformAudioAttributes.USAGE_MEDIA)
            .setContentType(PlatformAudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }
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
            // Accept delayed focus: when getting into the car the head-unit audio
            // route (BT/USB) often isn't established yet, so the initial focus
            // request can't be granted immediately. With delayed gain the system
            // grants focus a moment later via onAudioFocusChange(GAIN); we start
            // the pending station then instead of failing outright.
            .setAcceptsDelayedFocusGain(true)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
    }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            controllerScope.launch {
                if (blockPlaybackIfMobileDataForbidden()) return@launch
                attemptRecoveryAfterNetworkReturn()
            }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            controllerScope.launch {
                if (network == connectivityManager?.activeNetwork) {
                    blockPlaybackIfMobileDataForbidden()
                }
            }
        }

        override fun onLost(network: Network) {
            controllerScope.launch { networkLossObserved = true }
        }
    }

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        controllerScope.launch {
            settingsState.map { it.allowMobileData }.distinctUntilChanged().collect {
                if (exoPlayer?.playWhenReady == true || _playerState.value.isLoading) {
                    blockPlaybackIfMobileDataForbidden()
                }
            }
        }
        // React to EQ preset changes made from the settings screen — applied
        // immediately to the live Equalizer (if attached) and remembered for
        // the next attach() (station/session change). Derived from settingsState
        // (already stateIn'd against settingsRepository.data above) rather than
        // SettingsRepository's own granular eqPreset Flow, since that property is
        // built in SettingsRepository's own constructor and would observe an
        // unset `data` if a subclass (e.g. a test fake) overrides it.
        controllerScope.launch {
            settingsState
                .map { it.eqPreset }
                .distinctUntilChanged()
                .collect { preset -> equalizerController.applyPreset(preset) }
        }
    }

    private var onSessionPlayerChanged: ((Player) -> Unit)? = null

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: createPlayer(getSelectedBufferProfile()).also { player ->
            setupPlayerListeners(player)
            registerNetworkCallbackIfNeeded()
            registerAudioDeviceCallbackIfNeeded()
            player.volume = if (_playerState.value.isMuted) 0f else 1f
            exoPlayer = player
            // Media3 assigns the session id asynchronously and reports it through
            // onAudioSessionIdChanged; if it is already known, hand it over now so a
            // preset chosen later still finds the session.
            if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                equalizerController.attach(
                    player.audioSessionId,
                    outputAllowsEffect = isEqualizerAllowedOnCurrentOutput(),
                )
            }
            // Notify the session so it re-binds to the freshly created player. Runs on the
            // controller (main) thread, the same looper the player and session use.
            onSessionPlayerChanged?.invoke(player)
        }
    }

    private fun createPlayer(bufferProfile: String): ExoPlayer {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("RadioWave/1.0")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
        val bufferDurations = resolveBufferDurations(bufferProfile)

        // REA 95fcb33: eigener RenderersFactory (p000/qq2.java Konstruktor (cp3, Context),
        // buildRenderers inlined in qq2.m8241j): die Audio-Senke bekommt den
        // VisualizerAudioProcessor in die Prozessorkette. Er reicht das Audio unverändert durch.
        // REA-UNSICHER: ob buildAudioSink überschrieben oder setAudioProcessors benutzt wurde,
        // ist im Bytecode nicht unterscheidbar (beides ergibt DefaultAudioProcessorChain(ap4)).
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf(VisualizerAudioProcessor(visualizerTap)))
                .build()
        }

        return ExoPlayer.Builder(context, renderersFactory)
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
            // CPU wake lock held by Media3 exactly while playing or buffering, released
            // on pause/stop/release. Replaces our own lock, whose 30-min timeout was only
            // renewed on player state changes — long sessions
            // without state changes outlived it.
            // WifiLock stays off on purpose (WIFI_MODE_FULL is a no-op since API 29).
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .also { activeBufferProfile = bufferProfile }
    }

    private fun getSelectedBufferProfile(): String {
        if (isLowLoadModeEnabled()) {
            return AppSettings.BUFFER_SMALL
        }
        if (isTimeshiftGuardEnabled()) {
            return AppSettings.BUFFER_LARGE
        }
        val value = settingsState.value.bufferProfile
        return when (value) {
            AppSettings.BUFFER_SMALL,
            AppSettings.BUFFER_MEDIUM,
            AppSettings.BUFFER_LARGE,
            -> value

            else -> AppSettings.BUFFER_MEDIUM
        }
    }

    private fun isThermalModeEnabled(): Boolean = settingsState.value.thermalMode

    private fun isLowLoadModeEnabled(): Boolean {
        return isThermalModeEnabled() || isAutomotivePerformanceModeEnabled
    }

    private fun isTimeshiftGuardEnabled(): Boolean = settingsState.value.timeshiftGuard

    private fun isPlaybackBlockedByMobileDataPolicy(): Boolean {
        val allowMobileData = settingsState.value.allowMobileData
        if (allowMobileData) return false

        val manager = connectivityManager ?: return false
        val activeNetwork = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(activeNetwork) ?: return false

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return true
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return false
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return false

        return true
    }

    /** Stop the data source, not just the audio: paused ExoPlayer can keep buffering. */
    private fun blockPlaybackIfMobileDataForbidden(
        station: Station? = _playerState.value.currentStation,
    ): Boolean {
        if (!isPlaybackBlockedByMobileDataPolicy()) return false
        userPausedPlayback = true // returning Wi-Fi must not unexpectedly start playback
        streamFailoverJob?.cancel()
        reconnectJob?.cancel()
        bufferingWatchdogJob?.cancel()
        playbackLostRecoveryJob?.cancel()
        restartGuardJob?.cancel()
        coverArtFetchJob?.cancel()
        // REA 95fcb33: auch der EQ-Routen-Job endet (cp3.m1897e).
        equalizerRouteJob?.cancel()
        equalizerRouteJob = null
        shouldResumeAfterAudioFocusGain = false
        exoPlayer?.playWhenReady = false
        exoPlayer?.stop()
        abandonAudioFocus()
        _playerState.update {
            it.copy(
                currentStation = station,
                error = PlayerError.MobileDataBlocked,
                isPlaying = false,
                isLoading = false,
                isBuffering = false,
            )
        }
        return true
    }

    private fun resolveBufferDurations(profile: String): BufferDurations {
        return when (profile) {
            AppSettings.BUFFER_SMALL -> BufferDurations(
                minBufferMs = 30_000,
                maxBufferMs = 120_000,
                bufferForPlaybackMs = 1_800,
                bufferForPlaybackAfterRebufferMs = 4_000,
            )

            AppSettings.BUFFER_LARGE -> BufferDurations(
                minBufferMs = 75_000,
                maxBufferMs = 300_000,
                bufferForPlaybackMs = 3_500,
                bufferForPlaybackAfterRebufferMs = 8_000,
            )

            else -> BufferDurations(
                minBufferMs = 55_000,
                maxBufferMs = 210_000,
                bufferForPlaybackMs = 2_800,
                bufferForPlaybackAfterRebufferMs = 6_000,
            )
        }
    }

    private fun recreatePlayerForBufferProfileIfNeeded() {
        val selectedProfile = getSelectedBufferProfile()
        if (activeBufferProfile == null || activeBufferProfile == selectedProfile) return

        // Detach listener BEFORE release so it doesn't get callbacks during teardown.
        exoPlayer?.let { p -> exoPlayerListener?.let { p.removeListener(it) } }
        exoPlayerListener = null
        exoPlayer?.release()
        exoPlayer = null
        // The Equalizer is attached to the OLD player's audio session, which just died
        // with it — release it now rather than waiting for the new player's
        // onAudioSessionIdChanged to implicitly clean it up via attach()'s own
        // release()-first call. Without this, a stale Equalizer stays registered
        // against a dead session in AudioFlinger for however long it takes the new
        // player to reach a playing state (longer, or indefinite, if audio focus is
        // delayed or playback stalls before that happens).
        equalizerController.release()
    }

    private fun setupPlayerListeners(player: ExoPlayer) {
        // Remove any previous listener instance so we can deterministically clean up
        // on player release. Listeners are stored as a field for symmetry.
        exoPlayerListener?.let { player.removeListener(it) }
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // MediaSession commands can reach ExoPlayer without togglePlayPause().
                if (playWhenReady && blockPlaybackIfMobileDataForbidden()) return
                // FIX 2026-11-01 (Review P1-2): Play from steering wheel, Android Auto,
                // notification or a BT button bypasses togglePlayPause(). Treat it like a
                // manual resume, otherwise isPermanentPlaybackFailure stays set and every
                // later error is silently ignored (no reconnect after the next dead spot).
                if (playWhenReady &&
                    reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST &&
                    !isInternalRestartInProgress
                ) {
                    resetFailureStateForManualResume()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // REA 95fcb33 (bp3.mo1234V): the spectrum only runs while audio actually
                // plays; the EQ follows the output route, re-checked every 3 s while playing.
                visualizerTap.setPlaybackActive(isPlaying)
                refreshEqualizerForOutput()
                if (!isPlaying) {
                    equalizerRouteJob?.cancel()
                    equalizerRouteJob = null
                } else if (equalizerRouteJob?.isActive != true) {
                    equalizerRouteJob = controllerScope.launch {
                        while (isActive) {
                            delay(EQUALIZER_ROUTE_CHECK_INTERVAL_MS)
                            refreshEqualizerForOutput()
                        }
                    }
                }
                if (isPlaying && blockPlaybackIfMobileDataForbidden()) return
                val now = SystemClock.elapsedRealtime()
                _playerState.update {
                    val accumulatedDuration = when {
                        !isPlaying && lastPlaybackResumedAtElapsedMs != null -> {
                            it.playedDurationMs + (now - (lastPlaybackResumedAtElapsedMs ?: now)).coerceAtLeast(0L)
                        }
                        else -> it.playedDurationMs
                    }
                    it.copy(
                        isPlaying = isPlaying,
                        sessionStartedAtElapsedMs = when {
                            it.sessionStartedAtElapsedMs != null -> it.sessionStartedAtElapsedMs
                            isPlaying -> now
                            else -> null
                        },
                        playedDurationMs = accumulatedDuration,
                        error = if (isPlaying) null else it.error,
                    )
                }
                if (isPlaying) {
                    lastPlaybackResumedAtElapsedMs = now
                } else {
                    lastPlaybackResumedAtElapsedMs = null
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
                }
                // Notification updates on transient state changes are no longer needed —
                // Media3's DefaultMediaNotificationProvider redraws automatically from
                // the player's state listeners.
            }

            override fun onPlaybackStateChanged(state: Int) {
                if ((state == Player.STATE_BUFFERING || state == Player.STATE_READY) &&
                    blockPlaybackIfMobileDataForbidden()
                ) return
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
                        quickRetryCount = 0
                        playbackLostRecoveryAttempts = 0
                        // FIX 2026-11-01 (Review P1-2/P3): a working stream ends the failure
                        // episode; earlier failed URLs get a new chance next time.
                        isPermanentPlaybackFailure = false
                        currentStreamReachedReady = true
                        failedStreamUrls.clear()
                        streamFailoverJob?.cancel()
                        reconnectJob?.cancel()
                        bufferingWatchdogJob?.cancel()
                        playbackLostRecoveryJob?.cancel()
                        // Report click to Radio Browser once per station session so the
                        // community "topclick" rankings reflect actual usage. Opt-out via
                        // Settings -> Storage & Data -> Privacy (on by default).
                        val stationUuid = _playerState.value.currentStation?.uuid
                        if (
                            stationUuid != null &&
                            !clickRegisteredForCurrentSession &&
                            settingsState.value.reportStationClicks
                        ) {
                            clickRegisteredForCurrentSession = true
                            controllerScope.launch {
                                runCatching { stationRepository.registerClick(stationUuid) }
                            }
                        }
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
            }

            override fun onPlayerError(error: PlaybackException) {
                // REA 95fcb33 (bp3.mo1238z): neue Fehlerbehandlung.
                //  1. bis zu [maxQuickRetries] normale Reconnects (exponentiell, kein 350-ms-
                //     Schnellversuch mehr) — außer der Fehler ist dauerhaft (4xx, Format …),
                //  2. danach wird die URL als gescheitert gemerkt und eine andere Stream-
                //     Variante desselben Senders gesucht ([launchStreamFailover]),
                //  3. nach [maxFailedStreamUrls] URLs oder erschöpften Reconnects: Fehler.
                if (isStopping || userPausedPlayback || isPermanentPlaybackFailure) return
                val station = _playerState.value.currentStation ?: return
                val generation = playbackGeneration
                logError("Player error ${error.errorCode}: ${error.message}", error)
                bufferingWatchdogJob?.cancel()
                playbackLostRecoveryJob?.cancel()
                reconnectJob?.cancel()
                streamFailoverJob?.cancel()

                val permanent = isPermanentForCurrentStream(error)
                if (!permanent &&
                    quickRetryCount < maxQuickRetries &&
                    reconnectAttempts < maxReconnectAttempts
                ) {
                    quickRetryCount++
                    scheduleReconnect(station, delayOverrideMs = null, countAttempt = true)
                    return
                }

                // Deliberately NOT reported to Radio Browser: its API has no "broken"
                // endpoint (the former reportBrokenStream() upvoted the dead station).
                failedStreamUrls.add(station.streamUrl)
                if (failedStreamUrls.size >= maxFailedStreamUrls || reconnectAttempts >= maxReconnectAttempts) {
                    failPlayback(error, permanent)
                    return
                }
                _playerState.update {
                    it.copy(
                        error = null,
                        isPlaying = false,
                        isBuffering = true,
                        isLoading = true,
                    )
                }
                launchStreamFailover(station, generation, error, permanent)
            }

            override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                if (isLowLoadModeEnabled() && !canProcessMetadataUpdateNow()) {
                    return
                }
                val station = _playerState.value.currentStation
                val stationName = station?.name?.trim()
                val title = metadata.title?.toString()?.trim()
                val artist = metadata.artist?.toString()?.trim()
                // publishNowPlayingMetadata writes `liveArt ?: favicon` back onto the
                // MediaItem, and replaceMediaItem re-fires this callback with that
                // combined metadata — so the station favicon comes back around as
                // artworkUri. Filter it out here: metadata.albumArtUrl must only ever
                // hold GENUINE song art (stream-provided or iTunes-fetched), otherwise
                // scheduleCoverArtFetch would see the favicon echo as "cover already
                // present" and never fetch the real one.
                //
                // Low-load note: artwork is deliberately NOT nulled in automotive/thermal
                // mode anymore — the car is exactly where the user wants covers, and the
                // per-song iTunes cache keeps the cost to one lookup per track.
                // Scheme guard at the SOURCE: in-stream artwork URIs are untrusted input
                // (same trust level as the stream itself). Without this, a file:// or
                // content:// URI would flow into StreamMetadata.albumArtUrl and reach
                // Coil raw in PlayerScreen — the exact sink class closed everywhere else
                // (audit 2026-07-18, H1).
                val albumArtUrl = safeHttpUriOrNull(metadata.artworkUri?.toString())?.toString()
                    ?.takeUnless { it == station?.faviconUrl }
                val hasUsefulMetadata = (!title.isNullOrBlank() && !title.equals(stationName, ignoreCase = true)) ||
                    !artist.isNullOrBlank() ||
                    !albumArtUrl.isNullOrBlank()

                if (!hasUsefulMetadata) {
                    return
                }

                // Track whether anything actually changed so we can skip the
                // foreground-service notification update for duplicate ICY frames.
                var changed = false
                _playerState.update {
                    val previous = it.metadata ?: StreamMetadata()
                    val newTitle = title ?: previous.title
                    val newArtist = artist ?: previous.artist
                    // A different song must not keep the previous song's cover — drop
                    // the inherited art on track change so the station logo shows as
                    // fallback until (if) the iTunes lookup finds the new cover.
                    val songChanged = newTitle != previous.title || newArtist != previous.artist
                    val inheritedArt = previous.albumArtUrl.takeUnless { songChanged }
                    val noChange =
                        !songChanged &&
                            previous.albumArtUrl == (albumArtUrl ?: previous.albumArtUrl)
                    if (noChange) return@update it
                    changed = true
                    it.copy(
                        metadata = previous.copy(
                            title = newTitle,
                            artist = newArtist,
                            albumArtUrl = albumArtUrl ?: inheritedArt,
                        ),
                    )
                }
                if (!changed) return
                publishNowPlayingMetadata()
                scheduleCoverArtFetch()
            }

            override fun onMetadata(metadata: Metadata) {
                val streamTitle = extractStreamTitle(metadata) ?: return
                applyStreamTitle(streamTitle)
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                equalizerController.attach(
                    audioSessionId,
                    outputAllowsEffect = isEqualizerAllowedOnCurrentOutput(),
                )
            }

            // REA 95fcb33 (bp3.mo1231C): stream volume changes accompany most route
            // switches (BT connect/disconnect) — a cheap extra trigger for the EQ check.
            override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
                refreshEqualizerForOutput()
            }
        }
        exoPlayerListener = listener
        player.addListener(listener)
    }

    private fun maybeRecoverFromLostState(player: ExoPlayer, reason: String) {
        if (isStopping || userPausedPlayback || isPermanentPlaybackFailure) return
        if (isInternalRestartInProgress) return
        if (!player.playWhenReady) return
        val station = _playerState.value.currentStation ?: return
        triggerPlaybackLostRecovery(station, reason)
    }

    private fun startBufferingWatchdog(player: ExoPlayer) {
        bufferingWatchdogJob?.cancel()
        val stationUuid = _playerState.value.currentStation?.uuid ?: return
        // The 60s timeshift-guard threshold only makes sense when the LARGE buffer
        // is actually in use. In low-load mode (automotive / thermal) the buffer is
        // forced to SMALL, so a 60s stall window would leave the car silent far too
        // long before recovery kicks in — use the responsive default there.
        val stallThresholdMs = if (isTimeshiftGuardEnabled() && !isLowLoadModeEnabled()) {
            timeshiftGuardBufferingStallThresholdMs
        } else {
            defaultBufferingStallThresholdMs
        }

        bufferingWatchdogJob = controllerScope.launch {
            delay(stallThresholdMs)

            val currentStation = _playerState.value.currentStation ?: return@launch
            if (currentStation.uuid != stationUuid) return@launch
            if (isStopping || userPausedPlayback || isPermanentPlaybackFailure) return@launch
            if (!player.playWhenReady) return@launch
            if (player.playbackState != Player.STATE_BUFFERING) return@launch

            logWarning("Buffering stall detected after ${stallThresholdMs}ms")
            triggerPlaybackLostRecovery(currentStation, reason = "buffer-stall")
        }
    }

    private fun triggerPlaybackLostRecovery(station: Station, reason: String) {
        if (playbackLostRecoveryJob?.isActive == true) return
        if (reconnectJob?.isActive == true) return
        // FIX 2026-11-01 (Review P1-1): the IDLE that follows every player error must not
        // start a second recovery next to the stream failover — it used to restart the old,
        // broken URL 700 ms later and kill the freshly selected alternative.
        if (streamFailoverJob?.isActive == true) return
        if (isStopping || userPausedPlayback || isPermanentPlaybackFailure) return
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
            if (isStopping || userPausedPlayback || isPermanentPlaybackFailure) return@launch
            if (streamFailoverJob?.isActive == true || reconnectJob?.isActive == true) return@launch

            // FIX 2026-11-01: current state, not the captured object — after a failover the
            // station carries a different streamUrl.
            restartStream(player, currentStation)
        }
    }

    private fun extractStreamTitle(metadata: Metadata): String? {
        for (index in 0 until metadata.length()) {
            val entry = metadata[index]

            if (entry is IcyInfo) {
                // Prefer the typed IcyInfo.title field — stable across Media3 versions.
                val title = entry.title?.trim()
                if (!title.isNullOrBlank()) {
                    return title
                }
                // Fallback: some stations send raw ICY data not parsed into .title.
                // Use toString() only as a last resort and only for IcyInfo entries.
                val fromRaw = extractStreamTitleFromIcyText(entry.toString())
                if (!fromRaw.isNullOrBlank()) {
                    return fromRaw
                }
                // Don't fall through to the generic regex path for IcyInfo entries;
                // we've already exhausted its typed API.
                continue
            }
        }
        return null
    }

    /**
     * Parses `StreamTitle='...'` from raw ICY metadata text.
     * Only called as a last resort when [IcyInfo.title] returns null/blank.
     * The toString() format of [IcyInfo] is not a guaranteed API surface — if a
     * future Media3 version changes it this falls back to null gracefully.
     */
    private fun extractStreamTitleFromIcyText(text: String): String? {
        return STREAM_TITLE_REGEX.find(text)?.groupValues?.getOrNull(1)?.trim()
            ?.takeUnless { it.isBlank() }
    }

    private fun applyStreamTitle(rawTitle: String) {
        if (isLowLoadModeEnabled() && !canProcessMetadataUpdateNow()) return
        val titleText = rawTitle.trim().takeUnless { it.isBlank() } ?: return
        val stationName = _playerState.value.currentStation?.name?.trim()
        if (stationName != null && titleText.equals(stationName, ignoreCase = true)) {
            return
        }

        val parts = TITLE_SPLIT_REGEX.split(titleText, limit = 2)
        val parsedArtist = parts.getOrNull(0)?.trim().takeUnless { it.isNullOrBlank() }
        val parsedTitle = when {
            parts.size >= 2 -> parts[1].trim()
            else -> titleText
        }.takeUnless { it.isBlank() }

        var changed = false
        _playerState.update {
            val previous = it.metadata ?: StreamMetadata()
            val noChange =
                previous.title == (parsedTitle ?: previous.title) &&
                    previous.artist == (parsedArtist ?: previous.artist)
            if (noChange) return@update it
            changed = true
            it.copy(
                metadata = previous.copy(
                    title = parsedTitle ?: previous.title,
                    artist = parsedArtist ?: previous.artist,
                    // New song → the previous song's cover is stale. Clear it so the
                    // station logo shows as fallback until the iTunes lookup resolves.
                    albumArtUrl = null,
                ),
            )
        }
        if (changed) {
            publishNowPlayingMetadata()
            scheduleCoverArtFetch()
        }
    }

    /**
     * Mirrors the live now-playing info (parsed ICY title/artist, plus station artwork)
     * onto the *current session MediaItem* so every MediaController — the Android Auto
     * now-playing screen and the Media3 phone notification — reflects the running song,
     * not just the static station name + logo.
     *
     * Why this is necessary: ExoPlayer builds the [MediaMetadata] a controller sees by taking
     * the dynamic in-stream (ICY) metadata and then overlaying the current MediaItem's explicit
     * mediaMetadata on top — MediaItem fields win. Because [createMediaItem] sets the MediaItem
     * title to the station name, the raw ICY title is masked out of that combined metadata. The
     * phone in-app UI shows the song only because it reads [PlayerState.metadata] directly, a
     * path Android Auto has no access to. Writing the parsed values back onto the MediaItem
     * surfaces them to the session.
     *
     * [Player.replaceMediaItem] only swaps the item's metadata; because the stream URI is
     * unchanged it does NOT re-prepare or interrupt playback. The equality guard makes this
     * idempotent and also breaks the re-entrant onMediaMetadataChanged → publish → replace loop
     * (replaceMediaItem itself fires onMediaMetadataChanged again).
     */
    private fun publishNowPlayingMetadata() {
        val player = exoPlayer ?: return
        val index = player.currentMediaItemIndex
        if (index < 0 || index >= player.mediaItemCount) return
        val currentItem = player.currentMediaItem ?: return

        val updatedMetadata = buildNowPlayingMetadata(
            base = currentItem.mediaMetadata,
            station = _playerState.value.currentStation,
            meta = _playerState.value.metadata,
        )

        if (updatedMetadata == currentItem.mediaMetadata) return

        player.replaceMediaItem(
            index,
            currentItem.buildUpon().setMediaMetadata(updatedMetadata).build(),
        )
    }

    /**
     * Debounced lookup of the current song's cover via [coverArtRepository] (iTunes Search,
     * memory-cached per artist|title — genuine "no match" results for 30 min, so a no-cover
     * song doesn't re-hit the network on every ICY repeat). The repository itself returns
     * null without any request when the user disabled online cover lookup.
     *
     * Runs in ALL modes including automotive/thermal low-load — the car is precisely where
     * the user wants covers in the now-playing screen. The debounce collapses the ICY
     * title/artist double-fire on song change into a single lookup.
     *
     * Fallback contract (user requirement): if no cover is found, the lookup fails, or the
     * station sends no usable artist/title, [StreamMetadata.albumArtUrl] simply stays null
     * and [buildNowPlayingMetadata] falls back to the station favicon — logo, never blank.
     */
    private fun scheduleCoverArtFetch() {
        coverArtFetchJob?.cancel()
        val state = _playerState.value
        val meta = state.metadata ?: return
        // Genuine stream-provided art already present — nothing to look up.
        if (!meta.albumArtUrl.isNullOrBlank()) return
        val artist = meta.artist?.trim().takeUnless { it.isNullOrBlank() } ?: return
        val title = meta.title?.trim().takeUnless { it.isNullOrBlank() } ?: return
        val stationUuid = state.currentStation?.uuid ?: return

        coverArtFetchJob = controllerScope.launch {
            delay(coverArtFetchDebounceMs)
            // Scheme guard also on the iTunes result — API responses are parsed JSON
            // and should always be https, but albumArtUrl is a load-bearing sink.
            val coverUrl = runCatching { coverArtRepository.fetchCoverArt(artist, title) }
                .getOrNull()?.let { safeHttpUriOrNull(it)?.toString() } ?: return@launch
            var applied = false
            _playerState.update {
                // Station or song may have moved on while the lookup ran — never
                // attach a cover to a different track than it was fetched for.
                if (it.currentStation?.uuid != stationUuid) return@update it
                val current = it.metadata ?: return@update it
                if (current.title?.trim() != title || current.artist?.trim() != artist) {
                    return@update it
                }
                if (!current.albumArtUrl.isNullOrBlank()) return@update it
                applied = true
                it.copy(metadata = current.copy(albumArtUrl = coverUrl))
            }
            if (applied) publishNowPlayingMetadata()
        }
    }

    /**
     * Pure metadata combination used by [publishNowPlayingMetadata], extracted so the
     * masking-fix behaviour (live song title overriding the station name, artwork fall-back
     * to the station favicon, and the http/https-only scheme guard on both artwork sources)
     * can be unit-tested without a live player.
     *
     * Live song title/artist win when present; otherwise the title falls back to the station
     * name and the artist is cleared. Artwork prefers stream-provided album art, then the
     * station favicon, both filtered through [safeHttpUriOrNull].
     */
    @VisibleForTesting
    internal fun buildNowPlayingMetadata(
        base: MediaMetadata,
        station: Station?,
        meta: StreamMetadata?,
    ): MediaMetadata {
        val liveTitle = meta?.title?.trim().takeUnless { it.isNullOrBlank() }
        val liveArtist = meta?.artist?.trim().takeUnless { it.isNullOrBlank() }
        val liveArt = meta?.albumArtUrl?.let(::safeHttpUriOrNull)

        return base.buildUpon()
            .setTitle(liveTitle ?: station?.name)
            .setArtist(liveArtist)
            // Station name as third text line (Android Auto full-screen player and the
            // phone notification render albumTitle under title/artist). Only while a
            // live song title is showing — otherwise the title IS the station name and
            // it would appear twice.
            .setAlbumTitle(liveTitle?.let { station?.name })
            .setStation(station?.name)
            .setArtworkUri(liveArt ?: safeHttpUriOrNull(station?.faviconUrl))
            .build()
    }

    private fun registerNetworkCallbackIfNeeded() {
        if (isReleased) return
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
        if (isStopping || userPausedPlayback || isPermanentPlaybackFailure) return
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
        // REA 95fcb33 (vo3 Fall 2): a returning network gives every stream a new chance.
        failedStreamUrls.clear()
        quickRetryCount = 0
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
            // REA 95fcb33 (ap3 Fall 0): neue Abbruchbedingungen; neu gestartet wird der
            // AKTUELLE Sender aus dem State — nach einem Failover hat er eine andere streamUrl.
            if (isStopping || userPausedPlayback || isPermanentPlaybackFailure) return@launch
            val current = _playerState.value.currentStation ?: return@launch
            restartStream(player, current)
        }
    }

    /**
     * REA 95fcb33: neu (cp3.m1899h). Ends the automatic recovery for this session and shows
     * the mapped error. A permanent error additionally blocks all recovery paths until the
     * user starts or resumes playback.
     */
    private fun failPlayback(
        error: PlaybackException,
        permanent: Boolean = isPermanentForCurrentStream(error),
    ) {
        isPermanentPlaybackFailure = permanent
        reconnectJob?.cancel()
        playbackLostRecoveryJob?.cancel()
        bufferingWatchdogJob?.cancel()
        exoPlayer?.playWhenReady = false
        val mapped = mapPlayerError(error)
        _playerState.update {
            it.copy(
                error = mapped,
                isPlaying = false,
                isBuffering = false,
                isLoading = false,
            )
        }
    }

    /**
     * REA 95fcb33: neu (p000/wi0.java, letzter Fall). Looks for another stream of the same
     * station among the locally known variants and continues with it; gives up with
     * [failPlayback] when none is left.
     */
    private fun launchStreamFailover(
        station: Station,
        generation: Long,
        error: PlaybackException,
        permanent: Boolean,
    ) {
        playbackLostRecoveryJob?.cancel()
        streamFailoverJob = controllerScope.launch {
            val variants = try {
                stationRepository.getCachedStationVariants(station)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
            if (!isActive) return@launch
            if (generation != playbackGeneration) return@launch
            if (isStopping || userPausedPlayback || isPermanentPlaybackFailure) return@launch
            if (_playerState.value.currentStation?.streamUrl != station.streamUrl) return@launch

            val alternative = selectFailoverStream(station, variants, failedStreamUrls)
            if (alternative == null) {
                // FIX 2026-11-01 (Review P2-1): most stations have no known alternative URL.
                // For a transient error (dead spot, tunnel) keep the v1.0.19 behaviour:
                // exponential backoff up to maxReconnectAttempts on the same URL.
                if (!permanent && reconnectAttempts < maxReconnectAttempts) {
                    scheduleReconnect(station, delayOverrideMs = null, countAttempt = true)
                } else {
                    failPlayback(error, permanent)
                }
                return@launch
            }
            val next = station.copy(
                streamUrl = alternative.streamUrl,
                codec = alternative.codec,
                bitrate = alternative.bitrate,
            )
            quickRetryCount = 0
            currentStreamReachedReady = false
            coverArtFetchJob?.cancel()
            _playerState.update { it.copy(currentStation = next) }
            scheduleReconnect(next, delayOverrideMs = failoverRestartDelayMs, countAttempt = true)
        }
    }

    /** FIX 2026-11-01 (Review P2-2): see [isPermanentStreamError]. */
    private fun isPermanentForCurrentStream(error: PlaybackException): Boolean =
        isPermanentStreamError(error, streamWasReady = currentStreamReachedReady)

    /** A user-initiated resume gives every stream a new chance. */
    private fun resetFailureStateForManualResume() {
        isPermanentPlaybackFailure = false
        failedStreamUrls.clear()
        quickRetryCount = 0
        reconnectAttempts = 0
        playbackLostRecoveryAttempts = 0
    }

    private fun computeReconnectDelayMs(attempt: Int): Long {
        val exponential = initialReconnectDelayMs * (1 shl (attempt - 1))
        return min(exponential, maxReconnectDelayMs)
    }

    /** Returns null if the URI scheme is not allowed — callers handle by setting StreamBroken error. */
    private fun createMediaItemOrNull(station: Station): MediaItem? {
        val scheme = Uri.parse(station.streamUrl).scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            logWarning("Rejected stream URI with scheme '$scheme' for '${station.name}'")
            return null
        }
        return createMediaItem(station)
    }

    private fun createMediaItem(station: Station): MediaItem {
        return MediaItem.Builder()
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtworkUri(safeHttpUriOrNull(station.faviconUrl))
                    .build(),
            )
            .build()
    }

    /**
     * `faviconUrl` comes from the same community-editable Radio Browser API field as
     * `streamUrl` (see [createMediaItemOrNull]'s scheme check), so it needs the same
     * http/https-only guard — otherwise a station with `favicon = "file:///..."` or
     * `"content://..."` would have that URI handed straight to the media session /
     * notification / Coil image loader, which can resolve local files and other apps'
     * exported content providers.
     */
    private fun safeHttpUriOrNull(url: String?): Uri? {
        if (url.isNullOrBlank()) return null
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase()
        return if (scheme == "http" || scheme == "https") uri else null
    }

    private fun restartStream(player: ExoPlayer, station: Station) {
        if (blockPlaybackIfMobileDataForbidden(station)) return
        // Validate URI first — if invalid, surface a player error instead of crashing.
        val mediaItem = createMediaItemOrNull(station)
        if (mediaItem == null) {
            _playerState.update {
                it.copy(
                    error = PlayerError.StreamBroken,
                    isPlaying = false,
                    isBuffering = false,
                    isLoading = false,
                )
            }
            return
        }
        isInternalRestartInProgress = true
        restartGuardJob?.cancel()
        restartGuardJob = controllerScope.launch {
            delay(2_000L)
            isInternalRestartInProgress = false
        }
        player.stop()
        player.setMediaItem(mediaItem)
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

            else -> PlayerError.Unknown(error.message ?: context.getString(R.string.player_error_unknown))
        }
    }

    private suspend fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pausePlaybackForAudioFocus(resumeWhenFocusReturns = false)
                abandonAudioFocus()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pausePlaybackForAudioFocus(resumeWhenFocusReturns = true)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // REA 95fcb33 (os1 Fall 2) paused here when the setting was on.
                // FIX 2026-11-01: never pause for a duck request. Pausing a live stream
                // left it a few seconds behind live after every navigation prompt (the
                // buffer kept growing until the connection broke). With the setting on
                // the stream now keeps running silently and is audible again, live, as
                // soon as focus returns; otherwise it plays on at 25 %.
                duckPlaybackForAudioFocus(silent = settingsState.value.pauseForNavigation)
            }

            AudioManager.AUDIOFOCUS_GAIN -> resumePlaybackAfterAudioFocusGainIfNeeded()
        }
    }

    private fun pausePlaybackForAudioFocus(resumeWhenFocusReturns: Boolean) {
        // REA 95fcb33 (cp3.m1907p): ein laufender Failover wird verworfen.
        playbackGeneration++
        streamFailoverJob?.cancel()
        val player = exoPlayer ?: return
        val shouldResume = resumeWhenFocusReturns &&
            !userPausedPlayback &&
            _playerState.value.currentStation != null &&
            (player.isPlaying || player.playWhenReady)
        reconnectJob?.cancel()
        bufferingWatchdogJob?.cancel()
        playbackLostRecoveryJob?.cancel()
        player.volume = if (_playerState.value.isMuted) 0f else 1f
        player.pause()
        unregisterNetworkCallbackIfNeeded()
        shouldResumeAfterAudioFocusGain = shouldResume
        wasDuckedForAudioFocus = false
    }

    private fun duckPlaybackForAudioFocus(silent: Boolean = false) {
        val player = exoPlayer ?: return
        if (_playerState.value.isMuted) return
        player.volume = if (silent) 0f else NAVIGATION_DUCK_VOLUME
        wasDuckedForAudioFocus = true
    }

    private fun restoreVolumeAfterDuck() {
        if (!wasDuckedForAudioFocus) return
        applyAudibleVolume()
    }

    /**
     * Volume as the user set it: 0 when muted, full otherwise. Used on every focus
     * grant without looking at [wasDuckedForAudioFocus] — pause and abandonAudioFocus()
     * clear that flag but leave the player at the ducked 25 %, so the flag alone can't
     * tell whether a duck is still in effect.
     */
    private fun applyAudibleVolume() {
        exoPlayer?.volume = if (_playerState.value.isMuted) 0f else 1f
        wasDuckedForAudioFocus = false
    }

    private fun resumePlaybackAfterAudioFocusGainIfNeeded() {
        val player = exoPlayer ?: return
        restoreVolumeAfterDuck()
        if (!shouldResumeAfterAudioFocusGain) return
        shouldResumeAfterAudioFocusGain = false

        if (_playerState.value.currentStation == null) return
        if (userPausedPlayback) return
        if (blockPlaybackIfMobileDataForbidden()) return

        registerNetworkCallbackIfNeeded()
        val currentStation = _playerState.value.currentStation
        if (currentStation != null && (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED)) {
            restartStream(player, currentStation)
        } else {
            player.playWhenReady = true
            player.play()
        }
    }

    private enum class AudioFocusResult { GRANTED, DELAYED, FAILED }

    private fun requestAudioFocus(): AudioFocusResult {
        val manager = audioManager ?: return AudioFocusResult.GRANTED.also { applyAudibleVolume() }
        return when (manager.requestAudioFocus(audioFocusRequest)) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                // An immediate grant arrives as a return value, not as GAIN — so the
                // duck from an earlier CAN_DUCK would otherwise stick at 25 %.
                applyAudibleVolume()
                AudioFocusResult.GRANTED
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> AudioFocusResult.DELAYED
            else -> AudioFocusResult.FAILED
        }
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        shouldResumeAfterAudioFocusGain = false
        wasDuckedForAudioFocus = false
        manager.abandonAudioFocusRequest(audioFocusRequest)
    }

    private suspend fun ensureStationPoolLoaded(): List<Station> {
        if (stationPool.isNotEmpty()) return stationPool

        // Deduplicate concurrent loads: the first caller launches the fetch, later
        // callers join the same job. (The job was historically never assigned, making
        // this guard a silent no-op — audit 2026-07-18, L1.)
        stationPoolJob?.join()
        if (stationPool.isNotEmpty()) return stationPool

        val job = controllerScope.launch {
            val loaded = stationRepository.getTopStations().firstOrNull().orEmpty()
                .filter { it.streamUrl.isNotBlank() }
                .distinctBy { it.uuid }
            if (loaded.isNotEmpty()) {
                stationPool = loaded
            }
        }
        stationPoolJob = job
        job.join()
        return stationPool
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
        val currentPool = ensureStationPoolLoaded()

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
        // REA 95fcb33 (cp3.m1916z): Spektrum zurücksetzen, Failover-Zustand neu beginnen.
        visualizerTap.reset()
        playbackGeneration++
        streamFailoverJob?.cancel()
        isPermanentPlaybackFailure = false
        currentStreamReachedReady = false
        failedStreamUrls.clear()
        quickRetryCount = 0
        reconnectJob?.cancel()
        bufferingWatchdogJob?.cancel()
        playbackLostRecoveryJob?.cancel()
        restartGuardJob?.cancel()
        reconnectAttempts = 0
        playbackLostRecoveryAttempts = 0
        networkLossObserved = false
        userPausedPlayback = false
        isStopping = false
        clickRegisteredForCurrentSession = false

        if (blockPlaybackIfMobileDataForbidden(station)) return

        val focus = requestAudioFocus()
        if (focus == AudioFocusResult.FAILED) {
            _playerState.update {
                it.copy(
                    currentStation = station,
                    error = PlayerError.Unknown(context.getString(R.string.player_error_audio_focus_unavailable)),
                    isPlaying = false,
                    isLoading = false,
                    isBuffering = false,
                )
            }
            return
        }
        recreatePlayerForBufferProfileIfNeeded()

        lastPlaybackResumedAtElapsedMs = null
        // A cover lookup for the previous station's song must not land on this one.
        coverArtFetchJob?.cancel()
        _playerState.update {
            it.copy(
                currentStation = station,
                error = null,
                isLoading = true,
                isBuffering = true,
                sessionStartedAtElapsedMs = null,
                playedDurationMs = 0L,
                metadata = null,
            )
        }
        persistLastStation(station)

        val player = getOrCreatePlayer()
        ensureForegroundPlaybackServiceRunning()

        if (focus == AudioFocusResult.DELAYED) {
            // Focus will be granted shortly via onAudioFocusChange(GAIN). The player
            // is created (STATE_IDLE) and showing the loading state; the resume path
            // starts the stream once the grant arrives. Keeps the UI in "loading"
            // rather than flipping to an error the user has to recover from manually.
            shouldResumeAfterAudioFocusGain = true
            return
        }

        restartStream(player, station)
    }

    private fun persistLastStation(station: Station) {
        controllerScope.launch {
            settingsRepository.setLastStation(
                uuid = station.uuid,
                name = station.name,
                streamUrl = station.streamUrl,
                faviconUrl = station.faviconUrl,
                country = station.country,
            )
        }
    }

    override fun togglePlayPause() {
        exoPlayer?.let { player ->
            // REA 95fcb33 (lx3.m6552d): also pauses while connecting/buffering — the UI now
            // shows "pause" in that state too (MainPlaybackButton, mini player).
            val state = _playerState.value
            // FIX 2026-11-01 (Review P2-3): in an error state (recovery exhausted) playWhenReady
            // may still be true — the first tap must restart, not "pause" a silent player.
            val isActive = player.isPlaying ||
                (state.error == null && !userPausedPlayback &&
                    (state.isLoading || state.isBuffering || player.playWhenReady))
            if (isActive) {
                pauseForExternalPlayback(markAsUserPaused = true)
            } else {
                if (blockPlaybackIfMobileDataForbidden()) return
                val focus = requestAudioFocus()
                if (focus == AudioFocusResult.FAILED) {
                    _playerState.update {
                        it.copy(
                            error = PlayerError.Unknown(context.getString(R.string.player_error_audio_focus_unavailable)),
                            isPlaying = false,
                            isLoading = false,
                            isBuffering = false,
                        )
                    }
                    return
                }
                userPausedPlayback = false
                // REA 95fcb33: a manual resume gives every stream a new chance.
                resetFailureStateForManualResume()
                registerNetworkCallbackIfNeeded()
                if (focus == AudioFocusResult.DELAYED) {
                    // Wait for the system to grant focus, then resume — see startPlayback.
                    shouldResumeAfterAudioFocusGain = true
                    _playerState.update { it.copy(error = null, isLoading = true, isBuffering = true) }
                    return
                }
                val currentStation = _playerState.value.currentStation
                if (currentStation != null && (
                        state.error != null ||
                            player.playbackState == Player.STATE_IDLE ||
                            player.playbackState == Player.STATE_ENDED
                        )
                ) {
                    restartStream(player, currentStation)
                } else {
                    player.playWhenReady = true
                    player.play()
                }
            }
        }
    }

    override fun pauseForExternalPlayback() {
        pauseForExternalPlayback(markAsUserPaused = false)
    }

    private fun pauseForExternalPlayback(markAsUserPaused: Boolean) {
        // REA 95fcb33 (cp3.m1906o)
        playbackGeneration++
        streamFailoverJob?.cancel()
        val player = exoPlayer ?: return
        userPausedPlayback = markAsUserPaused
        shouldResumeAfterAudioFocusGain = false
        wasDuckedForAudioFocus = false
        reconnectJob?.cancel()
        bufferingWatchdogJob?.cancel()
        playbackLostRecoveryJob?.cancel()
        restartGuardJob?.cancel()
        player.playWhenReady = false
        player.pause()
        unregisterNetworkCallbackIfNeeded()
        abandonAudioFocus()
        _playerState.update {
            it.copy(
                isPlaying = false,
                isLoading = false,
                isBuffering = false,
                error = null,
            )
        }
    }

    private fun registerAudioDeviceCallbackIfNeeded() {
        if (audioDeviceCallback != null) return
        val manager = audioManager ?: return
        val callback = object : AudioDeviceCallback() {
            // REA 95fcb33 (C0760to Fall 1): neue Ausgabe → EQ-Freigabe neu prüfen.
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                refreshEqualizerForOutput()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                handleAudioOutputDevicesRemoved(removedDevices)
                refreshEqualizerForOutput()
            }
        }
        // null handler => callbacks are delivered on the main thread, matching the
        // thread the player + controllerScope live on.
        manager.registerAudioDeviceCallback(callback, null)
        audioDeviceCallback = callback
    }

    private fun unregisterAudioDeviceCallbackIfNeeded() {
        val manager = audioManager ?: return
        audioDeviceCallback?.let(manager::unregisterAudioDeviceCallback)
        audioDeviceCallback = null
    }

    /**
     * Pause when the audio route we were playing through physically disappears — the
     * user left the car (Android Auto over USB/wireless, or plain Bluetooth A2DP),
     * unplugged a dock, or pulled a USB/AUX cable. Without this, Android silently
     * re-routes the live stream to the phone's built-in speaker, so the radio keeps
     * blaring a few seconds after you've already walked away.
     *
     * This is the connection-agnostic backstop for the Android-Auto-controller pause in
     * [de.darksoon.radiowave.auto] onDisconnected: that path only fires for Auto
     * *projection* sessions, whereas most "radio in the car" listening is plain
     * Bluetooth where no MediaSession controller is ever involved. ExoPlayer's
     * becoming-noisy handling covers wired/BT in theory but is unreliable for some
     * car/USB routes — so we watch the hardware directly.
     *
     * We only pause once the LAST external output route is gone (no other headset/BT/
     * car device remains) so hopping between two external outputs keeps playing.
     */
    private fun handleAudioOutputDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
        val player = exoPlayer ?: return
        val wasPlaying = player.playWhenReady || player.isPlaying
        val externalOutputRemoved = removedDevices.any { it.isSink && isExternalOutputType(it.type) }
        if (!shouldPauseOnAudioOutputRemoval(
                wasPlaying = wasPlaying,
                externalOutputRemoved = externalOutputRemoved,
                externalOutputStillConnected = hasConnectedExternalOutput(),
            )
        ) return
        logDebug("External audio output removed with no external route left — pausing")
        pauseForExternalPlayback()
    }

    private fun hasConnectedExternalOutput(): Boolean {
        val manager = audioManager ?: return false
        return manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .any { isExternalOutputType(it.type) }
    }

    private fun isExternalOutputType(type: Int): Boolean = type in EXTERNAL_OUTPUT_DEVICE_TYPES

    /**
     * REA 95fcb33: neu (cp3.m1902k, Smali-geprüft). The EQ stays off while media is routed to
     * a Bluetooth sink: some BT stacks go silent with an effect in the chain
     * (settings_eq_bluetooth_note). API 33+ asks for the actual media route; older devices
     * (or an empty answer) fall back to "any connected output", which is stricter.
     */
    private fun isEqualizerAllowedOnCurrentOutput(): Boolean {
        val manager = audioManager ?: return false
        val routed: List<AudioDeviceInfo>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { manager.getAudioDevicesForAttributes(mediaPlaybackAttributes) }.getOrNull()
        } else {
            null
        }
        val devices = routed?.takeIf { it.isNotEmpty() }
            ?: manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        return devices.none { it.type in BLUETOOTH_OUTPUT_DEVICE_TYPES }
    }

    /** REA 95fcb33: neu (cp3.m1888c). */
    private fun refreshEqualizerForOutput() {
        equalizerController.setOutputAllowsEffect(isEqualizerAllowedOnCurrentOutput())
    }

    override fun toggleMute() {
        val player = exoPlayer ?: return
        val shouldMute = !_playerState.value.isMuted
        player.volume = if (shouldMute) 0f else 1f
        _playerState.update { it.copy(isMuted = shouldMute) }
    }

    override fun ensureSessionPlayer(): Player = getOrCreatePlayer()

    override fun setOnSessionPlayerChangedListener(listener: ((Player) -> Unit)?) {
        onSessionPlayerChanged = listener
    }

    override fun setAutomotivePerformanceModeEnabled(enabled: Boolean) {
        if (isAutomotivePerformanceModeEnabled == enabled) return
        isAutomotivePerformanceModeEnabled = enabled
        // Apply the new buffer profile in BOTH directions:
        //   - enabling AA: switch from MEDIUM/LARGE to SMALL (low load)
        //   - disabling AA (phone re-connects after AA disconnect): switch back
        // Without this branch the LARGE/MEDIUM-profile player would keep running
        // with the small buffer until the next manual play.
        val currentStation = _playerState.value.currentStation ?: return
        if (!_playerState.value.isPlaying) return
        controllerScope.launch {
            startPlayback(currentStation)
        }
    }

    override fun sessionPlayer(): Player? = exoPlayer

    override fun stop() {
        // REA 95fcb33 (cp3.m1891A)
        playbackGeneration++
        isStopping = true
        streamFailoverJob?.cancel()
        reconnectJob?.cancel()
        bufferingWatchdogJob?.cancel()
        playbackLostRecoveryJob?.cancel()
        restartGuardJob?.cancel()
        coverArtFetchJob?.cancel()
        reconnectAttempts = 0
        playbackLostRecoveryAttempts = 0
        networkLossObserved = false
        userPausedPlayback = false
        shouldResumeAfterAudioFocusGain = false
        wasDuckedForAudioFocus = false
        lastPlaybackResumedAtElapsedMs = null
        exoPlayer?.stop()
        playbackBackStack.clear()
        playbackForwardStack.clear()
        unregisterNetworkCallbackIfNeeded()
        stopForegroundPlaybackServiceIfRunning()
        abandonAudioFocus()
        _playerState.update { PlayerState() }
        // The player outlives stop() (it is a singleton), but PlayerState() just reset
        // isMuted to false. Without this the next play() ran silently at volume 0 while
        // the UI showed the speaker as on — and tapping it then muted for real.
        exoPlayer?.volume = 1f
        isStopping = false
    }

    override fun release() {
        isReleased = true
        isStopping = true
        controllerScope.cancel()
        // REA-UNSICHER: release() ist im Handy-Build nicht mehr vorhanden (von R8 entfernt,
        // weil ungenutzt); die neuen Jobs werden hier der Vollständigkeit halber beendet.
        streamFailoverJob?.cancel()
        equalizerRouteJob?.cancel()
        visualizerTap.reset()
        reconnectJob?.cancel()
        bufferingWatchdogJob?.cancel()
        playbackLostRecoveryJob?.cancel()
        restartGuardJob?.cancel()
        coverArtFetchJob?.cancel()
        reconnectAttempts = 0
        playbackLostRecoveryAttempts = 0
        networkLossObserved = false
        userPausedPlayback = false
        shouldResumeAfterAudioFocusGain = false
        wasDuckedForAudioFocus = false
        lastPlaybackResumedAtElapsedMs = null
        stationPoolJob?.cancel()
        playbackBackStack.clear()
        playbackForwardStack.clear()
        stationPool = emptyList()
        unregisterNetworkCallbackIfNeeded()
        unregisterAudioDeviceCallbackIfNeeded()
        stopForegroundPlaybackServiceIfRunning()
        abandonAudioFocus()
        // Detach listener before release to avoid callbacks during/after teardown.
        exoPlayer?.let { p -> exoPlayerListener?.let { p.removeListener(it) } }
        exoPlayerListener = null
        equalizerController.release()
        exoPlayer?.release()
        exoPlayer = null
        activeBufferProfile = null
        _playerState.update { PlayerState() }
        isStopping = false
    }

    /**
     * Boot the unified Media3 playback service (the same service that serves Android Auto).
     *
     * Just calling [ContextCompat.startForegroundService] is NOT enough for Media3:
     * `MediaSessionService` only promotes itself to foreground (and posts the
     * [DefaultMediaNotificationProvider] notification) once a [MediaController]
     * actually binds to the session. Without a bound controller the service is
     * started but stays in the background — no notification — and on Android 12+
     * we'd hit `ForegroundServiceDidNotStartInTimeException` after ~5s.
     *
     * Solution: bind an in-process [MediaController] to the service's session as
     * soon as phone playback starts. The bind triggers Media3's full foreground
     * lifecycle; the controller itself is otherwise unused.
     */
    private var phoneNotificationController: androidx.media3.session.MediaController? = null
    private var phoneNotificationControllerFuture:
        com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController>? = null

    private fun ensureForegroundPlaybackServiceRunning() {
        if (isReleased) return
        if (isForegroundServiceRunning) return
        try {
            val componentName = android.content.ComponentName(
                context.packageName,
                UNIFIED_PLAYBACK_SERVICE_CLASS,
            )
            // 1) Promote the service to "started" so it survives the app being
            //    backgrounded (display off). A bind-only controller would let
            //    the system reclaim the service after a few seconds idle.
            //    Media3 has 5s after startForegroundService to call startForeground;
            //    the controller bind below triggers that flow.
            val intent = android.content.Intent().setComponent(componentName)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)

            // 2) Bind a MediaController so Media3 sees a live controller and
            //    publishes the foreground notification via the
            //    DefaultMediaNotificationProvider installed in the service.
            val sessionToken = androidx.media3.session.SessionToken(context, componentName)
            val future = androidx.media3.session.MediaController.Builder(context, sessionToken)
                .buildAsync()
            phoneNotificationControllerFuture = future
            isForegroundServiceRunning = true
            future.addListener({
                try {
                    val controller = future.get()
                    // Guard: if the user stopped or released playback while the
                    // bind was in flight, stopForegroundPlaybackServiceIfRunning
                    // already set isForegroundServiceRunning=false, nulled the
                    // future field and released the future. Also catch rapid
                    // stop→play sequences where a NEW future has replaced this
                    // one — we'd otherwise overwrite the live controller with
                    // this stale one.
                    val isStaleBind = phoneNotificationControllerFuture !== future ||
                        !isForegroundServiceRunning ||
                        isReleased
                    if (isStaleBind) {
                        controller.release()
                    } else {
                        phoneNotificationController = controller
                    }
                } catch (error: Exception) {
                    logWarning("Unable to bind phone media controller: ${error.message}")
                    isForegroundServiceRunning = false
                }
            }, androidx.core.content.ContextCompat.getMainExecutor(context))
        } catch (error: Exception) {
            logWarning("Unable to start playback service: ${error.message}")
        }
    }

    private fun stopForegroundPlaybackServiceIfRunning() {
        if (!isForegroundServiceRunning) return
        try {
            // Release the controller first — Media3 sees "no controllers remain"
            // and demotes the service out of foreground, tearing the notification
            // down. Then explicitly stop the service we started via
            // startForegroundService so the "started" state is also cleared.
            phoneNotificationControllerFuture?.let { future ->
                androidx.media3.session.MediaController.releaseFuture(future)
            }
            phoneNotificationController?.release()
            phoneNotificationController = null
            phoneNotificationControllerFuture = null
            val componentName = android.content.ComponentName(
                context.packageName,
                UNIFIED_PLAYBACK_SERVICE_CLASS,
            )
            context.stopService(android.content.Intent().setComponent(componentName))
        } catch (error: Exception) {
            logWarning("Unable to release phone media controller: ${error.message}")
        } finally {
            isForegroundServiceRunning = false
            isInternalRestartInProgress = false
        }
    }

    private fun canProcessMetadataUpdateNow(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastMetadataUpdateAtElapsedMs < thermalMetadataUpdateMinIntervalMs) return false
        lastMetadataUpdateAtElapsedMs = now
        return true
    }

    /**
     * Pure decision for [handleAudioOutputDevicesRemoved], split out so the pause-on-
     * disconnect policy can be unit-tested without fabricating [AudioDeviceInfo]s.
     */
    @VisibleForTesting
    internal fun shouldPauseOnAudioOutputRemoval(
        wasPlaying: Boolean,
        externalOutputRemoved: Boolean,
        externalOutputStillConnected: Boolean,
    ): Boolean = wasPlaying && externalOutputRemoved && !externalOutputStillConnected

    @VisibleForTesting
    internal fun testSetPlayerState(state: PlayerState) {
        _playerState.value = state
    }

    @VisibleForTesting
    internal suspend fun testHandleAudioFocusChange(focusChange: Int) {
        handleAudioFocusChange(focusChange)
    }

    @VisibleForTesting
    internal fun testRequestAudioFocusGranted(): Boolean =
        requestAudioFocus() == AudioFocusResult.GRANTED

    @VisibleForTesting
    internal fun testPlayerVolume(): Float? = exoPlayer?.volume

    @VisibleForTesting
    internal fun testIsEqualizerAttached(): Boolean = equalizerController.isAttached

    @VisibleForTesting
    internal fun testStartFakeStreamFailover() {
        streamFailoverJob = controllerScope.launch { delay(60_000L) }
    }

    @VisibleForTesting
    internal fun testSetPlaybackLostRecoveryAttempts(value: Int) {
        playbackLostRecoveryAttempts = value
    }

    @VisibleForTesting
    internal fun testReconnectAttempts(): Int = reconnectAttempts

    @VisibleForTesting
    internal fun testPlaybackLostRecoveryAttempts(): Int = playbackLostRecoveryAttempts

    @VisibleForTesting
    internal fun testScheduleReconnect(
        station: Station,
        delayOverrideMs: Long? = null,
        countAttempt: Boolean = true,
    ) {
        scheduleReconnect(
            station = station,
            delayOverrideMs = delayOverrideMs,
            countAttempt = countAttempt,
        )
    }

    @VisibleForTesting
    internal fun testTriggerPlaybackLostRecovery(station: Station, reason: String) {
        triggerPlaybackLostRecovery(station, reason)
    }

    @VisibleForTesting
    internal fun testStartBufferingWatchdog(player: ExoPlayer) {
        startBufferingWatchdog(player)
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

        /**
         * Output [AudioDeviceInfo] types that represent an *external* audio route the
         * user is likely listening through away from the phone speaker — car, BT,
         * dock, wired, USB. When the last of these disappears we pause instead of
         * letting Android fall back to the built-in speaker. Newer constants (BLE/BUS)
         * are inlined at compile time and simply never match on older devices.
         */
        val EXTERNAL_OUTPUT_DEVICE_TYPES = setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_DOCK,
            AudioDeviceInfo.TYPE_AUX_LINE,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLE_BROADCAST,
            AudioDeviceInfo.TYPE_BUS,
        )
        const val thermalMetadataUpdateMinIntervalMs = 2_500L

        /** REA 95fcb33: cp3.f3367l0 — Bluetooth outputs on which the EQ stays off. */
        val BLUETOOTH_OUTPUT_DEVICE_TYPES = setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_HEARING_AID,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLE_BROADCAST,
        )

        /** REA 95fcb33: ba2 Fall 12, delay(3000). */
        const val EQUALIZER_ROUTE_CHECK_INTERVAL_MS = 3_000L
        // Hoisted regexes — compiled once, reused across all ICY metadata frames.
        val STREAM_TITLE_REGEX = Regex("(?i)StreamTitle='([^']*)'")
        val TITLE_SPLIT_REGEX = Regex("\\s[-–|]\\s")

        // Resolved by class name so core-player doesn't depend on the auto module.
        // The unified Media3 MediaLibraryService lives in the auto module today;
        // a rename/move to a core-player-resident class is a follow-up.
        const val UNIFIED_PLAYBACK_SERVICE_CLASS = "de.darksoon.radiowave.auto.RadioWaveAutoService"
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

        // REA 95fcb33 (p000/jx3.java): no load retries for any client error except
        // 408 Request Timeout / 429 Too Many Requests (was: only 404 and 410).
        if (exception is HttpDataSource.InvalidResponseCodeException &&
            exception.responseCode in 400..499 &&
            exception.responseCode != 408 &&
            exception.responseCode != 429
        ) {
            return C.TIME_UNSET
        }

        val baseDelayMs = min(1_000L * errorCount, 8_000L)
        return baseDelayMs
    }

    override fun getMinimumLoadableRetryCount(dataType: Int): Int {
        return 3
    }
}


private const val NAVIGATION_DUCK_VOLUME = 0.25f

/**
 * REA 95fcb33: neu (p000/ch4.java m1641r). An error the stream won't recover from by itself:
 * an HTTP client error (4xx except 408/429) anywhere in the first 12 causes, or a file-not-found,
 * content-type, container or decoder error.
 */
@UnstableApi
internal fun isPermanentStreamError(
    error: PlaybackException,
    streamWasReady: Boolean = false,
): Boolean {
    var cause: Throwable? = error
    repeat(12) {
        val current = cause ?: return@repeat
        if (current is HttpDataSource.InvalidResponseCodeException) {
            val code = current.responseCode
            return code in 400..499 && code != 408 && code != 429
        }
        cause = current.cause.takeUnless { it === current }
    }
    // FIX 2026-11-01 (Review P2-2): live Icecast/Shoutcast streams produce occasional
    // malformed-container / decode errors mid-stream (server hiccup, metadata glitch). Once
    // this URL has played, those are retried like network errors.
    if (streamWasReady && error.errorCode in TRANSIENT_AFTER_READY_ERROR_CODES) return false
    return error.errorCode in PERMANENT_ERROR_CODES
}

private val TRANSIENT_AFTER_READY_ERROR_CODES = setOf(
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_DECODING_FAILED,
)

private val PERMANENT_ERROR_CODES = setOf(
    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
    PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
)

/**
 * REA 95fcb33: Kandidatenwahl aus p000/wi0.java (Filterkette tm0/px3/zr1/sd4/zb2, Sortierung
 * e00/bg1). Same station identity ([duplicateKey]), not custom, not yet failed, never a
 * scheme downgrade, at most the original bitrate; https first, then highest bitrate.
 */
internal fun selectFailoverStream(
    station: Station,
    variants: List<Station>,
    failedStreamUrls: Set<String>,
): Station? {
    if (station.isCustom || station.uuid.startsWith("custom-")) return null
    val identity = station.duplicateKey() ?: return null
    val originalIsHttps = station.streamUrl.startsWith("https://", ignoreCase = true)
    val originalBitrate = station.bitrate ?: 0
    return variants.asSequence()
        .filter { !it.isCustom && it.duplicateKey() == identity }
        .filter { it.streamUrl != station.streamUrl && it.streamUrl !in failedStreamUrls }
        .filter { candidate ->
            val uri = runCatching { java.net.URI(candidate.streamUrl) }.getOrNull()
            uri?.host != null && (
                uri.scheme.equals("https", ignoreCase = true) ||
                    (!originalIsHttps && uri.scheme.equals("http", ignoreCase = true))
                )
        }
        .distinctBy { it.streamUrl }
        .filter { candidate ->
            val bitrate = candidate.bitrate ?: 0
            originalBitrate <= 0 || bitrate in 1..originalBitrate
        }
        .sortedWith(
            compareByDescending<Station> { it.streamUrl.startsWith("https://", ignoreCase = true) }
                .thenByDescending { it.bitrate ?: 0 },
        )
        .firstOrNull()
}
