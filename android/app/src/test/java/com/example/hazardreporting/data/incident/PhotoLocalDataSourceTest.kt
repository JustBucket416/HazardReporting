package com.example.hazardreporting.data.incident

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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

    private lateinit var photosDirectory: File
    private lateinit var dataSource: PhotoLocalDataSource

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(SOURCE_URI) } returns uri
        every { context.filesDir } returns temporaryFolder.root
        every { context.contentResolver } returns contentResolver
        photosDirectory = File(temporaryFolder.root, "photos")

        dataSource = PhotoLocalDataSource(context)
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
    fun testImportPhoto() {
        every { contentResolver.getType(uri) } returns "image/png"
        every { contentResolver.openInputStream(uri) } returns CONTENT.inputStream()

        val path = dataSource.importPhoto(SOURCE_URI)

        // Copied straight into files/photos/, keeping its image type
        val file = File(path)
        Truth.assertThat(file.parentFile).isEqualTo(photosDirectory)
        Truth.assertThat(file.extension).isEqualTo("png")
        Truth.assertThat(file.readBytes()).isEqualTo(CONTENT)
    }

    @Test
    fun testImportPhoto_UnknownType() {
        every { contentResolver.getType(uri) } returns null
        every { contentResolver.openInputStream(uri) } returns CONTENT.inputStream()

        val path = dataSource.importPhoto(SOURCE_URI)

        Truth.assertThat(File(path).extension).isEqualTo("jpg")
    }

    @Test
    fun testImportPhoto_Unreadable() {
        every { contentResolver.getType(uri) } returns "image/jpeg"
        every { contentResolver.openInputStream(uri) } returns null

        Assert.assertThrows(FileNotFoundException::class.java) { dataSource.importPhoto(SOURCE_URI) }

        Truth.assertThat(photosDirectory.listFiles()).isEmpty()
    }

    @Test
    fun testImportPhoto_ReadFails() {
        val failingStream = object : InputStream() {
            override fun read(): Int = throw IOException()
        }

        every { contentResolver.getType(uri) } returns "image/jpeg"
        every { contentResolver.openInputStream(uri) } returns failingStream

        Assert.assertThrows(IOException::class.java) { dataSource.importPhoto(SOURCE_URI) }

        // No partial copy is left behind
        Truth.assertThat(photosDirectory.listFiles()).isEmpty()
    }

    @Test
    fun testDeletePhoto() {
        val file = File(dataSource.createPhotoFile()).apply { writeBytes(CONTENT) }

        dataSource.deletePhoto(file.path)

        Truth.assertThat(file.exists()).isFalse()
    }

    @Test
    fun testDeletePhoto_OutsidePhotoDirectory() {
        val file = temporaryFolder.newFile("database.db")

        dataSource.deletePhoto(file.path)

        Truth.assertThat(file.exists()).isTrue()
    }

    private companion object {
        const val SOURCE_URI = "content://media/picker/0/42"
        val CONTENT = byteArrayOf(1, 2, 3, 4)
    }
}
