package com.example.hazardreporting.data.incident

import android.content.Context
import androidx.core.net.toUri
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/**
 * Stores photo files in the app's private `files/photos/` directory, which the camera writes to through
 * a `FileProvider` and gallery picks are copied into
 *
 * @property context gives access to the app's files and to the content of picked pictures
 *
 * @author Roman Pliskin on 29.10.2026
 */
class PhotoLocalDataSource @Inject constructor(
    private val context: Context,
) {

    private val photosDirectory: File
        get() = File(context.filesDir, PHOTOS_DIRECTORY)

    /**
     * Returns the absolute path of a new, not yet existing JPEG file in the photo directory
     */
    fun createPhotoFile(): String = newPhotoFile(JPEG_EXTENSION).absolutePath

    /**
     * Copies the picture at [sourceUri] into a new file in the photo directory, keeping its image type
     *
     * @return the absolute path of the copy
     * @throws IOException if the picture can't be read or written; no partial copy is left behind
     */
    fun importPhoto(sourceUri: String): String {
        val uri = sourceUri.toUri()
        val resolver = context.contentResolver
        val target = newPhotoFile(extensionOf(resolver.getType(uri)))
        try {
            val input = resolver.openInputStream(uri) ?: throw FileNotFoundException("Can't open $sourceUri")
            input.use { source -> target.outputStream().use { source.copyTo(it) } }
        } catch (e: IOException) {
            target.delete()
            throw e
        }
        return target.absolutePath
    }

    /**
     * Deletes the photo file at [path]. A path outside the photo directory is left alone
     */
    fun deletePhoto(path: String) {
        val file = File(path)
        if (file.canonicalFile.parentFile == photosDirectory.canonicalFile) file.delete()
    }

    private fun newPhotoFile(extension: String): File {
        val directory = photosDirectory
        directory.mkdirs()
        return File(directory, "${UUID.randomUUID()}.$extension")
    }

    // Only the types the server accepts keep their own extension; anything else is named as a JPEG
    private fun extensionOf(mimeType: String?): String = when (mimeType) {
        PNG_MIME_TYPE -> PNG_EXTENSION
        WEBP_MIME_TYPE -> WEBP_EXTENSION
        else -> JPEG_EXTENSION
    }

    private companion object {
        const val PHOTOS_DIRECTORY = "photos"
        const val JPEG_EXTENSION = "jpg"
        const val PNG_EXTENSION = "png"
        const val WEBP_EXTENSION = "webp"
        const val PNG_MIME_TYPE = "image/png"
        const val WEBP_MIME_TYPE = "image/webp"
    }
}
