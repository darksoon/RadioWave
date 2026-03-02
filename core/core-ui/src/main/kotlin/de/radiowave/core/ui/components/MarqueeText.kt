package de.radiowave.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textStyle: TextStyle,
    maxLines: Int = 1,
    enabled: Boolean = true,
    edgeFade: Boolean = true,
) {
    val marqueeModifier = if (enabled) {
        Modifier.basicMarquee()
    } else {
        Modifier
    }
    val fadeModifier = if (edgeFade) {
        Modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                val fadeWidth = 14.dp.toPx()
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startX = 0f,
                        endX = fadeWidth,
                    ),
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startX = size.width - fadeWidth,
                        endX = size.width,
                    ),
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
                )
            }
    } else {
        Modifier
    }

    Text(
        text = text,
        modifier = modifier.then(marqueeModifier).then(fadeModifier),
        color = color,
        style = textStyle,
        maxLines = maxLines,
        overflow = if (enabled) TextOverflow.Clip else TextOverflow.Ellipsis,
        textAlign = TextAlign.Start,
    )
}
