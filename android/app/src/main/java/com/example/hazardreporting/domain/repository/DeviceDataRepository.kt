package com.example.hazardreporting.domain.repository

/**
 * Everything the app keeps on the device about the signed-in technician, which logout wipes (ADR-0007)
 *
 * @author Roman Pliskin on 29.10.2026
 */
interface DeviceDataRepository {

    /**
     * Deletes every stored row: incidents, their photos and the catalog
     */
    suspend fun clearDatabase()

    /**
     * Deletes every photo file in the app's private photo storage
     */
    suspend fun deletePhotoFiles()

    /**
     * Clears the image loader's memory and disk caches
     */
    suspend fun clearImageCaches()
}
