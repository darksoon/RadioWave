// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.ui.components.ErrorState
import de.darksoon.radiowave.core.ui.components.HomeSkeletonLoader
import de.darksoon.radiowave.core.ui.components.StationLogoImage
import de.darksoon.radiowave.core.ui.components.StationNameText

import de.darksoon.radiowave.core.ui.theme.CardBodyStyle
import de.darksoon.radiowave.core.ui.theme.CardCaptionStyle

import de.darksoon.radiowave.core.ui.theme.SectionTitleStyle
import de.darksoon.radiowave.core.ui.theme.RadioAccent
import de.darksoon.radiowave.feature.home.R

import kotlin.random.Random

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStationClick: (Station) -> Unit,
    onViewAllFavorites: () -> Unit,
    onNavigateToBrowse: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val similarStations by viewModel.similarStations.collectAsStateWithLifecycle()
    val currentStation = playerState.currentStation

    LaunchedEffect(currentStation?.uuid) {
        if (currentStation != null) {
            viewModel.loadSimilarStationsFor(currentStation)
        } else {
            viewModel.clearSimilarStations()
        }
    }

    val hasData = uiState.recentStations.isNotEmpty() ||
        uiState.favoriteStations.isNotEmpty() ||
        uiState.topStations.isNotEmpty()
    val isInitialLoad = uiState.isLoading && !hasData

    if (isInitialLoad) {
        HomeSkeletonLoader(modifier = modifier)
    } else {
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = modifier,
        ) {
            HomeContent(
                uiState = uiState,
                currentStation = currentStation,
                similarStations = similarStations,
                onStationClick = onStationClick,
                onViewAllFavorites = onViewAllFavorites,
                onNavigateToBrowse = onNavigateToBrowse,
                onRetry = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    currentStation: Station?,
    similarStations: List<Station>,
    onStationClick: (Station) -> Unit,
    onViewAllFavorites: () -> Unit,
    onNavigateToBrowse: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.error != null -> {
            ErrorState(
                message = uiState.error,
                onRetry = onRetry,
                modifier = modifier,
            )
        }

        else -> {
            val recentStations = uiState.recentStations
            val favoriteStations = uiState.favoriteStations
            val excludedSuggestionIds = remember(favoriteStations, recentStations) {
                (favoriteStations.map { station -> station.uuid } + recentStations.map { station -> station.uuid }).toSet()
            }
            val suggestionPool = remember(uiState.topStations, recentStations, favoriteStations, excludedSuggestionIds) {
                (uiState.topStations + recentStations + favoriteStations)
                    .distinctBy { station -> station.uuid }
                    .filterNot { station -> station.uuid in excludedSuggestionIds }
            }
            val discoverPool = remember(uiState.topStations) {
                uiState.topStations.distinctBy { station -> station.uuid }
            }
            val discoverStations = remember(discoverPool, suggestionPool) {
                val pool = if (suggestionPool.isNotEmpty()) suggestionPool else discoverPool
                val seed = pool.fold(0L) { acc, s -> acc * 31 + s.uuid.hashCode() }
                pool.shuffled(Random(seed)).take(10)
            }

            Box(
                modifier = modifier
                    .fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp, bottom = 88.dp),
                ) {
                    if (favoriteStations.isNotEmpty()) {
                        SectionTitle(
                            title = stringResource(R.string.home_section_favorites),
                            actionLabel = stringResource(R.string.home_section_all),
                            onActionClick = onViewAllFavorites,
                        )
                        FavoriteStationGrid(
                            stations = favoriteStations,
                            currentStationUuid = currentStation?.uuid,
                            onStationClick = onStationClick,
                            onViewAllFavorites = onViewAllFavorites,
                        )
                    }

                    if (recentStations.isNotEmpty()) {
                        SectionTitle(
                            title = stringResource(R.string.home_section_recent),
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(recentStations, key = { station -> station.uuid }, contentType = { "station" }) { station ->
                                RecentStationCard(
                                    station = station,
                                    onClick = { onStationClick(station) },
                                )
                            }
                        }
                    }

                    if (currentStation != null && similarStations.isNotEmpty()) {
                        SectionTitle(
                            title = stringResource(R.string.home_section_similar_because, currentStation.name),
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(similarStations, key = { station -> station.uuid }, contentType = { "station" }) { station ->
                                RecentStationCard(
                                    station = station,
                                    onClick = { onStationClick(station) },
                                )
                            }
                        }
                    } else if (discoverStations.isNotEmpty()) {
                        SectionTitle(
                            title = stringResource(R.string.home_section_discover),
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(discoverStations, key = { station -> station.uuid }, contentType = { "station" }) { station ->
                                RecentStationCard(
                                    station = station,
                                    onClick = { onStationClick(station) },
                                )
                            }
                        }
                    }

                    if (
                        recentStations.isEmpty() &&
                        favoriteStations.isEmpty() &&
                        discoverStations.isEmpty() &&
                        similarStations.isEmpty()
                    ) {
                        Spacer(modifier = Modifier.height(18.dp))
                        EmptyStartCard(
                            onNavigateToBrowse = onNavigateToBrowse,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact favorites grid: small logo tiles in up to [FAVORITE_GRID_MAX_ROWS] rows, so a
 * phone shows eight favorites at once without horizontal swiping. When there are more
 * favorites than fit, the last cell becomes a "+N" tile that opens the full list.
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
                color = RadioAccent,
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
            style = SectionTitleStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (actionLabel != null && onActionClick != null) {
            Surface(
                onClick = onActionClick,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            ) {
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
        modifier = modifier
            .width(94.dp)
            .height(132.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
        ),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(16.dp)
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
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = RadioAccent.copy(alpha = 0.7f),
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

@Composable
private fun EmptyStartCard(
    onNavigateToBrowse: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        RadioAccent.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.04f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = RadioAccent.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp),
            )
            // Pass empty string so Browse opens to top stations instead of an
            // unhelpful filter on the literal word "popular".
            .clickable { onNavigateToBrowse("") }
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(RadioAccent.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = RadioAccent,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_empty_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.home_empty_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(RadioAccent.copy(alpha = 0.18f))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_empty_cta),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = RadioAccent,
                    )
                }
            }
        }
    }
}

