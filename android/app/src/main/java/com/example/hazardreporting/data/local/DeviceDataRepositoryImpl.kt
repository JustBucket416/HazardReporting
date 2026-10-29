package com.example.hazardreporting.data.local

import com.example.hazardreporting.data.incident.PhotoLocalDataSource
import com.example.hazardreporting.domain.repository.DeviceDataRepository
import com.example.hazardreporting.utils.CoroutineDispatchers
import javax.inject.Inject
import kotlinx.coroutines.withContext

/**
 * Implementation of [DeviceDataRepository]
 *
 * @property databaseLocalDataSource used for clearing the database
 * @property photoLocalDataSource used for deleting the photo files
 * @property imageCacheLocalDataSource used for clearing the image caches
 * @property dispatchers used for running on the IO dispatcher
 *
 * @author Roman Pliskin on 29.10.2026
 */
class DeviceDataRepositoryImpl @Inject constructor(
    private val databaseLocalDataSource: DatabaseLocalDataSource,
    private val photoLocalDataSource: PhotoLocalDataSource,
    private val imageCacheLocalDataSource: ImageCacheLocalDataSource,
    private val dispatchers: CoroutineDispatchers,
) : DeviceDataRepository {

    override suspend fun clearDatabase() = withContext(dispatchers.io) { databaseLocalDataSource.clearAllTables() }

    override suspend fun deletePhotoFiles() = withContext(dispatchers.io) { photoLocalDataSource.deleteAllPhotos() }

    override suspend fun clearImageCaches() = withContext(dispatchers.io) { imageCacheLocalDataSource.clear() }
}
