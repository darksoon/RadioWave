package de.radiowave.core.player

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import de.radiowave.core.model.PlayerError
import de.radiowave.core.model.PlayerState
import de.radiowave.core.model.Station
import de.radiowave.core.model.StreamMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlayerController {

    private var exoPlayer: ExoPlayer? = null

    private val _playerState = MutableStateFlow(PlayerState())
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context)
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
                        15000, // minBufferMs
                        50000, // maxBufferMs
                        2500,  // bufferForPlaybackMs
                        5000   // bufferForPlaybackAfterRebufferMs
                    )
                    .build()
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { player ->
                setupPlayerListeners(player)
                exoPlayer = player
            }
    }

    private fun setupPlayerListeners(player: ExoPlayer) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        Log.d("RadioWave", "Player: STATE_BUFFERING - buffering audio data")
                    }
                    Player.STATE_READY -> {
                        Log.d("RadioWave", "Player: STATE_READY - ready to play")
                    }
                    Player.STATE_ENDED -> {
                        Log.d("RadioWave", "Player: STATE_ENDED - stream ended")
                    }
                    Player.STATE_IDLE -> {
                        Log.d("RadioWave", "Player: STATE_IDLE - idle state")
                    }
                }
                _playerState.update {
                    it.copy(
                        isBuffering = state == Player.STATE_BUFFERING,
                        isLoading = state == Player.STATE_BUFFERING && it.currentStation != null,
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val playerError = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> PlayerError.NetworkError
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> PlayerError.StreamBroken
                    else -> PlayerError.Unknown(error.message ?: "Unknown Error")
                }
                _playerState.update {
                    it.copy(
                        error = playerError,
                        isPlaying = false,
                        isBuffering = false,
                    )
                }
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

    override suspend fun playStation(station: Station) {
        _playerState.update {
            it.copy(
                currentStation = station,
                error = null,
                isLoading = true,
            )
        }

        val player = getOrCreatePlayer()

        val mediaItem = MediaItem.Builder()
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtworkUri(station.faviconUrl?.let { android.net.Uri.parse(it) })
                    .build(),
            )
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    override fun stop() {
        exoPlayer?.stop()
        _playerState.update { PlayerState() }
    }

    override fun release() {
        exoPlayer?.release()
        exoPlayer = null
        _playerState.update { PlayerState() }
    }
}
