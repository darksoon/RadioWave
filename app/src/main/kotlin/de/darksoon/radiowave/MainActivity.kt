// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.annotation.StringRes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dagger.hilt.android.AndroidEntryPoint
import de.darksoon.radiowave.core.cast.CastManager
import de.darksoon.radiowave.core.model.AppSettings
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.ui.components.AmbientBackground
import de.darksoon.radiowave.core.ui.theme.DarkBackground
import de.darksoon.radiowave.core.ui.theme.DarkSurface
import de.darksoon.radiowave.core.ui.theme.RadioWaveTheme
import de.darksoon.radiowave.core.ui.theme.RadioAccent
import de.darksoon.radiowave.feature.browse.BrowseScreen
import de.darksoon.radiowave.feature.favorites.FavoritesScreen
import de.darksoon.radiowave.feature.home.HomeScreen
import de.darksoon.radiowave.feature.home.HomeViewModel
import de.darksoon.radiowave.feature.player.FloatingPlayerBar
import de.darksoon.radiowave.feature.player.PlayerScreen
import de.darksoon.radiowave.feature.settings.SettingsScreen
import de.darksoon.radiowave.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @javax.inject.Inject
    lateinit var castManager: CastManager

    private var shortcutTarget by mutableStateOf<String?>(null)
    private var shortcutRequestNonce by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        shortcutTarget = intent.shortcutTarget
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val context = LocalContext.current
            val prefs = remember(context) {
                context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)
            }
            var dynamicColors by remember {
                mutableStateOf(prefs.getBoolean(AppSettings.KEY_DYNAMIC_COLORS, false))
            }
            var themeMode by remember {
                mutableStateOf(
                    prefs.getString(AppSettings.KEY_THEME_MODE, AppSettings.THEME_DARK)
                        ?: AppSettings.THEME_DARK,
                )
            }
            val systemInDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                AppSettings.THEME_LIGHT -> false
                AppSettings.THEME_DARK -> true
                else -> systemInDark
            }

            DisposableEffect(prefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        AppSettings.KEY_APP_LANGUAGE -> {
                            val language = prefs.getString(
                                AppSettings.KEY_APP_LANGUAGE,
                                AppSettings.LANGUAGE_SYSTEM,
                            ) ?: AppSettings.LANGUAGE_SYSTEM
                            AppLanguageManager.applyLanguage(language)
                        }

                        AppSettings.KEY_DYNAMIC_COLORS -> {
                            dynamicColors = prefs.getBoolean(AppSettings.KEY_DYNAMIC_COLORS, false)
                        }

                        AppSettings.KEY_THEME_MODE -> {
                            themeMode = prefs.getString(
                                AppSettings.KEY_THEME_MODE,
                                AppSettings.THEME_DARK,
                            ) ?: AppSettings.THEME_DARK
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
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RadioWaveMainScreen(
                        shortcutTarget = shortcutTarget,
                        shortcutRequestNonce = shortcutRequestNonce,
                        onShortcutConsumed = { shortcutTarget = null },
                        castManager = castManager,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutTarget = intent.shortcutTarget
        shortcutRequestNonce++
    }
}

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    object Home : BottomNavItem(
        route = "home",
        titleRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    )

    object Browse : BottomNavItem(
        route = "browse",
        titleRes = R.string.nav_browse,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
    )

    object Favorites : BottomNavItem(
        route = "favorites",
        titleRes = R.string.nav_favorites,
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
    )

    object Settings : BottomNavItem(
        route = "settings",
        titleRes = R.string.nav_settings,
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

private data class OnboardingStepContent(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val icon: ImageVector,
    val accentColor: Color,
)

@Composable
private fun OnboardingDialog(
    step: Int,
    onStepChange: (Int) -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
) {
    val steps = listOf(
        OnboardingStepContent(
            titleRes = R.string.onboarding_step_welcome_title,
            bodyRes = R.string.onboarding_step_welcome_body,
            icon = Icons.Filled.Favorite,
            accentColor = Color(0xFF52E3D9),
        ),
        OnboardingStepContent(
            titleRes = R.string.onboarding_step_search_title,
            bodyRes = R.string.onboarding_step_search_body,
            icon = Icons.Filled.Search,
            accentColor = Color(0xFFCA4D95),
        ),
        OnboardingStepContent(
            titleRes = R.string.onboarding_step_player_title,
            bodyRes = R.string.onboarding_step_player_body,
            icon = Icons.Filled.PlayArrow,
            accentColor = Color(0xFF52E3D9),
        ),
        OnboardingStepContent(
            titleRes = R.string.onboarding_step_auto_title,
            bodyRes = R.string.onboarding_step_auto_body,
            icon = Icons.Filled.Star,
            accentColor = Color(0xFFCA4D95),
        ),
    )
    val safeStep = step.coerceIn(0, steps.lastIndex)
    val content = steps[safeStep]
    val isLast = safeStep >= steps.lastIndex

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Ambient gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                content.accentColor.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                            radius = 900f,
                        ),
                    ),
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Icon with glowing circle
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        content.accentColor.copy(alpha = 0.22f),
                                        Color.Transparent,
                                    ),
                                ),
                                shape = CircleShape,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = content.accentColor.copy(alpha = 0.12f),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = content.icon,
                            transitionSpec = {
                                (fadeIn() + slideInHorizontally { it / 3 })
                                    .togetherWith(fadeOut() + slideOutHorizontally { -it / 3 })
                            },
                            label = "onboardingIcon",
                        ) { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = content.accentColor,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Title
                AnimatedContent(
                    targetState = safeStep,
                    transitionSpec = {
                        (fadeIn() + slideInHorizontally { it / 4 })
                            .togetherWith(fadeOut() + slideOutHorizontally { -it / 4 })
                    },
                    label = "onboardingTitle",
                ) { s ->
                    Text(
                        text = stringResource(steps[s].titleRes),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Body
                AnimatedContent(
                    targetState = safeStep,
                    transitionSpec = {
                        (fadeIn()).togetherWith(fadeOut())
                    },
                    label = "onboardingBody",
                ) { s ->
                    Text(
                        text = stringResource(steps[s].bodyRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Page dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    steps.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == safeStep) 24.dp else 8.dp, 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (index == safeStep) content.accentColor
                                    else Color.White.copy(alpha = 0.25f),
                                ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Primary button
                Button(
                    onClick = { if (isLast) onFinish() else onStepChange(safeStep + 1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = content.accentColor,
                        contentColor = DarkBackground,
                    ),
                ) {
                    Text(
                        text = stringResource(
                            if (isLast) R.string.onboarding_finish else R.string.onboarding_next,
                        ),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Skip / Back row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (safeStep > 0) {
                        TextButton(onClick = { onStepChange(safeStep - 1) }) {
                            Text(
                                text = stringResource(R.string.onboarding_back),
                                color = Color.White.copy(alpha = 0.5f),
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (!isLast) {
                        TextButton(onClick = onSkip) {
                            Text(
                                text = stringResource(R.string.onboarding_skip),
                                color = Color.White.copy(alpha = 0.5f),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
            }
        }
    }
}

@Composable
fun RadioWaveMainScreen(
    shortcutTarget: String? = null,
    shortcutRequestNonce: Int = 0,
    onShortcutConsumed: () -> Unit = {},
    castManager: CastManager,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val view = LocalView.current
    val homeViewModel: HomeViewModel = hiltViewModel()
    val playerState: PlayerState by homeViewModel.playerState.collectAsStateWithLifecycle()
    val favoriteStationIds by homeViewModel.favoriteStationIds.collectAsStateWithLifecycle()
    val isCasting by castManager.isCasting.collectAsStateWithLifecycle()
    val isRemotePlaying by castManager.isRemotePlaying.collectAsStateWithLifecycle()
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
    var sleepTimerEndsAtElapsedMs by rememberSaveable { mutableStateOf<Long?>(null) }
    var showOnboarding by rememberSaveable {
        mutableStateOf(
            !prefs.getBoolean(AppSettings.KEY_FIRST_RUN_ONBOARDING_DONE, false),
        )
    }
    var onboardingStep by rememberSaveable { mutableIntStateOf(0) }
    val effectivePlayerState = if (isCasting) {
        playerState.copy(
            isPlaying = isRemotePlaying,
            isBuffering = false,
            isLoading = false,
        )
    } else {
        playerState
    }
    val handlePlayPause = {
        if (castManager.isCastSessionActive()) {
            castManager.togglePlayback()
        } else {
            homeViewModel.togglePlayPause()
        }
    }

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
        val shouldKeepOn = showFullscreenPlayer && keepScreenOnFullscreen
        if (shouldKeepOn) view.keepScreenOn = true
        onDispose {
            if (shouldKeepOn) view.keepScreenOn = false
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
        stationUuid in favoriteStationIds
    } ?: false

    LaunchedEffect(showPlayerBar) {
        if (!showPlayerBar) {
            showFullscreenPlayer = false
        }
    }

    LaunchedEffect(sleepTimerEndsAtElapsedMs) {
        val endsAt = sleepTimerEndsAtElapsedMs ?: return@LaunchedEffect
        val delayMs = (endsAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        kotlinx.coroutines.delay(delayMs)
        if (sleepTimerEndsAtElapsedMs == endsAt) {
            sleepTimerEndsAtElapsedMs = null
            showFullscreenPlayer = false
            homeViewModel.stopPlayback()
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

    LaunchedEffect(shortcutRequestNonce, shortcutTarget, currentStation) {
        when (shortcutTarget) {
            ShortcutTarget.BROWSE -> {
                navigateToTopLevel(BottomNavItem.Browse.route)
                onShortcutConsumed()
            }
            ShortcutTarget.FAVORITES -> {
                navigateToTopLevel(BottomNavItem.Favorites.route)
                onShortcutConsumed()
            }
            ShortcutTarget.SETTINGS -> {
                navigateToTopLevel(BottomNavItem.Settings.route)
                onShortcutConsumed()
            }
            ShortcutTarget.PLAYER -> {
                if (currentStation != null) {
                    showFullscreenPlayer = true
                } else {
                    navigateToTopLevel(BottomNavItem.Home.route)
                    Toast.makeText(
                        context,
                        context.getString(R.string.shortcut_player_unavailable),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                onShortcutConsumed()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface.copy(alpha = 0.92f),
                tonalElevation = 0.dp,
            ) {
                bottomNavItems.forEach { item ->
                    val title = stringResource(item.titleRes)
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
                                contentDescription = title,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = {
                            Text(
                                text = title,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RadioAccent,
                            selectedTextColor = RadioAccent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = RadioAccent.copy(alpha = 0.18f),
                        ),
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            AmbientBackground(modifier = Modifier.fillMaxSize())

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
                        SettingsScreen(
                            onRestartOnboarding = {
                                onboardingStep = 0
                                showOnboarding = true
                            },
                        )
                    }
                }
            }

            if (showPlayerBar) {
                FloatingPlayerBar(
                    playerState = effectivePlayerState,
                    isFavorite = isCurrentFavorite,
                    showMetadata = showMiniPlayerMetadata,
                    isCasting = isCasting,
                    onFavoriteClick = {
                        currentStation?.let { station ->
                            val willBeFavorite = !isCurrentFavorite
                            homeViewModel.toggleFavorite(station)
                            if (showQuickToasts) {
                                val message = if (willBeFavorite) {
                                    context.getString(R.string.toast_favorite_added)
                                } else {
                                    context.getString(R.string.toast_favorite_removed)
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onPlayPauseClick = handlePlayPause,
                    onBarClick = { showFullscreenPlayer = true },
                    onDismissed = {
                        showFullscreenPlayer = false
                        if (isCasting) {
                            castManager.stopCasting()
                        } else {
                            homeViewModel.stopPlayback()
                        }
                    },
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
                        playerState = effectivePlayerState,
                        isFavorite = isCurrentFavorite,
                        isCasting = isCasting,
                        onFavoriteClick = {
                            currentStation?.let { station ->
                                val willBeFavorite = !isCurrentFavorite
                                homeViewModel.toggleFavorite(station)
                                if (showQuickToasts) {
                                    val message = if (willBeFavorite) {
                                        context.getString(R.string.toast_favorite_added)
                                    } else {
                                        context.getString(R.string.toast_favorite_removed)
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onPlayPauseClick = handlePlayPause,
                        onPreviousStationClick = { homeViewModel.playPreviousStation() },
                        onVolumeToggle = { homeViewModel.toggleMute() },
                        onRandomStationClick = { homeViewModel.playRandomStation() },
                        onSleepTimerClick = { minutes ->
                            sleepTimerEndsAtElapsedMs = minutes?.let {
                                SystemClock.elapsedRealtime() + (it * 60_000L)
                            }
                        },
                        sleepTimerRemainingMs = sleepTimerEndsAtElapsedMs?.let { endsAt ->
                            (endsAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                        },
                        onDismiss = { showFullscreenPlayer = false },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (showOnboarding) {
                OnboardingDialog(
                    step = onboardingStep,
                    onStepChange = { onboardingStep = it },
                    onSkip = {
                        prefs.edit()
                            .putBoolean(AppSettings.KEY_FIRST_RUN_ONBOARDING_DONE, true)
                            .apply()
                        showOnboarding = false
                        onboardingStep = 0
                    },
                    onFinish = {
                        prefs.edit()
                            .putBoolean(AppSettings.KEY_FIRST_RUN_ONBOARDING_DONE, true)
                            .apply()
                        showOnboarding = false
                        onboardingStep = 0
                    },
                )
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



private object ShortcutTarget {
    const val BROWSE = "browse"
    const val FAVORITES = "favorites"
    const val PLAYER = "player"
    const val SETTINGS = "settings"
}

private val Intent?.shortcutTarget: String?
    get() = this
        ?.let { intent ->
            when (intent.action) {
                ACTION_OPEN_SEARCH -> ShortcutTarget.BROWSE
                ACTION_OPEN_FAVORITES -> ShortcutTarget.FAVORITES
                ACTION_OPEN_PLAYER -> ShortcutTarget.PLAYER
                ACTION_OPEN_SETTINGS -> ShortcutTarget.SETTINGS
                else -> null
            }
        }

const val ACTION_OPEN_SEARCH = "de.darksoon.radiowave.action.OPEN_SEARCH"
const val ACTION_OPEN_FAVORITES = "de.darksoon.radiowave.action.OPEN_FAVORITES"
const val ACTION_OPEN_PLAYER = "de.darksoon.radiowave.action.OPEN_PLAYER"
const val ACTION_OPEN_SETTINGS = "de.darksoon.radiowave.action.OPEN_SETTINGS"

