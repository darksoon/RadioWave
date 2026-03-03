package de.radiowave.feature.settings

import android.content.Context
import android.os.Build
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import de.radiowave.core.model.AppSettings
import de.radiowave.core.ui.theme.DarkCardBackground
import de.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.radiowave.core.ui.theme.TealAccent

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val prefs = context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)

    var themeMode by rememberSaveable {
        mutableStateOf(prefs.getString(AppSettings.KEY_THEME_MODE, AppSettings.THEME_SYSTEM) ?: AppSettings.THEME_SYSTEM)
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
    var autoPlayOnAndroidAutoConnect by rememberSaveable {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_AUTO_PLAY_ON_ANDROID_AUTO_CONNECT, true))
    }

    val appVersion = rememberAppVersion(context)
    val dynamicColorsSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var selectedCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Einstellungen",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                ),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Uebersichtlich: Allgemein, Sound, Benachrichtigung, Daten, Info",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkOnSurfaceVariant,
            )
        }

        if (selectedCategory == null) {
            item {
                SettingsCategoryCard(
                    title = "Allgemein",
                    subtitle = "Theme, Dynamic Colors, Hinweise",
                    onClick = { selectedCategory = SettingsCategory.GENERAL },
                )
            }
            item {
                SettingsCategoryCard(
                    title = "Sound",
                    subtitle = "Mini-Player, Qualitaet, Vollbild",
                    onClick = { selectedCategory = SettingsCategory.SOUND },
                )
            }
            item {
                SettingsCategoryCard(
                    title = "Benachrichtigung",
                    subtitle = "Buttons in der Notification",
                    onClick = { selectedCategory = SettingsCategory.NOTIFICATION },
                )
            }
            item {
                SettingsCategoryCard(
                    title = "Speicher & Daten",
                    subtitle = "Buffer, Streams, Cache, Verlauf",
                    onClick = { selectedCategory = SettingsCategory.DATA },
                )
            }
            item {
                SettingsCategoryCard(
                    title = "Info",
                    subtitle = "Version, Links, Projekt",
                    onClick = { selectedCategory = SettingsCategory.INFO },
                )
            }
        } else {
            item {
                SettingsDetailHeader(
                    title = selectedCategory!!.title,
                    onBack = { selectedCategory = null },
                )
            }
            item {
                when (selectedCategory) {
                    SettingsCategory.GENERAL -> SettingsCard(title = "Allgemein") {
                        SettingChoiceRow(
                            title = "Theme",
                            options = listOf(
                                ThemeOption(AppSettings.THEME_SYSTEM, "System"),
                                ThemeOption(AppSettings.THEME_DARK, "Dark"),
                                ThemeOption(AppSettings.THEME_LIGHT, "Light"),
                            ),
                            selectedValue = themeMode,
                            onSelected = { value ->
                                themeMode = value
                                prefs.edit().putString(AppSettings.KEY_THEME_MODE, value).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = "Dynamic Colors",
                            subtitle = "Material You Farben (Android 12+).",
                            checked = dynamicColors,
                            onCheckedChange = { checked ->
                                dynamicColors = checked
                                prefs.edit().putBoolean(AppSettings.KEY_DYNAMIC_COLORS, checked).apply()
                            },
                            enabled = dynamicColorsSupported,
                            disabledHint = "Nur auf Android 12+ verfuegbar",
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = "Kurze Hinweise anzeigen",
                            subtitle = "Zeigt Add/Remove Feedback als Toast.",
                            checked = showQuickToasts,
                            onCheckedChange = { checked ->
                                showQuickToasts = checked
                                prefs.edit().putBoolean(AppSettings.KEY_SHOW_QUICK_TOASTS, checked).apply()
                            },
                        )
                    }

                    SettingsCategory.SOUND -> SettingsCard(title = "Sound") {
                        SettingToggleRow(
                            title = "Metadaten im Mini-Player",
                            subtitle = "Zeigt Artist/Titel unter dem Sendernamen.",
                            checked = showMiniPlayerMetadata,
                            onCheckedChange = { checked ->
                                showMiniPlayerMetadata = checked
                                prefs.edit().putBoolean(AppSettings.KEY_SHOW_MINIPLAYER_METADATA, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = "Display anlassen im Vollbild-Player",
                            subtitle = "Verhindert das Dimmen waehrend Vollbild-Wiedergabe.",
                            checked = keepScreenOnFullscreen,
                            onCheckedChange = { checked ->
                                keepScreenOnFullscreen = checked
                                prefs.edit().putBoolean(AppSettings.KEY_KEEP_SCREEN_ON_FULLSCREEN, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingChoiceRow(
                            title = "Standard-Qualitaet",
                            options = listOf(
                                ThemeOption(AppSettings.QUALITY_AUTO, "Auto"),
                                ThemeOption(AppSettings.QUALITY_LOW, "Niedrig"),
                                ThemeOption(AppSettings.QUALITY_MEDIUM, "Mittel"),
                                ThemeOption(AppSettings.QUALITY_HIGH, "Hoch"),
                            ),
                            selectedValue = defaultAudioQuality,
                            onSelected = { value ->
                                defaultAudioQuality = value
                                prefs.edit().putString(AppSettings.KEY_DEFAULT_AUDIO_QUALITY, value).apply()
                            },
                            enabled = false,
                            disabledHint = "Noch nicht aktiv im Player",
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = "Autoplay bei Android Auto Verbindung",
                            subtitle = "Startet den zuletzt gehoerten Sender automatisch beim Auto-Connect.",
                            checked = autoPlayOnAndroidAutoConnect,
                            onCheckedChange = { checked ->
                                autoPlayOnAndroidAutoConnect = checked
                                prefs.edit()
                                    .putBoolean(AppSettings.KEY_AUTO_PLAY_ON_ANDROID_AUTO_CONNECT, checked)
                                    .apply()
                            },
                        )
                    }

                    SettingsCategory.NOTIFICATION -> SettingsCard(title = "Benachrichtigung") {
                        SettingToggleRow(
                            title = "Play/Pause Button",
                            subtitle = "Steuerung direkt in der Medien-Benachrichtigung.",
                            checked = showNotificationPlayPause,
                            onCheckedChange = { checked ->
                                showNotificationPlayPause = checked
                                prefs.edit().putBoolean(AppSettings.KEY_NOTIFICATION_SHOW_PLAY_PAUSE, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = "Previous Button",
                            subtitle = "Vorherigen Sender aus Verlauf abspielen.",
                            checked = showNotificationPrevious,
                            onCheckedChange = { checked ->
                                showNotificationPrevious = checked
                                prefs.edit().putBoolean(AppSettings.KEY_NOTIFICATION_SHOW_PREVIOUS, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = "Next Button",
                            subtitle = "Naechsten Sender aus Verlauf/Top-Sendern starten.",
                            checked = showNotificationNext,
                            onCheckedChange = { checked ->
                                showNotificationNext = checked
                                prefs.edit().putBoolean(AppSettings.KEY_NOTIFICATION_SHOW_NEXT, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = "Stop Button",
                            subtitle = "Streaming sofort beenden und Notification entfernen.",
                            checked = showNotificationStop,
                            onCheckedChange = { checked ->
                                showNotificationStop = checked
                                prefs.edit().putBoolean(AppSettings.KEY_NOTIFICATION_SHOW_STOP, checked).apply()
                            },
                        )
                    }

                    SettingsCategory.DATA -> SettingsCard(title = "Speicher & Daten") {
                        SettingToggleRow(
                            title = "Mobile Daten erlauben",
                            subtitle = "Wenn aus, streamt die App nur im WLAN.",
                            checked = allowMobileData,
                            onCheckedChange = { checked ->
                                allowMobileData = checked
                                prefs.edit().putBoolean(AppSettings.KEY_ALLOW_MOBILE_DATA, checked).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingChoiceRow(
                            title = "Buffer-Profil",
                            options = listOf(
                                ThemeOption(AppSettings.BUFFER_SMALL, "Klein"),
                                ThemeOption(AppSettings.BUFFER_MEDIUM, "Mittel"),
                                ThemeOption(AppSettings.BUFFER_LARGE, "Gross"),
                            ),
                            selectedValue = bufferProfile,
                            onSelected = { value ->
                                bufferProfile = value
                                prefs.edit().putString(AppSettings.KEY_BUFFER_PROFILE, value).apply()
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        SettingToggleRow(
                            title = "Unsichere HTTP-Streams anzeigen",
                            subtitle = "Wenn aus, werden HTTP-Sender ausgeblendet.",
                            checked = showInsecureStreams,
                            onCheckedChange = { checked ->
                                showInsecureStreams = checked
                                prefs.edit().putBoolean(AppSettings.KEY_SHOW_INSECURE_STREAMS, checked).apply()
                            },
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
                                    Toast.makeText(context, "Sender-Cache geleert", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Cache leeren")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.clearHistory()
                                    Toast.makeText(context, "Verlauf geloescht", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("History loeschen")
                            }
                        }
                    }

                    SettingsCategory.INFO -> SettingsCard(title = "Info") {
                        InfoTextRow(label = "Version", value = appVersion)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        LinkRow(
                            title = "GitHub Repository",
                            subtitle = "darksoon/RadioWave",
                            onClick = { uriHandler.openUri("https://github.com/darksoon/RadioWave") },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        LinkRow(
                            title = "Issues / Feedback",
                            subtitle = "Bugs und Feature-Wuensche",
                            onClick = { uriHandler.openUri("https://github.com/darksoon/RadioWave/issues") },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        LinkRow(
                            title = "Website",
                            subtitle = "sven-neurath.de",
                            onClick = { uriHandler.openUri("https://sven-neurath.de") },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        LinkRow(
                            title = "Buy a Coffee",
                            subtitle = "Support das Projekt",
                            onClick = { uriHandler.openUri("https://buymeacoffee.com/darksoon") },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        InfoTextRow(label = "Made by", value = "Sven Neurath")
                    }

                    null -> Unit
                }
            }
        }
    }
}

private enum class SettingsCategory(val title: String) {
    GENERAL("Allgemein"),
    SOUND("Sound"),
    NOTIFICATION("Benachrichtigung"),
    DATA("Speicher & Daten"),
    INFO("Info"),
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
                contentDescription = "Zurueck",
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
            Text("Open")
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
            packageInfo.versionName ?: "unbekannt"
        } catch (_: Exception) {
            "unbekannt"
        }
    }
}
