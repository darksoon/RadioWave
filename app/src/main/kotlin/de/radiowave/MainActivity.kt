package de.radiowave

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dagger.hilt.android.AndroidEntryPoint
import de.radiowave.core.model.AppSettings
import de.radiowave.core.model.PlayerState
import de.radiowave.core.ui.theme.DarkBackground
import de.radiowave.core.ui.theme.DarkSurface
import de.radiowave.core.ui.theme.RadioWaveTheme
import de.radiowave.core.ui.theme.TealAccent
import de.radiowave.feature.browse.BrowseScreen
import de.radiowave.feature.favorites.FavoritesScreen
import de.radiowave.feature.home.HomePremiumBackground
import de.radiowave.feature.home.HomeScreen
import de.radiowave.feature.home.HomeViewModel
import de.radiowave.feature.player.FloatingPlayerBar
import de.radiowave.feature.player.PlayerScreen
import de.radiowave.feature.settings.SettingsScreen
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember(context) {
                context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)
            }
            var themeMode by remember {
                mutableStateOf(
                    prefs.getString(AppSettings.KEY_THEME_MODE, AppSettings.THEME_SYSTEM)
                        ?: AppSettings.THEME_SYSTEM,
                )
            }
            var dynamicColors by remember {
                mutableStateOf(prefs.getBoolean(AppSettings.KEY_DYNAMIC_COLORS, false))
            }
            val systemDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                AppSettings.THEME_LIGHT -> false
                AppSettings.THEME_DARK -> true
                else -> systemDarkTheme
            }

            DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        AppSettings.KEY_THEME_MODE -> {
                            themeMode = prefs.getString(AppSettings.KEY_THEME_MODE, AppSettings.THEME_SYSTEM)
                                ?: AppSettings.THEME_SYSTEM
                        }

                        AppSettings.KEY_DYNAMIC_COLORS -> {
                            dynamicColors = prefs.getBoolean(AppSettings.KEY_DYNAMIC_COLORS, false)
                        }
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            RadioWaveTheme(
                darkTheme = useDarkTheme,
                dynamicColor = dynamicColors,
            ) {
                EnsureNotificationPermission()
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
    val context = LocalContext.current
    val view = LocalView.current
    val homeViewModel: HomeViewModel = hiltViewModel()
    val playerState: PlayerState by homeViewModel.playerState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val prefs = remember(context) {
        context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var showMiniPlayerMetadata by remember {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_SHOW_MINIPLAYER_METADATA, true))
    }
    var keepScreenOnFullscreen by remember {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_KEEP_SCREEN_ON_FULLSCREEN, false))
    }
    var showQuickToasts by remember {
        mutableStateOf(prefs.getBoolean(AppSettings.KEY_SHOW_QUICK_TOASTS, true))
    }
    var showFullscreenPlayer by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showFullscreenPlayer) {
        showFullscreenPlayer = false
    }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppSettings.KEY_SHOW_MINIPLAYER_METADATA -> {
                    showMiniPlayerMetadata = prefs.getBoolean(AppSettings.KEY_SHOW_MINIPLAYER_METADATA, true)
                }
                AppSettings.KEY_KEEP_SCREEN_ON_FULLSCREEN -> {
                    keepScreenOnFullscreen = prefs.getBoolean(AppSettings.KEY_KEEP_SCREEN_ON_FULLSCREEN, false)
                }
                AppSettings.KEY_SHOW_QUICK_TOASTS -> {
                    showQuickToasts = prefs.getBoolean(AppSettings.KEY_SHOW_QUICK_TOASTS, true)
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    DisposableEffect(showFullscreenPlayer, keepScreenOnFullscreen, view) {
        val previousValue = view.keepScreenOn
        view.keepScreenOn = previousValue || (showFullscreenPlayer && keepScreenOnFullscreen)
        onDispose {
            view.keepScreenOn = previousValue
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isSettingsRoute = currentDestination
        ?.hierarchy
        ?.any { destination -> destination.route == BottomNavItem.Settings.route } == true

    val currentStation = playerState.currentStation
    val showPlayerBar = currentStation != null && !isSettingsRoute
    val isCurrentFavorite = currentStation?.uuid?.let { stationUuid ->
        homeUiState.favoriteStations.any { station -> station.uuid == stationUuid }
    } ?: false

    LaunchedEffect(showPlayerBar) {
        if (!showPlayerBar) {
            showFullscreenPlayer = false
        }
    }

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
            HomePremiumBackground(
                modifier = Modifier.fillMaxSize(),
            )

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
                            viewModel = homeViewModel,
                        )
                    }
                    composable(BottomNavItem.Browse.route) {
                        BrowseScreen(viewModel = homeViewModel)
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
                    isFavorite = isCurrentFavorite,
                    showMetadata = showMiniPlayerMetadata,
                    onFavoriteClick = {
                        currentStation?.let { station ->
                            val willBeFavorite = !isCurrentFavorite
                            homeViewModel.toggleFavorite(station)
                            if (showQuickToasts) {
                                val message = if (willBeFavorite) {
                                    "Zu Favoriten hinzugefuegt"
                                } else {
                                    "Aus Favoriten entfernt"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onPlayPauseClick = { homeViewModel.togglePlayPause() },
                    onBarClick = { showFullscreenPlayer = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = innerPadding.calculateBottomPadding() + 12.dp,
                        )
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(32.dp)),
                )
            }

            if (showPlayerBar && showFullscreenPlayer) {
                Dialog(
                    onDismissRequest = { showFullscreenPlayer = false },
                    properties = DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = false,
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false,
                    ),
                ) {
                    PlayerScreen(
                        playerState = playerState,
                        isFavorite = isCurrentFavorite,
                        onFavoriteClick = {
                            currentStation?.let { station ->
                                val willBeFavorite = !isCurrentFavorite
                                homeViewModel.toggleFavorite(station)
                                if (showQuickToasts) {
                                    val message = if (willBeFavorite) {
                                        "Zu Favoriten hinzugefuegt"
                                    } else {
                                        "Aus Favoriten entfernt"
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onPlayPauseClick = { homeViewModel.togglePlayPause() },
                        onPreviousStationClick = { homeViewModel.playPreviousStation() },
                        onVolumeToggle = { homeViewModel.toggleMute() },
                        onRandomStationClick = { homeViewModel.playRandomStation() },
                        onDismiss = { showFullscreenPlayer = false },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EnsureNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
