package com.example.hazardreporting.data.incident

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifySequence
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * @author Roman Pliskin on 29.10.2026
 */
class PhotoLocalDataSourceTest {

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    private val context = mockk<Context>()
    private val contentResolver = mockk<ContentResolver>()
    private val uri = mockk<Uri>()
    private val transcoder = mockk<ImageTranscoder>()

    private lateinit var photosDirectory: File
    private lateinit var dataSource: PhotoLocalDataSource

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(SOURCE_URI) } returns uri
        every { context.filesDir } returns temporaryFolder.root
        every { context.contentResolver } returns contentResolver
        photosDirectory = File(temporaryFolder.root, "photos")

        dataSource = PhotoLocalDataSource(context, transcoder)
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun testCreatePhotoFile() {
        val path = dataSource.createPhotoFile()

        // A fresh JPEG path in files/photos/; the camera creates the file itself
        val file = File(path)
        Truth.assertThat(file.parentFile).isEqualTo(photosDirectory)
        Truth.assertThat(file.extension).isEqualTo("jpg")
        Truth.assertThat(photosDirectory.isDirectory).isTrue()
        Truth.assertThat(file.exists()).isFalse()
        Truth.assertThat(dataSource.createPhotoFile()).isNotEqualTo(path)
    }

    @Test
    fun testImportPhoto_Jpeg() {
        verifyCopiedAsIs(JPEG_CONTENT, expectedExtension = "jpg")
    }

    @Test
    fun testImportPhoto_Png() {
        verifyCopiedAsIs(PNG_CONTENT, expectedExtension = "png")
    }

    @Test
    fun testImportPhoto_Webp() {
        verifyCopiedAsIs(WEBP_CONTENT, expectedExtension = "webp")
    }

    @Test
    fun testImportPhoto_Heic() {
        every { contentResolver.openInputStream(uri) } returns HEIC_CONTENT.inputStream()
        every { transcoder.transcodeToJpeg(SOURCE_URI, any()) } answers { secondArg<File>().writeBytes(JPEG_CONTENT) }

        val path = dataSource.importPhoto(SOURCE_URI)

        // The server refuses HEIC, so the picture is re-encoded as a JPEG in files/photos/
        val file = File(path)
        verifySequence { transcoder.transcodeToJpeg(SOURCE_URI, file) }

        Truth.assertThat(file.parentFile).isEqualTo(photosDirectory)
        Truth.assertThat(file.extension).isEqualTo("jpg")
        Truth.assertThat(file.readBytes()).isEqualTo(JPEG_CONTENT)
    }

    @Test
    fun testImportPhoto_TooShortToRecognise() {
        val content = byteArrayOf(0xFF.toByte(), 0xD8.toByte())

        every { contentResolver.openInputStream(uri) } returns content.inputStream()
        justRun { transcoder.transcodeToJpeg(SOURCE_URI, any()) }

        val path = dataSource.importPhoto(SOURCE_URI)

        // Nothing it could be recognised as, so it is left to the decoder
        verifySequence { transcoder.transcodeToJpeg(SOURCE_URI, File(path)) }
    }

    @Test
    fun testImportPhoto_TranscodingFails() {
        every { contentResolver.openInputStream(uri) } returns HEIC_CONTENT.inputStream()
        every { transcoder.transcodeToJpeg(SOURCE_URI, any()) } answers {
            secondArg<File>().writeBytes(byteArrayOf(1))
            throw IOException()
        }

        Assert.assertThrows(IOException::class.java) { dataSource.importPhoto(SOURCE_URI) }

        // No partial copy is left behind
        Truth.assertThat(photosDirectory.listFiles()).isEmpty()
    }

    @Test
    fun testImportPhoto_Unreadable() {
        every { contentResolver.openInputStream(uri) } returns null

        Assert.assertThrows(FileNotFoundException::class.java) { dataSource.importPhoto(SOURCE_URI) }

        Truth.assertThat(photosDirectory.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun testImportPhoto_ReadFails() {
        val failingStream = object : InputStream() {
            override fun read(): Int = throw IOException()
        }

        every { contentResolver.openInputStream(uri) } returns failingStream

        Assert.assertThrows(IOException::class.java) { dataSource.importPhoto(SOURCE_URI) }

        // No partial copy is left behind
        Truth.assertThat(photosDirectory.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun testFormatOf() {
        val jpeg = File(dataSource.createPhotoFile()).apply { writeBytes(JPEG_CONTENT) }
        val png = File(photosDirectory, "b.png").apply { writeBytes(PNG_CONTENT) }
        val webp = File(photosDirectory, "c.webp").apply { writeBytes(WEBP_CONTENT) }

        Truth.assertThat(dataSource.formatOf(jpeg.path)).isEqualTo(ImageFormat.Jpeg)
        Truth.assertThat(dataSource.formatOf(png.path)).isEqualTo(ImageFormat.Png)
        Truth.assertThat(dataSource.formatOf(webp.path)).isEqualTo(ImageFormat.Webp)
    }

    @Test
    fun testFormatOf_ByContentNotName() {
        // E.g. a HEIC picture an older version imported under a .jpg name
        val file = File(dataSource.createPhotoFile()).apply { writeBytes(HEIC_CONTENT) }

        Truth.assertThat(dataSource.formatOf(file.path)).isNull()
    }

    @Test
    fun testFormatOf_Missing() {
        Truth.assertThat(dataSource.formatOf(File(photosDirectory, "gone.jpg").path)).isNull()
    }

    @Test
    fun testDeleteUnreferencedPhotos() {
        val referencedOld = photoFile("referenced.jpg", lastModified = OLD)
        val unreferencedOld = photoFile("orphan.jpg", lastModified = OLD)
        val unreferencedNew = photoFile("open-form.jpg", lastModified = CUTOFF)
        val outside = temporaryFolder.newFile("database.db").apply { setLastModified(OLD) }

        dataSource.deleteUnreferencedPhotos(setOf(referencedOld.absolutePath), modifiedBefore = CUTOFF)

        // Only an old file nobody refers to goes; a new one may belong to a form that is still open
        Truth.assertThat(referencedOld.exists()).isTrue()
        Truth.assertThat(unreferencedOld.exists()).isFalse()
        Truth.assertThat(unreferencedNew.exists()).isTrue()
        Truth.assertThat(outside.exists()).isTrue()
    }

    @Test
    fun testDeleteUnreferencedPhotos_NoDirectory() {
        dataSource.deleteUnreferencedPhotos(emptySet(), modifiedBefore = CUTOFF)

        Truth.assertThat(photosDirectory.exists()).isFalse()
    }

    @Test
    fun testDeleteAllPhotos() {
        photoFile("a.jpg", lastModified = CUTOFF)
        photoFile("b.jpg", lastModified = OLD)
        val outside = temporaryFolder.newFile("database.db")

        dataSource.deleteAllPhotos()

        Truth.assertThat(photosDirectory.exists()).isFalse()
        Truth.assertThat(outside.exists()).isTrue()
    }

    @Test
    fun testDeletePhoto() {
        val file = File(dataSource.createPhotoFile()).apply { writeBytes(JPEG_CONTENT) }

        dataSource.deletePhoto(file.path)

        Truth.assertThat(file.exists()).isFalse()
    }

    @Test
    fun testDeletePhoto_OutsidePhotoDirectory() {
        val file = temporaryFolder.newFile("database.db")

        dataSource.deletePhoto(file.path)

        Truth.assertThat(file.exists()).isTrue()
    }

    private fun photoFile(name: String, lastModified: Long): File {
        photosDirectory.mkdirs()
        return File(photosDirectory, name).apply {
            writeBytes(JPEG_CONTENT)
            setLastModified(lastModified)
        }
    }

    private fun verifyCopiedAsIs(content: ByteArray, expectedExtension: String) {
        every { contentResolver.openInputStream(uri) } returns content.inputStream()

        val path = dataSource.importPhoto(SOURCE_URI)

        // Copied straight into files/photos/ in its own format, whatever type the provider claims
        val file = File(path)
        verify(exactly = 0) { transcoder.transcodeToJpeg(any(), any()) }

        Truth.assertThat(file.parentFile).isEqualTo(photosDirectory)
        Truth.assertThat(file.extension).isEqualTo(expectedExtension)
        Truth.assertThat(file.readBytes()).isEqualTo(content)
    }

    private companion object {
        const val SOURCE_URI = "content://media/picker/0/42"
        const val CUTOFF = 1_793_000_000_000L
        const val OLD = CUTOFF - 1_000L
        val JPEG_CONTENT = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3)
        val PNG_CONTENT = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4, 5)
        val WEBP_CONTENT = "RIFF".toByteArray() + byteArrayOf(0, 0, 0, 0) + "WEBPVP8 ".toByteArray()
        val HEIC_CONTENT = byteArrayOf(0, 0, 0, 0x18) + "ftypheic".toByteArray()
    }
}
