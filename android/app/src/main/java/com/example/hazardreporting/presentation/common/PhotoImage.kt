package com.example.hazardreporting.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.example.hazardreporting.R
import com.example.hazardreporting.domain.model.Photo
import java.io.File

/**
 * Shows an incident's [photo], cropped to fill its bounds. It shimmers while the image loads and says so
 * if loading fails. Clip the modifier to a shape first if the photo needs one
 *
 * @param photo the photo: a file on the device or a URL on the server
 * @param contentDescription describes the photo for accessibility
 * @param modifier modifier that sizes the photo
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Composable
fun PhotoImage(photo: Photo, contentDescription: String?, modifier: Modifier = Modifier) {
    var state by remember(photo) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val isLoading = state is AsyncImagePainter.State.Empty || state is AsyncImagePainter.State.Loading

    Box(
        modifier = modifier
            .background(ShimmerDefaults.placeholderColor)
            .shimmer(ShimmerDefaults.highlightColor, enabled = isLoading),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = photo.imageModel(),
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            onState = { state = it },
        )
        if (state is AsyncImagePainter.State.Error) {
            Text(
                text = stringResource(R.string.photo_load_failed),
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Returns what Coil loads for the photo: its file or its URL
 */
private fun Photo.imageModel(): Any = when (this) {
    is Photo.Local -> File(path)
    is Photo.Remote -> url
}
