// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.darksoon.radiowave.core.model.PlayerError
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.ui.components.StationLogoImage
import de.darksoon.radiowave.core.ui.components.StationNameText
import de.darksoon.radiowave.core.ui.theme.DarkBackground
import de.darksoon.radiowave.core.ui.theme.DarkBorder
import de.darksoon.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.darksoon.radiowave.core.ui.theme.DarkSurface
import de.darksoon.radiowave.core.ui.theme.DarkSurfaceVariant
import de.darksoon.radiowave.core.ui.theme.RadioAccent
import de.darksoon.radiowave.feature.home.HomeViewModel
import de.darksoon.radiowave.feature.player.AudioOutputButton

// REA 95fcb33: komplett neue Datei ("Auto-Modus" in der App, nicht Android Auto).
// Fundstellen:
//  - CarModeScreen  = p000/C0299h5.java m4110e (971-1057), Inhalt p000/d50.java Fall 0
//  - Raster-Items   = p000/a50.java Fall 0 → p000/h50.java → Klick p000/e50.java
//  - CarStationTile = p000/C0299h5.java m4112f (1069-1081), Inhalt p000/b50.java Fall 0
//  - Play-Knopf     = p000/x91.java Fall 2, Schließen-Icon/Chip-Texte p000/ke0.java Fälle 8-10
//  - Display an     = p000/C0958z.java Fall 6 + p000/f50.java (View.keepScreenOn)
//  - Einbindung     = p000/yi2.java (Route "car"), siehe MainActivity.kt
// Ablage im app-Modul: die car_*-Strings liegen dort, und der Bildschirm braucht sowohl
// HomeViewModel (feature-home) als auch AudioOutputButton (feature-player).
// REA-UNSICHER: Datei-, Funktions- und Parameternamen geraten; Reihenfolge der Parameter laut
// Dekompilat (HomeViewModel, PlayerState, onPlayPause, onStationClick, onClose, modifier).
// REA-UNSICHER: Die Chips sind im Dekompilat p000/x80.m10834b(selected, onClick, label, …) —
// FilterChip angenommen.

private const val CAR_MODE_MAX_STATIONS = 6

/**
 * Dashboard-friendly full-screen mode: large targets, high contrast, the screen stays on.
 * Shows up to six favourites or recently played stations plus a now-playing bar.
 * Opened from Settings ("Car & devices"); the host hides navigation and the mini player.
 */
@Composable
fun CarModeScreen(
    homeViewModel: HomeViewModel,
    playerState: PlayerState,
    onPlayPauseClick: () -> Unit,
    onStationClick: (Station) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val currentStation = playerState.currentStation
    val isActive = playerState.isPlaying || playerState.isBuffering || playerState.isLoading
    val error = playerState.error
    val statusRes = when {
        error is PlayerError.MobileDataBlocked -> R.string.car_mode_wifi_required
        error != null -> R.string.car_mode_error
        playerState.isBuffering || playerState.isLoading -> R.string.car_mode_connecting
        playerState.isPlaying -> R.string.car_mode_now_playing
        else -> R.string.car_mode_paused
    }
    var showRecent by rememberSaveable { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isWide = configuration.screenWidthDp > configuration.screenHeightDp
    val stations = remember(uiState.favoriteStations, uiState.recentStations, showRecent) {
        (if (showRecent) uiState.recentStations else uiState.favoriteStations)
            .distinctBy { it.uuid }
            .take(CAR_MODE_MAX_STATIONS)
    }

    // "Screen stays on" — restored to the previous value when car mode closes.
    val view = LocalView.current
    DisposableEffect(view) {
        val previous = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground,
    ) {
        CarModeContent(
            stations = stations,
            isWide = isWide,
            currentStation = currentStation,
            isActive = isActive,
            statusRes = statusRes,
            showRecent = showRecent,
            onShowRecentChange = { showRecent = it },
            onPlayPauseClick = onPlayPauseClick,
            onStationClick = onStationClick,
            onClose = onClose,
        )
    }
}

@Composable
private fun CarModeContent(
    stations: List<Station>,
    isWide: Boolean,
    currentStation: Station?,
    isActive: Boolean,
    statusRes: Int,
    showRecent: Boolean,
    onShowRecentChange: (Boolean) -> Unit,
    onPlayPauseClick: () -> Unit,
    onStationClick: (Station) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.car_mode_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                Text(
                    text = stringResource(R.string.car_mode_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant,
                )
            }
            AudioOutputButton()
            Surface(
                onClick = onClose,
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = DarkSurface,
                border = BorderStroke(1.dp, DarkBorder.copy(alpha = 0.8f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.car_mode_close),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = !showRecent,
                onClick = { onShowRecentChange(false) },
                label = { Text(stringResource(R.string.car_favorites)) },
            )
            FilterChip(
                selected = showRecent,
                onClick = { onShowRecentChange(true) },
                label = { Text(stringResource(R.string.car_recent)) },
            )
        }

        if (stations.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isWide) 2 else 1),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(stations, key = { it.uuid }) { station ->
                    val isCurrent = currentStation?.uuid == station.uuid
                    CarStationTile(
                        station = station,
                        isCurrent = isCurrent,
                        isActive = isActive,
                        onClick = {
                            // Tapping the running station toggles it instead of restarting.
                            if (currentStation?.uuid == station.uuid) onPlayPauseClick() else onStationClick(station)
                        },
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.car_mode_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = DarkOnSurfaceVariant,
                )
            }
        }

        if (currentStation != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .background(DarkSurfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StationLogoImage(
                    imageUrl = currentStation.faviconUrl,
                    contentDescription = currentStation.name,
                    modifier = Modifier
                        .size(60.dp)
                        .background(DarkSurface, CircleShape),
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(statusRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = RadioAccent,
                    )
                    StationNameText(
                        name = currentStation.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    color = RadioAccent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(
                                if (isActive) R.string.car_mode_pause else R.string.car_mode_play,
                            ),
                            tint = DarkBackground,
                            modifier = Modifier.size(42.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CarStationTile(
    station: Station,
    isCurrent: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isCurrent) RadioAccent.copy(alpha = 0.16f) else DarkSurface,
        border = BorderStroke(
            width = if (isCurrent) 1.5.dp else 1.dp,
            color = if (isCurrent) RadioAccent.copy(alpha = 0.7f) else DarkBorder.copy(alpha = 0.8f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationLogoImage(
                imageUrl = station.faviconUrl,
                contentDescription = station.name,
                modifier = Modifier
                    .size(52.dp)
                    .background(DarkSurfaceVariant, CircleShape),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                StationNameText(
                    name = station.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                )
                val subtitle = listOfNotNull(
                    station.country?.takeUnless { it.isBlank() },
                    station.codec?.takeUnless { it.isBlank() }?.uppercase(),
                ).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // REA: Icon-Farbe hängt im Dekompilat nur an isActive (b50: z2 ? f9679d : f9684i),
            // das Pause-Symbol zusätzlich an isCurrent.
            Icon(
                imageVector = if (isActive && isCurrent) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = if (isActive) RadioAccent else DarkOnSurfaceVariant,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}
