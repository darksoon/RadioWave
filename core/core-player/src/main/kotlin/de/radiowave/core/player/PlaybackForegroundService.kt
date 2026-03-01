package de.radiowave.core.player

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
import de.radiowave.core.model.AppSettings
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Lightweight foreground service to keep playback process alive in background/screen-off state.
 */
@AndroidEntryPoint
class PlaybackForegroundService : Service() {
    @Inject
    lateinit var playerController: PlayerController

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        when (intent?.action) {
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

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        playerController.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(
        stationName: String,
        subtitle: String,
        isPlaying: Boolean,
    ): Notification {
        val prefs = getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)
        val showPrevious = prefs.getBoolean(AppSettings.KEY_NOTIFICATION_SHOW_PREVIOUS, true)
        val showPlayPause = prefs.getBoolean(AppSettings.KEY_NOTIFICATION_SHOW_PLAY_PAUSE, true)
        val showNext = prefs.getBoolean(AppSettings.KEY_NOTIFICATION_SHOW_NEXT, true)
        val showStop = prefs.getBoolean(AppSettings.KEY_NOTIFICATION_SHOW_STOP, true)

        val titleText = stationName.ifBlank { "RadioWave" }
        val contentText = subtitle.ifBlank {
            if (isPlaying) "Live stream playing" else "Playback paused"
        }
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                REQUEST_CONTENT,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag(),
            )
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)

        if (contentIntent != null) {
            builder.setContentIntent(contentIntent)
        }

        if (showPrevious) {
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    createServicePendingIntent(ACTION_PREVIOUS, REQUEST_PREVIOUS),
                ),
            )
        }
        if (showPlayPause) {
            builder.addAction(
                NotificationCompat.Action(
                    if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (isPlaying) "Pause" else "Play",
                    createServicePendingIntent(ACTION_TOGGLE_PLAY_PAUSE, REQUEST_TOGGLE),
                ),
            )
        }
        if (showNext) {
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_next,
                    "Next",
                    createServicePendingIntent(ACTION_NEXT, REQUEST_NEXT),
                ),
            )
        }
        if (showStop) {
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Stop",
                    createServicePendingIntent(ACTION_STOP_PLAYBACK, REQUEST_STOP_PLAYBACK),
                ),
            )
        }
        return builder.build()
    }

    private fun createServicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, PlaybackForegroundService::class.java).apply {
            this.action = action
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
        ).joinToString(" - ")
        if (metadataText.isNotBlank()) return metadataText
        return if (isPlaying) "Live stream playing" else "Playback paused"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Radio playback",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps radio playback active in background"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "radiowave_playback_foreground"
        private const val NOTIFICATION_ID = 42
        private const val EXTRA_STATION_NAME = "extra_station_name"
        private const val EXTRA_SUBTITLE = "extra_subtitle"
        private const val EXTRA_IS_PLAYING = "extra_is_playing"
        private const val ACTION_START = "de.radiowave.core.player.START_FOREGROUND"
        private const val ACTION_STOP_SERVICE = "de.radiowave.core.player.STOP_FOREGROUND"
        private const val ACTION_PREVIOUS = "de.radiowave.core.player.ACTION_PREVIOUS"
        private const val ACTION_NEXT = "de.radiowave.core.player.ACTION_NEXT"
        private const val ACTION_TOGGLE_PLAY_PAUSE = "de.radiowave.core.player.ACTION_TOGGLE_PLAY_PAUSE"
        private const val ACTION_STOP_PLAYBACK = "de.radiowave.core.player.ACTION_STOP_PLAYBACK"
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
