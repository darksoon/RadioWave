// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.player

import android.content.Intent
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.ui.components.MarqueeText
import de.darksoon.radiowave.core.ui.components.StreamQualityBadge
import de.darksoon.radiowave.core.ui.theme.DarkBackground
import de.darksoon.radiowave.core.ui.theme.DarkSurface
import de.darksoon.radiowave.feature.player.R
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    playerState: PlayerState,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onFavoriteClick: () -> Unit,
    onPreviousStationClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onVolumeToggle: () -> Unit,
    onRandomStationClick: () -> Unit,
    onSleepTimerClick: (Int?) -> Unit,
    sleepTimerRemainingMs: Long?,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val station = playerState.currentStation ?: return
    val context = LocalContext.current
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val isPlaying = playerState.isPlaying
    val metadataTitle = playerState.metadata?.title?.trim().takeUnless { it.isNullOrBlank() }
    val metadataArtist = playerState.metadata?.artist?.trim().takeUnless { it.isNullOrBlank() }
    val streamCoverUrl = playerState.metadata?.albumArtUrl?.trim().takeUnless { it.isNullOrBlank() }
    val fallbackLogoUrl = station.faviconUrl?.trim().takeUnless { it.isNullOrBlank() }

    val iTunesCoverUrl by viewModel.coverArtUrl.collectAsStateWithLifecycle()

    LaunchedEffect(metadataArtist, metadataTitle) {
        viewModel.loadCoverArt(metadataArtist, metadataTitle)
    }

    val artworkUrl = iTunesCoverUrl ?: streamCoverUrl ?: fallbackLogoUrl
    val blurArtworkUrl = iTunesCoverUrl ?: streamCoverUrl

    val titleLine = metadataTitle ?: station.name
    val artistLine = metadataArtist
    val titleArtistLine = listOfNotNull(
        titleLine.takeIf { it.isNotBlank() },
        artistLine.takeIf { !it.isNullOrBlank() },
    ).joinToString("  •  ")
    val stationLine = station.name
    val playedDurationMs = playerState.playedDurationMs
    var nowElapsedMs by remember(station.uuid) {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }

    LaunchedEffect(station.uuid, isPlaying) {
        while (isPlaying) {
            nowElapsedMs = SystemClock.elapsedRealtime()
            delay(1_000L)
        }
    }

    val livePlayedDurationMs = playedDurationMs + if (isPlaying) {
        (nowElapsedMs - (playerState.sessionStartedAtElapsedMs ?: nowElapsedMs)).coerceAtLeast(0L)
    } else {
        0L
    }
    val runtimeLabel = formatRuntime(livePlayedDurationMs.coerceAtLeast(0L))
    val liveBarTransition = rememberInfiniteTransition(label = "liveBar")
    val liveBarAlpha by liveBarTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liveBarAlpha",
    )
    val liveDotAlpha by liveBarTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "liveDotAlpha",
    )
    val artworkScale by liveBarTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.026f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "artworkScale",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        // Blur background from cover art or fallback gradient
        if (blurArtworkUrl != null) {
            SubcomposeAsyncImage(
                model = blurArtworkUrl,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .blur(80.dp),
                contentScale = ContentScale.Crop,
                loading = { BlurFallbackBackground() },
                error = { BlurFallbackBackground() },
            )
            // Dark overlay for better text readability
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(34.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF3FE6D0).copy(alpha = 0.24f),
                                Color(0xFFCA4D95).copy(alpha = 0.14f),
                                Color.Transparent,
                            ),
                            radius = 1200f,
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.06f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.player_screen_close),
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .size(30.dp)
                            .padding(3.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.player_screen_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { showSleepTimerDialog = true },
                        shape = CircleShape,
                        color = if (sleepTimerRemainingMs != null) {
                            Color(0xFF52E3D9).copy(alpha = 0.18f)
                        } else {
                            Color.White.copy(alpha = 0.06f)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = stringResource(R.string.player_sleep_timer),
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier
                                .size(30.dp)
                                .padding(6.dp),
                        )
                    }
                    Surface(
                        onClick = {
                            val shareUrl = station.homepageUrl?.takeIf { it.isNotBlank() } ?: station.streamUrl
                            val shareText = buildString {
                                append(context.getString(R.string.player_share_template, station.name))
                                append("\n")
                                append(shareUrl)
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_SUBJECT, station.name)
                                .putExtra(Intent.EXTRA_TEXT, shareText)
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    context.getString(R.string.player_share),
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.06f),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = stringResource(R.string.player_share),
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier
                                .size(30.dp)
                                .padding(6.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(DarkSurface),
            ) {
                SubcomposeAsyncImage(
                    model = artworkUrl,
                    contentDescription = station.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val scale = if (isPlaying) artworkScale else 1f
                            scaleX = scale
                            scaleY = scale
                        },
                    contentScale = ContentScale.Crop,
                    loading = {
                        ArtworkFallback(station.name)
                    },
                    error = {
                        ArtworkFallback(station.name)
                    },
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Black.copy(alpha = 0.4f),
                                ),
                            ),
                        ),
                )
                if (playerState.isBuffering) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.42f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(54.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF52E3D9).copy(alpha = 0.9f),
                            trackColor = Color.White.copy(alpha = 0.15f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            MarqueeText(
                text = titleArtistLine,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                enabled = true,
                edgeFade = false,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (fallbackLogoUrl != null) {
                    SubcomposeAsyncImage(
                        model = fallbackLogoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop,
                        loading = { StationLogoPlaceholder() },
                        error = { StationLogoPlaceholder() },
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = stationLine,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val visibleTags = remember(station.tags) {
                station.tags.filter { it.isNotBlank() && it.length <= 20 }.take(3)
            }
            if (station.codec != null || station.bitrate != null || visibleTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StreamQualityBadge(
                        codec = station.codec,
                        bitrate = station.bitrate,
                    )
                    visibleTags.forEach { tag ->
                        Text(
                            text = tag.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.White.copy(alpha = liveBarAlpha),
                trackColor = Color.White.copy(alpha = 0.22f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                Text(
                    text = runtimeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.68f),
                )
                Spacer(modifier = Modifier.weight(1f))
                if (sleepTimerRemainingMs != null) {
                    Text(
                        text = stringResource(R.string.player_sleep_timer_active, formatRuntime(sleepTimerRemainingMs)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.68f),
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color(0xFFFF3B3B).copy(alpha = liveDotAlpha)),
                        )
                        Text(
                            text = stringResource(R.string.player_live),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.68f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerIconButton(
                    icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    tint = if (isFavorite) Color(0xFFFF5A7A) else Color.White.copy(alpha = 0.9f),
                    onClick = onFavoriteClick,
                )
                PlayerIconButton(
                    icon = Icons.Filled.SkipPrevious,
                    tint = Color.White.copy(alpha = 0.78f),
                    onClick = onPreviousStationClick,
                )
                MainPlaybackButton(
                    isPlaying = isPlaying,
                    onClick = onPlayPauseClick,
                )
                PlayerIconButton(
                    icon = if (playerState.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    tint = if (playerState.isMuted) Color.White.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.78f),
                    onClick = onVolumeToggle,
                )
                PlayerIconButton(
                    icon = Icons.Outlined.Shuffle,
                    tint = Color.White.copy(alpha = 0.72f),
                    onClick = onRandomStationClick,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }

        if (showSleepTimerDialog) {
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                title = { Text(stringResource(R.string.player_sleep_timer)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SleepTimerOption(text = stringResource(R.string.player_sleep_timer_15m)) {
                            onSleepTimerClick(15)
                            showSleepTimerDialog = false
                        }
                        SleepTimerOption(text = stringResource(R.string.player_sleep_timer_30m)) {
                            onSleepTimerClick(30)
                            showSleepTimerDialog = false
                        }
                        SleepTimerOption(text = stringResource(R.string.player_sleep_timer_60m)) {
                            onSleepTimerClick(60)
                            showSleepTimerDialog = false
                        }
                        SleepTimerOption(text = stringResource(R.string.player_sleep_timer_90m)) {
                            onSleepTimerClick(90)
                            showSleepTimerDialog = false
                        }
                        if (sleepTimerRemainingMs != null) {
                            SleepTimerOption(text = stringResource(R.string.player_sleep_timer_off)) {
                                onSleepTimerClick(null)
                                showSleepTimerDialog = false
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text(stringResource(R.string.player_screen_close))
                    }
                },
            )
        }
    }
}

@Composable
private fun SleepTimerOption(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.06f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun MainPlaybackButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val ringTransition = rememberInfiniteTransition(label = "playbackRing")
    val ringRotation by ringTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2800,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "playbackRingRotation",
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer {
                        rotationZ = if (isPlaying) ringRotation else 0f
                    }
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF52E3D9),
                                Color(0xFFC94F99),
                            ),
                        ),
                        CircleShape,
                    ),
            )
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1D26)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) {
                        stringResource(R.string.player_pause)
                    } else {
                        stringResource(R.string.player_play)
                    },
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(42.dp)
                .padding(8.dp),
        )
    }
}

@Composable
private fun ArtworkFallback(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF294252),
                        Color(0xFF1A2234),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun BlurFallbackBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3FE6D0).copy(alpha = 0.24f),
                        Color(0xFFCA4D95).copy(alpha = 0.14f),
                        Color.Transparent,
                    ),
                    radius = 1200f,
                ),
            ),
    )
}

@Composable
private fun StationLogoPlaceholder() {
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
    )
}

private fun formatRuntime(durationMs: Long): String {
    val totalSeconds = durationMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%01d:%02d".format(minutes, seconds)
    }
}


