// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.darksoon.radiowave.core.model.AppSettings
import de.darksoon.radiowave.core.data.update.LocalIssueReporter
import de.darksoon.radiowave.core.ui.theme.DarkCardBackground
import de.darksoon.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.darksoon.radiowave.core.ui.theme.RadioAccent
import de.darksoon.radiowave.feature.settings.R

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onRestartOnboarding: () -> Unit = {},
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val prefs = context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)
    var issueReportRefreshKey by remember { mutableLongStateOf(0L) }
    // Read the crash report file on IO to avoid blocking the composition thread.
    val latestIssueReport by produceState<de.darksoon.radiowave.core.data.update.LocalIssueReport?>(
        initialValue = null,
        key1 = context,
        key2 = issueReportRefreshKey,
    ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            LocalIssueReporter.getLatestReport(context)
        }
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
    var limitAndroidAutoQuality by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_LIMIT_ANDROID_AUTO_QUALITY, true))
    }
    var confirmRemoveFavorite by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_CONFIRM_REMOVE_FAVORITE, false))
    }
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

    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
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
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
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
                SettingsSectionLabel(text = stringResource(R.string.settings_home_customize_title))
            }
            item {
                SettingsCategoryCard(
                    title = stringResource(R.string.settings_category_general_title),
                    subtitle = stringResource(R.string.settings_category_general_subtitle),
                    icon = Icons.Filled.Tune,
                    onClick = { selectedCategory = SettingsCategory.GENERAL },
                )
            }
            item {
                SettingsCategoryCard(
                    title = stringResource(R.string.settings_category_sound_title),
                    subtitle = stringResource(R.string.settings_category_sound_subtitle),
                    icon = Icons.Filled.VolumeUp,
                    onClick = { selectedCategory = SettingsCategory.SOUND },
                )
            }
            item {
                SettingsCategoryCard(
                    title = stringResource(R.string.settings_category_notification_title),
                    subtitle = stringResource(R.string.settings_category_notification_subtitle),
                    icon = Icons.Filled.Notifications,
                    onClick = { selectedCategory = SettingsCategory.NOTIFICATION },
                )
            }
            item {
                SettingsCategoryCard(
                    title = stringResource(R.string.settings_category_data_title),
                    subtitle = stringResource(R.string.settings_category_data_subtitle),
                    icon = Icons.Filled.Storage,
                    onClick = { selectedCategory = SettingsCategory.DATA },
                )
            }
            item {
                SettingsCategoryCard(
                    title = stringResource(R.string.settings_category_info_title),
                    subtitle = stringResource(R.string.settings_category_info_subtitle),
                    icon = Icons.Filled.Info,
                    onClick = { selectedCategory = SettingsCategory.INFO },
                )
            }
            item {
                SettingsSectionLabel(text = stringResource(R.string.settings_home_support_title))
            }
            item {
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
                    secondaryActionLabel = null,
                    onSecondaryActionClick = null,
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
                            title = stringResource(R.string.settings_theme_title),
                            options = listOf(
                                ThemeOption(AppSettings.THEME_DARK, stringResource(R.string.settings_theme_dark)),
                            ),
                            selectedValue = themeMode,
                            onSelected = { value ->
                                themeMode = value
                                prefs.edit().putString(AppSettings.KEY_THEME_MODE, value).apply()
                            },
                            enabled = false,
                            disabledHint = stringResource(R.string.settings_theme_dark_only),
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
                            title = stringResource(R.string.settings_dynamic_colors_title),
                            subtitle = stringResource(R.string.settings_dynamic_colors_subtitle),
                            checked = dynamicColors,
                            onCheckedChange = { checked ->
                                dynamicColors = checked
                                prefs.edit().putBoolean(AppSettings.KEY_DYNAMIC_COLORS, checked).apply()
                            },
                            enabled = dynamicColorsSupported,
                            disabledHint = stringResource(R.string.settings_dynamic_colors_disabled),
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = stringResource(R.string.settings_quick_hints_title),
                            subtitle = stringResource(R.string.settings_quick_hints_subtitle),
                            checked = showQuickToasts,
                            onCheckedChange = { checked ->
                                showQuickToasts = checked
                                prefs.edit().putBoolean(AppSettings.KEY_SHOW_QUICK_TOASTS, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = stringResource(R.string.settings_confirm_remove_favorite_title),
                            subtitle = stringResource(R.string.settings_confirm_remove_favorite_subtitle),
                            checked = confirmRemoveFavorite,
                            onCheckedChange = { checked ->
                                confirmRemoveFavorite = checked
                                prefs.edit().putBoolean(AppSettings.KEY_CONFIRM_REMOVE_FAVORITE, checked).apply()
                            },
                        )
                    }

                    SettingsCategory.SOUND -> SettingsCard(title = stringResource(R.string.settings_category_sound_title)) {
                        SettingToggleRow(
                            title = stringResource(R.string.settings_mini_player_metadata_title),
                            subtitle = stringResource(R.string.settings_mini_player_metadata_subtitle),
                            checked = showMiniPlayerMetadata,
                            onCheckedChange = { checked ->
                                showMiniPlayerMetadata = checked
                                prefs.edit().putBoolean(AppSettings.KEY_SHOW_MINIPLAYER_METADATA, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = stringResource(R.string.settings_keep_screen_on_title),
                            subtitle = stringResource(R.string.settings_keep_screen_on_subtitle),
                            checked = keepScreenOnFullscreen,
                            onCheckedChange = { checked ->
                                keepScreenOnFullscreen = checked
                                prefs.edit().putBoolean(AppSettings.KEY_KEEP_SCREEN_ON_FULLSCREEN, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingChoiceRow(
                            title = stringResource(R.string.settings_default_quality_title),
                            options = listOf(
                                ThemeOption(AppSettings.QUALITY_AUTO, stringResource(R.string.settings_quality_auto)),
                                ThemeOption(AppSettings.QUALITY_LOW, stringResource(R.string.settings_quality_low)),
                                ThemeOption(AppSettings.QUALITY_MEDIUM, stringResource(R.string.settings_quality_medium)),
                                ThemeOption(AppSettings.QUALITY_HIGH, stringResource(R.string.settings_quality_high)),
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
                        Text(
                            text = stringResource(R.string.settings_sound_section_android_auto),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = RadioAccent,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                        SettingToggleRow(
                            title = stringResource(R.string.settings_android_auto_quality_limit_title),
                            subtitle = stringResource(R.string.settings_android_auto_quality_limit_subtitle),
                            checked = limitAndroidAutoQuality,
                            onCheckedChange = { checked ->
                                limitAndroidAutoQuality = checked
                                prefs.edit()
                                    .putBoolean(AppSettings.KEY_LIMIT_ANDROID_AUTO_QUALITY, checked)
                                    .apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = stringResource(R.string.settings_auto_play_title),
                            subtitle = stringResource(R.string.settings_auto_play_subtitle),
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
                            title = stringResource(R.string.settings_notification_play_pause_title),
                            subtitle = stringResource(R.string.settings_notification_play_pause_subtitle),
                            checked = showNotificationPlayPause,
                            onCheckedChange = { checked ->
                                showNotificationPlayPause = checked
                                prefs.edit().putBoolean(AppSettings.KEY_NOTIFICATION_SHOW_PLAY_PAUSE, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = stringResource(R.string.settings_notification_previous_title),
                            subtitle = stringResource(R.string.settings_notification_previous_subtitle),
                            checked = showNotificationPrevious,
                            onCheckedChange = { checked ->
                                showNotificationPrevious = checked
                                prefs.edit().putBoolean(AppSettings.KEY_NOTIFICATION_SHOW_PREVIOUS, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = stringResource(R.string.settings_notification_next_title),
                            subtitle = stringResource(R.string.settings_notification_next_subtitle),
                            checked = showNotificationNext,
                            onCheckedChange = { checked ->
                                showNotificationNext = checked
                                prefs.edit().putBoolean(AppSettings.KEY_NOTIFICATION_SHOW_NEXT, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = stringResource(R.string.settings_notification_stop_title),
                            subtitle = stringResource(R.string.settings_notification_stop_subtitle),
                            checked = showNotificationStop,
                            onCheckedChange = { checked ->
                                showNotificationStop = checked
                                prefs.edit().putBoolean(AppSettings.KEY_NOTIFICATION_SHOW_STOP, checked).apply()
                            },
                        )
                    }

                    SettingsCategory.DATA -> SettingsCard(title = stringResource(R.string.settings_category_data_title)) {
                        SettingToggleRow(
                            title = stringResource(R.string.settings_mobile_data_title),
                            subtitle = stringResource(R.string.settings_mobile_data_subtitle),
                            checked = allowMobileData,
                            onCheckedChange = { checked ->
                                allowMobileData = checked
                                prefs.edit().putBoolean(AppSettings.KEY_ALLOW_MOBILE_DATA, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingChoiceRow(
                            title = stringResource(R.string.settings_buffer_profile_title),
                            options = listOf(
                                ThemeOption(AppSettings.BUFFER_SMALL, stringResource(R.string.settings_buffer_small)),
                                ThemeOption(AppSettings.BUFFER_MEDIUM, stringResource(R.string.settings_buffer_medium)),
                                ThemeOption(AppSettings.BUFFER_LARGE, stringResource(R.string.settings_buffer_large)),
                            ),
                            selectedValue = bufferProfile,
                            onSelected = { value ->
                                bufferProfile = value
                                prefs.edit().putString(AppSettings.KEY_BUFFER_PROFILE, value).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = stringResource(R.string.settings_outage_buffer_title),
                            subtitle = stringResource(R.string.settings_outage_buffer_subtitle),
                            checked = timeshiftGuard,
                            onCheckedChange = { checked ->
                                timeshiftGuard = checked
                                prefs.edit().putBoolean(AppSettings.KEY_TIMESHIFT_GUARD, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = stringResource(R.string.settings_thermal_mode_title),
                            subtitle = stringResource(R.string.settings_thermal_mode_subtitle),
                            checked = thermalMode,
                            onCheckedChange = { checked ->
                                thermalMode = checked
                                prefs.edit().putBoolean(AppSettings.KEY_THERMAL_MODE, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = stringResource(R.string.settings_insecure_streams_title),
                            subtitle = stringResource(R.string.settings_insecure_streams_subtitle),
                            checked = showInsecureStreams,
                            onCheckedChange = { checked ->
                                showInsecureStreams = checked
                                prefs.edit().putBoolean(AppSettings.KEY_SHOW_INSECURE_STREAMS, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
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
                                    Toast.makeText(context, context.getString(R.string.settings_station_cache_cleared), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.settings_clear_cache))
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearHistory()
                                    Toast.makeText(context, context.getString(R.string.settings_history_cleared), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.settings_clear_history))
                            }
                        }
                    }

                    SettingsCategory.INFO -> SettingsCard(title = stringResource(R.string.settings_category_info_title)) {
                        InfoTextRow(label = stringResource(R.string.settings_version_label), value = appVersion)
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
                            title = stringResource(R.string.settings_github_repository_title),
                            subtitle = stringResource(R.string.settings_github_repository_value),
                            onClick = { uriHandler.openUri("https://github.com/darksoon/RadioWave") },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        LinkRow(
                            title = stringResource(R.string.settings_issues_title),
                            subtitle = stringResource(R.string.settings_issues_subtitle),
                            onClick = { uriHandler.openUri("https://github.com/darksoon/RadioWave/issues") },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        LinkRow(
                            title = stringResource(R.string.settings_website_title),
                            subtitle = stringResource(R.string.settings_website_value),
                            onClick = { uriHandler.openUri("https://radiowave.sven-neurath.de") },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingActionRow(
                            title = stringResource(R.string.settings_onboarding_title),
                            subtitle = stringResource(R.string.settings_onboarding_subtitle),
                            actionLabel = stringResource(R.string.settings_onboarding_action),
                            onActionClick = onRestartOnboarding,
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        LinkRow(
                            title = stringResource(R.string.settings_kofi_title),
                            subtitle = stringResource(R.string.settings_kofi_subtitle),
                            onClick = { uriHandler.openUri("https://ko-fi.com/darksoon") },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        InfoTextRow(
                            label = stringResource(R.string.settings_made_by_label),
                            value = stringResource(R.string.settings_made_by_value),
                        )
                    }

                    null -> Unit
                }
            }
        }
    }

}

private enum class SettingsCategory {
    GENERAL,
    SOUND,
    NOTIFICATION,
    DATA,
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
        SettingsCategory.INFO -> context.getString(R.string.settings_category_info_title)
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = RadioAccent,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

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
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.07f),
                            Color.White.copy(alpha = 0.03f),
                        ),
                    ),
                )
                .padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkOnSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = RadioAccent.copy(alpha = 0.14f),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = donateTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = donateSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onPrimaryActionClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(primaryActionLabel)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.05f),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = thankYouTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = RadioAccent,
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
                OutlinedButton(
                    onClick = onSecondaryActionClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
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
    val annotatedText = remember(text, kevinLinkLabel, groupLinkLabel) {
        buildAnnotatedString {
            append(text)

            val linkStyle = SpanStyle(
                color = RadioAccent,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Medium,
            )

            val kevinStart = text.indexOf(kevinLinkLabel)
            if (kevinStart >= 0) {
                val kevinEnd = kevinStart + kevinLinkLabel.length
                addStyle(linkStyle, kevinStart, kevinEnd)
                addStringAnnotation("URL", "kevin", kevinStart, kevinEnd)
            }

            val groupStart = text.indexOf(groupLinkLabel)
            if (groupStart >= 0) {
                val groupEnd = groupStart + groupLinkLabel.length
                addStyle(linkStyle, groupStart, groupEnd)
                addStringAnnotation("URL", "group", groupStart, groupEnd)
            }
        }
    }

    ClickableText(
        text = annotatedText,
        style = MaterialTheme.typography.bodySmall.copy(color = DarkOnSurfaceVariant),
        onClick = { offset ->
            annotatedText
                .getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()
                ?.let { annotation ->
                    when (annotation.item) {
                        "kevin" -> onKevinLinkClick()
                        "group" -> onGroupLinkClick()
                    }
                }
        },
    )
}

@Composable
private fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.03f),
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
                    .background(RadioAccent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RadioAccent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(20.dp),
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
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.07f),
                            Color.White.copy(alpha = 0.03f),
                        ),
                    ),
                )
                .padding(top = 10.dp, bottom = 4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = RadioAccent,
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
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
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
            if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                OutlinedButton(
                    onClick = onSecondaryActionClick,
                    modifier = Modifier.weight(1f),
                ) {
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

