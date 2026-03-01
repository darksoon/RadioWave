package de.radiowave.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import de.radiowave.core.model.Station
import de.radiowave.core.ui.components.StationLogoImage
import de.radiowave.core.ui.theme.DarkCardBackground
import de.radiowave.core.ui.theme.DarkOnSurfaceVariant
import de.radiowave.core.ui.theme.DarkSurfaceVariant
import de.radiowave.core.ui.theme.TealAccent

@Composable
fun StationListItem(
    station: Station,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showPlayButton: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCardBackground.copy(alpha = 0.68f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant),
        ) {
            StationLogoImage(
                imageUrl = station.faviconUrl,
                contentDescription = station.name,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
            )

            val country: String? = station.country
            val tags: List<String> = station.tags
            if (country != null || tags.isNotEmpty()) {
                Text(
                    text = buildString {
                        country?.let { append(it) }
                        if (country != null && tags.isNotEmpty()) append(" • ")
                        append(tags.take(2).joinToString(", "))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        if (showPlayButton) {
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TealAccent),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play ${station.name}",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}
