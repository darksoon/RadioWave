package de.radiowave.core.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import de.radiowave.core.model.Station
import javax.inject.Inject

@AndroidEntryPoint
class RadioPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    
    @Inject
    lateinit var playerController: PlayerController

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            startForeground(NOTIFICATION_ID, buildNotification())
                        } else {
                            stopForeground(false)
                        }
                    }
                })
            }

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: List<MediaItem>,
                ): ListenableFuture<List<MediaItem>> {
                    val updatedItems = mediaItems.map { mediaItem ->
                        mediaItem.buildUpon()
                            .setUri(mediaItem.requestMetadata.mediaUri)
                            .build()
                    }
                    return Futures.immediateFuture(updatedItems)
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Radio Playback",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows the currently playing radio station"
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val player = mediaSession?.player ?: return createDefaultNotification()
        
        val mediaMetadata = player.mediaMetadata
        val title = mediaMetadata.title?.toString() ?: "RadioWave"
        val artist = mediaMetadata.artist?.toString() ?: "Playing"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    private fun createDefaultNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RadioWave")
            .setContentText("Ready to play")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "radio_playback_channel"
        private const val NOTIFICATION_ID = 1
        
        fun playStation(context: Context, station: Station) {
            val intent = Intent(context, RadioPlayerService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_STATION_URL, station.streamUrl)
                putExtra(EXTRA_STATION_NAME, station.name)
                putExtra(EXTRA_STATION_LOGO, station.faviconUrl)
            }

            context.startForegroundService(intent)
        }
        
        private const val ACTION_PLAY = "de.radiowave.player.ACTION_PLAY"
        private const val EXTRA_STATION_URL = "station_url"
        private const val EXTRA_STATION_NAME = "station_name"
        private const val EXTRA_STATION_LOGO = "station_logo"
    }
}
