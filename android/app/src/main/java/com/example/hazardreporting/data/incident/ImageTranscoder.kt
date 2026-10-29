package com.example.hazardreporting.data.incident

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.core.net.toUri
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

/**
 * Re-encodes pictures the server doesn't accept, e.g. HEIC photos from the gallery, as JPEG.
 * Uses [ImageDecoder] where it exists (API 28+, which also decodes HEIF) and [BitmapFactory] before that
 *
 * @property context gives access to the content of picked pictures
 *
 * @author Roman Pliskin on 29.10.2026
 */
class ImageTranscoder @Inject constructor(
    private val context: Context,
) {

    /**
     * Decodes the picture at [sourceUri] and writes it to [target] as a JPEG of quality [JPEG_QUALITY]
     *
     * @throws IOException if the picture can't be read or decoded, or [target] can't be written
     */
    fun transcodeToJpeg(sourceUri: String, target: File) {
        val bitmap = decode(sourceUri)
        try {
            val isWritten = target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            if (!isWritten) throw IOException("Can't encode $sourceUri as JPEG")
        } finally {
            bitmap.recycle()
        }
    }

    private fun decode(sourceUri: String): Bitmap {
        val uri = sourceUri.toUri()
        val resolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // A software bitmap, because a hardware one can't be compressed
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val input = resolver.openInputStream(uri) ?: throw FileNotFoundException("Can't open $sourceUri")
            input.use { BitmapFactory.decodeStream(it) } ?: throw IOException("Can't decode $sourceUri")
        }
    }

    private companion object {
        const val JPEG_QUALITY = 90
    }
}
