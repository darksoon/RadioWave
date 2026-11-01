// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.settings

// REA 95fcb33 — Einstellungen komplett neu gegliedert. Rekonstruiert aus jadx handy
// p000/gc5.java (m3830i = SettingsScreen, Zeilen 847–9984; m3822a..m3834m = Zeilen-Bausteine),
// p000/fh4.java (SettingsCategory), p000/gh4.java (SettingsEntry), p000/ii4.java
// (Kategorie-Karten der Übersicht), p000/is1.java (Fall 2: Eintrag in der Liste),
// p000/gi4.java (Auswahldialog), p000/bg1.java Fall 22 (Sortierung).
// Grundprinzip im Build: EINE flache Liste von SettingsEntry(key, category, searchTerms,
// content). Übersicht = Kategorie-Karten; Kategorie gewählt = alle Einträge der Kategorie;
// Suchtext vorhanden = alle Einträge, deren Kategorie-Titel oder Suchbegriffe den Text
// enthalten (kategorieübergreifend, mit Kategorie-Label über jeder Karte).
// Farben: im Build überall MaterialTheme.colorScheme statt der festen Dark-Farben aus
// v1.0.19 (Hell/System-Theme ist neu): White → onSurface, RadioAccent → primary,
// DarkOnSurfaceVariant → onSurfaceVariant (Feldzuordnung über ColorScheme.toString(), ob0.java).

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.darksoon.radiowave.core.data.repository.AppSettingsState
import de.darksoon.radiowave.core.data.repository.StationMirrorRepository
import de.darksoon.radiowave.core.data.update.LocalIssueReporter
import de.darksoon.radiowave.core.model.AppSettings
import de.darksoon.radiowave.core.model.DataUsageEstimate
import de.darksoon.radiowave.core.model.FavoritesImportOutcome
// REA-UNSICHER: Gemeinsame Audioausgabe-Schaltfläche (+ Dialog). Im Build eine Funktion
// (p000/za0.m11528l), die von Einstellungen, Player und Auto-Modus aufgerufen wird; ihre
// Strings (audio_*) liegen im Player-Modul. Paket/Name siehe Bericht „Änderungen an fremden Dateien“.
import de.darksoon.radiowave.feature.player.AudioOutputButton
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings categories in display order. REA 95fcb33: p000/fh4.java — Enum mit
 * Titel-String und Icon; Namen DISPLAY, PLAYBACK, CAR, NETWORK, PRIVACY, ADVANCED, INFO belegt.
 */
private enum class SettingsCategory(val titleRes: Int, val icon: ImageVector) {
    DISPLAY(R.string.settings_display_category, Icons.Filled.Tune),
    PLAYBACK(R.string.settings_playback_category, Icons.AutoMirrored.Filled.VolumeUp),
    CAR(R.string.settings_car_category, Icons.Filled.DirectionsCar),
    NETWORK(R.string.settings_network_category, Icons.Filled.Storage),
    PRIVACY(R.string.settings_privacy_category, Icons.Filled.Security),
    ADVANCED(R.string.settings_advanced_category, Icons.Filled.Build),
    INFO(R.string.settings_info_category, Icons.Filled.Info),
}

/**
 * One searchable settings block. [searchTerms] are string resources whose (localized)
 * text, together with the category title, is matched against the search query.
 * REA 95fcb33: p000/gh4.java — toString() belegt "SettingsEntry(key, category, searchTerms, content)".
 */
private data class SettingsEntry(
    val key: String,
    val category: SettingsCategory,
    val searchTerms: List<Int>,
    val content: @Composable () -> Unit,
)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onRestartOnboarding: () -> Unit = {},
    // REA 95fcb33: neuer vierter Parameter (gc5.m3830i, sj1Var2) — öffnet den Auto-Modus.
    // REA-UNSICHER: Name geraten.
    onOpenCarMode: () -> Unit = {},
) {
    val context = LocalContext.current
    // Config-change-aware resource lookups for use inside non-composable callbacks
    // (Compose lint flags Context.getString from LocalContext.current — see
    // LocalContextGetResourceValueCall). stringResource can't be called from these lambdas.
    val resources = LocalResources.current
    val uriHandler = LocalUriHandler.current
    val settingsFlow = viewModel.settings.collectAsStateWithLifecycle()
    val settings by settingsFlow
    var issueReportRefreshKey by remember { mutableLongStateOf(0L) }
    // Read the crash report file on IO to avoid blocking the composition thread.
    val latestIssueReport by produceState<de.darksoon.radiowave.core.data.update.LocalIssueReport?>(
        initialValue = null,
        key1 = context,
        key2 = issueReportRefreshKey,
    ) {
        value = withContext(Dispatchers.IO) {
            LocalIssueReporter.getLatestReport(context)
        }
    }

    val themeMode = settings.themeMode
    val appLanguage = settings.appLanguage
    val dynamicColors = settings.dynamicColors
    val showMiniPlayerMetadata = settings.showMiniplayerMetadata
    val keepScreenOnFullscreen = settings.keepScreenOnFullscreen
    val showQuickToasts = settings.showQuickToasts
    val showInsecureStreams = settings.showInsecureStreams
    val showNotificationPlayPause = settings.notificationShowPlayPause
    val showNotificationPrevious = settings.notificationShowPrevious
    val showNotificationNext = settings.notificationShowNext
    val showNotificationStop = settings.notificationShowStop
    val defaultAudioQuality = settings.defaultAudioQuality
    val eqPreset = settings.eqPreset
    val allowMobileData = settings.allowMobileData
    val bufferProfile = settings.bufferProfile
    val timeshiftGuard = settings.timeshiftGuard
    val thermalMode = settings.thermalMode
    val autoPlayOnAndroidAutoConnect = settings.autoPlayOnAndroidAutoConnect
    val autoPlayOnAppStart = settings.autoPlayOnAppStart
    val restoreLastView = settings.restoreLastView
    val limitAndroidAutoQuality = settings.limitAndroidAutoQuality
    val confirmRemoveFavorite = settings.confirmRemoveFavorite
    val appVersion = rememberAppVersion(context)
    val dynamicColorsSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var batteryOptimizationExcluded by remember {
        mutableStateOf(isBatteryOptimizationExcluded(context))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    // REA 95fcb33: Verlauf löschen fragt jetzt per Dialog nach (settings_history_explanation);
    // der Toast „Verlauf gelöscht“ ist entfallen (String im Build entfernt).
    var showClearHistoryDialog by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // SAF pickers for favorites export/import. Declared unconditionally so the launcher's
    // ActivityResultContract registration doesn't depend on the visible category.
    val exportFavoritesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportFavorites { favoritesJson ->
            coroutineScope.launch {
                val success = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(favoritesJson.toByteArray(Charsets.UTF_8))
                        } ?: error("openOutputStream returned null")
                    }.isSuccess
                }
                Toast.makeText(
                    context,
                    resources.getString(
                        if (success) R.string.settings_export_favorites_success
                        else R.string.settings_export_favorites_failed,
                    ),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
    val importFavoritesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val fileContent = withContext(Dispatchers.IO) { viewModel.readImportText(uri) }
            if (fileContent == null) {
                Toast.makeText(
                    context,
                    resources.getString(R.string.settings_import_favorites_read_failed),
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }
            viewModel.importFavorites(fileContent) { outcome ->
                val message = when (outcome) {
                    is FavoritesImportOutcome.Success -> resources.getString(
                        R.string.settings_import_favorites_result,
                        outcome.imported,
                        outcome.skippedExisting,
                        outcome.skippedInvalid,
                    )
                    FavoritesImportOutcome.ParseError ->
                        resources.getString(R.string.settings_import_favorites_parse_error)
                    is FavoritesImportOutcome.UnsupportedVersion ->
                        resources.getString(R.string.settings_import_favorites_unsupported_version)
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    val importPlaylistLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val result = viewModel.importPlaylist(uri)
            val message = if (result.totalFound == 0) {
                resources.getString(R.string.settings_import_playlist_empty)
            } else {
                resources.getString(R.string.settings_import_playlist_result, result.imported, result.skipped)
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryOptimizationExcluded = isBatteryOptimizationExcluded(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    // One scroll position per category (so returning to a category keeps its position),
    // one for the overview and one for search results.
    val overviewListState = rememberLazyListState()
    val categoryListStates: Map<SettingsCategory, LazyListState> =
        SettingsCategory.entries.associateWith { rememberLazyListState() }
    val searchListState = rememberLazyListState()
    LaunchedEffect(query) {
        // REA: p000/bc2.java Fall 0 — nicht-suspendierendes Scroll-Request an den Anfang.
        searchListState.requestScrollToItem(0)
    }

    // ---------------------------------------------------------------------------------
    // All settings entries. Keys like "GENERAL_1"/"SOUND_0"/"DATA_7" are the stable keys
    // from the build (they still carry the v1.0.19 category + index); new entries got
    // descriptive keys. The list order is the build's source order; bg1 Fall 22 only pulls
    // DATA_0 (mobile data), quality_wifi, quality_mobile to the top of their list.
    // ---------------------------------------------------------------------------------
    val entries: List<SettingsEntry> = listOf(
        SettingsEntry(
            key = "visualizer",
            category = SettingsCategory.DISPLAY,
            searchTerms = listOf(R.string.settings_visualizer_title, R.string.settings_visualizer_help),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_visualizer_title),
                subtitle = stringResource(R.string.settings_visualizer_help),
                checked = settingsFlow.value.visualizerEnabled,
                onCheckedChange = viewModel::setVisualizerEnabled,
            )
        },
        SettingsEntry(
            key = "navigation_focus",
            category = SettingsCategory.CAR,
            searchTerms = listOf(R.string.settings_navigation_title, R.string.settings_navigation_help),
        ) {
            // Effect: PlayerControllerImpl on AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
            // (navigation prompts): mute instead of ducking to 25 %, never pause (FIX 2026-11-01).
            SettingToggleRow(
                title = stringResource(R.string.settings_navigation_title),
                subtitle = stringResource(R.string.settings_navigation_help),
                checked = settingsFlow.value.pauseForNavigation,
                onCheckedChange = viewModel::setPauseForNavigation,
            )
        },
        SettingsEntry(
            key = "output",
            category = SettingsCategory.CAR,
            searchTerms = listOf(R.string.settings_output_title),
        ) {
            // REA: u22.f21052j → me0 Fall 6 → za0.m11528l(): nur die Schaltfläche „Audioausgabe“
            // (öffnet Dialog mit Systemsteuerung/Bluetooth-Einstellungen).
            AudioOutputButton()
        },
        SettingsEntry(
            key = "GENERAL_1",
            category = SettingsCategory.DISPLAY,
            searchTerms = listOf(
                R.string.settings_theme_title,
                R.string.settings_theme_system,
                R.string.settings_theme_light,
                R.string.settings_theme_dark,
            ),
        ) {
            // REA 95fcb33: Theme jetzt wählbar (v1.0.19: nur „Dunkel“, deaktiviert).
            SettingChoiceRow(
                title = stringResource(R.string.settings_theme_title),
                options = listOf(
                    ChoiceOption(AppSettings.THEME_SYSTEM, stringResource(R.string.settings_theme_system)),
                    ChoiceOption(AppSettings.THEME_LIGHT, stringResource(R.string.settings_theme_light)),
                    ChoiceOption(AppSettings.THEME_DARK, stringResource(R.string.settings_theme_dark)),
                ),
                selectedValue = themeMode,
                onSelected = { value -> viewModel.setThemeMode(value) },
                enabled = true,
            )
        },
        SettingsEntry(
            key = "GENERAL_2",
            category = SettingsCategory.DISPLAY,
            searchTerms = listOf(
                R.string.settings_language_title,
                R.string.settings_language_system,
                R.string.settings_language_de,
                R.string.settings_language_en,
            ),
        ) {
            SettingChoiceRow(
                title = stringResource(R.string.settings_language_title),
                options = listOf(
                    ChoiceOption(AppSettings.LANGUAGE_SYSTEM, stringResource(R.string.settings_language_system)),
                    ChoiceOption(AppSettings.LANGUAGE_DE, stringResource(R.string.settings_language_de)),
                    ChoiceOption(AppSettings.LANGUAGE_EN, stringResource(R.string.settings_language_en)),
                ),
                selectedValue = appLanguage,
                onSelected = { value ->
                    viewModel.setAppLanguage(value)
                    // AppCompatDelegate.setApplicationLocales triggers a configuration change
                    // which the system applies on next resume — no Activity.recreate().
                    applyAppLanguage(value)
                },
            )
        },
        SettingsEntry(
            key = "GENERAL_3",
            category = SettingsCategory.DISPLAY,
            searchTerms = listOf(
                R.string.settings_dynamic_colors_title,
                R.string.settings_dynamic_colors_subtitle,
                R.string.settings_dynamic_colors_disabled,
            ),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_dynamic_colors_title),
                subtitle = stringResource(R.string.settings_dynamic_colors_subtitle),
                checked = dynamicColors,
                onCheckedChange = { checked -> viewModel.setDynamicColors(checked) },
                enabled = dynamicColorsSupported,
                disabledHint = stringResource(R.string.settings_dynamic_colors_disabled),
            )
        },
        SettingsEntry(
            key = "SOUND_0",
            category = SettingsCategory.DISPLAY,
            searchTerms = listOf(R.string.settings_mini_player_metadata_title, R.string.settings_mini_player_metadata_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_mini_player_metadata_title),
                subtitle = stringResource(R.string.settings_mini_player_metadata_subtitle),
                checked = showMiniPlayerMetadata,
                onCheckedChange = { checked -> viewModel.setShowMiniplayerMetadata(checked) },
            )
        },
        SettingsEntry(
            key = "SOUND_1",
            category = SettingsCategory.DISPLAY,
            searchTerms = listOf(R.string.settings_keep_screen_on_title, R.string.settings_keep_screen_on_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_keep_screen_on_title),
                subtitle = stringResource(R.string.settings_keep_screen_on_subtitle),
                checked = keepScreenOnFullscreen,
                onCheckedChange = { checked -> viewModel.setKeepScreenOnFullscreen(checked) },
            )
        },
        SettingsEntry(
            key = "SOUND_7",
            category = SettingsCategory.DISPLAY,
            searchTerms = listOf(R.string.settings_restore_last_view_title, R.string.settings_restore_last_view_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_restore_last_view_title),
                subtitle = stringResource(R.string.settings_restore_last_view_subtitle),
                checked = restoreLastView,
                onCheckedChange = { checked -> viewModel.setRestoreLastView(checked) },
            )
        },
        SettingsEntry(
            key = "GENERAL_4",
            category = SettingsCategory.DISPLAY,
            searchTerms = listOf(R.string.settings_quick_hints_title, R.string.settings_quick_hints_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_quick_hints_title),
                subtitle = stringResource(R.string.settings_quick_hints_subtitle),
                checked = showQuickToasts,
                onCheckedChange = { checked -> viewModel.setShowQuickToasts(checked) },
            )
        },
        SettingsEntry(
            key = "GENERAL_5",
            category = SettingsCategory.DISPLAY,
            searchTerms = listOf(R.string.settings_confirm_remove_favorite_title, R.string.settings_confirm_remove_favorite_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_confirm_remove_favorite_title),
                subtitle = stringResource(R.string.settings_confirm_remove_favorite_subtitle),
                checked = confirmRemoveFavorite,
                onCheckedChange = { checked -> viewModel.setConfirmRemoveFavorite(checked) },
            )
        },
        // Notification buttons now live under "Darstellung & Bedienung" (no own category).
        SettingsEntry(
            key = "NOTIFICATION_0",
            category = SettingsCategory.DISPLAY,
            searchTerms = listOf(R.string.settings_notification_play_pause_title, R.string.settings_notification_play_pause_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_notification_play_pause_title),
                subtitle = stringResource(R.string.settings_notification_play_pause_subtitle),
                checked = showNotificationPlayPause,
                onCheckedChange = { checked -> viewModel.setNotificationShowPlayPause(checked) },
            )
        },
        SettingsEntry(
            key = "NOTIFICATION_1",
            category = SettingsCategory.DISPLAY,
            searchTerms = listOf(R.string.settings_notification_previous_title, R.string.settings_notification_previous_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_notification_previous_title),
                subtitle = stringResource(R.string.settings_notification_previous_subtitle),
                checked = showNotificationPrevious,
                onCheckedChange = { checked -> viewModel.setNotificationShowPrevious(checked) },
            )
        },
        SettingsEntry(
            key = "NOTIFICATION_2",
            category = SettingsCategory.DISPLAY,
            searchTerms = listOf(R.string.settings_notification_next_title, R.string.settings_notification_next_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_notification_next_title),
                subtitle = stringResource(R.string.settings_notification_next_subtitle),
                checked = showNotificationNext,
                onCheckedChange = { checked -> viewModel.setNotificationShowNext(checked) },
            )
        },
        SettingsEntry(
            key = "NOTIFICATION_3",
            category = SettingsCategory.DISPLAY,
            searchTerms = listOf(R.string.settings_notification_stop_title, R.string.settings_notification_stop_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_notification_stop_title),
                subtitle = stringResource(R.string.settings_notification_stop_subtitle),
                checked = showNotificationStop,
                onCheckedChange = { checked -> viewModel.setNotificationShowStop(checked) },
            )
        },
        SettingsEntry(
            key = "SOUND_2",
            category = SettingsCategory.PLAYBACK,
            searchTerms = listOf(
                R.string.settings_default_quality_title,
                R.string.settings_quality_auto,
                R.string.settings_quality_low,
                R.string.settings_quality_medium,
                R.string.settings_quality_high,
                R.string.settings_data_usage_estimate,
            ),
        ) {
            SettingChoiceRow(
                title = stringResource(R.string.settings_default_quality_title),
                options = listOf(
                    ChoiceOption(AppSettings.QUALITY_AUTO, stringResource(R.string.settings_quality_auto)),
                    ChoiceOption(AppSettings.QUALITY_LOW, stringResource(R.string.settings_quality_low)),
                    ChoiceOption(AppSettings.QUALITY_MEDIUM, stringResource(R.string.settings_quality_medium)),
                    ChoiceOption(AppSettings.QUALITY_HIGH, stringResource(R.string.settings_quality_high)),
                ),
                selectedValue = defaultAudioQuality,
                onSelected = { value -> viewModel.setDefaultAudioQuality(value) },
                enabled = true,
            )
            // REA 95fcb33: bei „Auto“ Erklärung der WLAN/Mobil-Wahl statt MB/h-Schätzung.
            val hint = if (defaultAudioQuality == AppSettings.QUALITY_AUTO) {
                stringResource(R.string.settings_auto_quality_help)
            } else {
                stringResource(
                    R.string.settings_data_usage_estimate,
                    DataUsageEstimate.megabytesPerHour(defaultAudioQuality).roundToInt(),
                )
            }
            Text(
                text = hint,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 0.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
        },
        SettingsEntry(
            key = "SOUND_3",
            category = SettingsCategory.PLAYBACK,
            searchTerms = listOf(
                R.string.settings_eq_preset_title,
                R.string.settings_eq_preset_flat,
                R.string.settings_eq_preset_bass_boost,
                R.string.settings_eq_preset_treble_boost,
                R.string.settings_eq_preset_vocal,
                R.string.settings_eq_bluetooth_note,
            ),
        ) {
            SettingChoiceRow(
                title = stringResource(R.string.settings_eq_preset_title),
                options = listOf(
                    ChoiceOption(AppSettings.EQ_PRESET_FLAT, stringResource(R.string.settings_eq_preset_flat)),
                    ChoiceOption(AppSettings.EQ_PRESET_BASS_BOOST, stringResource(R.string.settings_eq_preset_bass_boost)),
                    ChoiceOption(AppSettings.EQ_PRESET_TREBLE_BOOST, stringResource(R.string.settings_eq_preset_treble_boost)),
                    ChoiceOption(AppSettings.EQ_PRESET_VOCAL, stringResource(R.string.settings_eq_preset_vocal)),
                ),
                selectedValue = eqPreset,
                onSelected = { value -> viewModel.setEqPreset(value) },
                enabled = true,
            )
            // REA 95fcb33: Hinweis, dass der EQ auf Bluetooth-Kopfhörern aus bleibt
            // (Logik im EqualizerController/Player, nicht in diesem Modul).
            Text(
                text = stringResource(R.string.settings_eq_bluetooth_note),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
            )
        },
        SettingsEntry(
            key = "SOUND_6",
            category = SettingsCategory.PLAYBACK,
            searchTerms = listOf(R.string.settings_auto_play_app_start_title, R.string.settings_auto_play_app_start_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_auto_play_app_start_title),
                subtitle = stringResource(R.string.settings_auto_play_app_start_subtitle),
                checked = autoPlayOnAppStart,
                onCheckedChange = { checked -> viewModel.setAutoPlayOnAppStart(checked) },
            )
        },
        SettingsEntry(
            key = "GENERAL_0",
            category = SettingsCategory.CAR,
            searchTerms = listOf(
                R.string.settings_car_mode_title,
                R.string.settings_car_mode_subtitle,
                R.string.settings_car_mode_action,
            ),
        ) {
            // REA: p000/fa1.java Fall 3.
            SettingActionRow(
                title = stringResource(R.string.settings_car_mode_title),
                subtitle = stringResource(R.string.settings_car_mode_subtitle),
                actionLabel = stringResource(R.string.settings_car_mode_action),
                onActionClick = onOpenCarMode,
            )
        },
        SettingsEntry(
            key = "SOUND_4",
            category = SettingsCategory.CAR,
            searchTerms = listOf(R.string.settings_android_auto_quality_limit_title, R.string.settings_android_auto_quality_limit_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_android_auto_quality_limit_title),
                subtitle = stringResource(R.string.settings_android_auto_quality_limit_subtitle),
                checked = limitAndroidAutoQuality,
                onCheckedChange = { checked -> viewModel.setLimitAndroidAutoQuality(checked) },
            )
        },
        SettingsEntry(
            key = "SOUND_5",
            category = SettingsCategory.CAR,
            searchTerms = listOf(R.string.settings_auto_play_title, R.string.settings_auto_play_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_auto_play_title),
                subtitle = stringResource(R.string.settings_auto_play_subtitle),
                checked = autoPlayOnAndroidAutoConnect,
                onCheckedChange = { checked -> viewModel.setAutoPlayOnAndroidAutoConnect(checked) },
            )
        },
        SettingsEntry(
            key = "DATA_0",
            category = SettingsCategory.NETWORK,
            searchTerms = listOf(R.string.settings_mobile_data_title, R.string.settings_mobile_data_subtitle),
        ) {
            // Off = "Wiedergabe nur über WLAN" (summary on the NETWORK category card).
            SettingToggleRow(
                title = stringResource(R.string.settings_mobile_data_title),
                subtitle = stringResource(R.string.settings_mobile_data_subtitle),
                checked = allowMobileData,
                onCheckedChange = { checked -> viewModel.setAllowMobileData(checked) },
            )
        },
        SettingsEntry(
            key = "DATA_7",
            category = SettingsCategory.NETWORK,
            searchTerms = listOf(
                R.string.settings_station_mirror_title,
                R.string.settings_station_mirror_subtitle,
                R.string.settings_station_mirror_interval_title,
                R.string.settings_station_mirror_interval_daily,
                R.string.settings_station_mirror_interval_weekly,
                // REA-UNSICHER: Rest der Liste im Dekompilat abgeschnitten (gc5.java:6026);
                // naheliegend die übrigen station_mirror-Strings.
                R.string.settings_station_mirror_interval_manual,
                R.string.settings_station_mirror_wifi_only_title,
                R.string.settings_station_mirror_wifi_only_subtitle,
            ),
        ) {
            StationMirrorSettings(viewModel = viewModel, settings = settingsFlow.value)
        },
        SettingsEntry(
            key = "DATA_9",
            category = SettingsCategory.NETWORK,
            searchTerms = listOf(
                R.string.settings_clear_cache,
                R.string.settings_cache_explanation,
                R.string.settings_station_cache_cleared_mirror_kept,
                R.string.settings_station_cache_cleared,
            ),
        ) {
            // REA: p000/C0470lu.java Fall 4 + tr1.java default.
            SettingActionRow(
                title = stringResource(R.string.settings_clear_cache),
                subtitle = stringResource(R.string.settings_cache_explanation),
                actionLabel = stringResource(R.string.settings_clear_cache),
                onActionClick = {
                    viewModel.clearStationCache()
                    // With the offline directory on, only search/cover caches go —
                    // say so instead of claiming the stations were deleted.
                    val clearedMessage = if (settingsFlow.value.stationMirrorEnabled) {
                        R.string.settings_station_cache_cleared_mirror_kept
                    } else {
                        R.string.settings_station_cache_cleared
                    }
                    Toast.makeText(context, resources.getString(clearedMessage), Toast.LENGTH_LONG).show()
                },
            )
        },
        SettingsEntry(
            key = "DATA_6",
            category = SettingsCategory.PRIVACY,
            searchTerms = listOf(R.string.settings_online_cover_lookup_title, R.string.settings_online_cover_lookup_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_online_cover_lookup_title),
                subtitle = stringResource(R.string.settings_online_cover_lookup_subtitle),
                checked = settingsFlow.value.onlineCoverLookup,
                onCheckedChange = { checked -> viewModel.setOnlineCoverLookup(checked) },
            )
        },
        SettingsEntry(
            key = "DATA_5",
            category = SettingsCategory.PRIVACY,
            searchTerms = listOf(R.string.settings_report_station_clicks_title, R.string.settings_report_station_clicks_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_report_station_clicks_title),
                subtitle = stringResource(R.string.settings_report_station_clicks_subtitle),
                checked = settingsFlow.value.reportStationClicks,
                onCheckedChange = { checked -> viewModel.setReportStationClicks(checked) },
            )
        },
        SettingsEntry(
            key = "DATA_4",
            category = SettingsCategory.PRIVACY,
            searchTerms = listOf(R.string.settings_insecure_streams_title, R.string.settings_insecure_streams_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_insecure_streams_title),
                subtitle = stringResource(R.string.settings_insecure_streams_subtitle),
                checked = showInsecureStreams,
                onCheckedChange = { checked -> viewModel.setShowInsecureStreams(checked) },
            )
        },
        SettingsEntry(
            key = "DATA_10",
            category = SettingsCategory.PRIVACY,
            searchTerms = listOf(
                R.string.settings_favorites_backup_title,
                R.string.settings_favorites_backup_subtitle,
                R.string.settings_export_favorites,
                R.string.settings_import_favorites,
            ),
        ) {
            SettingActionRow(
                title = stringResource(R.string.settings_favorites_backup_title),
                subtitle = stringResource(R.string.settings_favorites_backup_subtitle),
                actionLabel = stringResource(R.string.settings_export_favorites),
                onActionClick = { exportFavoritesLauncher.launch(defaultFavoritesExportFileName()) },
                secondaryActionLabel = stringResource(R.string.settings_import_favorites),
                // Broadest possible filter — some apps export JSON tagged as text/plain or
                // octet-stream; content is validated by parsing.
                onSecondaryActionClick = { importFavoritesLauncher.launch(arrayOf("*/*")) },
            )
        },
        SettingsEntry(
            key = "DATA_11",
            category = SettingsCategory.PRIVACY,
            searchTerms = listOf(
                R.string.settings_import_playlist_title,
                R.string.settings_import_playlist_subtitle,
                R.string.settings_import_playlist_action,
            ),
        ) {
            SettingActionRow(
                title = stringResource(R.string.settings_import_playlist_title),
                subtitle = stringResource(R.string.settings_import_playlist_subtitle),
                actionLabel = stringResource(R.string.settings_import_playlist_action),
                onActionClick = { importPlaylistLauncher.launch(arrayOf("*/*")) },
            )
        },
        SettingsEntry(
            key = "DATA_1",
            category = SettingsCategory.ADVANCED,
            searchTerms = listOf(
                R.string.settings_buffer_profile_title,
                R.string.settings_buffer_small,
                R.string.settings_buffer_medium,
                R.string.settings_buffer_large,
            ),
        ) {
            SettingChoiceRow(
                title = stringResource(R.string.settings_buffer_profile_title),
                options = listOf(
                    ChoiceOption(AppSettings.BUFFER_SMALL, stringResource(R.string.settings_buffer_small)),
                    ChoiceOption(AppSettings.BUFFER_MEDIUM, stringResource(R.string.settings_buffer_medium)),
                    ChoiceOption(AppSettings.BUFFER_LARGE, stringResource(R.string.settings_buffer_large)),
                ),
                selectedValue = bufferProfile,
                onSelected = { value -> viewModel.setBufferProfile(value) },
            )
        },
        SettingsEntry(
            key = "DATA_2",
            category = SettingsCategory.ADVANCED,
            searchTerms = listOf(R.string.settings_outage_buffer_title, R.string.settings_outage_buffer_subtitle),
        ) {
            // REA 95fcb33: Titel umbenannt („Netzausfall-Puffer (MVP)“ →
            // „Kurze Verbindungsabbrüche überbrücken“), Key/Funktion (timeshift_guard) unverändert.
            SettingToggleRow(
                title = stringResource(R.string.settings_outage_buffer_title),
                subtitle = stringResource(R.string.settings_outage_buffer_subtitle),
                checked = timeshiftGuard,
                onCheckedChange = { checked -> viewModel.setTimeshiftGuard(checked) },
            )
        },
        SettingsEntry(
            key = "DATA_3",
            category = SettingsCategory.ADVANCED,
            searchTerms = listOf(R.string.settings_thermal_mode_title, R.string.settings_thermal_mode_subtitle),
        ) {
            SettingToggleRow(
                title = stringResource(R.string.settings_thermal_mode_title),
                subtitle = stringResource(R.string.settings_thermal_mode_subtitle),
                checked = thermalMode,
                onCheckedChange = { checked -> viewModel.setThermalMode(checked) },
            )
        },
        SettingsEntry(
            key = "DATA_8",
            category = SettingsCategory.ADVANCED,
            searchTerms = listOf(
                R.string.settings_battery_optimization_title,
                R.string.settings_battery_optimization_disabled,
                R.string.settings_battery_optimization_enabled,
                R.string.settings_battery_check_again,
                R.string.settings_battery_set_exception,
                R.string.settings_battery_settings,
            ),
        ) {
            SettingActionRow(
                title = stringResource(R.string.settings_battery_optimization_title),
                subtitle = if (batteryOptimizationExcluded) {
                    stringResource(R.string.settings_battery_optimization_disabled)
                } else {
                    stringResource(R.string.settings_battery_optimization_enabled)
                },
                actionLabel = if (batteryOptimizationExcluded) {
                    stringResource(R.string.settings_battery_check_again)
                } else {
                    stringResource(R.string.settings_battery_set_exception)
                },
                onActionClick = {
                    requestDisableBatteryOptimization(context)
                    batteryOptimizationExcluded = isBatteryOptimizationExcluded(context)
                },
                secondaryActionLabel = stringResource(R.string.settings_battery_settings),
                onSecondaryActionClick = { openBatteryOptimizationSettings(context) },
            )
        },
        SettingsEntry(
            key = "INFO_1",
            category = SettingsCategory.ADVANCED,
            searchTerms = listOf(R.string.settings_issue_report_status_label, R.string.settings_issue_report_status_none),
        ) {
            InfoTextRow(
                label = stringResource(R.string.settings_issue_report_status_label),
                value = latestIssueReport?.summary ?: stringResource(R.string.settings_issue_report_status_none),
            )
        },
        SettingsEntry(
            key = "INFO_2",
            category = SettingsCategory.ADVANCED,
            searchTerms = listOf(
                R.string.settings_issue_report_title,
                R.string.settings_issue_report_subtitle,
                R.string.settings_issue_report_share,
                R.string.settings_issue_report_status_none,
                R.string.settings_issue_report_open_issue,
            ),
        ) {
            SettingActionRow(
                title = stringResource(R.string.settings_issue_report_title),
                subtitle = stringResource(R.string.settings_issue_report_subtitle),
                actionLabel = stringResource(R.string.settings_issue_report_share),
                onActionClick = {
                    val shareIntent = LocalIssueReporter.buildShareIntent(context)
                    if (shareIntent != null) {
                        val chooser = Intent.createChooser(
                            shareIntent,
                            resources.getString(R.string.settings_issue_report_share),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivitySafely(context, chooser)
                    } else {
                        Toast.makeText(
                            context,
                            resources.getString(R.string.settings_issue_report_status_none),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                secondaryActionLabel = stringResource(R.string.settings_issue_report_open_issue),
                onSecondaryActionClick = {
                    val issueUrl = LocalIssueReporter.buildIssueUrl(context)
                    if (issueUrl != null) {
                        uriHandler.openUri(issueUrl)
                    } else {
                        Toast.makeText(
                            context,
                            resources.getString(R.string.settings_issue_report_status_none),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            )
        },
        SettingsEntry(
            key = "INFO_6",
            category = SettingsCategory.ADVANCED,
            searchTerms = listOf(
                R.string.settings_onboarding_title,
                R.string.settings_onboarding_subtitle,
                R.string.settings_onboarding_action,
            ),
        ) {
            SettingActionRow(
                title = stringResource(R.string.settings_onboarding_title),
                subtitle = stringResource(R.string.settings_onboarding_subtitle),
                actionLabel = stringResource(R.string.settings_onboarding_action),
                onActionClick = onRestartOnboarding,
            )
        },
        SettingsEntry(
            key = "INFO_0",
            category = SettingsCategory.INFO,
            searchTerms = listOf(R.string.settings_version_label),
        ) {
            InfoTextRow(label = stringResource(R.string.settings_version_label), value = appVersion)
        },
        SettingsEntry(
            key = "INFO_3",
            category = SettingsCategory.INFO,
            searchTerms = listOf(R.string.settings_github_repository_title, R.string.settings_github_repository_value),
        ) {
            LinkRow(
                title = stringResource(R.string.settings_github_repository_title),
                subtitle = stringResource(R.string.settings_github_repository_value),
                onClick = { uriHandler.openUri("https://github.com/darksoon/RadioWave") },
            )
        },
        SettingsEntry(
            key = "INFO_4",
            category = SettingsCategory.INFO,
            searchTerms = listOf(R.string.settings_issues_title, R.string.settings_issues_subtitle),
        ) {
            LinkRow(
                title = stringResource(R.string.settings_issues_title),
                subtitle = stringResource(R.string.settings_issues_subtitle),
                onClick = { uriHandler.openUri("https://github.com/darksoon/RadioWave/issues") },
            )
        },
        SettingsEntry(
            key = "INFO_5",
            category = SettingsCategory.INFO,
            searchTerms = listOf(R.string.settings_website_title, R.string.settings_website_value),
        ) {
            LinkRow(
                title = stringResource(R.string.settings_website_title),
                subtitle = stringResource(R.string.settings_website_value),
                onClick = { uriHandler.openUri("https://radiowave.sven-neurath.de") },
            )
        },
        SettingsEntry(
            key = "INFO_7",
            category = SettingsCategory.INFO,
            searchTerms = listOf(R.string.settings_kofi_title, R.string.settings_kofi_subtitle),
        ) {
            LinkRow(
                title = stringResource(R.string.settings_kofi_title),
                subtitle = stringResource(R.string.settings_kofi_subtitle),
                onClick = { uriHandler.openUri("https://ko-fi.com/darksoon") },
            )
        },
        SettingsEntry(
            key = "INFO_8",
            category = SettingsCategory.INFO,
            searchTerms = listOf(R.string.settings_made_by_label, R.string.settings_made_by_value),
        ) {
            InfoTextRow(
                label = stringResource(R.string.settings_made_by_label),
                value = stringResource(R.string.settings_made_by_value),
            )
        },
        SettingsEntry(
            key = "history",
            category = SettingsCategory.PRIVACY,
            searchTerms = listOf(R.string.settings_clear_history),
        ) {
            // REA: p000/C0343ic.java Fall 8.
            SettingActionRow(
                title = stringResource(R.string.settings_clear_history),
                subtitle = stringResource(R.string.settings_history_explanation),
                actionLabel = stringResource(R.string.settings_clear_history),
                onActionClick = { showClearHistoryDialog = true },
            )
        },
        SettingsEntry(
            key = "support",
            category = SettingsCategory.INFO,
            searchTerms = listOf(R.string.settings_home_support_title, R.string.settings_testers_title),
        ) {
            SettingsSupportCard(
                title = stringResource(R.string.settings_home_support_card_title),
                subtitle = stringResource(R.string.settings_home_support_card_subtitle),
                donateTitle = stringResource(R.string.settings_kofi_title),
                donateSubtitle = stringResource(R.string.settings_kofi_subtitle),
                thankYouTitle = stringResource(R.string.settings_testers_title),
                thankYouText = stringResource(R.string.settings_testers_value),
                kevinLinkLabel = stringResource(R.string.settings_support_kevin_link_label),
                onKevinLinkClick = { uriHandler.openUri("https://github.com/Kevin1321") },
                groupLinkLabel = stringResource(R.string.settings_support_group_link_label),
                onGroupLinkClick = { uriHandler.openUri("https://groups.google.com/u/1/g/radiowave-beta-tester") },
                primaryActionLabel = stringResource(R.string.settings_support_donate_button),
                onPrimaryActionClick = { uriHandler.openUri("https://ko-fi.com/darksoon") },
            )
        },
        SettingsEntry(
            key = "quality_wifi",
            category = SettingsCategory.NETWORK,
            searchTerms = listOf(R.string.settings_wifi_quality_title, R.string.settings_auto_quality_explanation),
        ) {
            // Only meaningful in quality mode "Auto" — otherwise disabled with explanation.
            SettingChoiceRow(
                title = stringResource(R.string.settings_wifi_quality_title),
                options = fixedQualityOptions(),
                selectedValue = settingsFlow.value.wifiAudioQuality,
                onSelected = viewModel::setWifiAudioQuality,
                enabled = defaultAudioQuality == AppSettings.QUALITY_AUTO,
                disabledHint = stringResource(R.string.settings_auto_quality_explanation),
            )
        },
        SettingsEntry(
            key = "quality_mobile",
            category = SettingsCategory.NETWORK,
            searchTerms = listOf(R.string.settings_mobile_quality_title, R.string.settings_auto_quality_explanation),
        ) {
            // Additionally requires mobile streaming to be allowed.
            SettingChoiceRow(
                title = stringResource(R.string.settings_mobile_quality_title),
                options = fixedQualityOptions(),
                selectedValue = settingsFlow.value.mobileAudioQuality,
                onSelected = viewModel::setMobileAudioQuality,
                enabled = defaultAudioQuality == AppSettings.QUALITY_AUTO && allowMobileData,
                disabledHint = stringResource(R.string.settings_auto_quality_explanation),
            )
        },
    ).sortedBy { entry ->
        // REA: p000/bg1.java Fall 22 — stabile Sortierung, nur diese drei rücken nach vorn.
        when (entry.key) {
            "DATA_0" -> 0
            "quality_wifi" -> 1
            "quality_mobile" -> 2
            else -> 3
        }
    }

    // Search: blank query → entries of the selected category; otherwise every entry whose
    // category title or one of its search terms contains the trimmed query (ignoring case).
    val visibleEntries = entries.filter { entry ->
        if (query.isBlank()) {
            entry.category == selectedCategory
        } else {
            val needle = query.trim()
            (listOf(context.getString(entry.category.titleRes)) + entry.searchTerms.map { resources.getString(it) })
                .any { it.contains(needle, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectedCategory != null || query.isNotBlank()) {
                IconButton(
                    onClick = {
                        query = ""
                        selectedCategory = null
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                    )
                }
            }
            Text(
                text = selectedCategory?.let { context.getString(it.titleRes) }
                    ?: stringResource(R.string.settings_title),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        // REA-UNSICHER: label vs. placeholder — der Slot ist Parameter 6 von OutlinedTextField
        // (y13.m11031a); singleLine = true ist belegt, Icons gibt es keine.
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            label = { Text(stringResource(R.string.settings_search)) },
            singleLine = true,
        )
        val listState = when {
            query.isNotBlank() -> searchListState
            selectedCategory == null -> overviewListState
            else -> categoryListStates[selectedCategory] ?: overviewListState
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (selectedCategory == null && query.isBlank()) {
                items(
                    items = SettingsCategory.entries,
                    key = { it.name },
                ) { category ->
                    SettingsCategoryCard(
                        title = context.getString(category.titleRes),
                        subtitle = categorySummary(category, settings, appVersion),
                        icon = category.icon,
                        onClick = { selectedCategory = category },
                    )
                }
            } else {
                if (visibleEntries.isEmpty()) {
                    item {
                        Text(stringResource(R.string.settings_search_empty))
                    }
                }
                items(
                    items = visibleEntries,
                    key = { it.key },
                ) { entry ->
                    Column {
                        // While searching, each hit is labelled with its category.
                        if (query.isNotBlank()) {
                            Text(
                                text = context.getString(entry.category.titleRes),
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        SettingsCard { entry.content() }
                    }
                }
            }
        }
    }

    BackHandler(enabled = query.isNotBlank()) {
        query = ""
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearHistoryDialog = false
                    },
                ) {
                    Text(stringResource(R.string.settings_clear_history))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
            title = { Text(stringResource(R.string.settings_clear_history)) },
            text = { Text(stringResource(R.string.settings_history_explanation)) },
        )
    }
}

/** REA: sh4 Fall 0 — Offline-Senderverzeichnis (Inhalt wie v1.0.19, nur Farben/Zeilen neu). */
@Composable
private fun StationMirrorSettings(viewModel: SettingsViewModel, settings: AppSettingsState) {
    SettingToggleRow(
        title = stringResource(R.string.settings_station_mirror_title),
        subtitle = stringResource(R.string.settings_station_mirror_subtitle),
        checked = settings.stationMirrorEnabled,
        onCheckedChange = { checked -> viewModel.setStationMirrorEnabled(checked) },
    )
    if (!settings.stationMirrorEnabled) return
    SettingChoiceRow(
        title = stringResource(R.string.settings_station_mirror_interval_title),
        options = listOf(
            ChoiceOption("24", stringResource(R.string.settings_station_mirror_interval_daily)),
            ChoiceOption("168", stringResource(R.string.settings_station_mirror_interval_weekly)),
            ChoiceOption("0", stringResource(R.string.settings_station_mirror_interval_manual)),
        ),
        selectedValue = settings.stationMirrorIntervalHours.toString(),
        onSelected = { value -> value.toIntOrNull()?.let(viewModel::setStationMirrorIntervalHours) },
    )
    SettingToggleRow(
        title = stringResource(R.string.settings_station_mirror_wifi_only_title),
        subtitle = stringResource(R.string.settings_station_mirror_wifi_only_subtitle),
        checked = settings.stationMirrorWifiOnly,
        onCheckedChange = { checked -> viewModel.setStationMirrorWifiOnly(checked) },
    )
    val mirrorSyncState by viewModel.mirrorSyncState.collectAsStateWithLifecycle()
    val syncStatusText = when (val state = mirrorSyncState) {
        StationMirrorRepository.SyncState.WaitingForWifi ->
            stringResource(R.string.settings_station_mirror_waiting_wifi)
        is StationMirrorRepository.SyncState.Failed ->
            stringResource(R.string.settings_station_mirror_failed)
        is StationMirrorRepository.SyncState.Running ->
            stringResource(R.string.settings_station_mirror_syncing, state.stationsSynced)
        else -> if (settings.stationMirrorLastSyncAtMs > 0L) {
            stringResource(
                R.string.settings_station_mirror_last_sync,
                settings.stationMirrorStationCount,
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(settings.stationMirrorLastSyncAtMs)),
            )
        } else {
            stringResource(R.string.settings_station_mirror_never_synced)
        }
    }
    SettingActionRow(
        title = stringResource(R.string.settings_station_mirror_status_title),
        subtitle = syncStatusText,
        actionLabel = stringResource(R.string.settings_station_mirror_sync_now),
        onActionClick = { viewModel.syncStationMirrorNow() },
    )
}

/**
 * One-line summary under each category card in the overview.
 * REA: p000/ii4.java — pro Kategorie fest verdrahtet.
 */
@Composable
private fun categorySummary(category: SettingsCategory, settings: AppSettingsState, appVersion: String): String =
    when (category) {
        SettingsCategory.DISPLAY -> listOf(
            stringResource(
                when (settings.themeMode) {
                    AppSettings.THEME_LIGHT -> R.string.settings_theme_light
                    AppSettings.THEME_DARK -> R.string.settings_theme_dark
                    else -> R.string.settings_theme_system
                },
            ),
            stringResource(
                when (settings.appLanguage) {
                    AppSettings.LANGUAGE_DE -> R.string.settings_language_de
                    AppSettings.LANGUAGE_EN -> R.string.settings_language_en
                    else -> R.string.settings_language_system
                },
            ),
        ).joinToString(" · ")
        SettingsCategory.PLAYBACK -> stringResource(
            when (settings.defaultAudioQuality) {
                AppSettings.QUALITY_LOW -> R.string.settings_quality_low
                AppSettings.QUALITY_MEDIUM -> R.string.settings_quality_medium
                AppSettings.QUALITY_HIGH -> R.string.settings_quality_high
                else -> R.string.settings_quality_auto
            },
        )
        SettingsCategory.CAR -> stringResource(
            if (settings.autoPlayOnAndroidAutoConnect) R.string.settings_auto_on else R.string.settings_auto_off,
        )
        // "Wiedergabe nur über WLAN" is the summary for allowMobileData == false — there is
        // no separate Wi-Fi-only switch.
        SettingsCategory.NETWORK -> stringResource(
            if (settings.allowMobileData) R.string.settings_mobile_allowed else R.string.settings_wifi_only,
        )
        SettingsCategory.PRIVACY -> stringResource(
            if (settings.onlineCoverLookup) R.string.settings_covers_on else R.string.settings_covers_off,
        )
        SettingsCategory.ADVANCED -> stringResource(R.string.settings_advanced_summary)
        SettingsCategory.INFO -> appVersion
    }

/** low/medium/high for the per-network Auto preferences (no "auto" there). REA: gc5.m3817M. */
@Composable
private fun fixedQualityOptions(): List<ChoiceOption> = listOf(
    ChoiceOption(AppSettings.QUALITY_LOW, stringResource(R.string.settings_quality_low)),
    ChoiceOption(AppSettings.QUALITY_MEDIUM, stringResource(R.string.settings_quality_medium)),
    ChoiceOption(AppSettings.QUALITY_HIGH, stringResource(R.string.settings_quality_high)),
)

private fun applyAppLanguage(language: String) {
    val locales = when (language) {
        AppSettings.LANGUAGE_DE -> LocaleListCompat.forLanguageTags("de")
        AppSettings.LANGUAGE_EN -> LocaleListCompat.forLanguageTags("en")
        else -> LocaleListCompat.getEmptyLocaleList()
    }
    AppCompatDelegate.setApplicationLocales(locales)
}

/** REA: gc5.m3833l — wie v1.0.19, Farben aus dem ColorScheme. */
@Composable
private fun SettingsSupportCard(
    title: String,
    subtitle: String,
    donateTitle: String,
    donateSubtitle: String,
    thankYouTitle: String,
    thankYouText: String,
    kevinLinkLabel: String,
    onKevinLinkClick: () -> Unit,
    groupLinkLabel: String,
    onGroupLinkClick: () -> Unit,
    primaryActionLabel: String,
    onPrimaryActionClick: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            colors.onSurface.copy(alpha = 0.07f),
                            colors.onSurface.copy(alpha = 0.03f),
                        ),
                    ),
                )
                .padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.onSurface,
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.primary.copy(alpha = 0.14f)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = donateTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = donateSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onPrimaryActionClick, modifier = Modifier.fillMaxWidth()) {
                        Text(primaryActionLabel)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.onSurface.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = thankYouTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SupportRichText(
                        text = thankYouText,
                        kevinLinkLabel = kevinLinkLabel,
                        onKevinLinkClick = onKevinLinkClick,
                        groupLinkLabel = groupLinkLabel,
                        onGroupLinkClick = onGroupLinkClick,
                    )
                }
            }
            if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onSecondaryActionClick, modifier = Modifier.fillMaxWidth()) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun SupportRichText(
    text: String,
    kevinLinkLabel: String,
    onKevinLinkClick: () -> Unit,
    groupLinkLabel: String,
    onGroupLinkClick: () -> Unit,
) {
    val latestKevinClick by rememberUpdatedState(onKevinLinkClick)
    val latestGroupClick by rememberUpdatedState(onGroupLinkClick)
    val linkColor = MaterialTheme.colorScheme.primary
    val annotatedText = remember(text, kevinLinkLabel, groupLinkLabel, linkColor) {
        val linkStyles = TextLinkStyles(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Medium,
            ),
        )
        buildAnnotatedString {
            append(text)
            val kevinStart = text.indexOf(kevinLinkLabel)
            if (kevinStart >= 0) {
                addLink(
                    LinkAnnotation.Clickable(
                        tag = "kevin",
                        styles = linkStyles,
                        linkInteractionListener = { latestKevinClick() },
                    ),
                    kevinStart,
                    kevinStart + kevinLinkLabel.length,
                )
            }
            val groupStart = text.indexOf(groupLinkLabel)
            if (groupStart >= 0) {
                addLink(
                    LinkAnnotation.Clickable(
                        tag = "group",
                        styles = linkStyles,
                        linkInteractionListener = { latestGroupClick() },
                    ),
                    groupStart,
                    groupStart + groupLinkLabel.length,
                )
            }
        }
    }
    Text(
        text = annotatedText,
        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
    )
}

/** REA: gc5.m3829h + v91 Fall 2 — Kategorie-Karte der Übersicht. */
@Composable
private fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            colors.onSurface.copy(alpha = 0.08f),
                            colors.onSurface.copy(alpha = 0.03f),
                        ),
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Card around one entry. REA: gc5.m3828g + C0255fz Fall 10. Anders als v1.0.19 ohne
 * Überschrift — die Kategorie steht in der Kopfzeile bzw. (bei Suche) über der Karte.
 */
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, colors.onSurface.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            colors.onSurface.copy(alpha = 0.07f),
                            colors.onSurface.copy(alpha = 0.03f),
                        ),
                    ),
                )
                .padding(top = 10.dp, bottom = 4.dp),
        ) {
            content()
        }
    }
}

/**
 * Whole row toggles (accessibility: one merged Switch node). REA: gc5.m3827f —
 * toggleable(role = Switch) + semantics(mergeDescendants) statt Switch-Callback.
 */
@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    disabledHint: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.58f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (enabled || disabledHint == null) subtitle else "$subtitle  ($disabledHint)",
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) colors.onSurfaceVariant else colors.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

/**
 * Choice as a single row (title + current value); tapping opens a radio-list dialog.
 * REA 95fcb33: gc5.m3826e + gi4.java — ersetzt die FilterChip-Reihe aus v1.0.19.
 */
@Composable
private fun SettingChoiceRow(
    title: String,
    options: List<ChoiceOption>,
    selectedValue: String,
    onSelected: (String) -> Unit,
    enabled: Boolean = true,
    disabledHint: String? = null,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showDialog = true }
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Text(
            text = title,
            color = colors.onSurface,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = options.firstOrNull { it.value == selectedValue }?.label ?: selectedValue,
            color = colors.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (!enabled && disabledHint != null) {
            Text(
                text = disabledHint,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (showDialog && enabled) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
            title = { Text(title) },
            text = {
                LazyColumn {
                    items(options, key = { it.value }) { option ->
                        val selected = option.value == selectedValue
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onSelected(option.value)
                                        showDialog = false
                                    },
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Text(
                                text = option.label,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp),
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun LinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onClick) {
            Text(stringResource(R.string.settings_open))
        }
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onActionClick, modifier = Modifier.weight(1f)) {
                Text(actionLabel)
            }
            if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                OutlinedButton(onClick = onSecondaryActionClick, modifier = Modifier.weight(1f)) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun InfoTextRow(
    label: String,
    value: String,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onSurface,
        )
    }
}

/** REA: p000/v25.java — (value, label)-Paar; Name aus v1.0.19 (ThemeOption) wäre auch möglich. */
private data class ChoiceOption(
    val value: String,
    val label: String,
)

@Composable
private fun rememberAppVersion(context: Context): String {
    return remember(context) {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: context.getString(R.string.settings_unknown)
        } catch (_: Exception) {
            context.getString(R.string.settings_unknown)
        }
    }
}

/** Default filename suggested to the SAF "Create Document" picker when exporting favorites. */
private fun defaultFavoritesExportFileName(): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    return "radiowave-favorites-$date.json"
}

private fun isBatteryOptimizationExcluded(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestDisableBatteryOptimization(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val packageUri = Uri.parse("package:${context.packageName}")
    val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val listIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching {
        context.startActivity(requestIntent)
    }.onFailure {
        runCatching { context.startActivity(listIntent) }
    }
}

private fun openBatteryOptimizationSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun startActivitySafely(context: Context, intent: Intent): Boolean {
    return runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
