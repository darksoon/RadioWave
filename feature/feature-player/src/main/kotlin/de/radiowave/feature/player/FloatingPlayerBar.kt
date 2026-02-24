package de.radiowave.feature.player

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import de.radiowave.core.model.PlayerState
import de.radiowave.core.model.Station
import de.radiowave.core.ui.theme.DarkCardBackground
import de.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.radiowave.core.ui.theme.DarkSurfaceVariant
import de.radiowave.core.ui.theme.TealAccent
import kotlinx.coroutines.delay

@Composable
fun FloatingPlayerBar(
    playerState: PlayerState,
    onPlayPauseClick: () -> Unit,
    onBarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentStation: Station? = playerState.currentStation
    val isPlaying: Boolean = playerState.isPlaying
    val isBuffering: Boolean = playerState.isBuffering
    val compactMetadata: String? = buildCompactMetadata(
        stationName = currentStation?.name,
        metadataTitle = playerState.metadata?.title,
        metadataArtist = playerState.metadata?.artist,
    )
    val sessionStartedAtMs: Long? = playerState.sessionStartedAtElapsedMs

    if (currentStation != null) {
        val stationName: String = currentStation.name
        val faviconUrl: String? = currentStation.faviconUrl
        var nowElapsedMs by remember(sessionStartedAtMs) {
            mutableLongStateOf(SystemClock.elapsedRealtime())
        }

        LaunchedEffect(sessionStartedAtMs, isPlaying, isBuffering) {
            if (sessionStartedAtMs == null) return@LaunchedEffect
            while (isPlaying || isBuffering) {
                nowElapsedMs = SystemClock.elapsedRealtime()
                delay(1_000L)
            }
        }

        val sessionDurationLabel = sessionStartedAtMs?.let { startedAt ->
            formatStreamDuration((nowElapsedMs - startedAt).coerceAtLeast(0L))
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onBarClick),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.24f),
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 12.dp,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color(0xCC212B37),
                                DarkCardBackground.copy(alpha = 0.86f),
                            ),
                        ),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(22.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.14f),
                                    Color(0x553E9AB8),
                                    Color.Transparent,
                                ),
                                radius = 420f,
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    TealAccent.copy(alpha = 0.1f),
                                    Color.Transparent,
                                ),
                                radius = 260f,
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.04f),
                                ),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(1.dp)
                        .border(
                            border = BorderStroke(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.14f),
                            ),
                            shape = RoundedCornerShape(13.dp),
                        ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.26f)),
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LinearProgressIndicator(
                        progress = {
                            when {
                                isBuffering -> 0.45f
                                isPlaying -> 1f
                                else -> 0f
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(2.dp),
                        color = TealAccent.copy(alpha = 0.9f),
                        trackColor = Color.Transparent,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant),
                        ) {
                            AsyncImage(
                                model = faviconUrl,
                                contentDescription = stationName,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Crop,
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stationName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White,
                            )

                            val showMetadataLine = isBuffering || compactMetadata != null || sessionDurationLabel != null
                            val secondaryLine = when {
                                isBuffering -> "Wird geladen..."
                                else -> compactMetadata
                            }
                            val secondaryColor = if (isBuffering) TealAccent else DarkOnSurfaceVariant

                            if (showMetadataLine) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                    if (secondaryLine != null) {
                                        Text(
                                            text = secondaryLine,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = secondaryColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }

                                    if (sessionDurationLabel != null) {
                                        StreamDurationBadge(
                                            text = sessionDurationLabel,
                                            modifier = Modifier.padding(start = 8.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        Box(
                            modifier = Modifier.padding(end = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.dp,
                                    color = TealAccent,
                                    trackColor = Color.Transparent,
                                )
                            }

                            Surface(
                                onClick = onPlayPauseClick,
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.18f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.28f),
                                ),
                                enabled = !isBuffering,
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .padding(6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamDurationBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.26f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.16f),
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private fun formatStreamDuration(durationMs: Long): String {
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

private fun buildCompactMetadata(
    stationName: String?,
    metadataTitle: String?,
    metadataArtist: String?,
): String? {
    val cleanedTitle = metadataTitle?.trim().takeUnless { it.isNullOrBlank() }
    val cleanedArtist = metadataArtist?.trim().takeUnless { it.isNullOrBlank() }
    val cleanedStation = stationName?.trim().takeUnless { it.isNullOrBlank() }

    val normalizedStation = cleanedStation?.lowercase()
    val normalizedTitle = cleanedTitle?.lowercase()
    val normalizedArtist = cleanedArtist?.lowercase()

    val effectiveTitle = cleanedTitle?.takeUnless {
        normalizedStation != null && normalizedTitle == normalizedStation
    }
    val effectiveArtist = cleanedArtist?.takeUnless {
        normalizedStation != null && normalizedArtist == normalizedStation
    }

    return when {
        effectiveArtist != null && effectiveTitle != null -> "$effectiveArtist - $effectiveTitle"
        effectiveTitle != null -> effectiveTitle
        effectiveArtist != null -> effectiveArtist
        else -> null
    }
}
