package de.radiowave.core.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Lightweight foreground service to keep playback process alive in background/screen-off state.
 */
class PlaybackForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            else -> {
                val stationName = intent?.getStringExtra(EXTRA_STATION_NAME).orEmpty()
                val subtitle = intent?.getStringExtra(EXTRA_SUBTITLE).orEmpty()
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(stationName, subtitle),
                )
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(stationName: String, subtitle: String): Notification {
        val titleText = stationName.ifBlank { "RadioWave" }
        val contentText = subtitle.ifBlank { "Live stream playing" }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
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
        private const val ACTION_START = "de.radiowave.core.player.START_FOREGROUND"
        private const val ACTION_STOP = "de.radiowave.core.player.STOP_FOREGROUND"

        fun start(
            context: Context,
            stationName: String,
            subtitle: String,
        ) {
            val intent = Intent(context, PlaybackForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_STATION_NAME, stationName)
                putExtra(EXTRA_SUBTITLE, subtitle)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PlaybackForegroundService::class.java))
        }
    }
}
