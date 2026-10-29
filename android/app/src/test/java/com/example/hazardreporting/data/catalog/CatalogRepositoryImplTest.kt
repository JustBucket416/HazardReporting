package com.example.hazardreporting.data.catalog

import app.cash.turbine.test
import com.example.hazardreporting.data.catalog.dto.CategoryDto
import com.example.hazardreporting.data.catalog.dto.SeverityDto
import com.example.hazardreporting.data.catalog.dto.SiteDto
import com.example.hazardreporting.data.catalog.entity.CategoryEntity
import com.example.hazardreporting.data.catalog.entity.SeverityEntity
import com.example.hazardreporting.data.catalog.entity.SiteEntity
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.model.Category
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.domain.model.Site
import com.example.hazardreporting.utils.CoroutineDispatchers
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogRepositoryImplTest {

    private val remoteDataSource = mockk<CatalogRemoteDataSource>()
    private val localDataSource = mockk<CatalogLocalDataSource>()
    private val converter = mockk<CatalogConverter>()
    private val dispatchers = mockk<CoroutineDispatchers>()

    private lateinit var repository: CatalogRepositoryImpl

    @Before
    fun setUp() {
        every { dispatchers.io } returns UnconfinedTestDispatcher()

        repository = CatalogRepositoryImpl(remoteDataSource, localDataSource, converter, dispatchers)
    }

    @Test
    fun testRefresh() = runTest {
        coEvery { remoteDataSource.fetchCategories() } returns listOf(CATEGORY_DTO, OTHER_CATEGORY_DTO)
        coEvery { remoteDataSource.fetchSeverities() } returns listOf(SEVERITY_DTO)
        coEvery { remoteDataSource.fetchSites() } returns listOf(SITE_DTO)
        every { converter.convert(CATEGORY_DTO, 0) } returns CATEGORY_ENTITY
        every { converter.convert(OTHER_CATEGORY_DTO, 1) } returns OTHER_CATEGORY_ENTITY
        every { converter.convert(SEVERITY_DTO) } returns SEVERITY_ENTITY
        every { converter.convert(SITE_DTO, 0) } returns SITE_ENTITY
        coJustRun { localDataSource.replaceCategories(listOf(CATEGORY_ENTITY, OTHER_CATEGORY_ENTITY)) }
        coJustRun { localDataSource.replaceSeverities(listOf(SEVERITY_ENTITY)) }
        coJustRun { localDataSource.replaceSites(listOf(SITE_ENTITY)) }

        repository.refresh()

        coVerifySequence {
            remoteDataSource.fetchCategories()
            remoteDataSource.fetchSeverities()
            remoteDataSource.fetchSites()
            converter.convert(CATEGORY_DTO, 0)
            converter.convert(OTHER_CATEGORY_DTO, 1)
            localDataSource.replaceCategories(listOf(CATEGORY_ENTITY, OTHER_CATEGORY_ENTITY))
            converter.convert(SEVERITY_DTO)
            localDataSource.replaceSeverities(listOf(SEVERITY_ENTITY))
            converter.convert(SITE_DTO, 0)
            localDataSource.replaceSites(listOf(SITE_ENTITY))
        }
    }

    @Test
    fun testRefresh_NoConnection() {
        coEvery { remoteDataSource.fetchCategories() } returns listOf(CATEGORY_DTO)
        coEvery { remoteDataSource.fetchSeverities() } throws NoConnectionException()

        Assert.assertThrows(NoConnectionException::class.java) {
            runTest { repository.refresh() }
        }

        // The stored catalog stays as it was, so it can still be observed offline
        coVerifySequence {
            remoteDataSource.fetchCategories()
            remoteDataSource.fetchSeverities()
        }
    }

    @Test
    fun testObserveCategories() = runTest {
        every { localDataSource.observeCategories() } returns flowOf(listOf(CATEGORY_ENTITY))
        every { converter.convert(CATEGORY_ENTITY) } returns CATEGORY

        repository.observeCategories().test {
            Truth.assertThat(awaitItem()).containsExactly(CATEGORY)
            awaitComplete()
        }

        verifySequence {
            localDataSource.observeCategories()
            converter.convert(CATEGORY_ENTITY)
        }
    }

    @Test
    fun testObserveSeverities() = runTest {
        every { localDataSource.observeSeverities() } returns flowOf(listOf(SEVERITY_ENTITY))
        every { converter.convert(SEVERITY_ENTITY) } returns SEVERITY

        repository.observeSeverities().test {
            Truth.assertThat(awaitItem()).containsExactly(SEVERITY)
            awaitComplete()
        }

        verifySequence {
            localDataSource.observeSeverities()
            converter.convert(SEVERITY_ENTITY)
        }
    }

    @Test
    fun testObserveSites() = runTest {
        every { localDataSource.observeSites() } returns flowOf(listOf(SITE_ENTITY))
        every { converter.convert(SITE_ENTITY) } returns SITE

        repository.observeSites().test {
            Truth.assertThat(awaitItem()).containsExactly(SITE)
            awaitComplete()
        }

        verifySequence {
            localDataSource.observeSites()
            converter.convert(SITE_ENTITY)
        }
    }

    private companion object {
        const val LAT = 59.9343
        const val LNG = 30.3351

        val CATEGORY_DTO = CategoryDto("c1", "Equipment")
        val OTHER_CATEGORY_DTO = CategoryDto("c7", "Other")
        val SEVERITY_DTO = SeverityDto("s1", "Low", 1)
        val SITE_DTO = SiteDto("site1", "North Plant", LAT, LNG)

        val CATEGORY_ENTITY = CategoryEntity("c1", "Equipment", 0)
        val OTHER_CATEGORY_ENTITY = CategoryEntity("c7", "Other", 1)
        val SEVERITY_ENTITY = SeverityEntity("s1", "Low", 1)
        val SITE_ENTITY = SiteEntity("site1", "North Plant", LAT, LNG, 0)

        val CATEGORY = Category("c1", "Equipment")
        val SEVERITY = Severity("s1", "Low", 1)
        val SITE = Site("site1", "North Plant", LAT, LNG)
    }
}
