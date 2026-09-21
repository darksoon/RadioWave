// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Station name for cards and tiles. Wraps only between words and never splits a word
 * across two lines: Android's line breaker otherwise breaks a word that is wider than
 * the tile (e.g. "Deutschlandfunk" in a narrow card) mid-word, which reads like a typo.
 *
 * If the longest word does not fit, the font is shrunk slightly (down to [minFontScale]);
 * if it still does not fit, the name is shown on a single ellipsized line instead. The
 * space for [minLines] lines stays reserved so cards in a row keep their alignment.
 */
@Composable
fun StationNameText(
    name: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 2,
    minLines: Int = 1,
    textAlign: TextAlign? = null,
    minFontScale: Float = 0.82f,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val baseStyle = LocalTextStyle.current.merge(style).copy(
        // Balanced, word-based breaks ("Radio / Paradise" instead of a ragged last line).
        lineBreak = LineBreak.Heading,
        hyphens = Hyphens.None,
    )
    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val fitted = remember(name, baseStyle, maxWidthPx, maxLines, minLines, minFontScale, density) {
            fitStationName(
                name = name,
                style = baseStyle,
                maxWidthPx = maxWidthPx,
                maxLines = maxLines,
                minLines = minLines,
                minFontScale = minFontScale,
                measureWidth = { text, textStyle ->
                    textMeasurer.measure(text = text, style = textStyle, maxLines = 1, softWrap = false).size.width
                },
                measureLinesHeight = { lines, textStyle ->
                    val sample = List(lines) { "A" }.joinToString(separator = "\n")
                    with(density) {
                        textMeasurer.measure(text = sample, style = textStyle).size.height.toDp()
                    }
                },
            )
        }
        // Callers that pass fillMaxWidth() expect textAlign to apply across the full width.
        val fillWidth = constraints.hasBoundedWidth && constraints.minWidth == constraints.maxWidth
        val widthModifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier
        Box(modifier = widthModifier.heightIn(min = fitted.reservedHeight)) {
            Text(
                modifier = widthModifier,
                text = name,
                style = fitted.style,
                color = color,
                maxLines = fitted.maxLines,
                minLines = fitted.minLines,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
            )
        }
    }
}

private data class FittedStationName(
    val style: TextStyle,
    val maxLines: Int,
    val minLines: Int,
    val reservedHeight: Dp,
)

private fun fitStationName(
    name: String,
    style: TextStyle,
    maxWidthPx: Int,
    maxLines: Int,
    minLines: Int,
    minFontScale: Float,
    measureWidth: (String, TextStyle) -> Int,
    measureLinesHeight: (Int, TextStyle) -> Dp,
): FittedStationName {
    val safeMaxLines = maxLines.coerceAtLeast(1)
    val safeMinLines = minLines.coerceIn(1, safeMaxLines)
    val asIs = FittedStationName(style, safeMaxLines, safeMinLines, 0.dp)
    // A single line can only be ellipsized, never split, so nothing to fit.
    if (safeMaxLines == 1 || maxWidthPx == Int.MAX_VALUE || maxWidthPx <= 0) return asIs

    val words = name.split(' ', '\t', '\n').filter { it.isNotEmpty() }
    if (words.isEmpty()) return asIs
    val longestWordPx = words.maxOf { word -> measureWidth(word, style) }
    if (longestWordPx <= maxWidthPx) return asIs

    val scale = maxWidthPx.toFloat() / longestWordPx
    if (style.fontSize.isSp && scale >= minFontScale) {
        // Small safety margin against rounding at the measured edge.
        val shrunk = style.copy(fontSize = style.fontSize * (scale * 0.98f))
        val recheckedLongest = words.maxOf { word -> measureWidth(word, shrunk) }
        if (recheckedLongest <= maxWidthPx) {
            return FittedStationName(shrunk, safeMaxLines, safeMinLines, 0.dp)
        }
    }
    // The word cannot fit even when shrunk: one ellipsized line instead of a mid-word
    // break, keeping the height of minLines so neighbouring cards stay aligned.
    val reserved = if (safeMinLines > 1) measureLinesHeight(safeMinLines, style) else 0.dp
    return FittedStationName(style, 1, 1, reserved)
}
