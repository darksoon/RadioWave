// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * App-wide backdrop behind the NavHost (MainActivity).
 *
 * REA 95fcb33 (p000/hk4.java#m4333a): the "deep space" Canvas of v1.0.19 (near-black base
 * 0xFF050913 plus purple and teal radial glows) is gone — its colour literals no longer exist in
 * the dex. The backdrop is now just the theme's `background`, so it follows the light theme.
 * Signature and the call in MainActivity are unchanged.
 */
@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        content()
    }
}
