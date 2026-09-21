// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.darksoon.radiowave.core.data.playlist.PlaylistParser
import de.darksoon.radiowave.core.data.repository.AppSettingsState
import de.darksoon.radiowave.core.data.repository.CoverArtRepository
import de.darksoon.radiowave.core.data.repository.CustomStationRepository
import de.darksoon.radiowave.core.data.repository.FavoriteRepository
import de.darksoon.radiowave.core.data.repository.OfflineFirstStationRepository
import de.darksoon.radiowave.core.data.repository.RecentRepository
import de.darksoon.radiowave.core.data.repository.SettingsRepository
import de.darksoon.radiowave.core.data.repository.StationMirrorRepository
import de.darksoon.radiowave.core.model.FavoritesImportOutcome
import de.darksoon.radiowave.core.model.Station
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Outcome of a playlist import, used to summarize the result to the user. */
data class PlaylistImportResult(
    val imported: Int,
    val skipped: Int,
    val totalFound: Int,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val recentRepository: RecentRepository,
    private val stationRepository: OfflineFirstStationRepository,
    private val settingsRepository: SettingsRepository,
    private val favoriteRepository: FavoriteRepository,
    private val customStationRepository: CustomStationRepository,
    private val coverArtRepository: CoverArtRepository,
    private val stationMirrorRepository: StationMirrorRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /** Live progress of the local station-directory mirror sync (settings UI). */
    val mirrorSyncState: StateFlow<StationMirrorRepository.SyncState> = stationMirrorRepository.syncState

    /** Snapshot of all settings — observed reactively by the UI. */
    val settings: StateFlow<AppSettingsState> = settingsRepository.data
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.initialSnapshotBlocking())

    // Setters — thin wrappers around SettingsRepository. UI dispatches onChange events here.
    fun setThemeMode(v: String) = viewModelScope.launch { settingsRepository.setThemeMode(v) }
    fun setAppLanguage(v: String) = viewModelScope.launch { settingsRepository.setAppLanguage(v) }
    fun setDynamicColors(v: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColors(v) }
    fun setShowMiniplayerMetadata(v: Boolean) = viewModelScope.launch { settingsRepository.setShowMiniplayerMetadata(v) }
    fun setKeepScreenOnFullscreen(v: Boolean) = viewModelScope.launch { settingsRepository.setKeepScreenOnFullscreen(v) }
    fun setShowQuickToasts(v: Boolean) = viewModelScope.launch { settingsRepository.setShowQuickToasts(v) }
    fun setShowInsecureStreams(v: Boolean) = viewModelScope.launch { settingsRepository.setShowInsecureStreams(v) }
    fun setNotificationShowPlayPause(v: Boolean) = viewModelScope.launch { settingsRepository.setNotificationShowPlayPause(v) }
    fun setNotificationShowStop(v: Boolean) = viewModelScope.launch { settingsRepository.setNotificationShowStop(v) }
    fun setNotificationShowPrevious(v: Boolean) = viewModelScope.launch { settingsRepository.setNotificationShowPrevious(v) }
    fun setNotificationShowNext(v: Boolean) = viewModelScope.launch { settingsRepository.setNotificationShowNext(v) }
    fun setDefaultAudioQuality(v: String) = viewModelScope.launch { settingsRepository.setDefaultAudioQuality(v) }
    fun setEqPreset(v: String) = viewModelScope.launch { settingsRepository.setEqPreset(v) }
    fun setAllowMobileData(v: Boolean) = viewModelScope.launch { settingsRepository.setAllowMobileData(v) }
    fun setBufferProfile(v: String) = viewModelScope.launch { settingsRepository.setBufferProfile(v) }
    fun setTimeshiftGuard(v: Boolean) = viewModelScope.launch { settingsRepository.setTimeshiftGuard(v) }
    fun setThermalMode(v: Boolean) = viewModelScope.launch { settingsRepository.setThermalMode(v) }
    fun setAutoPlayOnAndroidAutoConnect(v: Boolean) = viewModelScope.launch { settingsRepository.setAutoPlayOnAndroidAutoConnect(v) }
    fun setLimitAndroidAutoQuality(v: Boolean) = viewModelScope.launch { settingsRepository.setLimitAndroidAutoQuality(v) }
    fun setConfirmRemoveFavorite(v: Boolean) = viewModelScope.launch { settingsRepository.setConfirmRemoveFavorite(v) }
    fun setStationMirrorEnabled(v: Boolean) = viewModelScope.launch {
        settingsRepository.setStationMirrorEnabled(v)
        // First enable → kick off the initial download right away so the user sees
        // the mirror fill instead of waiting for the next app start.
        if (v) stationMirrorRepository.syncNow()
    }
    fun setStationMirrorIntervalHours(v: Int) = viewModelScope.launch { settingsRepository.setStationMirrorIntervalHours(v) }
    fun setStationMirrorWifiOnly(v: Boolean) = viewModelScope.launch { settingsRepository.setStationMirrorWifiOnly(v) }
    fun syncStationMirrorNow() = viewModelScope.launch { stationMirrorRepository.syncNow() }

    fun clearHistory() {
        viewModelScope.launch {
            recentRepository.clearHistory()
        }
    }

    fun clearStationCache() {
        viewModelScope.launch {
            stationMirrorRepository.clearCachedStations()
            // Also clear the in-memory search cache so results are visibly gone
            stationRepository.clearSearchCache()
            // ... and the in-memory cover-art cache (was never wired here — audit
            // 2026-07-18, M1).
            coverArtRepository.clearCache()
        }
    }

    /**
     * Serializes the current favorites to JSON and hands it to [onResult] — the caller (the
     * Composable) is responsible for writing it to the SAF `Uri` it obtained from the
     * `CreateDocument` picker, since that's a `ContentResolver`/`Context` concern.
     */
    fun exportFavorites(onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult(favoriteRepository.exportFavoritesJson())
        }
    }

    /** Parses and merges [json] (read by the caller from the SAF `OpenDocument` result). */
    fun importFavorites(json: String, onResult: (FavoritesImportOutcome) -> Unit) {
        viewModelScope.launch {
            onResult(favoriteRepository.importFavoritesJson(json))
        }
    }

    /**
     * Same validation as the manual "add custom station" flow in feature-favorites
     * (kept as a separate copy rather than shared code — the two features live in
     * different Gradle modules with no existing shared dependency for it). A
     * malformed URL here would otherwise reach ExoPlayer at play time and crash it.
     */
    private fun isValidStreamUrl(streamUrl: String): Boolean {
        val trimmed = streamUrl.trim()
        if (trimmed.isBlank()) return false
        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase()
        val host = uri.host
        return (scheme == "http" || scheme == "https") && !host.isNullOrBlank()
    }

    /**
     * Imports custom stations from an M3U/M3U8/PLS playlist file picked via SAF.
     * Reuses [isValidStreamUrl] so imported streams go through the same validation
     * as the manual "add custom station" flow, skips entries that are invalid
     * individually rather than failing the whole import, and dedupes both within
     * the playlist itself and against custom stations that already exist.
     *
     * Never throws — file I/O and parsing errors are treated as "no entries found"
     * so a bad file can't crash the caller.
     */
    suspend fun importPlaylist(uri: Uri): PlaylistImportResult {
        val entries = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    PlaylistParser.parse(queryFileName(uri), reader)
                }
            }
        }.getOrNull().orEmpty()

        if (entries.isEmpty()) {
            return PlaylistImportResult(imported = 0, skipped = 0, totalFound = 0)
        }

        val existingUrls = customStationRepository.getCustomStations().first().map { it.streamUrl }.toSet()
        val seenUrlsInBatch = mutableSetOf<String>()
        var imported = 0
        var skipped = 0
        for (entry in entries) {
            val streamUrl = entry.streamUrl.trim()
            val isNewInBatch = seenUrlsInBatch.add(streamUrl)
            if (!isValidStreamUrl(streamUrl) || !isNewInBatch || streamUrl in existingUrls) {
                skipped++
                continue
            }
            val station = Station(
                uuid = "custom-${UUID.randomUUID()}",
                name = entry.name.trim().ifBlank { streamUrl },
                streamUrl = streamUrl,
            )
            customStationRepository.addCustomStation(station)
            favoriteRepository.toggleFavorite(station.copy(isFavorite = false))
            imported++
        }
        return PlaylistImportResult(imported = imported, skipped = skipped, totalFound = entries.size)
    }

    /** Best-effort display name lookup for a SAF `Uri`, used for format detection by extension. */
    private fun queryFileName(uri: Uri): String? {
        if (uri.scheme != "content") return uri.lastPathSegment
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        }.getOrNull() ?: uri.lastPathSegment
    }
}
