package com.example.hazardreporting.data.session

import com.example.hazardreporting.data.session.dto.LoginRequestDto
import com.example.hazardreporting.data.session.dto.LoginResponseDto
import com.example.hazardreporting.domain.exception.InvalidCredentialsException
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coJustRun
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
class SessionRemoteDataSourceTest {

    private val api = mockk<SessionApi>()

    private val dataSource = SessionRemoteDataSource(api)

    @Test
    fun testLogin_Success() = runTest {
        val response = mockk<LoginResponseDto>()

        coEvery { api.login(LOGIN_REQUEST) } returns response

        val actualResponse = dataSource.login(EMAIL, PASSWORD)

        coVerifySequence { api.login(LOGIN_REQUEST) }

        Truth.assertThat(actualResponse).isSameInstanceAs(response)
    }

    @Test
    fun testLogin_InvalidCredentials() {
        coEvery { api.login(LOGIN_REQUEST) } throws httpException(401)

        Assert.assertThrows(InvalidCredentialsException::class.java) {
            runTest { dataSource.login(EMAIL, PASSWORD) }
        }
    }

    @Test
    fun testLogin_NoConnection() {
        coEvery { api.login(LOGIN_REQUEST) } throws IOException()

        Assert.assertThrows(NoConnectionException::class.java) {
            runTest { dataSource.login(EMAIL, PASSWORD) }
        }
    }

    @Test
    fun testLogin_ServerError() {
        val httpException = httpException(500)

        coEvery { api.login(LOGIN_REQUEST) } throws httpException

        val thrown = Assert.assertThrows(HttpException::class.java) {
            runTest { dataSource.login(EMAIL, PASSWORD) }
        }

        Truth.assertThat(thrown).isSameInstanceAs(httpException)
    }

    @Test
    fun testLogout_Success() = runTest {
        coJustRun { api.logout() }

        dataSource.logout()

        coVerifySequence { api.logout() }
    }

    @Test
    fun testLogout_Unauthorized() {
        coEvery { api.logout() } throws httpException(401)

        Assert.assertThrows(UnauthorizedException::class.java) {
            runTest { dataSource.logout() }
        }
    }

    @Test
    fun testLogout_NoConnection() {
        coEvery { api.logout() } throws IOException()

        Assert.assertThrows(NoConnectionException::class.java) {
            runTest { dataSource.logout() }
        }
    }

    private fun httpException(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    private companion object {
        const val EMAIL = "tech1@example.com"
        const val PASSWORD = "password"
        val LOGIN_REQUEST = LoginRequestDto(EMAIL, PASSWORD)
    }
}
