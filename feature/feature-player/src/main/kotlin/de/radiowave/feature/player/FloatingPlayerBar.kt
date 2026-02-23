package de.radiowave.feature.player

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkCardBackground,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 16.dp,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                LinearProgressIndicator(
                    progress = { if (isBuffering) 0.5f else if (isPlaying) 1f else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = TealAccent,
                    trackColor = DarkSurfaceVariant,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant),
                    ) {
                        AsyncImage(
                            model = faviconUrl,
                            contentDescription = stationName,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop,
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stationName,
                            style = MaterialTheme.typography.bodyLarge.copy(
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

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 2.dp,
                                color = TealAccent,
                                trackColor = Color.Transparent,
                            )
                        }

                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(TealAccent),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
