package com.example.hazardreporting.data.local

import com.example.hazardreporting.data.incident.PhotoLocalDataSource
import com.example.hazardreporting.utils.CoroutineDispatchers
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceDataRepositoryImplTest {

    private val databaseLocalDataSource = mockk<DatabaseLocalDataSource>()
    private val photoLocalDataSource = mockk<PhotoLocalDataSource>()
    private val imageCacheLocalDataSource = mockk<ImageCacheLocalDataSource>()
    private val dispatchers = mockk<CoroutineDispatchers>()

    private lateinit var repository: DeviceDataRepositoryImpl

    @Before
    fun setUp() {
        every { dispatchers.io } returns UnconfinedTestDispatcher()

        repository = DeviceDataRepositoryImpl(
            databaseLocalDataSource,
            photoLocalDataSource,
            imageCacheLocalDataSource,
            dispatchers,
        )
    }

    @Test
    fun testClearDatabase() = runTest {
        justRun { databaseLocalDataSource.clearAllTables() }

        repository.clearDatabase()

        verifySequence { databaseLocalDataSource.clearAllTables() }
    }

    @Test
    fun testDeletePhotoFiles() = runTest {
        justRun { photoLocalDataSource.deleteAllPhotos() }

        repository.deletePhotoFiles()

        verifySequence { photoLocalDataSource.deleteAllPhotos() }
    }

    @Test
    fun testClearImageCaches() = runTest {
        justRun { imageCacheLocalDataSource.clear() }

        repository.clearImageCaches()

        verifySequence { imageCacheLocalDataSource.clear() }
    }
}
