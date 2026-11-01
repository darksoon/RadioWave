// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.player

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import de.darksoon.radiowave.core.ui.theme.DarkBackground

// REA 95fcb33: neue Datei.
// Farbbestimmung = p000/em2.java m3006r (Zeilen 1379-1450) + m3008u (1461-1463);
// Aufruf auf Dispatchers.Default in p000/C0475lz.java Fall 12 (Fehler → DarkBackground);
// Cache = p000/AbstractC0832vm.java f22413a (SnapshotStateMap), gefüllt in
// p000/C0825vf.java Fall 4 (max. 24 Einträge, ältester fliegt raus), gelesen vom Mini-Player
// (p000/AbstractC0395jt.java:1993-1997).
// Der Anfang von m3006r ist exakt das inline-expandierte androidx.core Drawable.toBitmap(48, 48)
// (BitmapDrawable → ggf. createScaledBitmap, sonst auf ARGB_8888-Bitmap zeichnen).
// REA-UNSICHER: Datei-/Funktionsnamen geraten.

/** Last artwork colours by image URL, shared by the full player and the mini player. */
internal val ArtworkColorCache: SnapshotStateMap<String, Color> = mutableStateMapOf()

private const val ARTWORK_COLOR_CACHE_SIZE = 24
private const val SAMPLE_SIZE = 48

/** Fallback when the artwork has no usable colour at all (0xFF241A38). */
private val NeutralArtworkColor = Color(0xFF241A38)

internal fun rememberArtworkColor(url: String?, color: Color) {
    if (url == null) return
    ArtworkColorCache.remove(url)
    ArtworkColorCache[url] = color
    while (ArtworkColorCache.size > ARTWORK_COLOR_CACHE_SIZE) {
        ArtworkColorCache.remove(ArtworkColorCache.keys.first())
    }
}

/** Runs off the main thread; any failure yields [DarkBackground]. */
internal fun extractArtworkColorOrDefault(drawable: Drawable): Color =
    runCatching { extractArtworkColor(drawable) }.getOrDefault(DarkBackground)

/**
 * Dominant, saturated colour of [drawable], darkened to ~24 % so white text stays readable.
 *
 * Downscaled to 48×48; transparent (alpha < 128), very dark (max channel < 32) and grey
 * (channel spread < 18) pixels are ignored. The rest is quantised to 4 bit per channel and
 * every bucket scored by count × (spread + 2) in nibbles, so a vivid accent beats a large
 * dull area.
 */
internal fun extractArtworkColor(drawable: Drawable): Color {
    val bitmap: Bitmap = drawable.toBitmap(SAMPLE_SIZE, SAMPLE_SIZE)
    val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
    bitmap.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)

    val buckets = LinkedHashMap<Int, Int>()
    for (pixel in pixels) {
        if ((pixel ushr 24) < 128) continue
        val r = (pixel ushr 16) and 0xFF
        val g = (pixel ushr 8) and 0xFF
        val b = pixel and 0xFF
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        if (max < 32 || max - min < 18) continue
        val key = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
        buckets[key] = (buckets[key] ?: 0) + 1
    }

    val best = buckets.entries.maxByOrNull { (key, count) ->
        val nibbles = listOf((key shr 8) and 0xF, (key shr 4) and 0xF, key and 0xF)
        (nibbles.max() - nibbles.min() + 2) * count
    } ?: return NeutralArtworkColor

    val key = best.key
    return Color(
        red = darkenedChannel(key, 8),
        green = darkenedChannel(key, 4),
        blue = darkenedChannel(key, 0),
    )
}

private fun darkenedChannel(key: Int, shift: Int): Int =
    ((((key shr shift) and 0xF) * 16 + 8) * 0.24f).toInt()
