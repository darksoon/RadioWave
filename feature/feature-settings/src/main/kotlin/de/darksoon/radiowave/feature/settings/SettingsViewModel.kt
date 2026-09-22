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
import de.darksoon.radiowave.core.data.policy.StationInputValidator
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
import java.io.StringReader
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Outcome of a playlist import, used to summarize the result to the user. */
data class PlaylistImportResult(
    val imported: Int,
    /** All skipped entries (invalid, duplicate, over the cap), including [blocked]. */
    val skipped: Int,
    val totalFound: Int,
    /** Subset of [skipped] rejected by the content-policy blocklist. */
    val blocked: Int = 0,
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
    fun setAutoPlayOnAppStart(v: Boolean) = viewModelScope.launch { settingsRepository.setAutoPlayOnAppStart(v) }
    fun setRestoreLastView(v: Boolean) = viewModelScope.launch { settingsRepository.setRestoreLastView(v) }
    fun setLimitAndroidAutoQuality(v: Boolean) = viewModelScope.launch { settingsRepository.setLimitAndroidAutoQuality(v) }
    fun setConfirmRemoveFavorite(v: Boolean) = viewModelScope.launch { settingsRepository.setConfirmRemoveFavorite(v) }
    fun setReportStationClicks(v: Boolean) = viewModelScope.launch { settingsRepository.setReportStationClicks(v) }
    fun setOnlineCoverLookup(v: Boolean) = viewModelScope.launch { settingsRepository.setOnlineCoverLookup(v) }
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
     * Reads a picked import file as text, or null if it can't be opened or exceeds
     * [StationInputValidator.MAX_IMPORT_BYTES] (enforced while reading, so a huge file
     * is never buffered completely). Blocking — call from an IO dispatcher.
     */
    fun readImportText(uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { StationInputValidator.readBoundedText(it) }
    }.getOrNull()

    /**
     * Imports custom stations from an M3U/M3U8/PLS playlist file picked via SAF.
     * Every entry goes through [StationInputValidator] — the same gate as the manual
     * "add custom station" flow and the favorites JSON import — and is skipped
     * individually if invalid or blocked. Dedupes both within the playlist itself and
     * against custom stations that already exist; at most
     * [StationInputValidator.MAX_IMPORT_ENTRIES] entries are imported.
     *
     * Never throws — file I/O and parsing errors (including an oversized file) are
     * treated as "no entries found" so a bad file can't crash the caller.
     */
    suspend fun importPlaylist(uri: Uri): PlaylistImportResult {
        val entries = withContext(Dispatchers.IO) {
            runCatching {
                readImportText(uri)?.let { text ->
                    PlaylistParser.parse(queryFileName(uri), BufferedReader(StringReader(text)))
                }
            }.getOrNull().orEmpty()
        }

        if (entries.isEmpty()) {
            return PlaylistImportResult(imported = 0, skipped = 0, totalFound = 0)
        }

        val existingUrls = customStationRepository.getCustomStations().first().map { it.streamUrl }.toSet()
        val seenUrlsInBatch = mutableSetOf<String>()
        var imported = 0
        var skipped = (entries.size - StationInputValidator.MAX_IMPORT_ENTRIES).coerceAtLeast(0)
        var blocked = 0
        for (entry in entries.take(StationInputValidator.MAX_IMPORT_ENTRIES)) {
            val candidate = Station(
                uuid = "custom-${UUID.randomUUID()}",
                name = entry.name,
                streamUrl = entry.streamUrl,
            )
            val station = when (val result = StationInputValidator.validate(candidate)) {
                is StationInputValidator.Result.Valid -> result.station
                StationInputValidator.Result.Invalid -> {
                    skipped++
                    continue
                }
                StationInputValidator.Result.Blocked -> {
                    skipped++
                    blocked++
                    continue
                }
            }
            if (!seenUrlsInBatch.add(station.streamUrl) || station.streamUrl in existingUrls) {
                skipped++
                continue
            }
            customStationRepository.addCustomStation(station)
            favoriteRepository.toggleFavorite(station.copy(isFavorite = false))
            imported++
        }
        return PlaylistImportResult(imported = imported, skipped = skipped, totalFound = entries.size, blocked = blocked)
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
