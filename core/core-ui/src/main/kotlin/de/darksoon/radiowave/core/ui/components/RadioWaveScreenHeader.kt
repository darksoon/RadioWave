// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.darksoon.radiowave.core.ui.R

/**
 * Brand header for the top-level screens (Browse "browse_title", Favorites
 * "favorites_screen_title"): the v2 header art on the right at 70 % opacity, the wordmark
 * (light variant on light backgrounds) on top, and the screen title as a heading below.
 *
 * REA 95fcb33: p000/n63.java#m6996a(String, Modifier). Aufrufer: p000/C0844vy.java Fall 0
 * (Browse), p000/C0255fz.java Fall 5 (Favoriten). Ersetzt den bg_nebula-Look aus v1.0.19.
 * REA-UNSICHER: Name und Ablage in core-ui geraten (wird von zwei Feature-Modulen benutzt).
 */
@Composable
fun RadioWaveScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val useLightWordmark = colorScheme.background.luminance() > 0.5f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.radiowave_header_art_v2),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .widthIn(max = 220.dp)
                    .fillMaxWidth()
                    .height(80.dp),
                contentScale = ContentScale.Fit,
                alpha = 0.7f,
            )
            Image(
                painter = painterResource(
                    if (useLightWordmark) R.drawable.radiowave_wordmark_light_v2 else R.drawable.radiowave_wordmark_v2,
                ),
                contentDescription = "RadioWave",
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .fillMaxWidth()
                    .height(80.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            color = colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
