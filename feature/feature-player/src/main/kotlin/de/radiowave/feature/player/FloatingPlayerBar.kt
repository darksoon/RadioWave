package de.radiowave.feature.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import de.radiowave.core.model.PlayerState
import de.radiowave.core.model.Station
import de.radiowave.core.ui.theme.DarkCardBackground
import de.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.radiowave.core.ui.theme.DarkSurfaceVariant
import de.radiowave.core.ui.theme.TealAccent

@Composable
fun FloatingPlayerBar(
    playerState: PlayerState,
    onPlayPauseClick: () -> Unit,
    onBarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentStation: Station? = playerState.currentStation
    val isPlaying: Boolean = playerState.isPlaying
    val isBuffering: Boolean = playerState.isBuffering
    val metadataTitle: String? = playerState.metadata?.title

    if (currentStation != null) {
        val stationName: String = currentStation.name
        val faviconUrl: String? = currentStation.faviconUrl

        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onBarClick),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f),
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 10.dp,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.11f),
                                Color(0xAA26313D),
                                DarkCardBackground.copy(alpha = 0.9f),
                            ),
                        ),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.22f)),
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LinearProgressIndicator(
                        progress = {
                            when {
                                isBuffering -> 0.45f
                                isPlaying -> 1f
                                else -> 0f
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(2.dp),
                        color = TealAccent.copy(alpha = 0.9f),
                        trackColor = Color.Transparent,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant),
                        ) {
                            AsyncImage(
                                model = faviconUrl,
                                contentDescription = stationName,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Crop,
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stationName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White,
                            )

                            if (isBuffering) {
                                Text(
                                    text = "Wird geladen...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TealAccent,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 1.dp),
                                )
                            } else {
                                Text(
                                    text = metadataTitle ?: "Live Stream",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkOnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 1.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        Box(
                            modifier = Modifier.padding(end = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.dp,
                                    color = TealAccent,
                                    trackColor = Color.Transparent,
                                )
                            }

                            Surface(
                                onClick = onPlayPauseClick,
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.2f),
                                ),
                                enabled = !isBuffering,
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .padding(6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
