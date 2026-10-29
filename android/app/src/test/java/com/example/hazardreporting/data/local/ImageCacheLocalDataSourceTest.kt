package com.example.hazardreporting.data.local

import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class ImageCacheLocalDataSourceTest {

    private val imageLoader = mockk<ImageLoader>()

    private val dataSource = ImageCacheLocalDataSource(imageLoader)

    @Test
    fun testClear() {
        val memoryCache = mockk<MemoryCache>()
        val diskCache = mockk<DiskCache>()

        every { imageLoader.memoryCache } returns memoryCache
        every { imageLoader.diskCache } returns diskCache
        justRun { memoryCache.clear() }
        justRun { diskCache.clear() }

        dataSource.clear()

        verify(exactly = 1) { memoryCache.clear() }
        verify(exactly = 1) { diskCache.clear() }
    }

    @Test
    fun testClear_NoCaches() {
        every { imageLoader.memoryCache } returns null
        every { imageLoader.diskCache } returns null

        // Nothing to clear, and nothing fails
        dataSource.clear()
    }
}
