// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.feature.player

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.darksoon.radiowave.core.model.PlayerState
import de.darksoon.radiowave.core.model.Station
import de.darksoon.radiowave.core.ui.components.StationLogoImage
import de.darksoon.radiowave.core.ui.theme.DarkCardBackground
import de.darksoon.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.darksoon.radiowave.core.ui.theme.DarkSurfaceVariant
import de.darksoon.radiowave.core.ui.theme.RadioAccent
import de.darksoon.radiowave.core.ui.theme.RadioAccentLight
import de.darksoon.radiowave.feature.player.R

@Composable
fun BottomPlayerBar(
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

        Column(
            modifier = modifier.fillMaxWidth(),
        ) {
            LinearProgressIndicator(
                progress = { if (isBuffering) 0.5f else if (isPlaying) 1f else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = RadioAccent,
                trackColor = DarkSurfaceVariant,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCardBackground)
                    .clickable(onClick = onBarClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant),
                    ) {
                        StationLogoImage(
                            imageUrl = faviconUrl,
                            contentDescription = stationName,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop,
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stationName,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White,
                        )

                        if (isBuffering) {
                            Text(
                                text = stringResource(R.string.player_loading),
                                style = MaterialTheme.typography.bodySmall,
                                color = RadioAccentLight,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        } else {
                            Text(
                                text = metadataTitle ?: stringResource(R.string.player_live_stream),
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkOnSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(RadioAccent),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) {
                                stringResource(R.string.player_pause)
                            } else {
                                stringResource(R.string.player_play)
                            },
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
        }
    }
}

