// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.browse

import android.content.Context
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.darksoon.radiowave.core.ui.components.BrowseSkeletonLoader
import de.darksoon.radiowave.core.ui.components.StationLogoImage
import de.darksoon.radiowave.core.ui.components.StreamQualityBadge
import de.darksoon.radiowave.core.model.AppSettings
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.ui.theme.CardBodyStyle
import de.darksoon.radiowave.core.ui.theme.SectionTitleStyle
import de.darksoon.radiowave.core.ui.theme.RadioAccent
import de.darksoon.radiowave.feature.browse.R
import kotlinx.coroutines.launch

private data class CountryItem(
    val code: String,
    val nameRes: Int,
    val flag: String,
)

private val topCountries = listOf(
    CountryItem("DE", R.string.browse_country_germany, "\uD83C\uDDE9\uD83C\uDDEA"),
    CountryItem("US", R.string.browse_country_usa, "\uD83C\uDDFA\uD83C\uDDF8"),
    CountryItem("GB", R.string.browse_country_uk, "\uD83C\uDDEC\uD83C\uDDE7"),
    CountryItem("FR", R.string.browse_country_france, "\uD83C\uDDEB\uD83C\uDDF7"),
    CountryItem("IT", R.string.browse_country_italy, "\uD83C\uDDEE\uD83C\uDDF9"),
    CountryItem("ES", R.string.browse_country_spain, "\uD83C\uDDEA\uD83C\uDDF8"),
    CountryItem("NL", R.string.browse_country_netherlands, "\uD83C\uDDF3\uD83C\uDDF1"),
    CountryItem("PL", R.string.browse_country_poland, "\uD83C\uDDF5\uD83C\uDDF1"),
    CountryItem("AT", R.string.browse_country_austria, "\uD83C\uDDE6\uD83C\uDDF9"),
    CountryItem("CH", R.string.browse_country_switzerland, "\uD83C\uDDE8\uD83C\uDDED"),
    CountryItem("JP", R.string.browse_country_japan, "\uD83C\uDDEF\uD83C\uDDF5"),
    CountryItem("KR", R.string.browse_country_south_korea, "\uD83C\uDDF0\uD83C\uDDF7"),
    CountryItem("CA", R.string.browse_country_canada, "\uD83C\uDDE8\uD83C\uDDE6"),
)

private data class GenreItem(
    val labelRes: Int,
    val tag: String,
)

private val popularGenres = listOf(
    GenreItem(R.string.browse_genre_techno, "techno"),
    GenreItem(R.string.browse_genre_dance, "dance"),
    GenreItem(R.string.browse_genre_rock, "rock"),
    GenreItem(R.string.browse_genre_jazz, "jazz"),
    GenreItem(R.string.browse_genre_80s, "80s"),
    GenreItem(R.string.browse_genre_pop, "pop"),
    GenreItem(R.string.browse_genre_classical, "classical"),
    GenreItem(R.string.browse_genre_hip_hop, "hip hop"),
    GenreItem(R.string.browse_genre_chill, "chill"),
    GenreItem(R.string.browse_genre_house, "house"),
)

private data class QuickGenreItem(
    val labelRes: Int,
    val tag: String,
    val icon: ImageVector,
    val gradient: List<Color>,
)

private val quickGenres = listOf(
    QuickGenreItem(
        labelRes = R.string.browse_genre_techno,
        tag = "techno",
        icon = Icons.Filled.Memory,
        gradient = listOf(Color(0xFF5B4BFF), Color(0xFFB948FF)),
    ),
    QuickGenreItem(
        labelRes = R.string.browse_genre_rock,
        tag = "rock",
        icon = Icons.Filled.MusicNote,
        gradient = listOf(Color(0xFF9F1C2B), Color(0xFFFF4D64)),
    ),
    QuickGenreItem(
        labelRes = R.string.browse_genre_jazz,
        tag = "jazz",
        icon = Icons.Filled.Piano,
        gradient = listOf(Color(0xFF8A4E10), Color(0xFFFFB347)),
    ),
    QuickGenreItem(
        labelRes = R.string.browse_genre_chillout,
        tag = "chill",
        icon = Icons.Filled.Waves,
        gradient = listOf(Color(0xFF0C666B), Color(0xFF2EE6D6)),
    ),
)

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun BrowseScreen(
    initialGenre: String = "",
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var showInsecureStreams by remember {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_SHOW_INSECURE_STREAMS, true))
    }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == AppSettings.KEY_SHOW_INSECURE_STREAMS) {
                showInsecureStreams = prefs.getBoolean(AppSettings.KEY_SHOW_INSECURE_STREAMS, true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(initialGenre) {
        if (initialGenre.isNotEmpty()) {
            selectedGenre = initialGenre
            viewModel.onSearchQueryChange(initialGenre)
        }
    }

    val hasData = uiState.results.isNotEmpty()
    val isInitialLoad = uiState.isLoading && !hasData

    if (isInitialLoad) {
        BrowseSkeletonLoader(modifier = modifier)
        return@BrowseScreen
    }

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = modifier,
    ) {
        val favoriteIds by viewModel.favoriteStationIds.collectAsStateWithLifecycle()
        BrowseContent(
            uiState = uiState,
            favoriteIds = favoriteIds,
            searchQuery = searchQuery,
            selectedCountry = selectedCountry,
            selectedGenre = selectedGenre,
            showInsecureStreams = showInsecureStreams,
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
            onRefresh = { viewModel.refresh() },
            onStationClick = { station ->
                viewModel.playStation(station)
            },
            onToggleFavorite = { station ->
                viewModel.toggleFavorite(station)
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun BrowseContent(
    uiState: BrowseUiState,
    favoriteIds: Set<String>,
    searchQuery: String,
    selectedCountry: String?,
    selectedGenre: String?,
    showInsecureStreams: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onGenreSelected: (String?) -> Unit,
    onCountrySelected: (String?) -> Unit,
    onSortOptionChanged: (SortOption) -> Unit,
    onRefresh: () -> Unit,
    onStationClick: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortOptionsLocalized = listOf(
        SortOption.POPULARITY to stringResource(R.string.browse_sort_popularity),
        SortOption.NAME to stringResource(R.string.browse_sort_name),
        SortOption.COUNTRY to stringResource(R.string.browse_sort_country),
    )

    val visibleStations = remember(uiState.results, showInsecureStreams) {
        if (showInsecureStreams) {
            uiState.results
        } else {
            uiState.results.filterNot { station -> station.streamUrl.isInsecureHttpStream() }
        }
    }
    val stationGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    var advancedFiltersExpanded by remember { mutableStateOf(false) }
    val showScrollToTopButton by remember {
        derivedStateOf {
            stationGridState.firstVisibleItemIndex > 0 ||
                stationGridState.firstVisibleItemScrollOffset > 220
        }
    }
    val showHeader by remember {
        derivedStateOf {
            stationGridState.firstVisibleItemIndex == 0 &&
                stationGridState.firstVisibleItemScrollOffset < 160
        }
    }

    LaunchedEffect(showHeader) {
        if (!showHeader) advancedFiltersExpanded = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            AnimatedVisibility(
                visible = showHeader,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.browse_title),
                        style = SectionTitleStyle,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.browse_refresh),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                        stringResource(R.string.browse_search_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = RadioAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = RadioAccent,
                ),
            )

            AnimatedVisibility(
                visible = showHeader,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.browse_quick_genres),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(quickGenres, key = { item -> item.tag }, contentType = { "genre" }) { item ->
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
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { advancedFiltersExpanded = !advancedFiltersExpanded }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.browse_more_filters),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (advancedFiltersExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(
                            if (advancedFiltersExpanded) R.string.browse_cd_filter_collapse
                            else R.string.browse_cd_filter_expand,
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                            .padding(vertical = 10.dp),
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(topCountries, key = { country -> country.code }, contentType = { "country" }) { country ->
                                val isSelected = selectedCountry == country.code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onCountrySelected(if (isSelected) null else country.code) },
                                    label = {
                                        Text(
                                            text = "${country.flag} ${stringResource(country.nameRes)}",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RadioAccent,
                                        selectedLabelColor = Color.Black,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurface,
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
                            items(popularGenres, key = { genre -> genre.tag }, contentType = { "genre" }) { genre ->
                                val isSelected = selectedGenre == genre.tag
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onGenreSelected(if (isSelected) null else genre.tag) },
                                    label = {
                                        Text(
                                            text = stringResource(genre.labelRes),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RadioAccent,
                                        selectedLabelColor = Color.Black,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                )
                            }
                        }
                    }
                }
            }
                } // end AnimatedVisibility (genres + filters)
            }     // end outer Column wrapper

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
                        stringResource(R.string.browse_results_found, visibleStations.size)
                    } else if (searchQuery.isBlank() && selectedCountry == null) {
                        stringResource(R.string.browse_top_stations)
                    } else {
                        stringResource(R.string.browse_stations)
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                var sortMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    Row(
                        modifier = Modifier
                            .clickable { sortMenuExpanded = true }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val currentSortLabel = sortOptionsLocalized.find { it.first == uiState.sortOption }?.second
                            ?: stringResource(R.string.browse_sort_popularity)
                        Text(
                            text = stringResource(R.string.browse_sort_prefix, currentSortLabel),
                            style = MaterialTheme.typography.labelMedium,
                            color = RadioAccent,
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.browse_choose_sort),
                            tint = RadioAccent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        sortOptionsLocalized.forEach { (option, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        color = if (uiState.sortOption == option) RadioAccent else MaterialTheme.colorScheme.onSurface,
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
                    BrowseSkeletonLoader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
                visibleStations.isEmpty() && searchQuery.isBlank() && selectedCountry == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.browse_empty_prompt),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                visibleStations.isEmpty() -> {
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
                        columns = GridCells.Adaptive(minSize = 140.dp),
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
                            items = visibleStations,
                            key = { station -> station.uuid },
                            contentType = { "station" },
                        ) { station ->
                            StationGridCard(
                                station = station,
                                showInsecureBadge = showInsecureStreams,
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
            visible = showScrollToTopButton && visibleStations.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 152.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, RadioAccent.copy(alpha = 0.5f)),
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
                        contentDescription = stringResource(R.string.browse_to_top),
                        tint = RadioAccent,
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
            color = if (isSelected) RadioAccent else Color.White.copy(alpha = 0.14f),
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
                text = stringResource(item.labelRes),
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
    val suggestions = listOf(
        GenreItem(R.string.browse_genre_rock, "rock"),
        GenreItem(R.string.browse_genre_jazz, "jazz"),
        GenreItem(R.string.browse_genre_pop, "pop"),
        GenreItem(R.string.browse_genre_dance, "dance"),
        GenreItem(R.string.browse_genre_classical, "classical"),
    )
    
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF52E3D9).copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                    ),
                    shape = androidx.compose.foundation.shape.CircleShape,
                )
                .then(
                    Modifier.background(
                        Color(0xFF52E3D9).copy(alpha = 0.10f),
                        androidx.compose.foundation.shape.CircleShape,
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF52E3D9),
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.browse_empty_title),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.browse_empty_subtitle),
            color = Color.White.copy(alpha = 0.55f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        val contextText = when {
            searchQuery.isNotBlank() && selectedCountry != null -> {
                val countryName = topCountries.find { it.code == selectedCountry }?.let {
                    stringResource(it.nameRes)
                } ?: selectedCountry
                stringResource(R.string.browse_context_for_in, searchQuery, countryName)
            }
            searchQuery.isNotBlank() -> stringResource(R.string.browse_context_for, searchQuery)
            selectedCountry != null -> {
                val countryName = topCountries.find { it.code == selectedCountry }?.let {
                    stringResource(it.nameRes)
                } ?: selectedCountry
                stringResource(R.string.browse_context_in, countryName)
            }
            else -> ""
        }
        
        if (contextText.isNotBlank()) {
            Text(
                text = contextText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text(
            text = stringResource(R.string.browse_suggestions),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(suggestions, key = { suggestion -> suggestion.tag }, contentType = { "genre" }) { suggestion ->
                FilterChip(
                    selected = false,
                    onClick = { onSuggestionClick(suggestion.tag) },
                    label = {
                        Text(text = stringResource(suggestion.labelRes))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = RadioAccent,
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
    showInsecureBadge: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(186.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.09f),
                            Color.White.copy(alpha = 0.03f),
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    StationLogoImage(
                        imageUrl = station.faviconUrl,
                        contentDescription = station.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = station.name,
                    style = CardBodyStyle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = station.country?.takeIf { it.isNotBlank() } ?: " ",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = RadioAccent.copy(alpha = if (station.country.isNullOrBlank()) 0f else 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                )

                StreamQualityBadge(
                    codec = station.codec,
                    bitrate = station.bitrate,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 4.dp),
                )
            }
            if (showInsecureBadge && station.streamUrl.isInsecureHttpStream()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE65100).copy(alpha = 0.92f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    ),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 8.dp, start = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.browse_http_badge),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Surface(
                onClick = onToggleFavorite,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.browse_cd_toggle_favorite_remove
                        else R.string.browse_cd_toggle_favorite_add,
                    ),
                    tint = if (isFavorite) Color(0xFFFF5A7A) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp),
                )
            }
        }
    }
}

private fun String.isInsecureHttpStream(): Boolean {
    return startsWith("http://", ignoreCase = true)
}

