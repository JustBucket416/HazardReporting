package com.example.hazardreporting.data.incident

import com.google.common.truth.Truth
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class ImageFormatTest {

    @Test
    fun testDetect_Jpeg() {
        Truth.assertThat(ImageFormat.detect(bytes(0xFF, 0xD8, 0xFF, 0xE1, 0, 0))).isEqualTo(ImageFormat.Jpeg)
    }

    @Test
    fun testDetect_Png() {
        val header = bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D)

        Truth.assertThat(ImageFormat.detect(header)).isEqualTo(ImageFormat.Png)
    }

    @Test
    fun testDetect_Webp() {
        val header = "RIFF".toByteArray() + bytes(0x24, 0x10, 0, 0) + "WEBP".toByteArray()

        Truth.assertThat(ImageFormat.detect(header)).isEqualTo(ImageFormat.Webp)
    }

    @Test
    fun testDetect_RiffButNotWebp() {
        // A WAV file is a RIFF container too
        val header = "RIFF".toByteArray() + bytes(0x24, 0x10, 0, 0) + "WAVE".toByteArray()

        Truth.assertThat(ImageFormat.detect(header)).isNull()
    }

    @Test
    fun testDetect_Heic() {
        val header = bytes(0, 0, 0, 0x18) + "ftypheic".toByteArray()

        Truth.assertThat(ImageFormat.detect(header)).isNull()
    }

    @Test
    fun testDetect_TooShort() {
        Truth.assertThat(ImageFormat.detect(bytes(0xFF, 0xD8))).isNull()
        Truth.assertThat(ImageFormat.detect("RIFF".toByteArray() + bytes(0, 0, 0, 0) + "WEB".toByteArray())).isNull()
        Truth.assertThat(ImageFormat.detect(ByteArray(0))).isNull()
    }

    @Test
    fun testMimeTypesAndExtensions() {
        // The types the server accepts, and extensions that match them
        Truth.assertThat(ImageFormat.entries.map { it.mimeType to it.extension }).containsExactly(
            "image/jpeg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp",
        ).inOrder()
    }

    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
}
