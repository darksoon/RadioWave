// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import de.darksoon.radiowave.core.data.repository.AppSettingsState
import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.player.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Lightweight foreground service to keep playback process alive in background/screen-off state.
 */
@AndroidEntryPoint
class PlaybackForegroundService : Service() {
    @Inject
    lateinit var playerController: PlayerController

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var mediaSession: MediaSessionCompat? = null

    // Scope tied to the service lifetime; cancelled in onDestroy.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Lazily-initialised so settingsRepository is available; Eagerly started so
    // notification builds can read .value synchronously.
    private val settingsState: StateFlow<AppSettingsState> by lazy {
        settingsRepository.data.stateIn(
            scope = serviceScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettingsState.DEFAULTS,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        ensureMediaSession()

        // Always promote to foreground BEFORE handling any action.
        // On Android 12+ the OS throws ForegroundServiceDidNotStartInTimeException
        // if we process a button action without first calling startForeground(), e.g.
        // when the system restarts the service after a process kill and immediately
        // delivers a pending notification-button intent.
        val action = intent?.action
        if (action != ACTION_STOP_SERVICE && action != ACTION_STOP_PLAYBACK) {
            val state = playerController.playerState.value
            val stationName = state.currentStation?.name.orEmpty()
            val subtitle = refreshSubtitleFromState(state)
            startForeground(NOTIFICATION_ID, buildNotification(stationName, subtitle, state.isPlaying))
        }

        when (action) {
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_TOGGLE_PLAY_PAUSE -> {
                playerController.togglePlayPause()
                refreshFromPlayerState()
            }
            ACTION_PREVIOUS -> {
                playerController.playPreviousStation()
                refreshFromPlayerState()
            }
            ACTION_NEXT -> {
                playerController.playNextStation()
                refreshFromPlayerState()
            }
            ACTION_STOP_PLAYBACK -> {
                playerController.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            else -> {
                // Initial start: use extras from the intent (more accurate than player state).
                val stationName = intent?.getStringExtra(EXTRA_STATION_NAME).orEmpty()
                val subtitle = intent?.getStringExtra(EXTRA_SUBTITLE).orEmpty()
                val isPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, false) == true
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(stationName, subtitle, isPlaying),
                )
            }
        }

        return START_STICKY
    }

    private fun refreshSubtitleFromState(state: de.darksoon.radiowave.core.model.PlayerState): String {
        val metadata = state.metadata
        val metadataText = listOfNotNull(
            metadata?.artist?.trim().takeUnless { it.isNullOrBlank() },
            metadata?.title?.trim().takeUnless { it.isNullOrBlank() },
        ).joinToString(" • ")
        if (metadataText.isNotBlank()) return metadataText
        return defaultPlaybackStatusText(state.isPlaying)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        playerController.stop()
    }

    override fun onDestroy() {
        mediaSession?.apply {
            isActive = false
            release()
        }
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(
        stationName: String,
        subtitle: String,
        isPlaying: Boolean,
    ): Notification {
        val settings = settingsState.value
        val showPrevious = settings.notificationShowPrevious
        val showPlayPause = settings.notificationShowPlayPause
        val showNext = settings.notificationShowNext
        val showStop = settings.notificationShowStop

        val titleText = stationName.ifBlank { getString(R.string.notification_app_name) }
        val contentText = subtitle.ifBlank {
            defaultPlaybackStatusText(isPlaying)
        }
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            action = ACTION_OPEN_PLAYER
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                REQUEST_CONTENT,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag(),
            )
        }

        updateMediaSession(stationName = titleText, subtitle = contentText, isPlaying = isPlaying)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setSubText(
                if (isPlaying) {
                    getString(R.string.notification_status_live)
                } else {
                    getString(R.string.notification_status_paused)
                },
            )
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setShowWhen(false)
            .setStyle(MediaStyle().setMediaSession(mediaSession?.sessionToken))

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent)
        }

        var compactActionIndex = 0

        if (showPrevious) {
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_previous,
                    getString(R.string.notification_action_previous),
                    createServicePendingIntent(ACTION_PREVIOUS, REQUEST_PREVIOUS),
                ),
            )
            compactActionIndex++
        }
        if (showPlayPause) {
            val playPauseCompactIndex = compactActionIndex
            builder.addAction(
                NotificationCompat.Action(
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (isPlaying) {
                        getString(R.string.notification_action_pause)
                    } else {
                        getString(R.string.notification_action_play)
                    },
                    createServicePendingIntent(ACTION_TOGGLE_PLAY_PAUSE, REQUEST_TOGGLE),
                ),
            )
            compactActionIndex++
            builder.setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(
                        if (showPrevious) playPauseCompactIndex - 1 else playPauseCompactIndex,
                        playPauseCompactIndex,
                    ),
            )
        }
        if (showNext) {
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_next,
                    getString(R.string.notification_action_next),
                    createServicePendingIntent(ACTION_NEXT, REQUEST_NEXT),
                ),
            )
            compactActionIndex++
        }
        if (showStop) {
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    getString(R.string.notification_action_stop),
                    createServicePendingIntent(ACTION_STOP_PLAYBACK, REQUEST_STOP_PLAYBACK),
                ),
            )
        }
        return builder.build()
    }

    private fun createServicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this@PlaybackForegroundService, PlaybackForegroundService::class.java).apply {
            this.action = action
            setPackage(packageName)
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag(),
        )
    }

    private fun pendingIntentImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    private fun refreshFromPlayerState() {
        val state = playerController.playerState.value
        val stationName = state.currentStation?.name.orEmpty()
        val subtitle = buildSubtitle(
            isPlaying = state.isPlaying,
            metadataArtist = state.metadata?.artist,
            metadataTitle = state.metadata?.title,
        )
        startForeground(
            NOTIFICATION_ID,
            buildNotification(stationName, subtitle, state.isPlaying),
        )
    }

    private fun buildSubtitle(
        isPlaying: Boolean,
        metadataArtist: String?,
        metadataTitle: String?,
    ): String {
        val metadataText = listOfNotNull(
            metadataArtist?.trim().takeUnless { it.isNullOrBlank() },
            metadataTitle?.trim().takeUnless { it.isNullOrBlank() },
        ).joinToString(" • ")
        if (metadataText.isNotBlank()) return metadataText
        return defaultPlaybackStatusText(isPlaying)
    }

    private fun defaultPlaybackStatusText(isPlaying: Boolean): String {
        return if (isPlaying) {
            getString(R.string.notification_playing_fallback)
        } else {
            getString(R.string.notification_paused_fallback)
        }
    }

    private fun ensureMediaSession() {
        if (mediaSession != null) return
        mediaSession = MediaSessionCompat(this, "RadioWavePlaybackSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS,
            )
            setCallback(
                object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        playerController.togglePlayPause()
                        refreshFromPlayerState()
                    }

                    override fun onPause() {
                        playerController.togglePlayPause()
                        refreshFromPlayerState()
                    }

                    override fun onSkipToPrevious() {
                        playerController.playPreviousStation()
                        refreshFromPlayerState()
                    }

                    override fun onSkipToNext() {
                        playerController.playNextStation()
                        refreshFromPlayerState()
                    }

                    override fun onStop() {
                        playerController.stop()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                },
            )
            isActive = true
        }
    }

    private fun updateMediaSession(
        stationName: String,
        subtitle: String,
        isPlaying: Boolean,
    ) {
        val session = mediaSession ?: return
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, stationName)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, subtitle)
                .build(),
        )
        val playbackActions =
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(playbackActions)
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                if (isPlaying) 1f else 0f,
            )
            .build()
        session.setPlaybackState(playbackState)
        session.isActive = true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "radiowave_playback_foreground"
        private const val NOTIFICATION_ID = 42
        private const val EXTRA_STATION_NAME = "extra_station_name"
        private const val EXTRA_SUBTITLE = "extra_subtitle"
        private const val EXTRA_IS_PLAYING = "extra_is_playing"
        private const val ACTION_START = "de.darksoon.radiowave.core.player.START_FOREGROUND"
        private const val ACTION_STOP_SERVICE = "de.darksoon.radiowave.core.player.STOP_FOREGROUND"
        private const val ACTION_PREVIOUS = "de.darksoon.radiowave.core.player.ACTION_PREVIOUS"
        private const val ACTION_NEXT = "de.darksoon.radiowave.core.player.ACTION_NEXT"
        private const val ACTION_TOGGLE_PLAY_PAUSE = "de.darksoon.radiowave.core.player.ACTION_TOGGLE_PLAY_PAUSE"
        private const val ACTION_STOP_PLAYBACK = "de.darksoon.radiowave.core.player.ACTION_STOP_PLAYBACK"
        private const val ACTION_OPEN_PLAYER = "de.darksoon.radiowave.action.OPEN_PLAYER"
        private const val REQUEST_CONTENT = 1001
        private const val REQUEST_PREVIOUS = 1002
        private const val REQUEST_TOGGLE = 1003
        private const val REQUEST_NEXT = 1004
        private const val REQUEST_STOP_PLAYBACK = 1005

        fun start(
            context: Context,
            stationName: String,
            subtitle: String,
            isPlaying: Boolean,
        ) {
            val intent = Intent(context, PlaybackForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_STATION_NAME, stationName)
                putExtra(EXTRA_SUBTITLE, subtitle)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PlaybackForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}

