// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.darksoon.radiowave.core.data.repository.AppSettingsState
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.model.duplicateKeyOrUuid
import de.darksoon.radiowave.core.ui.R as CoreUiR
import de.darksoon.radiowave.core.ui.components.HomeSkeletonLoader
import de.darksoon.radiowave.core.ui.components.StationLogoImage
import de.darksoon.radiowave.core.ui.components.StationNameText
import de.darksoon.radiowave.core.ui.theme.CardBodyStyle
import de.darksoon.radiowave.core.ui.theme.CardCaptionStyle
import de.darksoon.radiowave.core.ui.theme.RadioAccent
import de.darksoon.radiowave.core.ui.theme.SectionTitleStyle
import de.darksoon.radiowave.feature.home.R
import de.darksoon.radiowave.feature.savedtracks.SavedTracksButton

/*
 * REA 95fcb33 — Startseite, komplett neu als LazyColumn mit festen Item-Keys.
 * Fundstellen (jadx handy, p000/):
 *   HomeScreen            = ls1.java#m6465h          HomeContent       = ls1.java#m6463f
 *   ContinueListeningCard = ls1.java#m6459b (+ w91 Fall 1, tr1 Fall 1)
 *   GenreTile             = ls1.java#m6462e          Genre-Reihe       = v91 Fall 1 + C0507mu Fall 7 + is1 Fall 0
 *   StationRow            = ls1.java#m6466i (+ C0144cz Fall 1, C0882wz Fall 1)
 *   RecentStationCard     = ls1.java#m6469l + C0255fz Fall 1
 *   FavoriteStationGrid   = ls1.java#m6460c + wa2 Fall 5, Tile = m6461d, "+N" = m6468k
 *   SectionTitle          = ls1.java#m6470m (+ y91 Fall 3)
 *   CustomizeHomeDialog   = ls1.java#m6464g + C0470lu Fall 1, fa1 Fall 1, C0491me Fall 3,
 *                           C0507mu Fall 8, C0882wz default, C0697rz Fall 16, C0808uz Fall 2,
 *                           C0845vz Fall 3, C0946yo Fall 2/3, ls1#m6467j
 *   Header ("brand")      = C0580ot Fall 15          Suchkarte ("search") = C0255fz Fall 6 + ke0 Fall 25
 *   Personalisieren-Zeile = C0946yo Fall 1           Genre-Titel = C0255fz Fall 7
 *   Lade-/Fehler-Items    = C0580ot Fall 16, C0993zy Fall 4/6/7
 *   StationArtwork        = ls1.java#m6471n, ArtworkFallback = ls1.java#m6458a
 * Entfallen gegenüber v1.0.19: EmptyStartCard, ErrorState als Vollbild (jetzt Item "load_error"),
 * Column+verticalScroll (jetzt LazyColumn), blur() in RecentStationCard.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStationClick: (Station) -> Unit,
    onViewAllFavorites: () -> Unit,
    onNavigateToBrowse: (String) -> Unit = {},
    // REA 95fcb33: Shell reicht den (ggf. Cast-) Player-Zustand und dessen Play/Pause durch;
    // null = eigener Zustand aus dem ViewModel.
    playerState: PlayerState? = null,
    onTogglePlayPause: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val genreState by viewModel.genreState.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val ownPlayerState by viewModel.playerState.collectAsStateWithLifecycle()
    val similarStations by viewModel.similarStations.collectAsStateWithLifecycle()
    val effectivePlayerState = playerState ?: ownPlayerState
    val currentStation = effectivePlayerState.currentStation

    LaunchedEffect(currentStation?.uuid) {
        if (currentStation != null) {
            viewModel.loadSimilarStationsFor(currentStation)
        } else {
            viewModel.clearSimilarStations()
        }
    }

    val hasData = uiState.recentStations.isNotEmpty() ||
        uiState.favoriteStations.isNotEmpty() ||
        uiState.topStations.isNotEmpty() ||
        genreState.stations.isNotEmpty()

    if (!uiState.isLoading || hasData) {
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = modifier,
        ) {
            HomeContent(
                uiState = uiState,
                genreState = genreState,
                settings = appSettings,
                onSaveHome = viewModel::updateHome,
                onSelectGenre = viewModel::selectGenre,
                onRetryGenre = viewModel::retryGenre,
                currentStation = currentStation,
                isPlaying = effectivePlayerState.isPlaying,
                isConnecting = effectivePlayerState.isLoading || effectivePlayerState.isBuffering,
                similarStations = similarStations,
                onStationClick = onStationClick,
                onViewAllFavorites = onViewAllFavorites,
                onNavigateToBrowse = onNavigateToBrowse,
                onTogglePlayPause = onTogglePlayPause ?: viewModel::togglePlayPause,
                onRetry = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    } else {
        HomeSkeletonLoader(modifier = modifier)
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    genreState: HomeGenreState,
    settings: AppSettingsState,
    onSaveHome: (genres: List<String>, showRecent: Boolean, showDiscover: Boolean) -> Unit,
    onSelectGenre: (String) -> Unit,
    onRetryGenre: () -> Unit,
    currentStation: Station?,
    isPlaying: Boolean,
    isConnecting: Boolean,
    similarStations: List<Station>,
    onStationClick: (Station) -> Unit,
    onViewAllFavorites: () -> Unit,
    onNavigateToBrowse: (String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCustomize by rememberSaveable { mutableStateOf(false) }

    // Variants of the same station (different stream URLs) collapse to one card.
    val recentStations = remember(uiState.recentStations) {
        uiState.recentStations.distinctBy { it.duplicateKeyOrUuid() }.take(12)
    }
    val continueStation = currentStation ?: recentStations.firstOrNull()
    // Discover = top stations minus favourites (recent ones may appear again — REA: v1.0.19
    // excluded recent too, the handy build only excludes favourites).
    val discoverStations = remember(uiState.topStations, uiState.favoriteStations) {
        val favoriteKeys = uiState.favoriteStations.map { it.duplicateKeyOrUuid() }.toSet()
        uiState.topStations
            .distinctBy { it.duplicateKeyOrUuid() }
            .filterNot { it.duplicateKeyOrUuid() in favoriteKeys }
            .take(12)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
    ) {
        item(key = "brand") { HomeHeader() }

        if (continueStation != null) {
            item(key = "continue") {
                val isActive = currentStation?.uuid == continueStation.uuid
                ContinueListeningCard(
                    station = continueStation,
                    isPlaying = isPlaying && isActive,
                    isConnecting = isConnecting && isActive,
                    onClick = {
                        if (currentStation?.uuid != continueStation.uuid) {
                            onStationClick(continueStation)
                        } else {
                            onTogglePlayPause()
                        }
                    },
                )
            }
        }

        item(key = "search") {
            HomeSearchCard(onClick = { onNavigateToBrowse("") })
        }

        item(key = "personalize") {
            SectionTitle(
                title = stringResource(R.string.home_your_genres),
                actionLabel = stringResource(R.string.home_customize),
                onActionClick = { showCustomize = true },
            )
        }

        item(key = "genres") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(settings.homeGenres, key = { genre -> genre }) { genre ->
                    GenreTile(
                        genre = genre,
                        isSelected = genreState.selectedGenre == genre,
                        onClick = { onSelectGenre(genre) },
                    )
                }
            }
        }

        item(key = "genre_title") {
            SectionTitle(
                title = stringResource(R.string.home_genre_stations, genreState.selectedGenre.capitalizeGenre()),
            )
        }

        if (genreState.stations.isNotEmpty()) {
            item(key = "genre_stations") {
                StationRow(stations = genreState.stations, onStationClick = onStationClick)
            }
        }

        if (genreState.isLoading && genreState.stations.isEmpty()) {
            item(key = "loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
        } else if (genreState.hasError || genreState.stations.isEmpty()) {
            item(key = "genre_error") {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = stringResource(
                            if (genreState.hasError) R.string.home_genre_error else R.string.home_genre_empty,
                        ),
                    )
                    TextButton(onClick = onRetryGenre) {
                        Text(stringResource(R.string.home_genre_retry))
                    }
                }
            }
        }

        if (uiState.favoriteStations.isNotEmpty()) {
            item(key = "favorites_title") {
                SectionTitle(
                    title = stringResource(R.string.home_section_favorites),
                    actionLabel = stringResource(R.string.home_section_all),
                    onActionClick = onViewAllFavorites,
                )
            }
            item(key = "favorites") {
                FavoriteStationGrid(
                    stations = uiState.favoriteStations,
                    currentStationUuid = currentStation?.uuid,
                    onStationClick = onStationClick,
                    onViewAllFavorites = onViewAllFavorites,
                )
            }
        }

        if (settings.homeShowRecent && recentStations.isNotEmpty()) {
            item(key = "recent_title") {
                SectionTitle(title = stringResource(R.string.home_section_recent))
            }
            item(key = "recent") {
                StationRow(stations = recentStations, onStationClick = onStationClick)
            }
        }

        if (settings.homeShowDiscover) {
            val showSimilar = currentStation != null && similarStations.isNotEmpty()
            val stations = if (showSimilar) similarStations else discoverStations
            if (stations.isNotEmpty()) {
                item(key = "discover_title") {
                    SectionTitle(
                        title = if (showSimilar) {
                            stringResource(R.string.home_section_similar_because, currentStation!!.name)
                        } else {
                            stringResource(R.string.home_section_discover)
                        },
                    )
                }
                item(key = "discover") {
                    StationRow(stations = stations, onStationClick = onStationClick)
                }
            }
        }

        if (uiState.error != null) {
            item(key = "load_error") {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onRetry) {
                        // REA: Handy-Build nutzt hier ebenfalls home_genre_retry (C0580ot Fall 19).
                        Text(stringResource(R.string.home_genre_retry))
                    }
                }
            }
        }
    }

    if (showCustomize) {
        CustomizeHomeDialog(
            settings = settings,
            onDismiss = { showCustomize = false },
            onSave = { genres, showRecent, showDiscover ->
                onSaveHome(genres, showRecent, showDiscover)
                showCustomize = false
            },
        )
    }
}

/** Wordmark (light/dark variant) plus the saved-songs shortcut. */
@Composable
private fun HomeHeader() {
    val useLightWordmark = MaterialTheme.colorScheme.background.luminance() > 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(
                if (useLightWordmark) CoreUiR.drawable.radiowave_wordmark_light_v2 else CoreUiR.drawable.radiowave_wordmark_v2,
            ),
            contentDescription = "RadioWave",
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            contentScale = ContentScale.Fit,
        )
        SavedTracksButton(iconOnly = true)
    }
}

/**
 * Entry to the station/genre search. Home has no inline search field — the card opens
 * Browse with an empty query (REA 95fcb33: p000/wr1.java Fall 0 → onNavigateToBrowse("")).
 */
@Composable
private fun HomeSearchCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.home_search_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * "Continue listening" card: the playing station, or the most recent one when nothing plays.
 * Tapping the playing station toggles play/pause instead of restarting it.
 */
@Composable
private fun ContinueListeningCard(
    station: Station,
    isPlaying: Boolean,
    isConnecting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, RadioAccent.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationArtwork(
                imageUrl = station.faviconUrl,
                stationName = station.name,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(64.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = stringResource(
                        when {
                            isConnecting -> R.string.home_continue_connecting
                            isPlaying -> R.string.home_continue_playing
                            else -> R.string.home_continue_listening
                        },
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(modifier = Modifier.height(2.dp))
                StationNameText(
                    name = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                val details = listOfNotNull(
                    station.country?.takeUnless { it.isBlank() },
                    station.codec?.takeUnless { it.isBlank() }?.uppercase(),
                    station.bitrate?.takeIf { it > 0 }?.let { "$it kbps" },
                ).joinToString(" · ")
                if (details.isNotBlank()) {
                    Text(
                        text = details,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val showPause = isPlaying || isConnecting
                    Icon(
                        imageVector = if (showPause) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (showPause) R.string.home_continue_pause else R.string.home_continue_play,
                        ),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@DrawableRes
private fun genreImage(genre: String): Int = when (genre) {
    "techno" -> CoreUiR.drawable.genre_techno_v1
    "jazz" -> CoreUiR.drawable.genre_jazz_v1
    "rock" -> CoreUiR.drawable.genre_rock_v1
    else -> CoreUiR.drawable.genre_chillout_v1
}

private fun String.capitalizeGenre(): String = replaceFirstChar { it.titlecase() }

@Composable
private fun GenreTile(
    genre: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(148.dp)
            .height(106.dp)
            .semantics { selected = isSelected },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(genreImage(genre)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        ),
                    ),
            )
            Text(
                text = genre.capitalizeGenre(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/** Horizontal row of station cards (genre stations, recently played, discover/similar). */
@Composable
private fun StationRow(
    stations: List<Station>,
    onStationClick: (Station) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(stations, key = { station -> station.uuid }, contentType = { "station" }) { station ->
            RecentStationCard(
                station = station,
                onClick = { onStationClick(station) },
            )
        }
    }
}

@Composable
private fun CustomizeHomeDialog(
    settings: AppSettingsState,
    onDismiss: () -> Unit,
    onSave: (genres: List<String>, showRecent: Boolean, showDiscover: Boolean) -> Unit,
) {
    // Dialog-local draft; nothing is written until "Save".
    var selectedGenres by remember { mutableStateOf(settings.homeGenres) }
    var showRecent by remember { mutableStateOf(settings.homeShowRecent) }
    var showDiscover by remember { mutableStateOf(settings.homeShowDiscover) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(selectedGenres, showRecent, showDiscover) }) {
                Text(stringResource(R.string.home_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.home_cancel))
            }
        },
        title = { Text(stringResource(R.string.home_customize)) },
        text = {
            LazyColumn {
                item {
                    Text(
                        text = stringResource(R.string.home_customize_help),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                // Selected genres in the user's order, then the unselected ones.
                val ordered = selectedGenres +
                    HomeGenreController.SUPPORTED_GENRES.filterNot { it in selectedGenres }
                items(ordered, key = { genre -> genre }) { genre ->
                    val isChecked = genre in selectedGenres
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedGenres = if (checked) selectedGenres + genre else selectedGenres - genre
                            },
                            // At least one genre must stay selected.
                            enabled = !isChecked || selectedGenres.size > 1,
                        )
                        Text(
                            text = genre.capitalizeGenre(),
                            modifier = Modifier.weight(1f),
                        )
                        if (selectedGenres.indexOf(genre) > 0) {
                            IconButton(
                                onClick = {
                                    val reordered = selectedGenres.toMutableList()
                                    val index = reordered.indexOf(genre)
                                    java.util.Collections.swap(reordered, index, index - 1)
                                    selectedGenres = reordered
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowUpward,
                                    contentDescription = stringResource(R.string.home_move_up, genre),
                                )
                            }
                        }
                    }
                }
                item {
                    CustomizeToggleRow(
                        label = stringResource(R.string.home_section_recent),
                        checked = showRecent,
                        onCheckedChange = { showRecent = it },
                    )
                }
                item {
                    CustomizeToggleRow(
                        label = stringResource(R.string.home_section_discover),
                        checked = showDiscover,
                        onCheckedChange = { showDiscover = it },
                    )
                }
            }
        },
    )
}

@Composable
private fun CustomizeToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = label,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
    }
}

/**
 * Compact favorites grid: small logo tiles in up to [FAVORITE_GRID_MAX_ROWS] rows, so a
 * phone shows eight favorites at once without horizontal swiping. When there are more
 * favorites than fit, the last cell becomes a "+N" tile that opens the full list.
 * REA 95fcb33: unverändert gegenüber v1.0.19 (wa2 Fall 5).
 */
@Composable
private fun FavoriteStationGrid(
    stations: List<Station>,
    currentStationUuid: String?,
    onStationClick: (Station) -> Unit,
    onViewAllFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        val columns = ((maxWidth + FavoriteGridSpacing) / (FavoriteTileMinWidth + FavoriteGridSpacing))
            .toInt()
            .coerceIn(3, 6)
        val capacity = columns * FAVORITE_GRID_MAX_ROWS
        val hiddenCount = if (stations.size > capacity) stations.size - (capacity - 1) else 0
        // A null cell stands for the trailing "+N" tile.
        val rows = remember(stations, columns, hiddenCount) {
            val cells: List<Station?> = if (hiddenCount > 0) {
                stations.take(capacity - 1) + null
            } else {
                stations
            }
            cells.chunked(columns)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { rowCells ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FavoriteGridSpacing),
                ) {
                    rowCells.forEach { station ->
                        if (station != null) {
                            FavoriteStationTile(
                                station = station,
                                isActive = station.uuid == currentStationUuid,
                                onClick = { onStationClick(station) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            MoreFavoritesTile(
                                hiddenCount = hiddenCount,
                                totalCount = stations.size,
                                onClick = onViewAllFavorites,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    // Keep tile widths identical in a partially filled last row.
                    repeat(columns - rowCells.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteStationTile(
    station: Station,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nowPlayingDescription = stringResource(R.string.home_favorite_now_playing)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            // clickable merges the tile's semantics: TalkBack reads the name once as a button.
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                if (isActive) stateDescription = nowPlayingDescription
            }
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = if (isActive) 1.5.dp else 1.dp,
                    color = if (isActive) RadioAccent.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(16.dp),
                ),
        ) {
            StationArtwork(
                imageUrl = station.faviconUrl,
                stationName = station.name,
                contentDescription = null,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxSize(),
            )
            if (isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(8.dp)
                        .background(RadioAccent, CircleShape)
                        .border(1.dp, Color.Black.copy(alpha = 0.35f), CircleShape),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        StationNameText(
            name = station.name,
            style = CardBodyStyle.copy(fontSize = 12.sp, lineHeight = 15.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            minLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun MoreFavoritesTile(
    hiddenCount: Int,
    totalCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = pluralStringResource(R.plurals.home_favorites_show_all, totalCount, totalCount)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            RadioAccent.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.05f),
                        ),
                    ),
                )
                .border(1.dp, RadioAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+$hiddenCount",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.home_section_all),
            style = CardBodyStyle.copy(fontSize = 12.sp, lineHeight = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val FavoriteTileMinWidth = 72.dp
private val FavoriteGridSpacing = 10.dp
private const val FAVORITE_GRID_MAX_ROWS = 2

@Composable
private fun SectionTitle(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = SectionTitleStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (actionLabel != null && onActionClick != null) {
            Surface(
                onClick = onActionClick,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            ) {
                // REA 95fcb33: p000/y91.java Fall 3 — identisch zu v1.0.19.
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentStationCard(
    station: Station,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(94.dp)
            .height(132.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        ) {
            // REA 95fcb33: der blur(16.dp) aus v1.0.19 ist entfallen, nur noch der Verlauf.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent,
                            ),
                            radius = 180f,
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            shape = CircleShape,
                        )
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    StationArtwork(
                        imageUrl = station.faviconUrl,
                        stationName = station.name,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                StationNameText(
                    name = station.name,
                    style = CardBodyStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                station.country?.let { country ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = country,
                        style = CardCaptionStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StationArtwork(
    imageUrl: String?,
    stationName: String,
    shape: Shape,
    contentDescription: String? = stationName,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackColors: List<Color> = defaultFallbackColors,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) {
            ArtworkFallback(
                stationName = stationName,
                colors = fallbackColors,
            )
        } else {
            StationLogoImage(
                imageUrl = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                loading = {
                    ArtworkFallback(
                        stationName = stationName,
                        colors = fallbackColors,
                    )
                },
                error = {
                    ArtworkFallback(
                        stationName = stationName,
                        colors = fallbackColors,
                    )
                },
            )
        }
    }
}

@Composable
private fun ArtworkFallback(
    stationName: String,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = colors,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stationName.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val defaultFallbackColors = listOf(
    RadioAccent.copy(alpha = 0.4f),
    Color(0xFF1A1F2B),
)
