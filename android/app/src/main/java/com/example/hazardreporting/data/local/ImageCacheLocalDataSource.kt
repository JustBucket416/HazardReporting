package com.example.hazardreporting.data.local

import coil3.ImageLoader
import javax.inject.Inject

/**
 * The caches of Coil's [ImageLoader], which hold downloaded photos
 *
 * @property imageLoader the app's image loader
 *
 * @author Roman Pliskin on 29.10.2026
 */
class ImageCacheLocalDataSource @Inject constructor(
    private val imageLoader: ImageLoader,
) {

    /**
     * Clears the memory cache and the disk cache. Blocks on the disk, so it must run off the main thread
     */
    fun clear() {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }
}
