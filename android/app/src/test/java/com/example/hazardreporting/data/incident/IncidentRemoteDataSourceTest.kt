package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dto.IncidentSummaryDto
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
class IncidentRemoteDataSourceTest {

    private val api = mockk<IncidentApi>()

    private val dataSource = IncidentRemoteDataSource(api)

    @Test
    fun testFetchSummaries() = runTest {
        val summaries = listOf(mockk<IncidentSummaryDto>())

        coEvery { api.getSummaries() } returns summaries

        val actualSummaries = dataSource.fetchSummaries()

        coVerifySequence { api.getSummaries() }

        Truth.assertThat(actualSummaries).isEqualTo(summaries)
    }

    @Test
    fun testFetchSummaries_NoConnection() {
        coEvery { api.getSummaries() } throws IOException()

        Assert.assertThrows(NoConnectionException::class.java) {
            runTest { dataSource.fetchSummaries() }
        }
    }

    @Test
    fun testFetchSummaries_Unauthorized() {
        coEvery { api.getSummaries() } throws httpException(401)

        Assert.assertThrows(UnauthorizedException::class.java) {
            runTest { dataSource.fetchSummaries() }
        }
    }

    @Test
    fun testFetchSummaries_ServerError() {
        val httpException = httpException(500)

        coEvery { api.getSummaries() } throws httpException

        val thrown = Assert.assertThrows(HttpException::class.java) {
            runTest { dataSource.fetchSummaries() }
        }

        Truth.assertThat(thrown).isSameInstanceAs(httpException)
    }

    private fun httpException(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))
}
