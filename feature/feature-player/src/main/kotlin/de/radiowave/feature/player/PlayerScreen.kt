package de.radiowave.feature.player

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import de.radiowave.core.model.PlayerState
import de.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.radiowave.core.ui.theme.DarkSurfaceVariant
import de.radiowave.core.ui.theme.TealAccent
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    playerState: PlayerState,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onFavoriteClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val station = playerState.currentStation ?: return
    val isPlaying = playerState.isPlaying
    val isBuffering = playerState.isBuffering
    val metaTitle = playerState.metadata?.title?.takeIf { !it.isNullOrBlank() }
    val metaArtist = playerState.metadata?.artist?.takeIf { !it.isNullOrBlank() }
    val artworkUrl = playerState.metadata?.albumArtUrl?.takeIf { !it.isNullOrBlank() } ?: station.faviconUrl
    val sessionStart = playerState.sessionStartedAtElapsedMs ?: remember(station.uuid) {
        SystemClock.elapsedRealtime()
    }
    var nowElapsedMs by remember(sessionStart) {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }

    LaunchedEffect(sessionStart, isPlaying, isBuffering) {
        while (isPlaying || isBuffering) {
            nowElapsedMs = SystemClock.elapsedRealtime()
            delay(1_000L)
        }
    }

    val runtimeLabel = formatPlayerRuntime((nowElapsedMs - sessionStart).coerceAtLeast(0L))

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF00B1120),
                        Color(0xF6141E31),
                        Color(0xF0080D17),
                    ),
                ),
            ),
            contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(32.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            TealAccent.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                        radius = 850f,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schliessen",
                        tint = Color.White,
                        modifier = Modifier
                            .size(34.dp)
                            .padding(7.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(DarkSurfaceVariant),
            ) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = station.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Black.copy(alpha = 0.45f),
                                ),
                            ),
                        ),
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = station.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 33.sp,
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (metaTitle != null || metaArtist != null || isBuffering) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        isBuffering -> "Wird geladen..."
                        metaArtist != null && metaTitle != null -> "$metaArtist - $metaTitle"
                        metaTitle != null -> metaTitle
                        else -> metaArtist ?: ""
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isBuffering) TealAccent else DarkOnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = runtimeLabel,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.84f),
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onFavoriteClick,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = if (isFavorite) 0.2f else 0.1f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isFavorite) Color(0xFFFF5A7A) else Color.White.copy(alpha = 0.25f),
                    ),
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFFF5A7A) else Color.White,
                        modifier = Modifier
                            .size(52.dp)
                            .padding(12.dp),
                    )
                }

                Spacer(modifier = Modifier.size(22.dp))

                Box(contentAlignment = Alignment.Center) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(76.dp),
                            strokeWidth = 2.dp,
                            color = TealAccent,
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )
                    }
                    Surface(
                        onClick = onPlayPauseClick,
                        shape = CircleShape,
                        color = TealAccent.copy(alpha = 0.92f),
                        enabled = !isBuffering,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier
                                .size(76.dp)
                                .padding(18.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

private fun formatPlayerRuntime(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
