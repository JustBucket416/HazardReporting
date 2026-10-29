package com.example.hazardreporting.data.incident

/**
 * An image format the server accepts for photo uploads. The server checks a file's first bytes against
 * the type it is sent as, so the app tells formats apart by the same signatures rather than by file names
 * or the type a content provider claims
 *
 * @property mimeType the type a file of this format is uploaded as
 * @property extension the extension a file of this format is stored with, without the dot
 *
 * @author Roman Pliskin on 29.10.2026
 */
enum class ImageFormat(val mimeType: String, val extension: String) {

    /**
     * JPEG: starts with `FF D8 FF`
     */
    Jpeg("image/jpeg", "jpg"),

    /**
     * PNG: starts with the 8-byte PNG signature
     */
    Png("image/png", "png"),

    /**
     * WebP: a RIFF container whose form type is `WEBP`
     */
    Webp("image/webp", "webp"),
    ;

    /**
     * Recognises formats by their first bytes
     */
    companion object {

        /**
         * How many leading bytes of a file [detect] needs
         */
        const val HEADER_SIZE = 12

        private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        private val RIFF_TAG = "RIFF".toByteArray(Charsets.US_ASCII)
        private val WEBP_TAG = "WEBP".toByteArray(Charsets.US_ASCII)
        private const val WEBP_TAG_OFFSET = 8

        /**
         * Returns the format whose signature [header], the first bytes of a file, starts with,
         * or null if it is none the server accepts (e.g. HEIC) or [header] is too short to tell
         */
        fun detect(header: ByteArray): ImageFormat? = when {
            header.startsWith(JPEG_SIGNATURE) -> Jpeg
            header.startsWith(PNG_SIGNATURE) -> Png
            header.startsWith(RIFF_TAG) && header.startsWith(WEBP_TAG, WEBP_TAG_OFFSET) -> Webp
            else -> null
        }

        private fun ByteArray.startsWith(prefix: ByteArray, offset: Int = 0): Boolean =
            size >= offset + prefix.size && prefix.indices.all { this[offset + it] == prefix[it] }
    }
}
