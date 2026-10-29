package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dto.CreateIncidentRequestDto
import com.example.hazardreporting.data.incident.dto.IncidentDto
import com.example.hazardreporting.data.incident.dto.IncidentSummaryDto
import com.example.hazardreporting.data.incident.dto.UpdateIncidentRequestDto
import com.example.hazardreporting.data.incident.dto.UploadResponseDto
import com.example.hazardreporting.data.incident.dto.WithdrawIncidentRequestDto
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import io.mockk.slot
import java.io.File
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
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

    @Test
    fun testCreateIncident() = runTest {
        val request = mockk<CreateIncidentRequestDto>()
        val incident = mockk<IncidentDto>()

        coEvery { api.createIncident(request) } returns incident

        Truth.assertThat(dataSource.createIncident(request)).isSameInstanceAs(incident)

        coVerifySequence { api.createIncident(request) }
    }

    @Test
    fun testCreateIncident_NoConnection() {
        coEvery { api.createIncident(any()) } throws IOException()

        Assert.assertThrows(NoConnectionException::class.java) {
            runTest { dataSource.createIncident(mockk()) }
        }
    }

    @Test
    fun testUpdateIncident() = runTest {
        val request = mockk<UpdateIncidentRequestDto>()
        val incident = mockk<IncidentDto>()

        coEvery { api.updateIncident("inc-1", request) } returns incident

        Truth.assertThat(dataSource.updateIncident("inc-1", request)).isSameInstanceAs(incident)

        coVerifySequence { api.updateIncident("inc-1", request) }
    }

    @Test
    fun testUpdateIncident_Conflict() {
        val httpException = httpException(409)

        coEvery { api.updateIncident("inc-1", any()) } throws httpException

        // Left to the caller, which knows what a 409 means for its request
        val thrown = Assert.assertThrows(HttpException::class.java) {
            runTest { dataSource.updateIncident("inc-1", mockk()) }
        }

        Truth.assertThat(thrown).isSameInstanceAs(httpException)
    }

    @Test
    fun testWithdrawIncident() = runTest {
        val incident = mockk<IncidentDto>()

        coEvery { api.withdrawIncident("inc-1", WithdrawIncidentRequestDto("withdrawn")) } returns incident

        Truth.assertThat(dataSource.withdrawIncident("inc-1")).isSameInstanceAs(incident)

        // Exactly { status: "withdrawn" }, the only body the server accepts for a withdrawal
        coVerifySequence { api.withdrawIncident("inc-1", WithdrawIncidentRequestDto("withdrawn")) }
    }

    @Test
    fun testWithdrawIncident_Unauthorized() {
        coEvery { api.withdrawIncident("inc-1", any()) } throws httpException(401)

        Assert.assertThrows(UnauthorizedException::class.java) {
            runTest { dataSource.withdrawIncident("inc-1") }
        }
    }

    @Test
    fun testUploadPhoto() = runTest {
        val file = File.createTempFile("photo", ".png").apply {
            writeBytes(byteArrayOf(1, 2, 3))
            deleteOnExit()
        }
        val part = slot<MultipartBody.Part>()

        coEvery { api.uploadPhoto(capture(part)) } returns UploadResponseDto("/uploads/ab12.png")

        val url = dataSource.uploadPhoto(file, "image/png")

        // Sent as the multipart field "file", declared with the type the server checks the content against
        Truth.assertThat(url).isEqualTo("/uploads/ab12.png")
        Truth.assertThat(part.captured.headers?.get("Content-Disposition"))
            .isEqualTo("form-data; name=\"file\"; filename=\"${file.name}\"")
        Truth.assertThat(part.captured.body.contentType()).isEqualTo("image/png".toMediaType())
        Truth.assertThat(part.captured.body.contentLength()).isEqualTo(3L)
    }

    @Test
    fun testUploadPhoto_Refused() {
        val httpException = httpException(400)

        coEvery { api.uploadPhoto(any()) } throws httpException

        val thrown = Assert.assertThrows(HttpException::class.java) {
            runTest { dataSource.uploadPhoto(File("photo.heic"), "image/jpeg") }
        }

        Truth.assertThat(thrown).isSameInstanceAs(httpException)
    }

    private fun httpException(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))
}
