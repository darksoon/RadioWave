// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// REA 95fcb33: beide Schemata aus p000/u25.java <clinit> rekonstruiert (Parameterreihenfolge
// über die bekannte Release-Klasse p000/n65.java ↔ 1.0.19 Theme.kt verifiziert). Sicherheit: hoch.
// Die Literale (0xFF3E285F usw.) stehen nicht in jb0 (Color.kt), sondern direkt in u25 → im
// Original vermutlich inline im Aufruf. REA-UNSICHER: ob sie als private vals in Theme.kt standen.
private val PremiumDarkColorScheme = darkColorScheme(
    primary = RadioAccent,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3E285F), // 1.0.19: RadioAccentLight
    onPrimaryContainer = Color(0xFFECDDFF), // 1.0.19: Color.Black
    secondary = MintAccent,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant, // 1.0.19: MintLight
    onSecondaryContainer = DarkOnSurface, // 1.0.19: Color.Black
    tertiary = RadioAccent,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
)

// REA 95fcb33: Light-Schema komplett neu abgestimmt (1.0.19 benutzte die Dark-Akzente auch im
// Hellmodus). Neu, weil Hell/System jetzt in den Einstellungen wählbar ist
// (settings_theme_light / settings_theme_system statt settings_theme_dark_only).
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6A3FD6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF2A1B4D),
    secondary = Color(0xFF5B4B8A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7F6),
    onSecondaryContainer = Color(0xFF1F1A2B), // Wert = LightOnSurface, aber als Literal in u25
    tertiary = Color(0xFF7C4DFF), // Wert = RadioAccentDeep, aber als Literal in u25
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightBorder,
    outlineVariant = LightBorder,
)

// REA 95fcb33: Signatur, Dynamic-Color-Zweig, SideEffect (Status-/Navigationsleisten-Icons)
// und MaterialTheme-Aufruf unverändert (p000/u25.m9906a ↔ p000/n65.m7871a; shapes = Default).
@Composable
fun RadioWaveTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> PremiumDarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

