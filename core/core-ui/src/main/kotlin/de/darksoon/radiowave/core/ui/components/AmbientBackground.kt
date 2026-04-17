// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAmbientLayers()
        }
        content()
    }
}

private fun DrawScope.drawAmbientLayers() {
    // Base: near-black deep space
    drawRect(color = Color(0xFF050913))

    // Purple glow — top left
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF3A1F6B).copy(alpha = 0.35f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.1f, size.height * 0.15f),
            radius = size.maxDimension * 0.55f,
        ),
        radius = size.maxDimension * 0.55f,
        center = Offset(size.width * 0.1f, size.height * 0.15f),
    )

    // Teal glow — bottom right
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF0D3B4A).copy(alpha = 0.3f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.9f, size.height * 0.8f),
            radius = size.maxDimension * 0.5f,
        ),
        radius = size.maxDimension * 0.5f,
        center = Offset(size.width * 0.9f, size.height * 0.8f),
    )

    // Subtle violet center-bottom warmth
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF2A1050).copy(alpha = 0.2f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.5f, size.height * 0.85f),
            radius = size.maxDimension * 0.4f,
        ),
        radius = size.maxDimension * 0.4f,
        center = Offset(size.width * 0.5f, size.height * 0.85f),
    )
}
