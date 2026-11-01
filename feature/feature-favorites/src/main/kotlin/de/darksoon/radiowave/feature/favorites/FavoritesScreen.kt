// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.favorites

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.darksoon.radiowave.core.data.policy.StationInputValidator
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.ui.components.ErrorState
import de.darksoon.radiowave.core.ui.components.FavoritesSkeletonLoader
import de.darksoon.radiowave.core.ui.components.LoadingState
import de.darksoon.radiowave.core.ui.components.RadioWaveScreenHeader
import de.darksoon.radiowave.core.ui.components.StationLogoImage
import de.darksoon.radiowave.core.ui.components.StationNameText
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
    val confirmRemove by viewModel.confirmRemove.collectAsStateWithLifecycle()
    var pendingUnfavoriteUuid by remember { mutableStateOf<String?>(null) }
    var pendingDeleteCustomUuid by remember { mutableStateOf<String?>(null) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    // Landscape is where car head units live: bigger targets, and removing a favorite always
    // asks first — a mis-tap while driving would otherwise drop it silently.
    val carLayout = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    when {
        uiState.isLoading -> FavoritesSkeletonLoader(modifier = modifier)
        uiState.error != null -> ErrorState(
            message = uiState.error ?: stringResource(R.string.favorites_unknown_error),
            onRetry = { viewModel.retry() },
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
                if (confirmRemove || carLayout) {
                    pendingUnfavoriteUuid = station.uuid
                } else {
                    viewModel.toggleFavorite(station)
                }
            },
            onMoveFavoriteUp = viewModel::moveFavoriteUp,
            onMoveFavoriteToTop = viewModel::moveFavoriteToTop,
            onDeleteCustomStation = { station -> pendingDeleteCustomUuid = station.uuid },
            onAddCustomStation = { showAddCustomDialog = true },
            carLayout = carLayout,
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
    carLayout: Boolean = false,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        // Fewer, larger cards in landscape so each target is easy to hit while driving.
        columns = Adaptive(minSize = if (carLayout) 210.dp else 148.dp),
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
                // REA 95fcb33 (C0255fz Fall 5): Titel jetzt über den Marken-Header aus core-ui
                // (Header-Art + Wortmarke + Titel) statt headlineLarge-Text in Weiß.
                RadioWaveScreenHeader(
                    title = stringResource(R.string.favorites_screen_title),
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
                            // REA 95fcb33 (s91 Fall 0): Textfarbe colorScheme.primary statt RadioAccent.
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        // REA 95fcb33 (a91 Fall 3): Item-Key mit Präfix, damit Favorit und eigener Sender mit
        // gleicher UUID nicht kollidieren.
        items(stations, key = { "favorite:${it.uuid}" }) { station ->
            val isActive = station.uuid == playingUuid
            FavoriteStationCard(
                large = carLayout,
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
                            // REA 95fcb33 (ke0 Fall 15): Text primary, Icon bleibt RadioAccent.
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }

        // REA 95fcb33 (a91 Fall 5)
        items(customStations, key = { "custom:${it.uuid}" }) { station ->
            val isActive = station.uuid == playingUuid
            FavoriteStationCard(
                large = carLayout,
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
    val urlValid = remember(url) {
        // don't show error until user types something
        url.isBlank() || StationInputValidator.isValidStreamUrl(url)
    }
    val blocked = remember(name, url) {
        name.isNotBlank() && url.isNotBlank() && urlValid &&
            StationInputValidator.validate(Station(uuid = "preview", name = name, streamUrl = url)) ==
            StationInputValidator.Result.Blocked
    }
    val canConfirm = name.isNotBlank() && url.isNotBlank() && urlValid && !blocked

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorites_add_custom_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(StationInputValidator.MAX_NAME_LENGTH) },
                    label = { Text(stringResource(R.string.favorites_add_custom_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it.take(StationInputValidator.MAX_URL_LENGTH) },
                    label = { Text(stringResource(R.string.favorites_add_custom_url_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = !urlValid || blocked,
                    supportingText = when {
                        !urlValid -> {
                            { Text(stringResource(R.string.favorites_add_custom_url_invalid)) }
                        }
                        blocked -> {
                            { Text(stringResource(R.string.favorites_add_custom_blocked)) }
                        }
                        else -> null
                    },
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
    large: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val cardHeight = if (large) 252.dp else 196.dp
    val logoSize = if (large) 84.dp else 64.dp
    val reorderButtonSize = if (large) 40.dp else 26.dp
    val playButtonSize = if (large) 52.dp else 34.dp
    val heartButtonSize = if (large) 44.dp else 30.dp
    val actionSpacing = if (large) 12.dp else 6.dp
    // REA 95fcb33 (za0.m11522g): „Nach oben“/„Ganz nach oben“ stecken jetzt in einem
    // „Mehr Aktionen“-Menü statt in zwei eigenen Kreisknöpfen.
    var showActions by remember { mutableStateOf(false) }
    Card(
        // Fixed height so all cards in the grid line up cleanly. Content inside
        // (logo + name + subtitle + actions) is sized to fit comfortably at 196dp;
        // longer names get ellipsised, tags are limited to one row.
        modifier = modifier.fillMaxWidth().height(cardHeight),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = if (isActive)
            BorderStroke(1.5.dp, RadioAccent.copy(alpha = 0.65f))
        else
            // REA 95fcb33: outline α0.5 statt White α0.14 (Hell-Theme-tauglich).
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        onClick = onPlayClick,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            // REA 95fcb33 (t91): Verlauf aus surfaceVariant statt Weiß-Transparenzen.
                            colors = if (isActive) listOf(
                                RadioAccent.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            ) else listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
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
                        modifier = Modifier.size(logoSize),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    StationNameText(
                        name = station.name,
                        style = (if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge)
                            .copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
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
                    horizontalArrangement = Arrangement.spacedBy(actionSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!isCustom) {
                        // REA 95fcb33 (t91, v91 Fall 0, ke0 Fälle 0/21–24, C0454le 13/14, ba1 0/1)
                        Box {
                            Surface(
                                onClick = { showActions = true },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                modifier = Modifier.size(reorderButtonSize).minimumInteractiveComponentSize(),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.favorites_more_actions),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxSize().padding(3.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = showActions,
                                onDismissRequest = { showActions = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.favorites_move_to_top)) },
                                    onClick = {
                                        showActions = false
                                        onMoveToTopClick()
                                    },
                                    leadingIcon = { Icon(Icons.Filled.VerticalAlignTop, contentDescription = null) },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.favorites_move_up)) },
                                    onClick = {
                                        showActions = false
                                        onMoveUpClick()
                                    },
                                    // REA-UNSICHER: Icon über em2.m2967A() (Material-Icon-Getter) – vermutlich
                                    // KeyboardArrowUp wie im bisherigen Knopf; Pfad nicht geprüft.
                                    leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null) },
                                )
                            }
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
                        modifier = Modifier.size(playButtonSize).minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = if (isActive && isAudioPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(
                                if (isActive && isAudioPlaying) R.string.favorites_cd_pause
                                else R.string.favorites_cd_play,
                            ),
                            tint = RadioAccent,
                            modifier = Modifier.fillMaxSize().padding(7.dp),
                        )
                    }

                    Surface(
                        onClick = onToggleFavorite,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.size(heartButtonSize).minimumInteractiveComponentSize(),
                    ) {
                        Icon(
                            imageVector = if (isCustom) Icons.Filled.Delete else Icons.Filled.Favorite,
                            contentDescription = if (isCustom)
                                stringResource(R.string.favorites_delete_custom_station) else null,
                            // REA 95fcb33 (x91 Fall 0): Tint colorScheme.primary statt 0xFFFF5A7A.
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                        )
                    }
                }
            }

            if (isActive) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    // REA 95fcb33: primary α0.88 statt RadioAccent α0.88.
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
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
            // Icon mit Glow-Effekt
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                RadioAccent.copy(alpha = 0.22f),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(RadioAccent.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = RadioAccent,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.favorites_empty_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.favorites_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.55f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                onClick = onAddCustom,
                shape = RoundedCornerShape(14.dp),
                color = RadioAccent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, RadioAccent.copy(alpha = 0.40f)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.favorites_cd_add),
                        tint = RadioAccent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.favorites_add_custom_station),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        // REA 95fcb33 (ke0 Fall 12): Text primary, Icon bleibt RadioAccent.
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
