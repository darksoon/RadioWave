// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale

/**
 * Station artwork URLs (`favicon`) come from the community-editable Radio Browser API, the
 * same trust level as `streamUrl` — but unlike `streamUrl`, this field was historically never
 * scheme-checked before reaching Coil, whose default loader also resolves `file://`/`content://`
 * URIs. A malicious/misconfigured `favicon` value could otherwise read local app files or
 * another app's exported content provider into the rendered image. This is the single
 * composable every screen uses to render station art, so filtering here covers every caller.
 */
private fun String?.asSafeHttpImageUrl(): String? {
    if (isNullOrBlank()) return null
    val scheme = runCatching { Uri.parse(this).scheme }.getOrNull()?.lowercase()
    return if (scheme == "http" || scheme == "https") this else null
}

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
    val safeImageUrl = imageUrl.asSafeHttpImageUrl()
    val request = remember(safeImageUrl, context) {
        ImageRequest.Builder(context)
            .data(safeImageUrl)
            .crossfade(200)
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

