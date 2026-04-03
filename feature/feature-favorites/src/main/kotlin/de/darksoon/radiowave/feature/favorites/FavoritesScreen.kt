// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells.Adaptive
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.ui.components.ErrorState
import de.darksoon.radiowave.core.ui.components.LoadingState
import de.darksoon.radiowave.core.ui.components.StationLogoImage
import de.darksoon.radiowave.core.ui.theme.TealAccent
import de.darksoon.radiowave.feature.favorites.R

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        uiState.isLoading -> {
            LoadingState(modifier = modifier)
        }

        uiState.error != null -> {
            ErrorState(
                message = uiState.error ?: stringResource(R.string.favorites_unknown_error),
                onRetry = {},
                modifier = modifier,
            )
        }

        uiState.stations.isEmpty() -> {
            EmptyFavorites(
                modifier = modifier,
            )
        }

        else -> {
            FavoritesContent(
                stations = uiState.stations,
                onPlayClick = viewModel::playStation,
                onToggleFavorite = viewModel::toggleFavorite,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun FavoritesContent(
    stations: List<Station>,
    onPlayClick: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = Adaptive(minSize = 116.dp),
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(stations, key = { station -> station.uuid }) { station ->
            FavoriteStationCard(
                station = station,
                onPlayClick = { onPlayClick(station) },
                onToggleFavorite = { onToggleFavorite(station) },
            )
        }
    }
}

@Composable
private fun FavoriteStationCard(
    station: Station,
    onPlayClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(196.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        onClick = onPlayClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StationLogo(
                    imageUrl = station.faviconUrl,
                    stationName = station.name,
                    modifier = Modifier.size(64.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = station.name,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                val subtitle = listOfNotNull(
                    station.country?.takeIf { it.isNotBlank() },
                    station.language?.takeIf { it.isNotBlank() },
                ).joinToString(separator = " • ")

                Text(
                    text = subtitle.ifBlank { " " },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (subtitle.isBlank()) 0f else 1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onPlayClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = TealAccent,
                        modifier = Modifier
                            .size(30.dp)
                            .padding(5.dp),
                    )
                }
                Surface(
                    onClick = onToggleFavorite,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF5A7A),
                        modifier = Modifier
                            .size(30.dp)
                            .padding(6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StationLogo(
    imageUrl: String?,
    stationName: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) {
            LogoFallback(stationName = stationName)
        } else {
            StationLogoImage(
                imageUrl = imageUrl,
                contentDescription = stationName,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    LogoFallback(stationName = stationName)
                },
                error = {
                    LogoFallback(stationName = stationName)
                },
            )
        }
    }
}

@Composable
private fun LogoFallback(
    stationName: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stationName.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun EmptyFavorites(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = TealAccent.copy(alpha = 0.9f),
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.favorites_empty_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.favorites_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

