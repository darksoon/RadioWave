package de.radiowave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.radiowave.core.model.PlayerState
import de.radiowave.core.ui.theme.RadioWaveTheme
import de.radiowave.feature.home.HomeScreen
import de.radiowave.feature.home.HomeViewModel
import de.radiowave.feature.player.BottomPlayerBar

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RadioWaveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RadioWaveMainScreen()
                }
            }
        }
    }
}

@Composable
fun RadioWaveMainScreen() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = hiltViewModel()
    val playerState: PlayerState by homeViewModel.playerState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomPlayerBar(
                playerState = playerState,
                onPlayPauseClick = { homeViewModel.togglePlayPause() },
                onBarClick = { /* TODO: Open full player */ },
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = "home",
            ) {
                composable("home") {
                    HomeScreen(
                        onStationClick = { station ->
                            homeViewModel.playStation(station)
                        },
                        onViewAllFavorites = {
                            // TODO: Navigate to favorites
                        },
                    )
                }
            }
        }
    }
}
