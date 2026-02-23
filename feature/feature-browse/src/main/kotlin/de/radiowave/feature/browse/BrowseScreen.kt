package de.radiowave.feature.browse

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import de.radiowave.core.model.Station
import de.radiowave.core.ui.theme.DarkBackground
import de.radiowave.core.ui.theme.DarkCardBackground
import de.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.radiowave.core.ui.theme.DarkSurfaceVariant
import de.radiowave.core.ui.theme.TealAccent
import de.radiowave.feature.home.HomeUiState
import de.radiowave.feature.home.HomeViewModel
import de.radiowave.feature.home.SortOption

private data class CountryItem(
    val code: String,
    val name: String,
    val flag: String,
)

private val topCountries = listOf(
    CountryItem("DE", "Deutschland", "🇩🇪"),
    CountryItem("US", "USA", "🇺🇸"),
    CountryItem("GB", "UK", "🇬🇧"),
    CountryItem("FR", "Frankreich", "🇫🇷"),
    CountryItem("IT", "Italien", "🇮🇹"),
    CountryItem("ES", "Spanien", "🇪🇸"),
    CountryItem("NL", "Niederlande", "🇳🇱"),
    CountryItem("PL", "Polen", "🇵🇱"),
    CountryItem("AT", "Österreich", "🇦🇹"),
    CountryItem("CH", "Schweiz", "🇨🇭"),
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

private val sortOptions = listOf(
    SortOption.POPULARITY to "Beliebtheit",
    SortOption.NAME to "Name",
    SortOption.COUNTRY to "Land",
)

@Composable
fun BrowseScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    var selectedGenre by remember { mutableStateOf<String?>(null) }

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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Entdecken",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Nach Land",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = DarkOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(topCountries) { country ->
                val isSelected = selectedCountry == country.code
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onCountrySelected(if (isSelected) null else country.code)
                    },
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Nach Genre",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = DarkOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(popularGenres) { (displayName, tag) ->
                val isSelected = selectedGenre == tag
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onGenreSelected(if (isSelected) null else tag)
                    },
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
                    columns = GridCells.Fixed(2),
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
                    items(uiState.topStations) { station ->
                        StationGridCard(
                            station = station,
                            onClick = { onStationClick(station) },
                        )
                    }
                }
            }
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
                "Für \"$searchQuery\" in $countryName"
            }
            searchQuery.isNotBlank() -> "Für \"$searchQuery\""
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
            text = "Vorschläge:",
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCardBackground,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
        ),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant),
            ) {
                SubcomposeAsyncImage(
                    model = station.faviconUrl,
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
                    fontSize = 14.sp,
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
    }
}
