package de.radiowave.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

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
    val request = ImageRequest.Builder(context)
        .data(imageUrl)
        .crossfade(false)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .allowHardware(true)
        .build()

    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = { loading() },
        error = { error() },
    )
}

