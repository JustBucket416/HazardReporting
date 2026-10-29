package com.example.hazardreporting.data.remote

import com.example.hazardreporting.domain.exception.InvalidCredentialsException
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class RemoteErrorInterceptorTest {

    private val chain = mockk<Interceptor.Chain>()
    private var unauthorizedCount = 0

    private val interceptor = RemoteErrorInterceptor(onUnauthorized = { unauthorizedCount++ })

    @Test
    fun testIntercept_Success() {
        val request = request(SUMMARY_URL)
        val response = response(request, HTTP_OK)

        every { chain.request() } returns request
        every { chain.proceed(request) } returns response

        val actualResponse = interceptor.intercept(chain)

        Truth.assertThat(actualResponse).isSameInstanceAs(response)
        Truth.assertThat(unauthorizedCount).isEqualTo(0)
    }

    @Test
    fun testIntercept_NoConnection() {
        val request = request(SUMMARY_URL)
        val ioException = IOException()

        every { chain.request() } returns request
        every { chain.proceed(request) } throws ioException

        val thrown = Assert.assertThrows(NoConnectionException::class.java) { interceptor.intercept(chain) }

        Truth.assertThat(thrown).hasCauseThat().isSameInstanceAs(ioException)
    }

    @Test
    fun testIntercept_Unauthorized() {
        val request = request(SUMMARY_URL)

        every { chain.request() } returns request
        every { chain.proceed(request) } returns response(request, HTTP_UNAUTHORIZED)

        Assert.assertThrows(UnauthorizedException::class.java) { interceptor.intercept(chain) }

        // The signed-in session is marked as rejected
        Truth.assertThat(unauthorizedCount).isEqualTo(1)
    }

    @Test
    fun testIntercept_ExplicitAuthorization() {
        val request = Request.Builder().url(LOGOUT_URL).header(AUTHORIZATION, "Bearer $OTHER_TOKEN").build()

        every { chain.request() } returns request
        every { chain.proceed(request) } returns response(request, HTTP_UNAUTHORIZED)

        Assert.assertThrows(UnauthorizedException::class.java) { interceptor.intercept(chain) }

        // A request that names its own session isn't about the signed-in one
        Truth.assertThat(unauthorizedCount).isEqualTo(0)
    }

    @Test
    fun testIntercept_Login() {
        val request = request(LOGIN_URL)

        every { chain.request() } returns request
        every { chain.proceed(request) } returns response(request, HTTP_UNAUTHORIZED)

        // A 401 to a login means wrong credentials, not a rejected session
        Assert.assertThrows(InvalidCredentialsException::class.java) { interceptor.intercept(chain) }

        Truth.assertThat(unauthorizedCount).isEqualTo(0)
    }

    @Test
    fun testIntercept_OtherError() {
        val request = request(SUMMARY_URL)
        val response = response(request, HTTP_SERVER_ERROR)

        every { chain.request() } returns request
        every { chain.proceed(request) } returns response

        val actualResponse = interceptor.intercept(chain)

        // Left to Retrofit, which throws it as an HttpException
        Truth.assertThat(actualResponse).isSameInstanceAs(response)
        Truth.assertThat(unauthorizedCount).isEqualTo(0)
    }

    private fun request(url: String) = Request.Builder().url(url).build()

    private fun response(request: Request, code: Int) = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("")
        .body("".toResponseBody())
        .build()

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val OTHER_TOKEN = "other-token"
        const val HTTP_OK = 200
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_SERVER_ERROR = 500
        const val SUMMARY_URL = "http://10.0.2.2:3001/incidents/summary"
        const val LOGIN_URL = "http://10.0.2.2:3001/login"
        const val LOGOUT_URL = "http://10.0.2.2:3001/logout"
    }
}
