// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.feature.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.radiowave.core.model.AppSettings
import de.radiowave.core.data.update.GitHubReleaseUpdater
import de.radiowave.core.data.update.LocalIssueReporter
import de.radiowave.core.data.update.UpdateDownloadProgress
import de.radiowave.core.ui.theme.DarkCardBackground
import de.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.radiowave.core.ui.theme.TealAccent
import de.radiowave.feature.settings.R
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isGerman = configuration.locales[0]?.language?.equals("de", ignoreCase = true) == true
    fun tr(de: String, en: String): String = if (isGerman) de else en
    val uriHandler = LocalUriHandler.current
    val prefs = context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)
    val updateUiState by viewModel.updateUiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var issueReportRefreshKey by remember { mutableLongStateOf(0L) }
    val latestIssueReport = remember(context, issueReportRefreshKey) {
        LocalIssueReporter.getLatestReport(context)
    }

    var themeMode by rememberSaveable {
        mutableStateOf(prefs.getString(AppSettings.KEY_THEME_MODE, AppSettings.THEME_DARK) ?: AppSettings.THEME_DARK)
    }
    var appLanguage by rememberSaveable {
        mutableStateOf(
            prefs.getString(AppSettings.KEY_APP_LANGUAGE, AppSettings.LANGUAGE_SYSTEM)
                ?: AppSettings.LANGUAGE_SYSTEM,
        )
    }
    var dynamicColors by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_DYNAMIC_COLORS, false))
    }
    var showMiniPlayerMetadata by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_SHOW_MINIPLAYER_METADATA, true))
    }
    var keepScreenOnFullscreen by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_KEEP_SCREEN_ON_FULLSCREEN, false))
    }
    var showQuickToasts by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_SHOW_QUICK_TOASTS, true))
    }
    var showInsecureStreams by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_SHOW_INSECURE_STREAMS, true))
    }
    var showNotificationPlayPause by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_NOTIFICATION_SHOW_PLAY_PAUSE, true))
    }
    var showNotificationPrevious by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_NOTIFICATION_SHOW_PREVIOUS, true))
    }
    var showNotificationNext by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_NOTIFICATION_SHOW_NEXT, true))
    }
    var showNotificationStop by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_NOTIFICATION_SHOW_STOP, true))
    }
    var defaultAudioQuality by rememberSaveable {
        mutableStateOf(
            prefs.getString(AppSettings.KEY_DEFAULT_AUDIO_QUALITY, AppSettings.QUALITY_AUTO)
                ?: AppSettings.QUALITY_AUTO,
        )
    }
    var allowMobileData by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_ALLOW_MOBILE_DATA, true))
    }
    var bufferProfile by rememberSaveable {
        mutableStateOf(
            prefs.getString(AppSettings.KEY_BUFFER_PROFILE, AppSettings.BUFFER_MEDIUM)
                ?: AppSettings.BUFFER_MEDIUM,
        )
    }
    var timeshiftGuard by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_TIMESHIFT_GUARD, true))
    }
    var thermalMode by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_THERMAL_MODE, false))
    }
    var autoPlayOnAndroidAutoConnect by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_AUTO_PLAY_ON_ANDROID_AUTO_CONNECT, true))
    }
    var updateCheckEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_UPDATE_CHECK_ENABLED, true))
    }
    var updatePopupEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_UPDATE_POPUP_ENABLED, true))
    }
    var updateBetaChannelEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_UPDATE_BETA_CHANNEL_ENABLED, false))
    }
    var manualUpdateCheckRequested by rememberSaveable { mutableStateOf(false) }
    var showUpdateAvailableDialog by rememberSaveable { mutableStateOf(false) }
    var updateInstallInProgress by remember { mutableStateOf(false) }
    var updateInstallProgress by remember { mutableStateOf<UpdateDownloadProgress?>(null) }

    val appVersion = rememberAppVersion(context)
    val dynamicColorsSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var batteryOptimizationExcluded by remember {
        mutableStateOf(isBatteryOptimizationExcluded(context))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }

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

    LaunchedEffect(Unit) {
        if (themeMode != AppSettings.THEME_DARK) {
            themeMode = AppSettings.THEME_DARK
            prefs.edit().putString(AppSettings.KEY_THEME_MODE, AppSettings.THEME_DARK).apply()
        }
    }

    LaunchedEffect(updateUiState.isChecking, updateUiState.hasUpdate, manualUpdateCheckRequested) {
        if (!manualUpdateCheckRequested || updateUiState.isChecking) return@LaunchedEffect
        manualUpdateCheckRequested = false
        when {
            updateUiState.hasUpdate && updateUiState.latestRelease != null -> {
                showUpdateAvailableDialog = true
            }
            updateUiState.lastError != null -> {
                Toast.makeText(
                    context,
                    tr("Update-Pruefung fehlgeschlagen", "Update check failed"),
                    Toast.LENGTH_SHORT,
                ).show()
            }
            else -> {
                Toast.makeText(
                    context,
                    tr("Kein Update verfuegbar", "No update available"),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                ),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_overview),
                style = MaterialTheme.typography.bodyMedium,
                color = DarkOnSurfaceVariant,
            )
        }

        if (selectedCategory == null) {
            item {
                SettingsCategoryCard(
                    title = stringResource(R.string.settings_category_general_title),
                    subtitle = stringResource(R.string.settings_category_general_subtitle),
                    onClick = { selectedCategory = SettingsCategory.GENERAL },
                )
            }
            item {
                SettingsCategoryCard(
                    title = stringResource(R.string.settings_category_sound_title),
                    subtitle = stringResource(R.string.settings_category_sound_subtitle),
                    onClick = { selectedCategory = SettingsCategory.SOUND },
                )
            }
            item {
                SettingsCategoryCard(
                    title = stringResource(R.string.settings_category_notification_title),
                    subtitle = stringResource(R.string.settings_category_notification_subtitle),
                    onClick = { selectedCategory = SettingsCategory.NOTIFICATION },
                )
            }
            item {
                SettingsCategoryCard(
                    title = stringResource(R.string.settings_category_data_title),
                    subtitle = stringResource(R.string.settings_category_data_subtitle),
                    onClick = { selectedCategory = SettingsCategory.DATA },
                )
            }
            item {
                SettingsCategoryCard(
                    title = stringResource(R.string.settings_category_updates_title),
                    subtitle = stringResource(R.string.settings_category_updates_subtitle),
                    onClick = { selectedCategory = SettingsCategory.UPDATES },
                )
            }
            item {
                SettingsCategoryCard(
                    title = stringResource(R.string.settings_category_info_title),
                    subtitle = stringResource(R.string.settings_category_info_subtitle),
                    onClick = { selectedCategory = SettingsCategory.INFO },
                )
            }
        } else {
            item {
                SettingsDetailHeader(
                    title = categoryTitle(selectedCategory!!, context),
                    onBack = { selectedCategory = null },
                )
            }
            item {
                when (selectedCategory) {
                    SettingsCategory.GENERAL -> SettingsCard(title = stringResource(R.string.settings_category_general_title)) {
                        SettingChoiceRow(
                            title = tr("Theme", "Theme"),
                            options = listOf(
                                ThemeOption(AppSettings.THEME_DARK, tr("Dunkel", "Dark")),
                            ),
                            selectedValue = themeMode,
                            onSelected = { value ->
                                themeMode = value
                                prefs.edit().putString(AppSettings.KEY_THEME_MODE, value).apply()
                            },
                            enabled = false,
                            disabledHint = tr("Nur Dark-Mode aktiv", "Dark mode only"),
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingChoiceRow(
                            title = stringResource(R.string.settings_language_title),
                            options = listOf(
                                ThemeOption(AppSettings.LANGUAGE_SYSTEM, stringResource(R.string.settings_language_system)),
                                ThemeOption(AppSettings.LANGUAGE_DE, stringResource(R.string.settings_language_de)),
                                ThemeOption(AppSettings.LANGUAGE_EN, stringResource(R.string.settings_language_en)),
                            ),
                            selectedValue = appLanguage,
                            onSelected = { value ->
                                appLanguage = value
                                prefs.edit().putString(AppSettings.KEY_APP_LANGUAGE, value).apply()
                                applyAppLanguage(value)
                                (context as? Activity)?.recreate()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = tr("Dynamic Colors", "Dynamic colors"),
                            subtitle = tr("Material You Farben (Android 12+).", "Material You colors (Android 12+)."),
                            checked = dynamicColors,
                            onCheckedChange = { checked ->
                                dynamicColors = checked
                                prefs.edit().putBoolean(AppSettings.KEY_DYNAMIC_COLORS, checked).apply()
                            },
                            enabled = dynamicColorsSupported,
                            disabledHint = tr("Nur auf Android 12+ verfuegbar", "Only available on Android 12+"),
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = tr("Kurze Hinweise anzeigen", "Show quick hints"),
                            subtitle = tr("Zeigt Add/Remove Feedback als Toast.", "Shows add/remove feedback as a toast."),
                            checked = showQuickToasts,
                            onCheckedChange = { checked ->
                                showQuickToasts = checked
                                prefs.edit().putBoolean(AppSettings.KEY_SHOW_QUICK_TOASTS, checked).apply()
                            },
                        )
                    }

                    SettingsCategory.SOUND -> SettingsCard(title = stringResource(R.string.settings_category_sound_title)) {
                        SettingToggleRow(
                            title = tr("Metadaten im Mini-Player", "Metadata in mini player"),
                            subtitle = tr("Zeigt Artist/Titel unter dem Sendernamen.", "Shows artist/title below the station name."),
                            checked = showMiniPlayerMetadata,
                            onCheckedChange = { checked ->
                                showMiniPlayerMetadata = checked
                                prefs.edit().putBoolean(AppSettings.KEY_SHOW_MINIPLAYER_METADATA, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = tr("Display anlassen im Vollbild-Player", "Keep screen on in fullscreen player"),
                            subtitle = tr("Verhindert das Dimmen waehrend Vollbild-Wiedergabe.", "Prevents dimming during fullscreen playback."),
                            checked = keepScreenOnFullscreen,
                            onCheckedChange = { checked ->
                                keepScreenOnFullscreen = checked
                                prefs.edit().putBoolean(AppSettings.KEY_KEEP_SCREEN_ON_FULLSCREEN, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingChoiceRow(
                            title = tr("Standard-Qualitaet", "Default quality"),
                            options = listOf(
                                ThemeOption(AppSettings.QUALITY_AUTO, "Auto"),
                                ThemeOption(AppSettings.QUALITY_LOW, tr("Niedrig", "Low")),
                                ThemeOption(AppSettings.QUALITY_MEDIUM, tr("Mittel", "Medium")),
                                ThemeOption(AppSettings.QUALITY_HIGH, tr("Hoch", "High")),
                            ),
                            selectedValue = defaultAudioQuality,
                            onSelected = { value ->
                                defaultAudioQuality = value
                                prefs.edit().putString(AppSettings.KEY_DEFAULT_AUDIO_QUALITY, value).apply()
                            },
                            enabled = true,
                            disabledHint = null,
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = tr("Autoplay bei Android Auto Verbindung", "Autoplay on Android Auto connect"),
                            subtitle = tr("Startet den zuletzt gehoerten Sender automatisch beim Auto-Connect.", "Starts the last station automatically when Android Auto connects."),
                            checked = autoPlayOnAndroidAutoConnect,
                            onCheckedChange = { checked ->
                                autoPlayOnAndroidAutoConnect = checked
                                prefs.edit()
                                    .putBoolean(AppSettings.KEY_AUTO_PLAY_ON_ANDROID_AUTO_CONNECT, checked)
                                    .apply()
                            },
                        )
                    }

                    SettingsCategory.NOTIFICATION -> SettingsCard(title = stringResource(R.string.settings_category_notification_title)) {
                        SettingToggleRow(
                            title = tr("Play/Pause Button", "Play/Pause button"),
                            subtitle = tr("Steuerung direkt in der Medien-Benachrichtigung.", "Controls directly in media notification."),
                            checked = showNotificationPlayPause,
                            onCheckedChange = { checked ->
                                showNotificationPlayPause = checked
                                prefs.edit().putBoolean(AppSettings.KEY_NOTIFICATION_SHOW_PLAY_PAUSE, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = tr("Previous Button", "Previous button"),
                            subtitle = tr("Vorherigen Sender aus Verlauf abspielen.", "Play previous station from history."),
                            checked = showNotificationPrevious,
                            onCheckedChange = { checked ->
                                showNotificationPrevious = checked
                                prefs.edit().putBoolean(AppSettings.KEY_NOTIFICATION_SHOW_PREVIOUS, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = tr("Next Button", "Next button"),
                            subtitle = tr("Naechsten Sender aus Verlauf/Top-Sendern starten.", "Start next station from history/top stations."),
                            checked = showNotificationNext,
                            onCheckedChange = { checked ->
                                showNotificationNext = checked
                                prefs.edit().putBoolean(AppSettings.KEY_NOTIFICATION_SHOW_NEXT, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = tr("Stop Button", "Stop button"),
                            subtitle = tr("Streaming sofort beenden und Notification entfernen.", "Stop streaming immediately and remove notification."),
                            checked = showNotificationStop,
                            onCheckedChange = { checked ->
                                showNotificationStop = checked
                                prefs.edit().putBoolean(AppSettings.KEY_NOTIFICATION_SHOW_STOP, checked).apply()
                            },
                        )
                    }

                    SettingsCategory.DATA -> SettingsCard(title = stringResource(R.string.settings_category_data_title)) {
                        SettingToggleRow(
                            title = tr("Mobile Daten erlauben", "Allow mobile data"),
                            subtitle = tr("Wenn aus, streamt die App nur im WLAN.", "If disabled, app only streams on Wi-Fi."),
                            checked = allowMobileData,
                            onCheckedChange = { checked ->
                                allowMobileData = checked
                                prefs.edit().putBoolean(AppSettings.KEY_ALLOW_MOBILE_DATA, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingChoiceRow(
                            title = tr("Buffer-Profil", "Buffer profile"),
                            options = listOf(
                                ThemeOption(AppSettings.BUFFER_SMALL, tr("Klein", "Small")),
                                ThemeOption(AppSettings.BUFFER_MEDIUM, tr("Mittel", "Medium")),
                                ThemeOption(AppSettings.BUFFER_LARGE, tr("Gross", "Large")),
                            ),
                            selectedValue = bufferProfile,
                            onSelected = { value ->
                                bufferProfile = value
                                prefs.edit().putString(AppSettings.KEY_BUFFER_PROFILE, value).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = tr("Netzausfall-Puffer (MVP)", "Outage buffer (MVP)"),
                            subtitle = tr("Erhoeht Stream-Puffer und wartet laenger bei kurzen Verbindungsabbruechen.", "Increases stream buffer and waits longer during short connection drops."),
                            checked = timeshiftGuard,
                            onCheckedChange = { checked ->
                                timeshiftGuard = checked
                                prefs.edit().putBoolean(AppSettings.KEY_TIMESHIFT_GUARD, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = tr("Hitzemodus (Auto/Charging)", "Thermal mode (car/charging)"),
                            subtitle = tr("Reduziert Last: kleines Buffer-Profil + weniger Metadaten-/Artwork-Updates.", "Reduces load: smaller buffer profile + fewer metadata/artwork updates."),
                            checked = thermalMode,
                            onCheckedChange = { checked ->
                                thermalMode = checked
                                prefs.edit().putBoolean(AppSettings.KEY_THERMAL_MODE, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = tr("Unsichere HTTP-Streams anzeigen", "Show insecure HTTP streams"),
                            subtitle = tr("Wenn aus, werden HTTP-Sender ausgeblendet.", "If disabled, HTTP stations are hidden."),
                            checked = showInsecureStreams,
                            onCheckedChange = { checked ->
                                showInsecureStreams = checked
                                prefs.edit().putBoolean(AppSettings.KEY_SHOW_INSECURE_STREAMS, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingActionRow(
                            title = tr("Akku-Optimierung", "Battery optimization"),
                            subtitle = if (batteryOptimizationExcluded) {
                                tr(
                                    "Fuer RadioWave deaktiviert. Background-Playback ist besser abgesichert.",
                                    "Disabled for RadioWave. Background playback is better protected.",
                                )
                            } else {
                                tr(
                                    "Aktiv. Kann auf manchen Geraeten Background-Audio aggressiv beenden.",
                                    "Enabled. Can aggressively stop background audio on some devices.",
                                )
                            },
                            actionLabel = if (batteryOptimizationExcluded) tr("Erneut pruefen", "Check again") else tr("Ausnahme setzen", "Set exception"),
                            onActionClick = {
                                requestDisableBatteryOptimization(context)
                                batteryOptimizationExcluded = isBatteryOptimizationExcluded(context)
                            },
                            secondaryActionLabel = tr("Akku-Einstellungen", "Battery settings"),
                            onSecondaryActionClick = { openBatteryOptimizationSettings(context) },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearStationCache()
                                    Toast.makeText(context, tr("Sender-Cache geleert", "Station cache cleared"), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(tr("Cache leeren", "Clear cache"))
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearHistory()
                                    Toast.makeText(context, tr("Verlauf geloescht", "History cleared"), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(tr("History loeschen", "Clear history"))
                            }
                        }
                    }

                    SettingsCategory.UPDATES -> SettingsCard(title = stringResource(R.string.settings_category_updates_title)) {
                        InfoTextRow(label = stringResource(R.string.settings_updates_installed_version), value = appVersion)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        InfoTextRow(
                            label = stringResource(R.string.settings_updates_latest_release),
                            value = updateUiState.latestRelease?.tag ?: stringResource(R.string.settings_updates_not_checked),
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        InfoTextRow(
                            label = stringResource(R.string.settings_updates_status),
                            value = when {
                                updateUiState.latestRelease == null -> stringResource(R.string.settings_updates_status_unknown)
                                updateUiState.hasUpdate -> stringResource(R.string.settings_updates_status_available)
                                else -> stringResource(R.string.settings_updates_status_current)
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = stringResource(R.string.settings_updates_auto_check_title),
                            subtitle = stringResource(R.string.settings_updates_auto_check_subtitle),
                            checked = updateCheckEnabled,
                            onCheckedChange = { checked ->
                                updateCheckEnabled = checked
                                prefs.edit().putBoolean(AppSettings.KEY_UPDATE_CHECK_ENABLED, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = stringResource(R.string.settings_updates_popup_title),
                            subtitle = stringResource(R.string.settings_updates_popup_subtitle),
                            checked = updatePopupEnabled,
                            onCheckedChange = { checked ->
                                updatePopupEnabled = checked
                                prefs.edit().putBoolean(AppSettings.KEY_UPDATE_POPUP_ENABLED, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = stringResource(R.string.settings_updates_beta_channel_title),
                            subtitle = stringResource(R.string.settings_updates_beta_channel_subtitle),
                            checked = updateBetaChannelEnabled,
                            onCheckedChange = { checked ->
                                updateBetaChannelEnabled = checked
                                prefs.edit()
                                    .putBoolean(AppSettings.KEY_UPDATE_BETA_CHANNEL_ENABLED, checked)
                                    .apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingActionRow(
                            title = stringResource(R.string.settings_updates_manual_title),
                            subtitle = buildUpdateCheckSubtitle(context, updateUiState.lastCheckedAtMs, updateUiState.lastError),
                            actionLabel = if (updateUiState.isChecking) {
                                stringResource(R.string.settings_updates_manual_checking)
                            } else {
                                stringResource(R.string.settings_updates_manual_check)
                            },
                            onActionClick = {
                                if (!updateUiState.isChecking) {
                                    manualUpdateCheckRequested = true
                                    viewModel.checkForUpdates(
                                        currentVersionName = appVersion,
                                        includePrerelease = updateBetaChannelEnabled,
                                    )
                                }
                            },
                            secondaryActionLabel = stringResource(R.string.settings_updates_manual_release_page),
                            onSecondaryActionClick = {
                                val releaseUrl = updateUiState.latestRelease?.htmlUrl
                                    ?: "https://github.com/darksoon/RadioWave/releases"
                                uriHandler.openUri(releaseUrl)
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingActionRow(
                            title = tr("Update-Popup testen", "Test update popup"),
                            subtitle = tr(
                                "Zeigt denselben Dialog wie bei einem automatischen Update-Treffer.",
                                "Shows the same dialog used when an automatic update is found.",
                            ),
                            actionLabel = tr("Popup zeigen", "Show popup"),
                            onActionClick = {
                                when {
                                    updateUiState.hasUpdate && updateUiState.latestRelease != null -> {
                                        showUpdateAvailableDialog = true
                                    }
                                    updateUiState.isChecking -> Unit
                                    else -> {
                                        manualUpdateCheckRequested = true
                                        viewModel.checkForUpdates(
                                            currentVersionName = appVersion,
                                            includePrerelease = updateBetaChannelEnabled,
                                        )
                                        Toast.makeText(
                                            context,
                                            tr("Pruefe auf Update...", "Checking for update..."),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            },
                            secondaryActionLabel = tr("Release-Seite", "Release page"),
                            onSecondaryActionClick = {
                                val releaseUrl = updateUiState.latestRelease?.htmlUrl
                                    ?: "https://github.com/darksoon/RadioWave/releases"
                                uriHandler.openUri(releaseUrl)
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        InfoTextBlockRow(
                            label = stringResource(R.string.settings_updates_auto_beta_label),
                            value = stringResource(R.string.settings_updates_auto_beta_text),
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingActionRow(
                            title = stringResource(R.string.settings_updates_auto_help_title),
                            subtitle = stringResource(R.string.settings_updates_auto_help_subtitle),
                            actionLabel = stringResource(R.string.settings_updates_auto_open),
                            onActionClick = { openAndroidAutoApp(context) },
                            secondaryActionLabel = stringResource(R.string.settings_updates_auto_guide),
                            onSecondaryActionClick = {
                                uriHandler.openUri("https://github.com/darksoon/RadioWave/blob/main/docs/ANDROID_AUTO_DEV_MODE.md")
                            },
                        )
                        updateUiState.latestRelease?.body
                            ?.takeIf { it.isNotBlank() }
                            ?.let { notes ->
                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                InfoTextBlockRow(
                                    label = stringResource(R.string.settings_updates_notes_preview),
                                    value = notes.lineSequence()
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                                        .take(6)
                                        .joinToString("\n"),
                                )
                            }
                    }

                    SettingsCategory.INFO -> SettingsCard(title = stringResource(R.string.settings_category_info_title)) {
                        InfoTextRow(label = tr("Version", "Version"), value = appVersion)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        InfoTextRow(
                            label = stringResource(R.string.settings_issue_report_status_label),
                            value = latestIssueReport?.summary ?: stringResource(R.string.settings_issue_report_status_none),
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingActionRow(
                            title = stringResource(R.string.settings_issue_report_title),
                            subtitle = stringResource(R.string.settings_issue_report_subtitle),
                            actionLabel = stringResource(R.string.settings_issue_report_share),
                            onActionClick = {
                                val shareIntent = LocalIssueReporter.buildShareIntent(context)
                                if (shareIntent != null) {
                                    val chooser = Intent.createChooser(
                                        shareIntent,
                                        context.getString(R.string.settings_issue_report_share),
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivitySafely(context, chooser)
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_issue_report_status_none),
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
                                        context.getString(R.string.settings_issue_report_status_none),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        LinkRow(
                            title = tr("GitHub Repository", "GitHub repository"),
                            subtitle = "darksoon/RadioWave",
                            onClick = { uriHandler.openUri("https://github.com/darksoon/RadioWave") },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        LinkRow(
                            title = tr("Issues / Feedback", "Issues / feedback"),
                            subtitle = tr("Bugs und Feature-Wuensche", "Bugs and feature requests"),
                            onClick = { uriHandler.openUri("https://github.com/darksoon/RadioWave/issues") },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        LinkRow(
                            title = tr("Website", "Website"),
                            subtitle = "sven-neurath.de",
                            onClick = { uriHandler.openUri("https://sven-neurath.de") },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        LinkRow(
                            title = "Ko-fi",
                            subtitle = tr("Support das Projekt", "Support the project"),
                            onClick = { uriHandler.openUri("https://ko-fi.com/darksoon") },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        InfoTextRow(label = tr("Made by", "Made by"), value = "Sven Neurath")
                    }

                    null -> Unit
                }
            }
        }
    }

    if (showUpdateAvailableDialog && updateUiState.latestRelease != null) {
        val release = updateUiState.latestRelease!!
        AlertDialog(
            onDismissRequest = { showUpdateAvailableDialog = false },
            title = { Text(text = tr("Update verfuegbar", "Update available")) },
            text = {
                Column {
                    Text(
                        text = tr(
                            "Neue Version gefunden: ${release.tag}",
                            "New version found: ${release.tag}",
                        ),
                    )
                    release.body
                        .takeIf { it.isNotBlank() }
                        ?.let { notes ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = notes.lineSequence()
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .take(6)
                                    .joinToString("\n"),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    if (updateInstallInProgress) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = tr(
                                "Download laeuft: ${buildUpdateProgressText(updateInstallProgress)}",
                                "Download running: ${buildUpdateProgressText(updateInstallProgress)}",
                            ),
                            color = TealAccent,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val percent = updateInstallProgress?.percent
                        if (percent != null) {
                            LinearProgressIndicator(
                                progress = { percent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (updateInstallInProgress) return@TextButton
                        updateInstallInProgress = true
                        updateInstallProgress = null
                        coroutineScope.launch {
                            val result = GitHubReleaseUpdater.downloadAndStartInstall(
                                context = context,
                                release = release,
                                onProgress = { progress ->
                                    updateInstallProgress = progress
                                },
                            )
                            updateInstallInProgress = false
                            result.onFailure { error ->
                                val message = when (error.message) {
                                    "Please allow installs from unknown apps and try again" -> {
                                        tr(
                                            "Erlaube Installationen aus unbekannten Quellen und tippe dann erneut auf Update.",
                                            "Allow installs from unknown apps, then tap Update again.",
                                        )
                                    }
                                    "No package installer activity found" -> {
                                        tr(
                                            "Auf diesem Geraet wurde kein Paket-Installer gefunden.",
                                            "No package installer was found on this device.",
                                        )
                                    }
                                    else -> {
                                        error.message ?: tr("unbekannt", "unknown")
                                    }
                                }
                                Toast.makeText(
                                    context,
                                    tr(
                                        "Update fehlgeschlagen: $message",
                                        "Update failed: $message",
                                    ),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                ) {
                    Text(text = tr(if (updateInstallInProgress) "Lade..." else "Update", if (updateInstallInProgress) "Downloading..." else "Update"))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            if (updateInstallInProgress) return@TextButton
                            uriHandler.openUri(release.htmlUrl)
                        },
                    ) {
                        Text(text = tr("Release-Seite", "Release page"))
                    }
                    TextButton(
                        onClick = {
                            if (updateInstallInProgress) return@TextButton
                            showUpdateAvailableDialog = false
                        },
                    ) {
                        Text(text = tr("Spaeter", "Later"))
                    }
                }
            },
        )
    }
}

private enum class SettingsCategory {
    GENERAL,
    SOUND,
    NOTIFICATION,
    DATA,
    UPDATES,
    INFO,
}

private fun applyAppLanguage(language: String) {
    val locales = when (language) {
        AppSettings.LANGUAGE_DE -> LocaleListCompat.forLanguageTags("de")
        AppSettings.LANGUAGE_EN -> LocaleListCompat.forLanguageTags("en")
        else -> LocaleListCompat.getEmptyLocaleList()
    }
    AppCompatDelegate.setApplicationLocales(locales)
}

private fun categoryTitle(category: SettingsCategory, context: Context): String {
    return when (category) {
        SettingsCategory.GENERAL -> context.getString(R.string.settings_category_general_title)
        SettingsCategory.SOUND -> context.getString(R.string.settings_category_sound_title)
        SettingsCategory.NOTIFICATION -> context.getString(R.string.settings_category_notification_title)
        SettingsCategory.DATA -> context.getString(R.string.settings_category_data_title)
        SettingsCategory.UPDATES -> context.getString(R.string.settings_category_updates_title)
        SettingsCategory.INFO -> context.getString(R.string.settings_category_info_title)
    }
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCardBackground.copy(alpha = 0.62f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TealAccent,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant,
                )
            }
            Text(
                text = ">",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun SettingsDetailHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
                tint = Color.White,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCardBackground.copy(alpha = 0.62f),
        ),
    ) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TealAccent,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
            content()
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    disabledHint: String? = null,
) {
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
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.58f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (enabled || disabledHint == null) subtitle else "$subtitle  ($disabledHint)",
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) DarkOnSurfaceVariant else DarkOnSurfaceVariant.copy(alpha = 0.6f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun SettingChoiceRow(
    title: String,
    options: List<ThemeOption>,
    selectedValue: String,
    onSelected: (String) -> Unit,
    enabled: Boolean = true,
    disabledHint: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.58f),
        )
        if (!enabled && disabledHint != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = disabledHint,
                style = MaterialTheme.typography.bodySmall,
                color = DarkOnSurfaceVariant.copy(alpha = 0.6f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option.value == selectedValue,
                    onClick = { onSelected(option.value) },
                    enabled = enabled,
                    label = { Text(option.label) },
                )
            }
        }
    }
}

@Composable
private fun LinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = DarkOnSurfaceVariant,
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
    secondaryActionLabel: String,
    onSecondaryActionClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = DarkOnSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onActionClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(actionLabel)
            }
            OutlinedButton(
                onClick = onSecondaryActionClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(secondaryActionLabel)
            }
        }
    }

}

@Composable
private fun InfoTextRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = DarkOnSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
    }
}

private data class ThemeOption(
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

@Composable
private fun InfoTextBlockRow(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = DarkOnSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
        )
    }
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

private fun buildUpdateCheckSubtitle(context: Context, lastCheckedAtMs: Long?, error: String?): String {
    val timePart = lastCheckedAtMs?.let {
        val formatted = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(it))
        context.getString(R.string.settings_update_last_check, formatted)
    } ?: context.getString(R.string.settings_update_never_checked)
    return if (error.isNullOrBlank()) {
        timePart
    } else {
        "$timePart  ${context.getString(R.string.settings_update_error_prefix, error)}"
    }
}

private fun buildUpdateProgressText(progress: UpdateDownloadProgress?): String {
    if (progress == null) return "0 MB"
    val downloadedMb = progress.downloadedBytes / (1024f * 1024f)
    val totalMb = progress.totalBytes.takeIf { it > 0L }?.let { it / (1024f * 1024f) }
    return if (totalMb != null) {
        String.format("%.1f / %.1f MB (%d%%)", downloadedMb, totalMb, progress.percent ?: 0)
    } else {
        String.format("%.1f MB", downloadedMb)
    }
}

private fun openAndroidAutoApp(context: Context) {
    val packageName = "com.google.android.projection.gearhead"
    val pm = context.packageManager
    val launchIntent = pm.getLaunchIntentForPackage(packageName)
    if (launchIntent != null && startActivitySafely(context, launchIntent)) {
        return
    }

    val settingsIntent = Intent("com.google.android.projection.gearhead.SETTINGS")
        .setPackage(packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (startActivitySafely(context, settingsIntent)) {
        return
    }

    val appDetailsIntent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:$packageName"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (startActivitySafely(context, appDetailsIntent)) {
        Toast.makeText(
            context,
            context.getString(R.string.settings_android_auto_fallback_toast),
            Toast.LENGTH_SHORT,
        ).show()
        return
    }

    Toast.makeText(context, context.getString(R.string.settings_android_auto_not_available), Toast.LENGTH_SHORT).show()
}

private fun startActivitySafely(context: Context, intent: Intent): Boolean {
    return runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

