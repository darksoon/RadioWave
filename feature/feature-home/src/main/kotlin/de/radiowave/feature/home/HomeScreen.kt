package de.radiowave.feature.home

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import de.radiowave.core.model.Station
import de.radiowave.core.ui.components.ErrorState
import de.radiowave.core.ui.components.LoadingState
import de.radiowave.core.ui.theme.DarkBackground
import de.radiowave.core.ui.theme.DarkCardBackground
import de.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.radiowave.core.ui.theme.TealAccent

private data class CategoryItem(
    val name: String,
    val icon: ImageVector,
    val color: Color,
)

private val discoverCategories = listOf(
    CategoryItem("News", Icons.Default.Explore, Color(0xFFE57373)),
    CategoryItem("Sport", Icons.Default.Sports, Color(0xFF64B5F6)),
    CategoryItem("Musik", Icons.Default.MusicNote, Color(0xFF81C784)),
    CategoryItem("Charts", Icons.Default.Favorite, Color(0xFFBA68C8)),
    CategoryItem("Podcasts", Icons.Default.Explore, Color(0xFFFFB74D)),
)

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
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp),
            ) {
                Text(
                    text = "RadioWave",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )

                SectionTitle("Entdecken", Icons.Default.Explore)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(discoverCategories) { category ->
                        CategoryCard(
                            category = category,
                            onClick = { onNavigateToBrowse(category.name.lowercase()) },
                        )
                    }
                }

                if (uiState.recentStations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionTitle("Zuletzt gehört", Icons.Default.History)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(uiState.recentStations) { station ->
                            SquareStationCard(
                                station = station,
                                onClick = { onStationClick(station) },
                            )
                        }
                    }
                }

                if (uiState.favoriteStations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionTitle("Deine Favoriten", Icons.Default.Favorite)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(uiState.favoriteStations) { station ->
                            SquareStationCard(
                                station = station,
                                onClick = { onStationClick(station) },
                            )
                        }
                    }
                }

                if (uiState.recentStations.isEmpty() && uiState.favoriteStations.isEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = DarkCardBackground,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = TealAccent,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Entdecke tausende Radiosender",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tippe auf \"Browse\" um zu starten",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DarkOnSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = TealAccent,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun SquareStationCard(
    station: Station,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stationName: String = station.name
    val faviconUrl: String? = station.faviconUrl

    Card(
        modifier = modifier
            .size(140.dp)
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCardBackground,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
        ),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            SubcomposeAsyncImage(
                model = faviconUrl,
                contentDescription = stationName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black,
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            ) {
                Text(
                    text = stationName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: CategoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = category.color.copy(alpha = 0.15f),
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp,
            ),
            onClick = onClick,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.name,
                    modifier = Modifier.size(32.dp),
                    tint = category.color,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = category.name,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
