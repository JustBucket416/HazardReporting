package com.example.hazardreporting.data.session

import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class UnauthorizedInterceptorTest {

    private val chain = mockk<Interceptor.Chain>()
    private val response = mockk<Response>()
    private var unauthorizedCount = 0

    private val interceptor = UnauthorizedInterceptor(onUnauthorized = { unauthorizedCount++ })

    @Test
    fun testIntercept_Unauthorized() {
        every { chain.request() } returns request(SUMMARY_URL)
        every { chain.proceed(any()) } returns response
        every { response.code } returns HTTP_UNAUTHORIZED

        val actualResponse = interceptor.intercept(chain)

        // Reported, and the response still reaches the caller unchanged
        Truth.assertThat(unauthorizedCount).isEqualTo(1)
        Truth.assertThat(actualResponse).isSameInstanceAs(response)
    }

    @Test
    fun testIntercept_OtherCode() {
        every { chain.request() } returns request(SUMMARY_URL)
        every { chain.proceed(any()) } returns response
        every { response.code } returns HTTP_FORBIDDEN

        val actualResponse = interceptor.intercept(chain)

        Truth.assertThat(unauthorizedCount).isEqualTo(0)
        Truth.assertThat(actualResponse).isSameInstanceAs(response)
    }

    @Test
    fun testIntercept_Login() {
        every { chain.request() } returns request(LOGIN_URL)
        every { chain.proceed(any()) } returns response
        every { response.code } returns HTTP_UNAUTHORIZED

        interceptor.intercept(chain)

        // A 401 to a login means wrong credentials, not a rejected session
        Truth.assertThat(unauthorizedCount).isEqualTo(0)
    }

    @Test
    fun testIntercept_ExplicitAuthorization() {
        val request = Request.Builder().url(LOGOUT_URL).header(AUTHORIZATION, "Bearer $OTHER_TOKEN").build()

        every { chain.request() } returns request
        every { chain.proceed(any()) } returns response
        every { response.code } returns HTTP_UNAUTHORIZED

        interceptor.intercept(chain)

        // A request that names its own session isn't about the signed-in one
        Truth.assertThat(unauthorizedCount).isEqualTo(0)
    }

    private fun request(url: String) = Request.Builder().url(url).build()

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val OTHER_TOKEN = "other-token"
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val SUMMARY_URL = "http://10.0.2.2:3001/incidents/summary"
        const val LOGIN_URL = "http://10.0.2.2:3001/login"
        const val LOGOUT_URL = "http://10.0.2.2:3001/logout"
    }
}
