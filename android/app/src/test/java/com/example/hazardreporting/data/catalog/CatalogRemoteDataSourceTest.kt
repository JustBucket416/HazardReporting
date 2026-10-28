package com.example.hazardreporting.data.catalog

import com.example.hazardreporting.data.catalog.dto.CategoryDto
import com.example.hazardreporting.data.catalog.dto.SeverityDto
import com.example.hazardreporting.data.catalog.dto.SiteDto
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * @author Roman Pliskin on 29.10.2026
 */
class CatalogRemoteDataSourceTest {

    private val api = mockk<CatalogApi>()

    private val dataSource = CatalogRemoteDataSource(api)

    @Test
    fun testFetchCategories() = runTest {
        val categories = listOf(CategoryDto("c1", "Electrical"))

        coEvery { api.getCategories() } returns categories

        val actualCategories = dataSource.fetchCategories()

        coVerifySequence { api.getCategories() }

        Truth.assertThat(actualCategories).isEqualTo(categories)
    }

    @Test
    fun testFetchSeverities() = runTest {
        val severities = listOf(SeverityDto("s1", "Low", 1))

        coEvery { api.getSeverities() } returns severities

        val actualSeverities = dataSource.fetchSeverities()

        coVerifySequence { api.getSeverities() }

        Truth.assertThat(actualSeverities).isEqualTo(severities)
    }

    @Test
    fun testFetchSites() = runTest {
        val sites = listOf(SiteDto("site1", "North Plant", 59.9343, 30.3351))

        coEvery { api.getSites() } returns sites

        val actualSites = dataSource.fetchSites()

        coVerifySequence { api.getSites() }

        Truth.assertThat(actualSites).isEqualTo(sites)
    }

    @Test
    fun testFetchCategories_NoConnection() {
        coEvery { api.getCategories() } throws IOException()

        Assert.assertThrows(NoConnectionException::class.java) {
            runTest { dataSource.fetchCategories() }
        }
    }

    @Test
    fun testFetchSeverities_Unauthorized() {
        coEvery { api.getSeverities() } throws HttpException(Response.error<Any>(401, "".toResponseBody()))

        Assert.assertThrows(UnauthorizedException::class.java) {
            runTest { dataSource.fetchSeverities() }
        }
    }

    @Test
    fun testFetchSites_ServerError() {
        val httpException = HttpException(Response.error<Any>(500, "".toResponseBody()))

        coEvery { api.getSites() } throws httpException

        val thrown = Assert.assertThrows(HttpException::class.java) {
            runTest { dataSource.fetchSites() }
        }

        Truth.assertThat(thrown).isSameInstanceAs(httpException)
    }
}
