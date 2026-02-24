package de.radiowave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.radiowave.core.model.PlayerState
import de.radiowave.core.ui.theme.DarkBackground
import de.radiowave.core.ui.theme.DarkSurface
import de.radiowave.core.ui.theme.RadioWaveTheme
import de.radiowave.core.ui.theme.TealAccent
import de.radiowave.feature.browse.BrowseScreen
import de.radiowave.feature.favorites.FavoritesScreen
import de.radiowave.feature.home.HomeScreen
import de.radiowave.feature.home.HomePremiumBackground
import de.radiowave.feature.home.HomeViewModel
import de.radiowave.feature.player.FloatingPlayerBar
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
        title = "Suchen",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
    )

    object Favorites : BottomNavItem(
        route = "favorites",
        title = "Favoriten",
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
    )

    object Settings : BottomNavItem(
        route = "settings",
        title = "Einstellungen",
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

    val currentStation = playerState.currentStation
    val showPlayerBar = currentStation != null

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 0.dp,
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navigateToTopLevel(item.route)
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealAccent,
                            selectedTextColor = TealAccent,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = TealAccent.copy(alpha = 0.18f),
                        ),
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            val isHomeRoute = currentDestination
                ?.hierarchy
                ?.any { destination -> destination.route == BottomNavItem.Home.route } == true

            if (isHomeRoute) {
                HomePremiumBackground(
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                NavHost(
                    navController = navController,
                    startDestination = BottomNavItem.Home.route,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(BottomNavItem.Home.route) {
                        HomeScreen(
                            onStationClick = { station ->
                                homeViewModel.playStation(station)
                            },
                            onViewAllFavorites = {
                                navigateToTopLevel(BottomNavItem.Favorites.route)
                            },
                            onNavigateToBrowse = { category ->
                                homeViewModel.onSearchQueryChange(category)
                                navigateToTopLevel(BottomNavItem.Browse.route)
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

            if (showPlayerBar) {
                FloatingPlayerBar(
                    playerState = playerState,
                    onPlayPauseClick = { homeViewModel.togglePlayPause() },
                    onBarClick = { },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
