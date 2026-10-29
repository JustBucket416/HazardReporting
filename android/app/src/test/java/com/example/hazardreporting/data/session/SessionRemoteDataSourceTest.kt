package com.example.hazardreporting.data.session

import com.example.hazardreporting.data.session.dto.LoginRequestDto
import com.example.hazardreporting.data.session.dto.LoginResponseDto
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerifySequence
import io.mockk.mockk
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
        coJustRun { api.logout(AUTHORIZATION) }

        dataSource.logout(TOKEN)

        // The token is sent explicitly, so a session that was never stored can be closed
        coVerifySequence { api.logout(AUTHORIZATION) }
    }

    private fun httpException(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    private companion object {
        const val EMAIL = "tech1@example.com"
        const val PASSWORD = "password"
        const val TOKEN = "token"
        const val AUTHORIZATION = "Bearer token"
        val LOGIN_REQUEST = LoginRequestDto(EMAIL, PASSWORD)
    }
}
