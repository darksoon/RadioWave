// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.ui.theme

import androidx.compose.ui.graphics.Color

// REA 95fcb33: rekonstruiert aus p000/jb0.java (Handy) ↔ p000/dc0.java (Release 944fec5).
// Reihenfolge = Reihenfolge der Initialisierung im <clinit> von jb0, Werte exakt.
// Namen der bereits in 1.0.19 vorhandenen Konstanten sind übernommen; neue Namen sind
// sinngemäß vergeben (R8 hat die Originalnamen entfernt).

// Dark purple / white brand palette
val DarkBackground = Color(0xFF14111D)
val DarkSurface = Color(0xFF1C1730)
val DarkSurfaceVariant = Color(0xFF27203D)
val DarkCardBackground = Color(0xFF221B35)

// Purple / soft-white accent colors
val RadioAccent = Color(0xFFB388FF)
val RadioAccentLight = Color(0xFFD6BDFF)

// REA-UNSICHER: Name geraten. Neu in 95fcb33 (jb0.f9681f), steht zwischen RadioAccentLight
// und MintAccent. Wird als zweite Farbe eines Verlaufs RadioAccentLight → 0xFF7C4DFF
// (Kreis 84.dp, p000/x91.java:201) und als tertiary im Light-Scheme genutzt.
val RadioAccentDeep = Color(0xFF7C4DFF)

val MintAccent = Color(0xFFF3EEFF)
val MintLight = Color(0xFFFFFFFF)

// Additional UI Colors
val DarkOnSurface = Color(0xFFF6F3FF)
val DarkOnSurfaceVariant = Color(0xFFD0C9E3)
val DarkOverlay = Color(0x99000000)
val DarkBorder = Color(0xFF4B4462)

val LightBackground = Color(0xFFFCFAFF)
val LightSurface = Color(0xFFF5F1FB)
val LightSurfaceVariant = Color(0xFFEEE8F8)
val LightCardBackground = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1F1A2B)
val LightOnSurfaceVariant = Color(0xFF665F78)
val LightBorder = Color(0xFFD9D0EA)

// REA 95fcb33: neue Status-/Signalfarben. In 1.0.19 standen 0xFFFF8A8A und 0xFFFFC266 noch
// als Literale in PlayerScreen.kt (statusColor()); im Handy-Build liegen sie als Felder in
// der Color.kt-Klasse (jb0) und werden von dort referenziert.
// REA-UNSICHER: alle fünf Namen geraten, Werte und Reihenfolge sicher.

/** LIVE-Badge/-Punkt (p000/C0299h5.java:2081 und :3636, Hintergrund 16 % / Rand 45 % Alpha). */
val LiveRed = Color(0xFFFF6B7D)

/** Fehler-/„Getrennt“-Text (LiveStatus.Disconnected, player_error_retry). */
val ErrorSoft = Color(0xFFFF8A8A)

/** Warnung (LiveStatus.WifiRequired). */
val WarningAmber = Color(0xFFFFC266)

// REA-UNSICHER: im Handy-Build nirgends referenziert (nur die Konstruktion im <clinit> blieb
// erhalten) – im Quellcode also deklariert, aber ungenutzt.
val SuccessGreen = Color(0xFF57D9A3)
val DangerRed = Color(0xFFFF5A6E)

/**
 * Gemeinsamer Verlauf der Genre-Kacheln (jb0.f9695t = listOf(...)).
 * p000/AbstractC0920xz.java:94-259: alle vier Genre-Items (techno, rock, jazz, "chill")
 * bekommen dieselbe Liste – die individuellen Verläufe aus BrowseScreen.kt (1.0.19) sind weg.
 * REA-UNSICHER: Name geraten.
 */
val GenreGradient = listOf(Color(0xFF6C4BD6), Color(0xFF3A2A6B))
