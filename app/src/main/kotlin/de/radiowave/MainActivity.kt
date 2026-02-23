package de.radiowave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.radiowave.core.model.PlayerState
import de.radiowave.core.ui.theme.DarkBackground
import de.radiowave.core.ui.theme.DarkCardBackground
import de.radiowave.core.ui.theme.DarkOverlay
import de.radiowave.core.ui.theme.DarkSurface
import de.radiowave.core.ui.theme.DarkSurfaceVariant
import de.radiowave.core.ui.theme.RadioWaveTheme
import de.radiowave.core.ui.theme.TealAccent
import de.radiowave.core.ui.theme.TealLight
import de.radiowave.feature.browse.BrowseScreen
import de.radiowave.feature.favorites.FavoritesScreen
import de.radiowave.feature.home.HomeScreen
import de.radiowave.feature.home.HomeViewModel
import de.radiowave.feature.player.BottomPlayerBar
import de.radiowave.feature.settings.SettingsScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RadioWaveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground,
                ) {
                    RadioWaveMainScreen()
                }
            }
        }
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    object Home : BottomNavItem(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )

    object Browse : BottomNavItem(
        route = "browse",
        title = "Browse",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
    )

    object Favorites : BottomNavItem(
        route = "favorites",
        title = "Favorites",
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
    )

    object Settings : BottomNavItem(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Browse,
    BottomNavItem.Favorites,
    BottomNavItem.Settings,
)

@Composable
fun RadioWaveMainScreen() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val playerState: PlayerState by homeViewModel.playerState.collectAsState()
    var selectedNavItem by remember { mutableIntStateOf(0) }

    val currentStation = playerState.currentStation
    val showPlayerBar = currentStation != null

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                DarkOverlay,
                            ),
                        ),
                    ),
            ) {
                if (showPlayerBar) {
                    BottomPlayerBar(
                        playerState = playerState,
                        onPlayPauseClick = { homeViewModel.togglePlayPause() },
                        onBarClick = { /* TODO: Open full player */ },
                    )
                }

                NavigationBar(
                    containerColor = DarkSurface.copy(alpha = 0.95f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(72.dp),
                ) {
                    bottomNavItems.forEachIndexed { index, item ->
                        val selected = selectedNavItem == index
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                selectedNavItem = index
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .then(
                                            if (selected) {
                                                Modifier
                                                    .background(
                                                        TealAccent.copy(alpha = 0.15f),
                                                        shape = androidx.compose.foundation.shape.CircleShape,
                                                    )
                                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                            } else {
                                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            },
                                        ),
                                ) {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (selected) TealAccent else Color.Gray,
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = if (selected) TealAccent else Color.Gray,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TealAccent,
                                selectedTextColor = TealAccent,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onStationClick = { station ->
                        homeViewModel.playStation(station)
                    },
                    onViewAllFavorites = {
                        selectedNavItem = 2
                        navController.navigate(BottomNavItem.Favorites.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(BottomNavItem.Browse.route) {
                BrowseScreen()
            }
            composable(BottomNavItem.Favorites.route) {
                FavoritesScreen()
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
