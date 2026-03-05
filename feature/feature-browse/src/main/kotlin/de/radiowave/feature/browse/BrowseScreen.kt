package de.radiowave.feature.browse

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.radiowave.core.ui.components.StationLogoImage
import de.radiowave.core.model.AppSettings
import de.radiowave.core.model.Station
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

@Composable
fun BrowseScreen(
    initialGenre: String = "",
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
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

    BrowseContent(
        uiState = uiState,
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
    val configuration = LocalConfiguration.current
    val isGerman = configuration.locales[0]?.language?.equals("de", ignoreCase = true) == true
    fun tr(de: String, en: String): String = if (isGerman) de else en

    val sortOptionsLocalized = listOf(
        SortOption.POPULARITY to tr("Beliebtheit", "Popularity"),
        SortOption.NAME to tr("Name", "Name"),
        SortOption.COUNTRY to tr("Land", "Country"),
    )

    val favoriteIds = uiState.favoriteStations.map { station -> station.uuid }.toSet()
    val visibleStations = remember(uiState.topStations, showInsecureStreams) {
        if (showInsecureStreams) {
            uiState.topStations
        } else {
            uiState.topStations.filterNot { station -> station.streamUrl.isInsecureHttpStream() }
        }
    }
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
            .background(MaterialTheme.colorScheme.background),
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
                    text = tr("Entdecken & Suche", "Discover & Search"),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = tr("Aktualisieren", "Refresh"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        tr("Sender, Genre, Land...", "Station, genre, country..."),
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
                    focusedBorderColor = TealAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
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
                        text = tr("Schnell-Genres", "Quick Genres"),
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
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { advancedFiltersExpanded = !advancedFiltersExpanded }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = tr("Weitere Filter", "More filters"),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = if (advancedFiltersExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
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
                        tr("${visibleStations.size} Sender gefunden", "${visibleStations.size} stations found")
                    } else if (searchQuery.isBlank() && selectedCountry == null) {
                        tr("Top Sender", "Top stations")
                    } else {
                        tr("Sender", "Stations")
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
                            ?: tr("Beliebtheit", "Popularity")
                        Text(
                            text = tr("Sort: ", "Sort: ") + currentSortLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = TealAccent,
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = tr("Sortierung waehlen", "Choose sorting"),
                            tint = TealAccent,
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
                                        color = if (uiState.sortOption == option) TealAccent else MaterialTheme.colorScheme.onSurface,
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
                            text = tr("Lade Sender...", "Loading stations..."),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                visibleStations.isEmpty() && searchQuery.isBlank() && selectedCountry == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tr("Gib einen Suchbegriff ein\noder waehle ein Genre/Land", "Enter a search term\nor pick a genre/country"),
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
                        isGerman = isGerman,
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
                        contentDescription = tr("Nach oben", "To top"),
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
    isGerman: Boolean,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun tr(de: String, en: String): String = if (isGerman) de else en
    val suggestions = listOf("Rock", "Jazz", "Pop", "Dance", "Classical")
    
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = tr("Keine Sender gefunden", "No stations found"),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val contextText = when {
            searchQuery.isNotBlank() && selectedCountry != null -> {
                val countryName = topCountries.find { it.code == selectedCountry }?.name ?: selectedCountry
                tr("Fuer \"$searchQuery\" in $countryName", "For \"$searchQuery\" in $countryName")
            }
            searchQuery.isNotBlank() -> tr("Fuer \"$searchQuery\"", "For \"$searchQuery\"")
            selectedCountry != null -> {
                val countryName = topCountries.find { it.code == selectedCountry }?.name ?: selectedCountry
                tr("In $countryName", "In $countryName")
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
            text = tr("Vorschlaege:", "Suggestions:"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    showInsecureBadge: Boolean,
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
            if (showInsecureBadge && station.streamUrl.isInsecureHttpStream()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE65100).copy(alpha = 0.92f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f),
                    ),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 8.dp, start = 8.dp),
                ) {
                    Text(
                        text = "HTTP",
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

private fun String.isInsecureHttpStream(): Boolean {
    return startsWith("http://", ignoreCase = true)
}
