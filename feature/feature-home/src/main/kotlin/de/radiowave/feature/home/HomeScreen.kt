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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import de.radiowave.core.model.Station
import de.radiowave.core.ui.components.ErrorState
import de.radiowave.core.ui.components.LoadingState
import de.radiowave.core.ui.theme.DarkCardBackground
import de.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.radiowave.core.ui.theme.DarkSurfaceVariant
import de.radiowave.core.ui.theme.MintAccent
import de.radiowave.core.ui.theme.TealAccent
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
        onStationClick = { station ->
            viewModel.playStation(station)
            onStationClick(station)
        },
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
                uiState.topStations.take(10)
            }
            val favoriteStations = if (uiState.favoriteStations.isNotEmpty()) {
                uiState.favoriteStations
            } else {
                uiState.topStations.take(5)
            }
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
                        .padding(top = 14.dp, bottom = 88.dp),
                ) {
                    HomeHeader()

                    if (favoriteStations.isNotEmpty()) {
                        SectionTitle(
                            title = "Favoriten",
                            actionLabel = "Alle",
                            onActionClick = onViewAllFavorites,
                        )
                        FavoriteStationGrid(
                            stations = favoriteStations.take(6),
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
    val nebulaDrift by transition.animateFloat(
        initialValue = -0.04f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 72_000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nebula-drift",
    )

    Box(
        modifier = modifier.background(Color(0xFF000514)),
    ) {
        NebulaLayer(
            drift = nebulaDrift,
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
    drift: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier,
    ) {
        val horizontalShift = size.width * drift
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0F001E),
                    Color(0xFF07021A),
                    Color(0xFF000514),
                ),
                center = center.copy(
                    x = center.x + horizontalShift,
                    y = center.y * 0.55f,
                ),
                radius = size.maxDimension * 0.95f,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TealAccent.copy(alpha = 0.14f),
                    MintAccent.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                center = center.copy(
                    x = center.x - size.width * 0.43f + horizontalShift * 0.35f,
                    y = center.y * 0.25f,
                ),
                radius = size.maxDimension * 0.58f,
            ),
            radius = size.maxDimension * 0.62f,
            center = center.copy(
                x = center.x - size.width * 0.43f + horizontalShift * 0.35f,
                y = center.y * 0.25f,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF6E43D6).copy(alpha = 0.12f),
                    Color.Transparent,
                ),
                center = center.copy(
                    x = center.x + size.width * 0.44f - horizontalShift * 0.3f,
                    y = center.y * 0.72f,
                ),
                radius = size.maxDimension * 0.54f,
            ),
            radius = size.maxDimension * 0.56f,
            center = center.copy(
                x = center.x + size.width * 0.44f - horizontalShift * 0.3f,
                y = center.y * 0.72f,
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
            val starCount = 60
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
                    maxAlpha = lerp(0.7f, 1.0f, random.nextFloat()),
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
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .aspectRatio(1f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.1f)),
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
            Text(
                text = station.name,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun FavoriteStationGrid(
    stations: List<Station>,
    onStationClick: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        stations.chunked(3).take(2).forEach { rowStations ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowStations.forEach { station ->
                    FavoriteHeroCard(
                        station = station,
                        onClick = { onStationClick(station) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowStations.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "RadioWave",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
            ),
            color = Color.White,
        )
        Spacer(modifier = Modifier.weight(1f))
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
            ),
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(42.dp)
                    .padding(5.dp),
            )
        }
    }
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
            SubcomposeAsyncImage(
                model = imageUrl,
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
