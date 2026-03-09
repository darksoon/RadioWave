// SPDX-License-Identifier: GPL-3.0-or-later

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import de.radiowave.core.model.PlayerState
import de.radiowave.core.model.Station
import de.radiowave.core.ui.components.MarqueeText
import de.radiowave.core.ui.components.StationLogoImage
import de.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.radiowave.core.ui.theme.DarkSurfaceVariant
import de.radiowave.core.ui.theme.TealAccent
import kotlinx.coroutines.delay

@Composable
fun FloatingPlayerBar(
    playerState: PlayerState,
    isFavorite: Boolean,
    showMetadata: Boolean,
    onFavoriteClick: () -> Unit,
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
        val effectiveSessionStartMs = sessionStartedAtMs ?: remember(currentStation.uuid) {
            SystemClock.elapsedRealtime()
        }
        var nowElapsedMs by remember(effectiveSessionStartMs) {
            mutableLongStateOf(SystemClock.elapsedRealtime())
        }

        LaunchedEffect(effectiveSessionStartMs, isPlaying, isBuffering) {
            while (isPlaying || isBuffering) {
                nowElapsedMs = SystemClock.elapsedRealtime()
                delay(1_000L)
            }
        }

        val sessionDurationLabel = formatStreamDuration((nowElapsedMs - effectiveSessionStartMs).coerceAtLeast(0L))

        val playerShape = RoundedCornerShape(32.dp)
        Box(
            modifier = modifier
                .clickable(onClick = onBarClick)
                .clip(playerShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xE0202C3A),
                            Color(0xCC161C2A),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            TealAccent.copy(alpha = 0.72f),
                            Color(0xFF7A56FF).copy(alpha = 0.72f),
                        ),
                    ),
                    shape = playerShape,
                ),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(18.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent,
                            ),
                            radius = 420f,
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant),
                ) {
                    StationLogoImage(
                        imageUrl = faviconUrl,
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

                    val showMetadataLine = isBuffering || (showMetadata && compactMetadata != null) || sessionDurationLabel.isNotBlank()
                    val secondaryLine = when {
                        isBuffering -> "Wird geladen..."
                        showMetadata -> compactMetadata
                        else -> null
                    }
                    val secondaryColor = if (isBuffering) TealAccent else DarkOnSurfaceVariant

                    if (showMetadataLine) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MarqueeText(
                                text = secondaryLine ?: "",
                                textStyle = MaterialTheme.typography.bodySmall,
                                color = secondaryColor,
                                modifier = Modifier.weight(1f),
                                enabled = shouldEnableMiniPlayerMarquee(secondaryLine),
                                edgeFade = true,
                            )

                            StreamDurationBadge(
                                text = sessionDurationLabel,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                Row(
                    modifier = Modifier.padding(end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        onClick = onFavoriteClick,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = if (isFavorite) 0.24f else 0.12f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isFavorite) {
                                Color(0xFFFF5A7A).copy(alpha = 0.75f)
                            } else {
                                Color.White.copy(alpha = 0.2f)
                            },
                        ),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "Favorit entfernen" else "Zu Favoriten",
                            tint = if (isFavorite) Color(0xFFFF5A7A) else Color.White.copy(alpha = 0.92f),
                            modifier = Modifier
                                .size(32.dp)
                                .padding(7.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 1.25.dp,
                                color = TealAccent.copy(alpha = 0.95f),
                                trackColor = Color.White.copy(alpha = 0.2f),
                            )
                        }

                        Surface(
                            onClick = onPlayPauseClick,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.16f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.24f),
                            ),
                            enabled = !isBuffering,
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun shouldEnableMiniPlayerMarquee(text: String?): Boolean {
    if (text.isNullOrBlank()) return false
    return text.trim().length > 28
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

