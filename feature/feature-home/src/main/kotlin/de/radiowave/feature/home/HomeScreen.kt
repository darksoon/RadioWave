package de.radiowave.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.radiowave.core.model.Station
import de.radiowave.core.ui.components.ErrorState
import de.radiowave.core.ui.components.LoadingState
import de.radiowave.core.ui.components.StationLogoImage
import de.radiowave.core.ui.R as CoreUiR
import de.radiowave.core.ui.theme.DarkCardBackground
import de.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.radiowave.core.ui.theme.DarkSurfaceVariant
import de.radiowave.core.ui.theme.MintAccent
import de.radiowave.core.ui.theme.TealAccent
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun HomeScreen(
    onStationClick: (Station) -> Unit,
    onViewAllFavorites: () -> Unit,
    onNavigateToBrowse: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        uiState = uiState,
        onStationClick = onStationClick,
        onViewAllFavorites = onViewAllFavorites,
        onNavigateToBrowse = onNavigateToBrowse,
        onRetry = { viewModel.refresh() },
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onStationClick: (Station) -> Unit,
    onViewAllFavorites: () -> Unit,
    onNavigateToBrowse: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> {
            LoadingState(modifier = modifier)
        }

        uiState.error != null -> {
            ErrorState(
                message = uiState.error,
                onRetry = onRetry,
                modifier = modifier,
            )
        }

        else -> {
            val recentStations = if (uiState.recentStations.isNotEmpty()) {
                uiState.recentStations
            } else {
                emptyList()
            }
            val favoriteStations = uiState.favoriteStations
            val excludedSuggestionIds = (
                favoriteStations.map { station -> station.uuid } +
                    recentStations.map { station -> station.uuid }
                ).toSet()
            val suggestionPool = (uiState.topStations + recentStations + favoriteStations)
                .distinctBy { station -> station.uuid }
                .filterNot { station -> station.uuid in excludedSuggestionIds }
            val discoverPool = uiState.topStations
                .distinctBy { station -> station.uuid }
            val discoverStations = remember(
                key1 = (discoverPool + suggestionPool).joinToString(separator = "|") { station -> station.uuid },
            ) {
                when {
                    suggestionPool.isNotEmpty() -> suggestionPool.shuffled().take(10)
                    discoverPool.isNotEmpty() -> discoverPool.shuffled().take(10)
                    else -> emptyList()
                }
            }

            Box(
                modifier = modifier
                    .fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp, bottom = 88.dp),
                ) {
                    if (favoriteStations.isNotEmpty()) {
                        SectionTitle(
                            title = "Favoriten",
                            actionLabel = "Alle",
                            onActionClick = onViewAllFavorites,
                        )
                        FavoriteStationCarousel(
                            stations = favoriteStations,
                            onStationClick = onStationClick,
                        )
                    }

                    if (recentStations.isNotEmpty()) {
                        SectionTitle(
                            title = "Zuletzt gehoert",
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(recentStations) { station ->
                                RecentStationCard(
                                    station = station,
                                    onClick = { onStationClick(station) },
                                )
                            }
                        }
                    }

                    if (discoverStations.isNotEmpty()) {
                        SectionTitle(
                            title = "Entdecken",
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(discoverStations) { station ->
                                RecentStationCard(
                                    station = station,
                                    onClick = { onStationClick(station) },
                                )
                            }
                        }
                    }

                    if (
                        recentStations.isEmpty() &&
                        favoriteStations.isEmpty() &&
                        discoverStations.isEmpty()
                    ) {
                        Spacer(modifier = Modifier.height(18.dp))
                        EmptyStartCard(
                            onNavigateToBrowse = onNavigateToBrowse,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomePremiumBackground(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "premium-bg")
    val twinkleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 10_000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "star-twinkle-progress",
    )
    val nebulaFarDriftX by transition.animateFloat(
        initialValue = -0.05f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 140_000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nebula-far-drift-x",
    )
    val nebulaFarDriftY by transition.animateFloat(
        initialValue = -0.03f,
        targetValue = 0.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 110_000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nebula-far-drift-y",
    )
    val nebulaNearDriftX by transition.animateFloat(
        initialValue = 0.04f,
        targetValue = -0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 90_000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nebula-near-drift-x",
    )
    val nebulaNearDriftY by transition.animateFloat(
        initialValue = -0.02f,
        targetValue = 0.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 76_000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nebula-near-drift-y",
    )
    val nebulaBreath by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 48_000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nebula-breath",
    )

    Box(modifier = modifier.background(Color(0xFF050913))) {
        Image(
            painter = painterResource(id = CoreUiR.drawable.bg_nebula),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.5f,
        )
        NebulaLayer(
            farDriftX = nebulaFarDriftX,
            farDriftY = nebulaFarDriftY,
            nearDriftX = nebulaNearDriftX,
            nearDriftY = nebulaNearDriftY,
            breath = nebulaBreath,
            modifier = Modifier.fillMaxSize(),
        )
        StarFieldLayer(
            twinkleProgress = twinkleProgress,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun NebulaLayer(
    farDriftX: Float,
    farDriftY: Float,
    nearDriftX: Float,
    nearDriftY: Float,
    breath: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier,
    ) {
        val farShiftX = size.width * farDriftX
        val farShiftY = size.height * farDriftY
        val nearShiftX = size.width * nearDriftX
        val nearShiftY = size.height * nearDriftY

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0A1222),
                    Color(0xFF081020),
                    Color(0xFF050913),
                ),
                center = center.copy(
                    x = center.x + farShiftX,
                    y = center.y * 0.55f + farShiftY,
                ),
                radius = size.maxDimension * 0.95f,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TealAccent.copy(alpha = 0.1f * breath),
                    MintAccent.copy(alpha = 0.05f * breath),
                    Color.Transparent,
                ),
                center = center.copy(
                    x = center.x - size.width * 0.43f + farShiftX * 0.35f + nearShiftX * 0.22f,
                    y = center.y * 0.25f + farShiftY * 0.45f + nearShiftY * 0.18f,
                ),
                radius = size.maxDimension * 0.58f,
            ),
            radius = size.maxDimension * 0.62f,
            center = center.copy(
                x = center.x - size.width * 0.43f + farShiftX * 0.35f + nearShiftX * 0.22f,
                y = center.y * 0.25f + farShiftY * 0.45f + nearShiftY * 0.18f,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF7C53D8).copy(alpha = 0.08f * breath),
                    Color.Transparent,
                ),
                center = center.copy(
                    x = center.x + size.width * 0.44f - farShiftX * 0.3f + nearShiftX * 0.4f,
                    y = center.y * 0.72f - farShiftY * 0.35f + nearShiftY * 0.25f,
                ),
                radius = size.maxDimension * 0.54f,
            ),
            radius = size.maxDimension * 0.56f,
            center = center.copy(
                x = center.x + size.width * 0.44f - farShiftX * 0.3f + nearShiftX * 0.4f,
                y = center.y * 0.72f - farShiftY * 0.35f + nearShiftY * 0.25f,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF5ED3D6).copy(alpha = 0.06f * breath),
                    Color.Transparent,
                ),
                center = center.copy(
                    x = center.x + size.width * 0.04f + nearShiftX * 0.3f,
                    y = center.y * 0.16f + nearShiftY * 0.4f,
                ),
                radius = size.maxDimension * 0.42f,
            ),
            radius = size.maxDimension * 0.45f,
            center = center.copy(
                x = center.x + size.width * 0.04f + nearShiftX * 0.3f,
                y = center.y * 0.16f + nearShiftY * 0.4f,
            ),
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.14f),
                    Color.Black.copy(alpha = 0.26f),
                ),
            ),
        )
    }
}

private data class StarParticle(
    val xFactor: Float,
    val yFactor: Float,
    val radiusPx: Float,
    val phase: Float,
    val speed: Float,
    val minAlpha: Float,
    val maxAlpha: Float,
)

@Composable
private fun StarFieldLayer(
    twinkleProgress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.drawWithCache {
            val random = Random(9917)
            val starCount = 36
            val stars = List(starCount) {
                StarParticle(
                    xFactor = random.nextFloat(),
                    yFactor = random.nextFloat(),
                    radiusPx = lerp(
                        start = 1.dp.toPx(),
                        stop = 3.dp.toPx(),
                        fraction = random.nextFloat(),
                    ),
                    phase = random.nextFloat() * (2f * PI.toFloat()),
                    speed = lerp(0.65f, 1.35f, random.nextFloat()),
                    minAlpha = lerp(0.3f, 0.45f, random.nextFloat()),
                    maxAlpha = lerp(0.55f, 0.85f, random.nextFloat()),
                )
            }

            onDrawBehind {
                val progressAngle = twinkleProgress * (2f * PI.toFloat())
                stars.forEach { star ->
                    val pulse = ((sin(progressAngle * star.speed + star.phase) + 1f) * 0.5f)
                    val alpha = lerp(star.minAlpha, star.maxAlpha, pulse)
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = star.radiusPx,
                        center = Offset(
                            x = size.width * star.xFactor,
                            y = size.height * star.yFactor,
                        ),
                    )
                }
            }
        },
    ) {}
}

@Composable
private fun FavoriteHeroCard(
    station: Station,
    onClick: () -> Unit,
    cardWidth: Dp = 140.dp,
    cardHeight: Dp = 156.dp,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .height(cardHeight)
            .width(cardWidth),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        onClick = onClick,
    ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.06f),
                                Color.White.copy(alpha = 0.1f),
                            ),
                        ),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = (-8).dp, y = (-10).dp)
                    .blur(24.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                TealAccent.copy(alpha = 0.1f),
                                Color.Transparent,
                            ),
                            radius = 320f,
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(12.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.18f),
                            ),
                        ),
                    ),
            )
            StationArtwork(
                imageUrl = station.faviconUrl,
                stationName = station.name,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.42f),
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.26f),
                            ),
                        ),
                        shape = RoundedCornerShape(20.dp),
                    ),
            )
            Text(
                text = station.name,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun FavoriteStationCarousel(
    stations: List<Station>,
    onStationClick: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val itemWidth = 140.dp
    val itemSpacing = 10.dp
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val sidePadding = ((maxWidth - itemWidth) / 2).coerceAtLeast(20.dp)
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(
                lazyListState = listState,
                snapPosition = SnapPosition.Center,
            ),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            items(stations, key = { station -> station.uuid }) { station ->
                val transform = rememberCarouselTransform(listState, station.uuid)
                FavoriteHeroCard(
                    station = station,
                    onClick = { onStationClick(station) },
                    cardWidth = itemWidth,
                    modifier = Modifier.graphicsLayer {
                        scaleX = transform.scale
                        scaleY = transform.scale
                        alpha = transform.alpha
                        rotationY = transform.rotationY
                    },
                )
            }
        }
    }
}

private data class CarouselTransform(
    val scale: Float,
    val alpha: Float,
    val rotationY: Float,
)

@Composable
private fun rememberCarouselTransform(
    listState: LazyListState,
    itemKey: String,
): CarouselTransform {
    val viewportCenter = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2f
    val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
        (info.key as? String) == itemKey
    }
    if (itemInfo == null) {
        return CarouselTransform(
            scale = 0.86f,
            alpha = 0.72f,
            rotationY = 0f,
        )
    }
    val itemCenter = itemInfo.offset + itemInfo.size / 2f
    val distance = (itemCenter - viewportCenter)
    val normalized = (abs(distance) / (itemInfo.size * 1.4f)).coerceIn(0f, 1f)

    val scale = lerp(start = 1.02f, stop = 0.84f, fraction = normalized)
    val alpha = lerp(start = 1f, stop = 0.62f, fraction = normalized)
    val rotation = ((distance / itemInfo.size) * 10f).coerceIn(-10f, 10f)
    return CarouselTransform(
        scale = scale,
        alpha = alpha,
        rotationY = -rotation,
    )
}

@Composable
private fun SectionTitle(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
            ),
            color = Color.White,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (actionLabel != null && onActionClick != null) {
            Surface(
                onClick = onActionClick,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentStationCard(
    station: Station,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .width(94.dp)
            .height(132.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.14f),
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
        ),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.1f)),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(16.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent,
                            ),
                            radius = 180f,
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = Color(0xFFFF7043),
                            shape = CircleShape,
                        )
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    StationArtwork(
                        imageUrl = station.faviconUrl,
                        stationName = station.name,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                    minLines = 2,
                )
                station.country?.let { country ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = country,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = DarkOnSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StationArtwork(
    imageUrl: String?,
    stationName: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackColors: List<Color> = defaultFallbackColors,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(DarkSurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) {
            ArtworkFallback(
                stationName = stationName,
                colors = fallbackColors,
            )
        } else {
            StationLogoImage(
                imageUrl = imageUrl,
                contentDescription = stationName,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                loading = {
                    ArtworkFallback(
                        stationName = stationName,
                        colors = fallbackColors,
                    )
                },
                error = {
                    ArtworkFallback(
                        stationName = stationName,
                        colors = fallbackColors,
                    )
                },
            )
        }
    }
}

@Composable
private fun ArtworkFallback(
    stationName: String,
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = colors,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stationName.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = Color.White,
        )
    }
}

private val defaultFallbackColors = listOf(
    TealAccent.copy(alpha = 0.4f),
    Color(0xFF1A1F2B),
)

@Composable
private fun EmptyStartCard(
    onNavigateToBrowse: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCardBackground.copy(alpha = 0.72f),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.08f),
        ),
        onClick = { onNavigateToBrowse("popular") },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Text(
                text = "Build your radio feed",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Discover trending stations and start your first favorites collection.",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkOnSurfaceVariant,
            )
        }
    }
}
