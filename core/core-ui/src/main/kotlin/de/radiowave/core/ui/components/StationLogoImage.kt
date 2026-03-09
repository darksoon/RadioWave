// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import coil.size.Precision
import coil.size.Scale

@Composable
fun StationLogoImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    loading: @Composable () -> Unit = {},
    error: @Composable () -> Unit = loading,
) {
    val context = LocalContext.current
    val request = remember(imageUrl, context) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .allowHardware(true)
            // Logos are rendered small in lists; lower decode precision reduces fling jank.
            .precision(Precision.INEXACT)
            .scale(Scale.FILL)
            .build()
    }
    val painter = rememberAsyncImagePainter(model = request)
    val state = painter.state

    Box(modifier = modifier) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
        )

        when (state) {
            is AsyncImagePainter.State.Error -> error()
            is AsyncImagePainter.State.Loading -> loading()
            else -> Unit
        }
    }
}

