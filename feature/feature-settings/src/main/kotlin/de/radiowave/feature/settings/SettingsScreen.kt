package de.radiowave.feature.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.radiowave.core.model.AppSettings
import de.radiowave.core.ui.theme.DarkBackground
import de.radiowave.core.ui.theme.DarkCardBackground
import de.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.radiowave.core.ui.theme.TealAccent

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)

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
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_SHOW_INSECURE_STREAMS, false))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
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
                text = "Basisfunktionen fuer Wiedergabe und UI",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkOnSurfaceVariant,
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkCardBackground.copy(alpha = 0.86f),
                ),
            ) {
                Column(modifier = Modifier.padding(vertical = 2.dp)) {
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
                        title = "Screen im Fullscreen-Player anlassen",
                        subtitle = "Verhindert Display-Timeout im Player.",
                        checked = keepScreenOnFullscreen,
                        onCheckedChange = { checked ->
                            keepScreenOnFullscreen = checked
                            prefs.edit().putBoolean(AppSettings.KEY_KEEP_SCREEN_ON_FULLSCREEN, checked).apply()
                        },
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    SettingToggleRow(
                        title = "Kurze Hinweise anzeigen",
                        subtitle = "Zeigt Add/Remove-Feedback als Toast.",
                        checked = showQuickToasts,
                        onCheckedChange = { checked ->
                            showQuickToasts = checked
                            prefs.edit().putBoolean(AppSettings.KEY_SHOW_QUICK_TOASTS, checked).apply()
                        },
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    SettingToggleRow(
                        title = "Unsichere HTTP-Streams anzeigen",
                        subtitle = "Wenn aus, werden HTTP-Sender in Suche/Entdecken ausgeblendet.",
                        checked = showInsecureStreams,
                        onCheckedChange = { checked ->
                            showInsecureStreams = checked
                            prefs.edit().putBoolean(AppSettings.KEY_SHOW_INSECURE_STREAMS, checked).apply()
                        },
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkCardBackground.copy(alpha = 0.7f),
                ),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = "Info",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TealAccent,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Login/Favoriten-Sync kann spaeter als separates Feature in diesem Bereich folgen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkOnSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
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
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = DarkOnSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
