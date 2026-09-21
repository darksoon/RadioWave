// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.player

import android.content.Intent
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.mediarouter.app.MediaRouteButton
import coil.compose.SubcomposeAsyncImage
import com.google.android.gms.cast.framework.CastButtonFactory
import de.darksoon.radiowave.core.model.PlayerError
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.ui.components.MarqueeText
import de.darksoon.radiowave.core.ui.components.StationLogoImage
import de.darksoon.radiowave.core.ui.components.StationNameText
import de.darksoon.radiowave.core.ui.components.asSafeHttpImageUrl
import de.darksoon.radiowave.core.ui.components.StreamQualityBadge
import de.darksoon.radiowave.core.ui.theme.DarkBackground
import de.darksoon.radiowave.core.ui.theme.DarkSurface
import de.darksoon.radiowave.feature.player.R
import de.darksoon.radiowave.core.player.R as CorePlayerR
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    playerState: PlayerState,
    isFavorite: Boolean,
    isCasting: Boolean,
    onDismiss: () -> Unit,
    onFavoriteClick: () -> Unit,
    onPreviousStationClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onVolumeToggle: () -> Unit,
    onRandomStationClick: () -> Unit,
    onSleepTimerClick: (Int?) -> Unit,
    sleepTimerEndsAtElapsedMs: Long?,
    modifier: Modifier = Modifier,
    onSimilarStationClick: ((Station) -> Unit)? = null,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val station = playerState.currentStation ?: return
    val context = LocalContext.current
    // Config-change-aware resource lookups for the share callback below (Compose lint
    // LocalContextGetResourceValueCall flags Context.getString from LocalContext.current).
    val resources = LocalResources.current
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val isPlaying = playerState.isPlaying
    val metadataTitle = playerState.metadata?.title?.trim().takeUnless { it.isNullOrBlank() }
    val metadataArtist = playerState.metadata?.artist?.trim().takeUnless { it.isNullOrBlank() }
    // Scheme-guard every URL that reaches Coil below — this screen bypasses
    // StationLogoImage and calls SubcomposeAsyncImage directly, so the http/https
    // filter must be applied here (audit 2026-07-18, H1).
    val streamCoverUrl = playerState.metadata?.albumArtUrl?.trim().asSafeHttpImageUrl()
    val fallbackLogoUrl = station.faviconUrl?.trim().asSafeHttpImageUrl()

    val iTunesCoverUrl by viewModel.coverArtUrl.collectAsStateWithLifecycle()
    val safeITunesCoverUrl = iTunesCoverUrl.asSafeHttpImageUrl()

    LaunchedEffect(metadataArtist, metadataTitle) {
        viewModel.loadCoverArt(metadataArtist, metadataTitle)
    }

    val similarStations by viewModel.similarStations.collectAsStateWithLifecycle()
    // Keyed on the station only: one directory lookup per station, not per recomposition.
    // The ViewModel also keeps the result when the player is closed and reopened.
    LaunchedEffect(station.uuid, onSimilarStationClick != null) {
        if (onSimilarStationClick != null) viewModel.loadSimilarStations(station)
    }
    val visibleSimilarStations = similarStations.takeIf { it.sourceStationUuid == station.uuid }

    val artworkUrl = safeITunesCoverUrl ?: streamCoverUrl ?: fallbackLogoUrl
    val blurArtworkUrl = safeITunesCoverUrl ?: streamCoverUrl

    val titleLine = metadataTitle ?: station.name
    val artistLine = metadataArtist
    val titleArtistLine = listOfNotNull(
        titleLine.takeIf { it.isNotBlank() },
        artistLine.takeIf { !it.isNullOrBlank() },
    ).joinToString("  •  ")
    val stationLine = station.name
    var nowElapsedMs by remember(station.uuid) {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }

    LaunchedEffect(station.uuid, isPlaying, sleepTimerEndsAtElapsedMs) {
        // Tick every second while playing (for the runtime label) OR while a sleep timer
        // is active (so its countdown keeps updating even if playback is paused).
        while (isPlaying || sleepTimerEndsAtElapsedMs != null) {
            nowElapsedMs = SystemClock.elapsedRealtime()
            delay(1_000L)
        }
    }

    // Remaining sleep-timer time, recomputed against the per-second `nowElapsedMs` ticker
    // above so the countdown actually ticks down. Passing a value snapshotted once by the
    // caller left it frozen — MainActivity does not recompose every second.
    val sleepTimerRemainingMs = sleepTimerEndsAtElapsedMs?.let {
        (it - nowElapsedMs).coerceAtLeast(0L)
    }

    // The controller reports the mobile-data block as a generic error with this message.
    val wifiRequiredMessage = stringResource(CorePlayerR.string.player_error_wifi_required)
    val isWifiRequiredError = (playerState.error as? PlayerError.Unknown)?.message == wifiRequiredMessage
    val liveStatus = resolveLiveStatus(playerState, isWifiRequiredError)
    val connectedForMs = connectedDurationMs(playerState, nowElapsedMs)
    val liveBarTransition = rememberInfiniteTransition(label = "liveBar")
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

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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
                .navigationBarsPadding()
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
                            .padding(3.dp)
                            .minimumInteractiveComponentSize(),
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                                .padding(6.dp)
                                .minimumInteractiveComponentSize(),
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.06f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .padding(6.dp)
                                .minimumInteractiveComponentSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            AndroidView(
                                factory = { castContext ->
                                    MediaRouteButton(castContext).apply {
                                        background = null
                                        CastButtonFactory.setUpMediaRouteButton(castContext, this)
                                    }
                                },
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            val shareText = buildString {
                                if (metadataTitle != null && metadataArtist != null) {
                                    append(resources.getString(R.string.player_share_with_track, metadataArtist, metadataTitle, station.name))
                                } else if (metadataTitle != null) {
                                    append(resources.getString(R.string.player_share_with_title, metadataTitle, station.name))
                                } else {
                                    append(resources.getString(R.string.player_share_station, station.name))
                                }
                                val homepage = station.homepageUrl?.takeIf { it.isNotBlank() }
                                if (homepage != null) {
                                    append("\n")
                                    append(homepage)
                                }
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_SUBJECT, station.name)
                                .putExtra(Intent.EXTRA_TEXT, shareText)
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    resources.getString(R.string.player_share),
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
                                .padding(6.dp)
                                .minimumInteractiveComponentSize(),
                        )
                    }
                }
            }

            val visibleTags = remember(station.tags) {
                station.tags.filter { it.isNotBlank() && it.length <= 20 }.take(3)
            }

            if (isLandscape) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Artwork — left column, square, fills available height
                    PlayerArtwork(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f, matchHeightConstraintsFirst = true),
                        artworkUrl = artworkUrl,
                        stationName = station.name,
                        isPlaying = isPlaying,
                        isBuffering = playerState.isBuffering,
                        artworkScale = artworkScale,
                    )

                    // Controls — right column, scrollable for very small landscape heights
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        PlayerErrorBanner(
                            error = playerState.error,
                            isWifiRequired = isWifiRequiredError,
                            onRetry = onPlayPauseClick,
                        )
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
                        Spacer(modifier = Modifier.height(6.dp))
                        if (isCasting) {
                            Text(
                                text = stringResource(R.string.player_casting_to_tv),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF52E3D9),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
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
                        if (station.codec != null || station.bitrate != null || visibleTags.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StreamQualityBadge(codec = station.codec, bitrate = station.bitrate)
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
                        Spacer(modifier = Modifier.height(10.dp))
                        LiveStatusRow(
                            status = liveStatus,
                            connectedForMs = connectedForMs,
                            isCasting = isCasting,
                            sleepTimerRemainingMs = sleepTimerRemainingMs,
                            liveDotAlpha = liveDotAlpha,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlayerIconButton(
                                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                tint = if (isFavorite) Color(0xFFFF5A7A) else Color.White.copy(alpha = 0.9f),
                                onClick = onFavoriteClick,
                                contentDescription = stringResource(
                                    if (isFavorite) R.string.player_cd_favorite_remove
                                    else R.string.player_cd_favorite_add,
                                ),
                            )
                            PlayerIconButton(
                                icon = Icons.Filled.SkipPrevious,
                                tint = Color.White.copy(alpha = 0.78f),
                                onClick = onPreviousStationClick,
                                contentDescription = stringResource(R.string.player_cd_skip_previous),
                            )
                            MainPlaybackButton(isPlaying = isPlaying, onClick = onPlayPauseClick)
                            PlayerIconButton(
                                icon = if (playerState.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                tint = if (playerState.isMuted) Color.White.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.78f),
                                onClick = onVolumeToggle,
                                contentDescription = stringResource(
                                    if (playerState.isMuted) R.string.player_cd_volume_unmute
                                    else R.string.player_cd_volume_mute,
                                ),
                            )
                            PlayerIconButton(
                                icon = Icons.Outlined.Shuffle,
                                tint = Color.White.copy(alpha = 0.72f),
                                onClick = onRandomStationClick,
                                contentDescription = stringResource(R.string.player_cd_shuffle),
                            )
                        }
                        if (onSimilarStationClick != null && visibleSimilarStations != null) {
                            SimilarStationsSection(
                                similar = visibleSimilarStations,
                                onStationClick = onSimilarStationClick,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                // Portrait layout — scrollable for small screens / large font scale
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                Spacer(modifier = Modifier.height(20.dp))

                PlayerArtwork(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    artworkUrl = artworkUrl,
                    stationName = station.name,
                    isPlaying = isPlaying,
                    isBuffering = playerState.isBuffering,
                    artworkScale = artworkScale,
                )

                Spacer(modifier = Modifier.height(14.dp))

                PlayerErrorBanner(
                    error = playerState.error,
                    isWifiRequired = isWifiRequiredError,
                    onRetry = onPlayPauseClick,
                )

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
                if (isCasting) {
                    Text(
                        text = stringResource(R.string.player_casting_to_tv),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF52E3D9),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
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

                LiveStatusRow(
                    status = liveStatus,
                    connectedForMs = connectedForMs,
                    isCasting = isCasting,
                    sleepTimerRemainingMs = sleepTimerRemainingMs,
                    liveDotAlpha = liveDotAlpha,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerIconButton(
                        icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        tint = if (isFavorite) Color(0xFFFF5A7A) else Color.White.copy(alpha = 0.9f),
                        onClick = onFavoriteClick,
                        contentDescription = stringResource(
                            if (isFavorite) R.string.player_cd_favorite_remove
                            else R.string.player_cd_favorite_add,
                        ),
                    )
                    PlayerIconButton(
                        icon = Icons.Filled.SkipPrevious,
                        tint = Color.White.copy(alpha = 0.78f),
                        onClick = onPreviousStationClick,
                        contentDescription = stringResource(R.string.player_cd_skip_previous),
                    )
                    MainPlaybackButton(
                        isPlaying = isPlaying,
                        onClick = onPlayPauseClick,
                    )
                    PlayerIconButton(
                        icon = if (playerState.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        tint = if (playerState.isMuted) Color.White.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.78f),
                        onClick = onVolumeToggle,
                        contentDescription = stringResource(
                            if (playerState.isMuted) R.string.player_cd_volume_unmute
                            else R.string.player_cd_volume_mute,
                        ),
                    )
                    PlayerIconButton(
                        icon = Icons.Outlined.Shuffle,
                        tint = Color.White.copy(alpha = 0.72f),
                        onClick = onRandomStationClick,
                        contentDescription = stringResource(R.string.player_cd_shuffle),
                    )
                }
                if (onSimilarStationClick != null && visibleSimilarStations != null) {
                    SimilarStationsSection(
                        similar = visibleSimilarStations,
                        onStationClick = onSimilarStationClick,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                } // end portrait scroll Column
            }
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
private fun PlayerErrorBanner(error: PlayerError?, isWifiRequired: Boolean, onRetry: () -> Unit) {
    if (error == null) return
    val message = when {
        isWifiRequired -> stringResource(R.string.player_error_wifi_required_hint)
        error is PlayerError.NetworkError -> stringResource(R.string.player_error_network)
        error is PlayerError.StreamBroken -> stringResource(R.string.player_error_stream_broken)
        error is PlayerError.Unknown -> error.message
        else -> stringResource(R.string.player_error_network)
    }
    // Retry re-triggers requestAudioFocus()+startPlayback() with no guard of its own — unlike
    // the mini/fullscreen play-pause buttons, which disable while isBuffering. Without this,
    // spam-tapping Retry fires overlapping playback attempts and redundant audio-focus churn.
    // A short cooldown after each tap is enough; the player's own state (buffering/new error)
    // takes over from there.
    var retryCooldownActive by remember { mutableStateOf(false) }
    LaunchedEffect(retryCooldownActive) {
        if (retryCooldownActive) {
            kotlinx.coroutines.delay(1_500L)
            retryCooldownActive = false
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFF3B3B).copy(alpha = 0.14f))
            .border(1.dp, Color(0xFFFF3B3B).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.weight(1f),
        )
        Surface(
            onClick = {
                if (!retryCooldownActive) {
                    retryCooldownActive = true
                    onRetry()
                }
            },
            enabled = !retryCooldownActive,
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFF3B3B).copy(alpha = 0.18f),
        ) {
            Text(
                text = stringResource(R.string.player_error_retry),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFFF8A8A).copy(alpha = if (retryCooldownActive) 0.5f else 1f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun PlayerArtwork(
    modifier: Modifier,
    artworkUrl: String?,
    stationName: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    artworkScale: Float,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(DarkSurface),
    ) {
        SubcomposeAsyncImage(
            model = artworkUrl,
            contentDescription = stationName,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = if (isPlaying) artworkScale else 1f
                    scaleX = scale
                    scaleY = scale
                },
            contentScale = ContentScale.Crop,
            loading = { ArtworkFallback(stationName) },
            error = { ArtworkFallback(stationName) },
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
        if (isBuffering) {
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
}

/**
 * Live stream status in place of a position bar: a LIVE badge plus a short state text
 * ("Connected for 12:34", "Reconnecting…", "Wi-Fi only…"). A seek bar only makes sense
 * once there is real timeshift.
 */
@Composable
private fun LiveStatusRow(
    status: LiveStatus,
    connectedForMs: Long?,
    isCasting: Boolean,
    sleepTimerRemainingMs: Long?,
    liveDotAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val statusText = when (status) {
        LiveStatus.Live -> when {
            isCasting -> stringResource(R.string.player_casting_to_tv)
            connectedForMs != null -> stringResource(R.string.player_status_connected_for, formatRuntime(connectedForMs))
            else -> stringResource(R.string.player_live_stream)
        }
        LiveStatus.Connecting -> stringResource(R.string.player_status_connecting)
        LiveStatus.Reconnecting -> stringResource(R.string.player_status_reconnecting)
        LiveStatus.Paused -> stringResource(R.string.player_status_paused)
        LiveStatus.WifiRequired -> stringResource(R.string.player_status_wifi_required)
        LiveStatus.Disconnected -> stringResource(R.string.player_status_disconnected)
    }
    val statusColor = when (status) {
        LiveStatus.Live -> if (isCasting) Color(0xFF52E3D9) else Color.White.copy(alpha = 0.78f)
        LiveStatus.Connecting, LiveStatus.Reconnecting -> Color(0xFF52E3D9)
        LiveStatus.Paused -> Color.White.copy(alpha = 0.55f)
        LiveStatus.WifiRequired -> Color(0xFFFFC266)
        LiveStatus.Disconnected -> Color(0xFFFF8A8A)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            // One TalkBack stop: "LIVE, Connected for 12:34".
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveBadge(isLive = status == LiveStatus.Live, dotAlpha = liveDotAlpha)
        Spacer(modifier = Modifier.width(8.dp))
        when (status) {
            LiveStatus.Connecting, LiveStatus.Reconnecting -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = statusColor,
                    trackColor = Color.White.copy(alpha = 0.15f),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            LiveStatus.WifiRequired -> {
                Icon(
                    imageVector = Icons.Filled.WifiOff,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            else -> Unit
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (sleepTimerRemainingMs != null) {
            Text(
                text = stringResource(R.string.player_sleep_timer_active, formatRuntime(sleepTimerRemainingMs)),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.68f),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun LiveBadge(isLive: Boolean, dotAlpha: Float) {
    val red = Color(0xFFFF3B3B)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isLive) red.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
            .border(
                width = 1.dp,
                color = if (isLive) red.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isLive) red.copy(alpha = dotAlpha) else Color.White.copy(alpha = 0.35f)),
        )
        Text(
            text = stringResource(R.string.player_live),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = if (isLive) 0.92f else 0.5f),
        )
    }
}

/**
 * Compact horizontal row of stations related to the current one. Hidden while loading,
 * offline or when nothing was found; the caption says what the picks are based on.
 */
@Composable
private fun SimilarStationsSection(
    similar: SimilarStations,
    onStationClick: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (similar.stations.isEmpty()) return
    val reason = when {
        similar.matchedTags.isNotEmpty() -> stringResource(
            R.string.player_similar_based_on_tags,
            similar.matchedTags.joinToString(", "),
        )
        similar.matchedCountry != null -> stringResource(
            R.string.player_similar_based_on_country,
            similar.matchedCountry,
        )
        else -> null
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.player_similar_stations),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.semantics { heading() },
        )
        if (reason != null) {
            Text(
                text = reason,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(similar.stations, key = { it.uuid }, contentType = { "similarStation" }) { item ->
                SimilarStationTile(
                    station = item,
                    onClick = { onStationClick(item) },
                )
            }
        }
    }
}

@Composable
private fun SimilarStationTile(
    station: Station,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF294252), Color(0xFF1A2234)),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            // Initial underneath, so a missing or failed logo still leaves a readable tile.
            Text(
                text = station.name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White.copy(alpha = 0.85f),
            )
            StationLogoImage(
                imageUrl = station.faviconUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        StationNameText(
            name = station.name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 2,
            minLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
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
    contentDescription: String? = null,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(42.dp)
                .padding(8.dp)
                .minimumInteractiveComponentSize(),
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


