package com.example.hazardreporting.data.incident

import android.content.Context
import androidx.core.net.toUri
import java.io.BufferedInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

/**
 * Stores photo files in the app's private `files/photos/` directory, which the camera writes to through
 * a `FileProvider` and gallery picks are copied into. Every stored file is in a format the server accepts
 * ([ImageFormat]); a picked picture in any other format is re-encoded as JPEG on import
 *
 * @property context gives access to the app's files and to the content of picked pictures
 * @property transcoder re-encodes picked pictures the server wouldn't accept
 *
 * @author Roman Pliskin on 29.10.2026
 */
class PhotoLocalDataSource @Inject constructor(
    private val context: Context,
    private val transcoder: ImageTranscoder,
) {

    private val photosDirectory: File
        get() = File(context.filesDir, PHOTOS_DIRECTORY)

    /**
     * Returns the absolute path of a new, not yet existing JPEG file in the photo directory
     */
    fun createPhotoFile(): String = newPhotoFile(ImageFormat.Jpeg).absolutePath

    /**
     * Copies the picture at [sourceUri] into a new file in the photo directory. A JPEG, PNG or WebP picture,
     * recognised by its first bytes, is copied as it is; anything else, e.g. HEIC, is re-encoded as JPEG,
     * because the server would refuse it
     *
     * @return the absolute path of the copy
     * @throws IOException if the picture can't be read, decoded or written; no partial copy is left behind
     */
    fun importPhoto(sourceUri: String): String {
        val uri = sourceUri.toUri()
        val input = context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException("Can't open $sourceUri")
        return BufferedInputStream(input).use { source ->
            source.mark(ImageFormat.HEADER_SIZE)
            val format = ImageFormat.detect(source.readHeader())
            source.reset()

            val target = newPhotoFile(format ?: ImageFormat.Jpeg)
            try {
                if (format != null) {
                    target.outputStream().use { source.copyTo(it) }
                } else {
                    transcoder.transcodeToJpeg(sourceUri, target)
                }
            } catch (e: IOException) {
                target.delete()
                throw e
            }
            target.absolutePath
        }
    }

    /**
     * Returns the size of the file at [path] in bytes, or 0 if it doesn't exist
     */
    fun sizeOf(path: String): Long = File(path).length()

    /**
     * Returns the format of the photo file at [path], recognised by its first bytes, or null if the file
     * doesn't exist or is in a format the server doesn't accept
     *
     * @throws IOException if the file exists but can't be read
     */
    fun formatOf(path: String): ImageFormat? {
        val file = File(path)
        if (!file.isFile) return null
        return file.inputStream().use { ImageFormat.detect(it.readHeader()) }
    }

    /**
     * Deletes the files in the photo directory that no photo row refers to, e.g. ones a form took or imported
     * before the app was killed without saving or discarding it. Only files last modified before [modifiedBefore]
     * go, so the file of a photo on a form that is still open, or that the camera is writing, is never deleted
     *
     * @param referencedPaths absolute paths of every file a photo row refers to
     * @param modifiedBefore epoch milliseconds; newer files are kept
     */
    fun deleteUnreferencedPhotos(referencedPaths: Set<String>, modifiedBefore: Long) {
        photosDirectory.listFiles()
            ?.filter { it.isFile && it.absolutePath !in referencedPaths && it.lastModified() < modifiedBefore }
            ?.forEach { it.delete() }
    }

    /**
     * Deletes the photo directory with every file in it
     */
    fun deleteAllPhotos() {
        photosDirectory.deleteRecursively()
    }

    /**
     * Deletes the photo file at [path]. A path outside the photo directory is left alone
     */
    fun deletePhoto(path: String) {
        val file = File(path)
        if (file.canonicalFile.parentFile == photosDirectory.canonicalFile) file.delete()
    }

    private fun newPhotoFile(format: ImageFormat): File {
        val directory = photosDirectory
        directory.mkdirs()
        return File(directory, "${UUID.randomUUID()}.${format.extension}")
    }

    // Reads up to the header's size; a shorter stream gives a shorter header, which matches no format
    private fun InputStream.readHeader(): ByteArray {
        val header = ByteArray(ImageFormat.HEADER_SIZE)
        var length = 0
        while (length < header.size) {
            val read = read(header, length, header.size - length)
            if (read < 0) break
            length += read
        }
        return header.copyOf(length)
    }

    private companion object {
        const val PHOTOS_DIRECTORY = "photos"
    }
}
