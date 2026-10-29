package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dto.IncidentDto
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

    @Test
    fun testFetchIncident() = runTest {
        val incident = mockk<IncidentDto>()

        coEvery { api.getIncident("inc-1") } returns incident

        val actualIncident = dataSource.fetchIncident("inc-1")

        coVerifySequence { api.getIncident("inc-1") }

        Truth.assertThat(actualIncident).isSameInstanceAs(incident)
    }

    @Test
    fun testFetchIncident_NoConnection() {
        coEvery { api.getIncident("inc-1") } throws IOException()

        Assert.assertThrows(NoConnectionException::class.java) {
            runTest { dataSource.fetchIncident("inc-1") }
        }
    }

    @Test
    fun testFetchIncident_NotFound() {
        val httpException = httpException(404)

        coEvery { api.getIncident("inc-1") } throws httpException

        val thrown = Assert.assertThrows(HttpException::class.java) {
            runTest { dataSource.fetchIncident("inc-1") }
        }

        Truth.assertThat(thrown).isSameInstanceAs(httpException)
    }

    private fun httpException(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))
}
