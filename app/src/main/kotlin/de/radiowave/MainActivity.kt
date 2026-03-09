// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import de.radiowave.core.ui.theme.RadioWaveTheme
import de.radiowave.core.ui.theme.TealAccent
import de.radiowave.feature.browse.BrowseScreen
import de.radiowave.feature.favorites.FavoritesScreen
import de.radiowave.feature.home.HomeScreen
import de.radiowave.feature.home.HomeViewModel
import de.radiowave.feature.player.FloatingPlayerBar
import de.radiowave.feature.player.PlayerScreen
import de.radiowave.feature.settings.SettingsScreen
import de.radiowave.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import de.radiowave.core.data.update.GitHubReleaseInfo
import de.radiowave.core.data.update.GitHubReleaseUpdater
import de.radiowave.core.data.update.UpdateDownloadProgress
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var shortcutTarget by mutableStateOf<String?>(null)
    private var shortcutRequestNonce by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        shortcutTarget = intent.shortcutTarget
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember(context) {
                context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)
            }
            var dynamicColors by remember {
                mutableStateOf(prefs.getBoolean(AppSettings.KEY_DYNAMIC_COLORS, false))
            }
            val useDarkTheme = true

            LaunchedEffect(Unit) {
                if (prefs.getString(AppSettings.KEY_THEME_MODE, AppSettings.THEME_DARK) != AppSettings.THEME_DARK) {
                    prefs.edit().putString(AppSettings.KEY_THEME_MODE, AppSettings.THEME_DARK).apply()
                }
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

@Composable
fun RadioWaveMainScreen(
    shortcutTarget: String? = null,
    shortcutRequestNonce: Int = 0,
    onShortcutConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val view = LocalView.current
    val homeViewModel: HomeViewModel = hiltViewModel()
    val playerState: PlayerState by homeViewModel.playerState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val prefs = remember(context) {
        context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)
    }
    val coroutineScope = rememberCoroutineScope()
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
    var showFirstRunInfo by rememberSaveable {
        mutableStateOf(
            !prefs.getBoolean(AppSettings.KEY_FIRST_RUN_ONBOARDING_DONE, false),
        )
    }
    var availableUpdate by remember { mutableStateOf<GitHubReleaseInfo?>(null) }
    var updateInProgress by remember { mutableStateOf(false) }
    var updateProgress by remember { mutableStateOf<UpdateDownloadProgress?>(null) }

    suspend fun checkForAppUpdate(force: Boolean) {
        val autoCheckEnabled = prefs.getBoolean(AppSettings.KEY_UPDATE_CHECK_ENABLED, true)
        if (!autoCheckEnabled) return
        val now = System.currentTimeMillis()
        val lastCheckAt = prefs.getLong(AppSettings.KEY_LAST_UPDATE_CHECK_AT_MS, 0L)
        if (!force && now - lastCheckAt < UPDATE_CHECK_INTERVAL_MS) return
        prefs.edit().putLong(AppSettings.KEY_LAST_UPDATE_CHECK_AT_MS, now).apply()
        val versionName = readVersionName(context)
        val includePrerelease = prefs.getBoolean(AppSettings.KEY_UPDATE_BETA_CHANNEL_ENABLED, false)
        runCatching {
            GitHubReleaseUpdater.checkForUpdate(
                currentVersionName = versionName,
                includePrerelease = includePrerelease,
            )
        }.onSuccess { update ->
            if (update == null) return@onSuccess
            val popupEnabled = prefs.getBoolean(AppSettings.KEY_UPDATE_POPUP_ENABLED, true)
            if (!popupEnabled) return@onSuccess
            val dismissedTag = prefs.getString(AppSettings.KEY_LAST_DISMISSED_UPDATE_TAG, null)
            if (dismissedTag.equals(update.tag, ignoreCase = true)) return@onSuccess
            availableUpdate = update
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

    LaunchedEffect(showFirstRunInfo) {
        if (!showFirstRunInfo) {
            checkForAppUpdate(force = false)
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
                containerColor = MaterialTheme.colorScheme.surface,
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
                            selectedIconColor = TealAccent,
                            selectedTextColor = TealAccent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
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
                                    context.getString(R.string.toast_favorite_added)
                                } else {
                                    context.getString(R.string.toast_favorite_removed)
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
                                        context.getString(R.string.toast_favorite_added)
                                    } else {
                                        context.getString(R.string.toast_favorite_removed)
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

            if (showFirstRunInfo) {
                AlertDialog(
                    onDismissRequest = { },
                    title = {
                        Text(stringResource(R.string.first_run_title))
                    },
                    text = {
                        Text(stringResource(R.string.first_run_body))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                prefs.edit()
                                    .putBoolean(AppSettings.KEY_FIRST_RUN_ONBOARDING_DONE, true)
                                    .apply()
                                showFirstRunInfo = false
                                coroutineScope.launch {
                                    checkForAppUpdate(force = true)
                                }
                            },
                        ) {
                            Text(stringResource(R.string.first_run_confirm))
                        }
                    },
                )
            }

            val updateRelease = availableUpdate
            if (updateRelease != null) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text(stringResource(R.string.update_dialog_title)) },
                    text = {
                        Column {
                            Text(
                                "${stringResource(R.string.update_dialog_version, updateRelease.tag)}\n\n" +
                                    "${previewReleaseNotes(context, updateRelease.body)}",
                            )
                            if (updateInProgress) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val progress = updateProgress
                                val progressText = buildUpdateProgressText(context, progress)
                                Text(
                                    text = stringResource(R.string.update_dialog_download_running, progressText),
                                    color = TealAccent,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val percent = progress?.percent
                                if (percent != null) {
                                    LinearProgressIndicator(
                                        progress = { percent / 100f },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (updateInProgress) return@TextButton
                                updateInProgress = true
                                updateProgress = null
                                coroutineScope.launch {
                                    val result = GitHubReleaseUpdater.downloadAndStartInstall(
                                        context = context,
                                        release = updateRelease,
                                        onProgress = { progress ->
                                            updateProgress = progress
                                        },
                                    )
                                    updateInProgress = false
                                    result.onFailure { error ->
                                        val message = when (error.message) {
                                            "Please allow installs from unknown apps and try again" -> {
                                                context.getString(R.string.update_dialog_install_permission_required)
                                            }
                                            "No package installer activity found" -> {
                                                context.getString(R.string.update_dialog_no_installer)
                                            }
                                            else -> {
                                                error.message ?: context.getString(R.string.update_dialog_failed_unknown)
                                            }
                                        }
                                        Toast.makeText(
                                            context,
                                            context.getString(
                                                R.string.update_dialog_failed,
                                                message,
                                            ),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                }
                            },
                        ) {
                            Text(
                                if (updateInProgress) {
                                    stringResource(R.string.update_dialog_downloading)
                                } else {
                                    stringResource(R.string.update_dialog_update_action)
                                },
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                if (updateInProgress) return@TextButton
                                prefs.edit()
                                    .putString(AppSettings.KEY_LAST_DISMISSED_UPDATE_TAG, updateRelease.tag)
                                    .apply()
                                availableUpdate = null
                            },
                        ) {
                            Text(stringResource(R.string.update_dialog_later_action))
                        }
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

private fun previewReleaseNotes(context: Context, notes: String): String {
    if (notes.isBlank()) return context.getString(R.string.update_dialog_release_notes_fallback)
    return notes
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(10)
        .joinToString("\n")
}

private fun readVersionName(context: Context): String {
    return runCatching {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: ""
    }.getOrDefault("")
}

private fun buildUpdateProgressText(context: Context, progress: UpdateDownloadProgress?): String {
    if (progress == null) return context.getString(R.string.update_progress_preparing)
    val downloadedMb = progress.downloadedBytes / (1024f * 1024f)
    val totalMb = progress.totalBytes.takeIf { it > 0L }?.let { it / (1024f * 1024f) }
    return if (totalMb != null) {
        context.getString(
            R.string.update_progress_known,
            downloadedMb,
            totalMb,
            progress.percent ?: 0,
        )
    } else {
        context.getString(R.string.update_progress_unknown, downloadedMb)
    }
}

private const val UPDATE_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L

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

const val ACTION_OPEN_SEARCH = "de.radiowave.action.OPEN_SEARCH"
const val ACTION_OPEN_FAVORITES = "de.radiowave.action.OPEN_FAVORITES"
const val ACTION_OPEN_PLAYER = "de.radiowave.action.OPEN_PLAYER"
const val ACTION_OPEN_SETTINGS = "de.radiowave.action.OPEN_SETTINGS"

