package de.radiowave.feature.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import de.radiowave.core.model.Station
import de.radiowave.core.ui.theme.DarkBackground
import de.radiowave.core.ui.theme.DarkCardBackground
import de.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.radiowave.core.ui.theme.DarkSurfaceVariant
import de.radiowave.core.ui.theme.TealAccent
import de.radiowave.feature.home.HomeUiState
import de.radiowave.feature.home.HomeViewModel
import de.radiowave.feature.home.SortOption
import kotlinx.coroutines.launch

private data class CountryItem(
    val code: String,
    val name: String,
    val flag: String,
)

private val topCountries = listOf(
    CountryItem("DE", "Deutschland", "\uD83C\uDDE9\uD83C\uDDEA"),
    CountryItem("US", "USA", "\uD83C\uDDFA\uD83C\uDDF8"),
    CountryItem("GB", "UK", "\uD83C\uDDEC\uD83C\uDDE7"),
    CountryItem("FR", "Frankreich", "\uD83C\uDDEB\uD83C\uDDF7"),
    CountryItem("IT", "Italien", "\uD83C\uDDEE\uD83C\uDDF9"),
    CountryItem("ES", "Spanien", "\uD83C\uDDEA\uD83C\uDDF8"),
    CountryItem("NL", "Niederlande", "\uD83C\uDDF3\uD83C\uDDF1"),
    CountryItem("PL", "Polen", "\uD83C\uDDF5\uD83C\uDDF1"),
    CountryItem("AT", "Oesterreich", "\uD83C\uDDE6\uD83C\uDDF9"),
    CountryItem("CH", "Schweiz", "\uD83C\uDDE8\uD83C\uDDED"),
    CountryItem("JP", "Japan", "\uD83C\uDDEF\uD83C\uDDF5"),
    CountryItem("KR", "Suedkorea", "\uD83C\uDDF0\uD83C\uDDF7"),
    CountryItem("CA", "Kanada", "\uD83C\uDDE8\uD83C\uDDE6"),
)

private val popularGenres = listOf(
    "Techno" to "techno",
    "Dance" to "dance",
    "Rock" to "rock",
    "Jazz" to "jazz",
    "80s" to "80s",
    "Pop" to "pop",
    "Classical" to "classical",
    "Hip Hop" to "hip hop",
    "Chill" to "chill",
    "House" to "house",
)

private data class QuickGenreItem(
    val label: String,
    val tag: String,
    val icon: ImageVector,
    val gradient: List<Color>,
)

private val quickGenres = listOf(
    QuickGenreItem(
        label = "Techno",
        tag = "techno",
        icon = Icons.Filled.Memory,
        gradient = listOf(Color(0xFF5B4BFF), Color(0xFFB948FF)),
    ),
    QuickGenreItem(
        label = "Rock",
        tag = "rock",
        icon = Icons.Filled.MusicNote,
        gradient = listOf(Color(0xFF9F1C2B), Color(0xFFFF4D64)),
    ),
    QuickGenreItem(
        label = "Jazz",
        tag = "jazz",
        icon = Icons.Filled.Piano,
        gradient = listOf(Color(0xFF8A4E10), Color(0xFFFFB347)),
    ),
    QuickGenreItem(
        label = "Chillout",
        tag = "chill",
        icon = Icons.Filled.Waves,
        gradient = listOf(Color(0xFF0C666B), Color(0xFF2EE6D6)),
    ),
)

private val sortOptions = listOf(
    SortOption.POPULARITY to "Beliebtheit",
    SortOption.NAME to "Name",
    SortOption.COUNTRY to "Land",
)

@Composable
fun BrowseScreen(
    initialGenre: String = "",
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    var selectedGenre by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialGenre) {
        if (initialGenre.isNotEmpty()) {
            selectedGenre = initialGenre
            viewModel.onSearchQueryChange(initialGenre)
        }
    }

    BrowseContent(
        uiState = uiState,
        searchQuery = searchQuery,
        selectedCountry = selectedCountry,
        selectedGenre = selectedGenre,
        onSearchQueryChange = { query ->
            selectedGenre = null
            viewModel.onSearchQueryChange(query)
        },
        onGenreSelected = { genre ->
            selectedGenre = genre
            viewModel.onSearchQueryChange(genre ?: "")
        },
        onCountrySelected = { countryCode ->
            viewModel.onCountrySelected(countryCode)
        },
        onSortOptionChanged = { sortOption ->
            viewModel.onSortOptionChanged(sortOption)
        },
        onRefresh = {
            viewModel.refresh()
        },
        onStationClick = { station ->
            viewModel.playStation(station)
        },
        onToggleFavorite = { station ->
            viewModel.toggleFavorite(station)
        },
        modifier = modifier,
    )
}

@Composable
private fun BrowseContent(
    uiState: HomeUiState,
    searchQuery: String,
    selectedCountry: String?,
    selectedGenre: String?,
    onSearchQueryChange: (String) -> Unit,
    onGenreSelected: (String?) -> Unit,
    onCountrySelected: (String?) -> Unit,
    onSortOptionChanged: (SortOption) -> Unit,
    onRefresh: () -> Unit,
    onStationClick: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    val favoriteIds = uiState.favoriteStations.map { station -> station.uuid }.toSet()
    val stationGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    var advancedFiltersExpanded by remember { mutableStateOf(false) }
    val showDiscoverChrome by remember {
        derivedStateOf {
            stationGridState.firstVisibleItemIndex == 0 &&
                stationGridState.firstVisibleItemScrollOffset < 24
        }
    }
    val showScrollToTopButton by remember {
        derivedStateOf {
            stationGridState.firstVisibleItemIndex > 0 ||
                stationGridState.firstVisibleItemScrollOffset > 220
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Entdecken & Suche",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Aktualisieren",
                        tint = DarkOnSurfaceVariant,
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = {
                    Text(
                        "Sender, Genre, Land...",
                        color = DarkOnSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = DarkOnSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkCardBackground,
                    unfocusedContainerColor = DarkCardBackground,
                    focusedBorderColor = TealAccent,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = TealAccent,
                ),
            )

            AnimatedVisibility(
                visible = showDiscoverChrome,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Quick Genres",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = DarkOnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(quickGenres) { item ->
                            QuickGenreCard(
                                item = item,
                                isSelected = selectedGenre == item.tag,
                                onClick = {
                                    onGenreSelected(if (selectedGenre == item.tag) null else item.tag)
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .animateContentSize(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkCardBackground)
                                .clickable { advancedFiltersExpanded = !advancedFiltersExpanded }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Weitere Filter",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = if (advancedFiltersExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = DarkOnSurfaceVariant,
                            )
                        }

                        AnimatedVisibility(
                            visible = advancedFiltersExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkCardBackground.copy(alpha = 0.9f))
                                    .padding(vertical = 10.dp),
                            ) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(topCountries) { country ->
                                        val isSelected = selectedCountry == country.code
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onCountrySelected(if (isSelected) null else country.code) },
                                            label = {
                                                Text(
                                                    text = "${country.flag} ${country.name}",
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp,
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TealAccent,
                                                selectedLabelColor = Color.Black,
                                                containerColor = DarkSurfaceVariant,
                                                labelColor = Color.White,
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(popularGenres) { (displayName, tag) ->
                                        val isSelected = selectedGenre == tag
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onGenreSelected(if (isSelected) null else tag) },
                                            label = {
                                                Text(
                                                    text = displayName,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TealAccent,
                                                selectedLabelColor = Color.Black,
                                                containerColor = DarkSurfaceVariant,
                                                labelColor = Color.White,
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (uiState.searchResultCount > 0) {
                        "${uiState.searchResultCount} Sender gefunden"
                    } else if (searchQuery.isBlank() && selectedCountry == null) {
                        "Top Sender"
                    } else {
                        "Sender"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = DarkOnSurfaceVariant,
                )

                var sortMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    Row(
                        modifier = Modifier
                            .clickable { sortMenuExpanded = true }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val currentSortLabel = sortOptions.find { it.first == uiState.sortOption }?.second ?: "Beliebtheit"
                        Text(
                            text = "Sort: $currentSortLabel",
                            style = MaterialTheme.typography.labelMedium,
                            color = TealAccent,
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Sortierung wählen",
                            tint = TealAccent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        containerColor = DarkCardBackground,
                    ) {
                        sortOptions.forEach { (option, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        color = if (uiState.sortOption == option) TealAccent else Color.White,
                                        fontWeight = if (uiState.sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    onSortOptionChanged(option)
                                    sortMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Lade Sender...",
                            color = DarkOnSurfaceVariant,
                        )
                    }
                }
                uiState.topStations.isEmpty() && searchQuery.isBlank() && selectedCountry == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Gib einen Suchbegriff ein\noder wähle ein Genre/Land",
                            color = DarkOnSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                uiState.topStations.isEmpty() -> {
                    EmptyState(
                        searchQuery = searchQuery,
                        selectedCountry = selectedCountry,
                        onSuggestionClick = { suggestion ->
                            onSearchQueryChange(suggestion)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        state = stationGridState,
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 140.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(
                            items = uiState.topStations,
                            key = { station -> station.uuid },
                            contentType = { "station" },
                        ) { station ->
                            StationGridCard(
                                station = station,
                                isFavorite = station.uuid in favoriteIds,
                                onToggleFavorite = { onToggleFavorite(station) },
                                onClick = { onStationClick(station) },
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showScrollToTopButton && uiState.topStations.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 152.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = DarkCardBackground.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, TealAccent.copy(alpha = 0.5f)),
                modifier = Modifier
                    .size(44.dp)
                    .clickable {
                        coroutineScope.launch {
                            stationGridState.animateScrollToItem(0)
                        }
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Nach oben",
                        tint = TealAccent,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickGenreCard(
    item: QuickGenreItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
        ),
        border = BorderStroke(
            width = if (isSelected) 1.6.dp else 1.dp,
            color = if (isSelected) TealAccent else Color.White.copy(alpha = 0.14f),
        ),
        modifier = Modifier
            .width(86.dp)
            .height(82.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(item.gradient))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.96f),
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = item.label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyState(
    searchQuery: String,
    selectedCountry: String?,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestions = listOf("Rock", "Jazz", "Pop", "Dance", "Classical")
    
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Keine Sender gefunden",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val contextText = when {
            searchQuery.isNotBlank() && selectedCountry != null -> {
                val countryName = topCountries.find { it.code == selectedCountry }?.name ?: selectedCountry
                "FÃ¼r \"$searchQuery\" in $countryName"
            }
            searchQuery.isNotBlank() -> "FÃ¼r \"$searchQuery\""
            selectedCountry != null -> {
                val countryName = topCountries.find { it.code == selectedCountry }?.name ?: selectedCountry
                "In $countryName"
            }
            else -> ""
        }
        
        if (contextText.isNotBlank()) {
            Text(
                text = contextText,
                color = DarkOnSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text(
            text = "VorschlÃ¤ge:",
            color = DarkOnSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(suggestions) { suggestion ->
                FilterChip(
                    selected = false,
                    onClick = { onSuggestionClick(suggestion) },
                    label = {
                        Text(text = suggestion)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkSurfaceVariant,
                        labelColor = TealAccent,
                    ),
                    shape = RoundedCornerShape(20.dp),
                )
            }
        }
    }
}

@Composable
private fun StationGridCard(
    station: Station,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCardBackground,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
        ),
        onClick = onClick,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant),
                ) {
                    val imageRequest = ImageRequest.Builder(context)
                        .data(station.faviconUrl)
                        .crossfade(false)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .allowHardware(true)
                        .build()
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = station.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                )

                station.country?.let { country ->
                    Text(
                        text = country,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TealAccent.copy(alpha = 0.8f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                    )
                }
            }
            Surface(
                onClick = onToggleFavorite,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.34f),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f),
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color(0xFFFF5A7A) else Color.White.copy(alpha = 0.84f),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp),
                )
            }
        }
    }
}

