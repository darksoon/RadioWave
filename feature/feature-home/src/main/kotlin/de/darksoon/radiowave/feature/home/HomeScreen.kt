// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.ui.components.ErrorState
import de.darksoon.radiowave.core.ui.components.LoadingState
import de.darksoon.radiowave.core.ui.components.StationLogoImage

import de.darksoon.radiowave.core.ui.theme.CardBodyStyle
import de.darksoon.radiowave.core.ui.theme.CardCaptionStyle
import de.darksoon.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.darksoon.radiowave.core.ui.theme.DarkSurfaceVariant

import de.darksoon.radiowave.core.ui.theme.SectionTitleStyle
import de.darksoon.radiowave.core.ui.theme.RadioAccent
import de.darksoon.radiowave.feature.home.R
import kotlin.math.abs

import kotlin.random.Random

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

    HomeContent(
        uiState = uiState,
        currentStation = currentStation,
        similarStations = similarStations,
        onStationClick = onStationClick,
        onViewAllFavorites = onViewAllFavorites,
        onNavigateToBrowse = onNavigateToBrowse,
        onRetry = { viewModel.refresh() },
        modifier = modifier,
    )
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
        uiState.isLoading -> {
            LoadingState(modifier = modifier)
        }

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
                        FavoriteStationCarousel(
                            stations = favoriteStations,
                            onStationClick = onStationClick,
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
                            items(recentStations, key = { station -> station.uuid }) { station ->
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
                            items(similarStations, key = { station -> station.uuid }) { station ->
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
                            items(discoverStations, key = { station -> station.uuid }) { station ->
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

@Composable
private fun FavoriteHeroCard(
    station: Station,
    onClick: () -> Unit,
    cardWidth: Dp = 140.dp,
    cardHeight: Dp = 156.dp,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .height(cardHeight)
            .width(cardWidth),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        onClick = onClick,
    ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.06f),
                                Color.White.copy(alpha = 0.1f),
                            ),
                        ),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = (-8).dp, y = (-10).dp)
                    .blur(24.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                RadioAccent.copy(alpha = 0.1f),
                                Color.Transparent,
                            ),
                            radius = 320f,
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(12.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.18f),
                            ),
                        ),
                    ),
            )
            StationArtwork(
                imageUrl = station.faviconUrl,
                stationName = station.name,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.42f),
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.26f),
                            ),
                        ),
                        shape = RoundedCornerShape(20.dp),
                    ),
            )
            Text(
                text = station.name,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun FavoriteStationCarousel(
    stations: List<Station>,
    onStationClick: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val itemWidth = 140.dp
    val itemSpacing = 10.dp
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val sidePadding = ((maxWidth - itemWidth) / 2).coerceAtLeast(20.dp)
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(
                lazyListState = listState,
                snapPosition = SnapPosition.Center,
            ),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            items(stations, key = { station -> station.uuid }) { station ->
                val transform = rememberCarouselTransform(listState, station.uuid)
                FavoriteHeroCard(
                    station = station,
                    onClick = { onStationClick(station) },
                    cardWidth = itemWidth,
                    modifier = Modifier.graphicsLayer {
                        scaleX = transform.scale
                        scaleY = transform.scale
                        alpha = transform.alpha
                        rotationY = transform.rotationY
                    },
                )
            }
        }
    }
}

private data class CarouselTransform(
    val scale: Float,
    val alpha: Float,
    val rotationY: Float,
)

@Composable
private fun rememberCarouselTransform(
    listState: LazyListState,
    itemKey: String,
): CarouselTransform {
    val viewportCenter = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2f
    val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
        (info.key as? String) == itemKey
    }
    if (itemInfo == null) {
        return CarouselTransform(
            scale = 0.86f,
            alpha = 0.72f,
            rotationY = 0f,
        )
    }
    val itemCenter = itemInfo.offset + itemInfo.size / 2f
    val distance = (itemCenter - viewportCenter)
    val normalized = (abs(distance) / (itemInfo.size * 1.4f)).coerceIn(0f, 1f)

    val scale = lerp(start = 1.02f, stop = 0.84f, fraction = normalized)
    val alpha = lerp(start = 1f, stop = 0.62f, fraction = normalized)
    val rotation = ((distance / itemInfo.size) * 10f).coerceIn(-10f, 10f)
    return CarouselTransform(
        scale = scale,
        alpha = alpha,
        rotationY = -rotation,
    )
}

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
                Text(
                    text = station.name,
                    style = CardBodyStyle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 2,
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
                contentDescription = stationName,
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        ),
        onClick = { onNavigateToBrowse("popular") },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

