// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.darksoon.radiowave.core.ui.theme.RadioAccent

@Composable
fun StreamQualityBadge(
    codec: String?,
    bitrate: Int?,
    modifier: Modifier = Modifier,
) {
    val label = formatStreamQualityLabel(codec, bitrate) ?: return
    val isHighQuality = (bitrate ?: 0) >= 128
    val badgeColor = if (isHighQuality) {
        RadioAccent.copy(alpha = 0.18f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    val textColor = if (isHighQuality) {
        RadioAccent.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

fun formatStreamQualityLabel(codec: String?, bitrate: Int?): String? {
    val normalizedCodec = codec?.trim().takeUnless { it.isNullOrBlank() }?.uppercase()
    val normalizedBitrate = bitrate?.takeIf { it > 0 }
    return when {
        normalizedCodec != null && normalizedBitrate != null -> "$normalizedCodec $normalizedBitrate kbps"
        normalizedCodec != null -> normalizedCodec
        normalizedBitrate != null -> "$normalizedBitrate kbps"
        else -> null
    }
}
