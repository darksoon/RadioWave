// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.savedtracks

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.darksoon.radiowave.core.model.SavedTrack

/*
 * REA 95fcb33: Alles aus p000/z03.java#m11363a (SavedTracksButton) und #m11364b
 * (SaveTrackButton) plus deren Lambdas:
 *  - Listen-Dialog: C0343ic Fall 7 (Schließen), C0780u7 Fall 22 (Löschen-Knopf nur bei
 *    nicht-leerer Liste), me0 Fall 4 (Titel), C0470lu Fall 3 + C0640qf Fall 7 (LazyColumn),
 *    is1 Fall 1 (Zeile), C0808uz Fall 4/5 (Kopieren/Entfernen), C0845vz Fall 5 (Löschen-Icon).
 *  - Bestätigung: C0780u7 Fall 21 + C0197ee Fall 26, C0343ic Fall 6, me0 Fall 2/3.
 *  - Speichern-Knopf: p000/op3.java default-Zweig (Speichern bzw. Entfernen = Umschalter).
 * Verwendung: Startseiten-Header (iconOnly = true, Farbe primary) und Player
 * (C0299h5.java ~Z. 4226: SaveTrackButton + SavedTracksButton nebeneinander, Farbe Weiß).
 */

/**
 * Opens the list of saved songs. On Home it is an icon in the header, in the player a text
 * button next to [SaveTrackButton].
 */
@Composable
fun SavedTracksButton(
    tint: Color = MaterialTheme.colorScheme.primary,
    iconOnly: Boolean = false,
    viewModel: SavedTracksViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var showList by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val hasError by viewModel.hasError.collectAsStateWithLifecycle()

    if (iconOnly) {
        IconButton(onClick = { showList = true }) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = stringResource(R.string.saved_tracks_title),
                tint = tint,
            )
        }
    } else {
        TextButton(onClick = { showList = true }) {
            Text(text = stringResource(R.string.saved_tracks_title), color = tint)
        }
    }

    if (showList) {
        AlertDialog(
            onDismissRequest = { showList = false },
            confirmButton = {
                TextButton(onClick = { showList = false }) {
                    Text(stringResource(R.string.saved_tracks_close))
                }
            },
            dismissButton = {
                if (tracks.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) {
                        Text(stringResource(R.string.saved_tracks_clear))
                    }
                }
            },
            title = { Text(stringResource(R.string.saved_tracks_title)) },
            text = {
                SavedTracksList(
                    tracks = tracks,
                    hasError = hasError,
                    onCopy = { track -> copyToClipboard(context, track) },
                    onRemove = { track -> viewModel.remove(track.id) },
                )
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        confirmClear = false
                    },
                ) {
                    Text(stringResource(R.string.saved_tracks_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.saved_tracks_close))
                }
            },
            title = { Text(stringResource(R.string.saved_tracks_clear)) },
            text = { Text(stringResource(R.string.saved_tracks_clear_confirm)) },
        )
    }
}

@Composable
private fun SavedTracksList(
    tracks: List<SavedTrack>,
    hasError: Boolean,
    onCopy: (SavedTrack) -> Unit,
    onRemove: (SavedTrack) -> Unit,
) {
    LazyColumn {
        item {
            Text(
                text = stringResource(R.string.saved_tracks_explanation),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (hasError) {
            item {
                Text(
                    text = stringResource(R.string.saved_tracks_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else if (tracks.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.saved_tracks_empty),
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
        items(tracks, key = { track -> track.id }) { track ->
            SavedTrackRow(
                track = track,
                onCopy = { onCopy(track) },
                onRemove = { onRemove(track) },
            )
        }
    }
}

@Composable
private fun SavedTrackRow(
    track: SavedTrack,
    onCopy: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = track.title, style = MaterialTheme.typography.titleSmall)
            track.artist?.let { artist ->
                Text(text = artist, style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = track.stationName, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = stringResource(R.string.saved_tracks_copy),
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = stringResource(R.string.saved_tracks_remove, track.title),
            )
        }
    }
}

/** "Artist — Title" (or just the title) as plain text; the clip label is the title. */
private fun copyToClipboard(context: Context, track: SavedTrack) {
    val text = listOfNotNull(track.artist, track.title).joinToString(" — ")
    context.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText(track.title, text))
}

/**
 * Player button that bookmarks the current ICY title, or removes the bookmark when this
 * song is already saved. Disabled (38 % alpha) while the station sends no title.
 *
 * [title]/[artist] are the parsed stream metadata the player shows (PlayerState.metadata);
 * matching against existing bookmarks ignores case and surrounding whitespace.
 */
@Composable
fun SaveTrackButton(
    title: String?,
    artist: String?,
    stationName: String,
    tint: Color,
    viewModel: SavedTracksViewModel = hiltViewModel(),
) {
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val hasError by viewModel.hasError.collectAsStateWithLifecycle()
    val trimmedTitle = title?.trim()
    val normalizedArtist = artist?.trim().orEmpty()
    val savedTrack = tracks.firstOrNull { track ->
        track.title.equals(trimmedTitle, ignoreCase = true) &&
            (track.artist ?: "").equals(normalizedArtist, ignoreCase = true)
    }
    val enabled = !title.isNullOrBlank()
    val color = if (enabled) tint else tint.copy(alpha = 0.38f)

    TextButton(
        onClick = {
            if (savedTrack == null) {
                if (title != null) viewModel.save(title, artist, stationName)
            } else {
                viewModel.remove(savedTrack.id)
            }
        },
        enabled = enabled,
    ) {
        Icon(
            imageVector = if (savedTrack == null) Icons.Outlined.BookmarkBorder else Icons.Filled.Bookmark,
            contentDescription = null,
            tint = color,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(
                when {
                    hasError -> R.string.saved_tracks_error
                    savedTrack == null -> R.string.saved_tracks_save
                    else -> R.string.saved_tracks_saved
                },
            ),
            color = color,
        )
    }
}
