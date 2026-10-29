package com.example.hazardreporting.data.catalog

import com.example.hazardreporting.data.catalog.dao.CategoryDao
import com.example.hazardreporting.data.catalog.dao.SeverityDao
import com.example.hazardreporting.data.catalog.dao.SiteDao
import com.example.hazardreporting.data.catalog.entity.CategoryEntity
import com.example.hazardreporting.data.catalog.entity.SeverityEntity
import com.example.hazardreporting.data.catalog.entity.SiteEntity
import com.google.common.truth.Truth
import io.mockk.coJustRun
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class CatalogLocalDataSourceTest {

    private val categoryDao = mockk<CategoryDao>()
    private val severityDao = mockk<SeverityDao>()
    private val siteDao = mockk<SiteDao>()

    private val dataSource = CatalogLocalDataSource(categoryDao, severityDao, siteDao)

    @Test
    fun testObserveCategories() {
        val flow = mockk<Flow<List<CategoryEntity>>>()

        every { categoryDao.observeAll() } returns flow

        Truth.assertThat(dataSource.observeCategories()).isSameInstanceAs(flow)

        verifySequence { categoryDao.observeAll() }
    }

    @Test
    fun testObserveSeverities() {
        val flow = mockk<Flow<List<SeverityEntity>>>()

        every { severityDao.observeAll() } returns flow

        Truth.assertThat(dataSource.observeSeverities()).isSameInstanceAs(flow)

        verifySequence { severityDao.observeAll() }
    }

    @Test
    fun testObserveSites() {
        val flow = mockk<Flow<List<SiteEntity>>>()

        every { siteDao.observeAll() } returns flow

        Truth.assertThat(dataSource.observeSites()).isSameInstanceAs(flow)

        verifySequence { siteDao.observeAll() }
    }

    @Test
    fun testReplaceCategories() = runTest {
        val categories = listOf(CategoryEntity("c1", "Electrical", 0))

        coJustRun { categoryDao.replaceAll(categories) }

        dataSource.replaceCategories(categories)

        coVerifySequence { categoryDao.replaceAll(categories) }
    }

    @Test
    fun testReplaceSeverities() = runTest {
        val severities = listOf(SeverityEntity("s1", "Low", 1))

        coJustRun { severityDao.replaceAll(severities) }

        dataSource.replaceSeverities(severities)

        coVerifySequence { severityDao.replaceAll(severities) }
    }

    @Test
    fun testReplaceSites() = runTest {
        val sites = listOf(SiteEntity("site1", "North Plant", 59.9343, 30.3351, 0))

        coJustRun { siteDao.replaceAll(sites) }

        dataSource.replaceSites(sites)

        coVerifySequence { siteDao.replaceAll(sites) }
    }
}
