// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Community popularity signal from the Radio Browser API's click-count — aggregate,
 * public data (not user-specific), so this is a tracking-free "trending" indicator
 * matching RadioWave's privacy-first identity. Only ever non-zero on stations from a
 * fresh network response (see [de.darksoon.radiowave.core.model.Station.clickCount]'s
 * doc comment) — renders nothing otherwise.
 */
@Composable
fun PopularityBadge(clickCount: Int, modifier: Modifier = Modifier) {
    val label = formatPopularityLabel(clickCount) ?: return
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

/** Below [MIN_NOTABLE_CLICKS] the count isn't a meaningful signal — most of Radio
 *  Browser's 45,000+ stations have very few clicks, so showing "🔥 3" everywhere
 *  would be noise rather than a useful trending indicator. */
private const val MIN_NOTABLE_CLICKS = 50

internal fun formatPopularityLabel(clickCount: Int): String? {
    if (clickCount < MIN_NOTABLE_CLICKS) return null
    val formatted = when {
        clickCount >= 1_000_000 -> "%.1fM".format(Locale.US, clickCount / 1_000_000f)
        clickCount >= 1_000 -> "%.1fk".format(Locale.US, clickCount / 1_000f)
        else -> clickCount.toString()
    }
    return "🔥 $formatted"
}
