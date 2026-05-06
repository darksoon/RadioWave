// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val skeletonBase = Color.White.copy(alpha = 0.06f)

/**
 * Single shared shimmer brush — one InfiniteTransition per skeleton root,
 * passed down to avoid per-box allocations.
 */
@Composable
private fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )
    return remember(alpha) {
        Brush.linearGradient(
            colors = listOf(
                skeletonBase,
                Color.White.copy(alpha = 0.06f + 0.08f * alpha),
                skeletonBase,
            ),
        )
    }
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    brush: Brush,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush),
    )
}

/** Skeleton für eine horizontale Senderreihe (LazyRow) im HomeScreen */
@Composable
fun SkeletonStationRow(
    modifier: Modifier = Modifier,
    cardWidth: Int = 120,
    cardHeight: Int = 140,
    count: Int = 5,
) {
    val brush = rememberShimmerBrush()
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false,
    ) {
        items(count) {
            Column(
                modifier = Modifier
                    .width(cardWidth.dp)
                    .height(cardHeight.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(skeletonBase)
                    .padding(10.dp),
            ) {
                SkeletonBox(
                    modifier = Modifier.fillMaxWidth().height((cardHeight * 0.5).dp).clip(RoundedCornerShape(10.dp)),
                    brush = brush,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp), brush = brush)
                Spacer(modifier = Modifier.height(5.dp))
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.55f).height(10.dp), brush = brush)
            }
        }
    }
}

/** Skeleton für ein Grid (Browse/Favorites) */
@Composable
fun SkeletonGrid(
    modifier: Modifier = Modifier,
    columns: Int = 3,
    rows: Int = 3,
    cardHeight: Int = 120,
) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(columns) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(cardHeight.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(brush),
                    )
                }
            }
        }
    }
}

/** Skeleton für eine komplette Home-Seite */
@Composable
fun HomeSkeletonLoader(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        SkeletonBox(modifier = Modifier.padding(horizontal = 20.dp).width(120.dp).height(16.dp), brush = brush)
        Spacer(modifier = Modifier.height(10.dp))
        SkeletonStationRow(cardWidth = 130, cardHeight = 150, count = 5)
        Spacer(modifier = Modifier.height(20.dp))
        SkeletonBox(modifier = Modifier.padding(horizontal = 20.dp).width(100.dp).height(16.dp), brush = brush)
        Spacer(modifier = Modifier.height(10.dp))
        SkeletonStationRow(cardWidth = 130, cardHeight = 150, count = 5)
        Spacer(modifier = Modifier.height(20.dp))
        SkeletonBox(modifier = Modifier.padding(horizontal = 20.dp).width(140.dp).height(16.dp), brush = brush)
        Spacer(modifier = Modifier.height(10.dp))
        SkeletonStationRow(cardWidth = 130, cardHeight = 150, count = 5)
    }
}

/** Skeleton für Browse-Grid */
@Composable
fun BrowseSkeletonLoader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(top = 8.dp)) {
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonGrid(columns = 3, rows = 4, cardHeight = 120)
    }
}

/** Skeleton für Favorites-Grid */
@Composable
fun FavoritesSkeletonLoader(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(top = 16.dp)) {
        SkeletonGrid(columns = 2, rows = 4, cardHeight = 170)
    }
}
