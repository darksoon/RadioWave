// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.favorites

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.darksoon.radiowave.core.model.AppSettings
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.ui.components.ErrorState
import de.darksoon.radiowave.core.ui.components.LoadingState
import de.darksoon.radiowave.core.ui.components.StationLogoImage
import de.darksoon.radiowave.core.ui.components.StreamQualityBadge
import de.darksoon.radiowave.core.ui.theme.RadioAccent
import de.darksoon.radiowave.feature.favorites.R

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)
    }
    val confirmRemove = remember(prefs) {
        prefs.getBoolean(AppSettings.KEY_CONFIRM_REMOVE_FAVORITE, false)
    }
    var pendingUnfavoriteUuid by remember { mutableStateOf<String?>(null) }
    var pendingDeleteCustomUuid by remember { mutableStateOf<String?>(null) }
    var showAddCustomDialog by remember { mutableStateOf(false) }

    when {
        uiState.isLoading -> LoadingState(modifier = modifier)
        uiState.error != null -> ErrorState(
            message = uiState.error ?: stringResource(R.string.favorites_unknown_error),
            onRetry = {},
            modifier = modifier,
        )
        uiState.stations.isEmpty() && uiState.customStations.isEmpty() -> EmptyFavorites(
            onAddCustom = { showAddCustomDialog = true },
            modifier = modifier,
        )
        else -> FavoritesContent(
            stations = uiState.stations,
            customStations = uiState.customStations,
            playingUuid = playerState.currentStation?.uuid,
            isAudioPlaying = playerState.isPlaying,
            onPlayClick = viewModel::playStation,
            onToggleFavorite = { station ->
                if (confirmRemove) {
                    pendingUnfavoriteUuid = station.uuid
                } else {
                    viewModel.toggleFavorite(station)
                }
            },
            onMoveFavoriteUp = viewModel::moveFavoriteUp,
            onMoveFavoriteToTop = viewModel::moveFavoriteToTop,
            onDeleteCustomStation = { station -> pendingDeleteCustomUuid = station.uuid },
            onAddCustomStation = { showAddCustomDialog = true },
            modifier = modifier,
        )
    }

    val stationToUnfavorite = uiState.stations.firstOrNull { it.uuid == pendingUnfavoriteUuid }
    if (stationToUnfavorite != null) {
        AlertDialog(
            onDismissRequest = { pendingUnfavoriteUuid = null },
            title = { Text(stringResource(R.string.favorites_confirm_remove_title)) },
            text = {
                Text(stringResource(R.string.favorites_confirm_remove_message, stationToUnfavorite.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleFavorite(stationToUnfavorite)
                    pendingUnfavoriteUuid = null
                }) {
                    Text(stringResource(R.string.favorites_confirm_remove_yes), color = Color(0xFFFF5A7A))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnfavoriteUuid = null }) {
                    Text(stringResource(R.string.favorites_confirm_remove_no))
                }
            },
        )
    }

    val stationToDelete = uiState.customStations.firstOrNull { it.uuid == pendingDeleteCustomUuid }
    if (stationToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteCustomUuid = null },
            title = { Text(stringResource(R.string.favorites_confirm_delete_custom_title)) },
            text = {
                Text(stringResource(R.string.favorites_confirm_delete_custom_message, stationToDelete.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomStation(stationToDelete)
                    pendingDeleteCustomUuid = null
                }) {
                    Text(stringResource(R.string.favorites_confirm_delete_custom_yes), color = Color(0xFFFF5A7A))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCustomUuid = null }) {
                    Text(stringResource(R.string.favorites_add_custom_cancel))
                }
            },
        )
    }

    if (showAddCustomDialog) {
        AddCustomStationDialog(
            onConfirm = { name, url ->
                viewModel.addCustomStation(name, url)
                showAddCustomDialog = false
            },
            onDismiss = { showAddCustomDialog = false },
        )
    }
}

@Composable
private fun FavoritesContent(
    stations: List<Station>,
    customStations: List<Station>,
    playingUuid: String?,
    isAudioPlaying: Boolean,
    onPlayClick: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    onMoveFavoriteUp: (Station) -> Unit,
    onMoveFavoriteToTop: (Station) -> Unit,
    onDeleteCustomStation: (Station) -> Unit,
    onAddCustomStation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = Adaptive(minSize = 116.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.favorites_screen_title),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                if (stations.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RadioAccent.copy(alpha = 0.14f),
                    ) {
                        Text(
                            text = stations.size.toString(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = RadioAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        items(stations, key = { it.uuid }) { station ->
            val isActive = station.uuid == playingUuid
            FavoriteStationCard(
                station = station,
                isActive = isActive,
                isAudioPlaying = isAudioPlaying,
                onPlayClick = { onPlayClick(station) },
                onToggleFavorite = { onToggleFavorite(station) },
                onMoveUpClick = { onMoveFavoriteUp(station) },
                onMoveToTopClick = { onMoveFavoriteToTop(station) },
            )
        }

        // "My Stations" section
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (stations.isNotEmpty()) 8.dp else 0.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.favorites_custom_section_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.85f),
                )
                Surface(
                    onClick = onAddCustomStation,
                    shape = RoundedCornerShape(10.dp),
                    color = RadioAccent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, RadioAccent.copy(alpha = 0.35f)),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = RadioAccent,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = stringResource(R.string.favorites_add_custom_station),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = RadioAccent,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }

        items(customStations, key = { it.uuid }) { station ->
            val isActive = station.uuid == playingUuid
            FavoriteStationCard(
                station = station,
                isActive = isActive,
                isAudioPlaying = isAudioPlaying,
                isCustom = true,
                onPlayClick = { onPlayClick(station) },
                onToggleFavorite = { onDeleteCustomStation(station) },
                onMoveUpClick = {},
                onMoveToTopClick = {},
            )
        }
    }
}

@Composable
private fun AddCustomStationDialog(
    onConfirm: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val canConfirm = name.isNotBlank() && url.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorites_add_custom_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.favorites_add_custom_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.favorites_add_custom_url_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (canConfirm) onConfirm(name, url) }, enabled = canConfirm) {
                Text(stringResource(R.string.favorites_add_custom_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.favorites_add_custom_cancel))
            }
        },
    )
}

@Composable
private fun FavoriteStationCard(
    station: Station,
    isActive: Boolean,
    isAudioPlaying: Boolean,
    onPlayClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveUpClick: () -> Unit,
    onMoveToTopClick: () -> Unit,
    isCustom: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().height(196.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = if (isActive)
            BorderStroke(1.5.dp, RadioAccent.copy(alpha = 0.65f))
        else
            BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        onClick = onPlayClick,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = if (isActive) listOf(
                                RadioAccent.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.04f),
                            ) else listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.04f),
                            ),
                        ),
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StationLogo(
                        imageUrl = station.faviconUrl,
                        stationName = station.name,
                        isActive = isActive,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (subtitle.isBlank()) 0f else 1f,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    StreamQualityBadge(
                        codec = station.codec,
                        bitrate = station.bitrate,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!isCustom) {
                        Surface(
                            onClick = onMoveToTopClick,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerticalAlignTop,
                                contentDescription = stringResource(R.string.favorites_move_to_top),
                                tint = Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxSize().padding(5.dp),
                            )
                        }
                        Surface(
                            onClick = onMoveUpClick,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.favorites_move_up),
                                tint = Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                            )
                        }
                    }

                    Surface(
                        onClick = onPlayClick,
                        shape = CircleShape,
                        color = if (isActive) RadioAccent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = if (isActive) RadioAccent.copy(alpha = 0.65f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        ),
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = if (isActive && isAudioPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = RadioAccent,
                            modifier = Modifier.fillMaxSize().padding(7.dp),
                        )
                    }

                    Surface(
                        onClick = onToggleFavorite,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            imageVector = if (isCustom) Icons.Filled.Delete else Icons.Filled.Favorite,
                            contentDescription = if (isCustom)
                                stringResource(R.string.favorites_delete_custom_station) else null,
                            tint = Color(0xFFFF5A7A),
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                        )
                    }
                }
            }

            if (isActive) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = RadioAccent.copy(alpha = 0.88f),
                ) {
                    Text(
                        text = "▶",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
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
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isActive) Modifier.border(2.dp, RadioAccent.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                else Modifier,
            )
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
                loading = { LogoFallback(stationName = stationName) },
                error = { LogoFallback(stationName = stationName) },
            )
        }
    }
}

@Composable
private fun LogoFallback(stationName: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
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
private fun EmptyFavorites(onAddCustom: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = RadioAccent.copy(alpha = 0.9f),
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.favorites_empty_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.favorites_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                onClick = onAddCustom,
                shape = RoundedCornerShape(12.dp),
                color = RadioAccent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, RadioAccent.copy(alpha = 0.35f)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = RadioAccent, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource(R.string.favorites_add_custom_station),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = RadioAccent,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}
