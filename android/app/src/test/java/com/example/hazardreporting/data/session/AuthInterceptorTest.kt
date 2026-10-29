package com.example.hazardreporting.data.session

import com.example.hazardreporting.domain.model.Session
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
import com.google.common.truth.Truth
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class AuthInterceptorTest {

    private val sessionHolder = mockk<SessionHolder>()
    private val chain = mockk<Interceptor.Chain>()
    private val response = mockk<Response>()

    private val interceptor = AuthInterceptor(sessionHolder)

    @Test
    fun testIntercept_SignedIn() {
        val proceededRequest = slot<Request>()

        every { chain.request() } returns request(CATEGORIES_URL)
        every { sessionHolder.sessionBlocking() } returns SESSION
        every { chain.proceed(capture(proceededRequest)) } returns response

        val actualResponse = interceptor.intercept(chain)

        Truth.assertThat(actualResponse).isSameInstanceAs(response)
        Truth.assertThat(proceededRequest.captured.header(AUTHORIZATION)).isEqualTo("Bearer $TOKEN")
    }

    @Test
    fun testIntercept_SignedOut() {
        val proceededRequest = slot<Request>()

        every { chain.request() } returns request(CATEGORIES_URL)
        every { sessionHolder.sessionBlocking() } returns null
        every { chain.proceed(capture(proceededRequest)) } returns response

        val actualResponse = interceptor.intercept(chain)

        Truth.assertThat(actualResponse).isSameInstanceAs(response)
        Truth.assertThat(proceededRequest.captured.header(AUTHORIZATION)).isNull()
    }

    @Test
    fun testIntercept_Login() {
        val proceededRequest = slot<Request>()

        every { chain.request() } returns request(LOGIN_URL)
        every { chain.proceed(capture(proceededRequest)) } returns response

        val actualResponse = interceptor.intercept(chain)

        // Login never waits for the session
        verify { sessionHolder wasNot Called }

        Truth.assertThat(actualResponse).isSameInstanceAs(response)
        Truth.assertThat(proceededRequest.captured.header(AUTHORIZATION)).isNull()
    }

    @Test
    fun testIntercept_ExplicitAuthorization() {
        val request = Request.Builder().url(LOGOUT_URL).header(AUTHORIZATION, "Bearer $OTHER_TOKEN").build()
        val proceededRequest = slot<Request>()

        every { chain.request() } returns request
        every { chain.proceed(capture(proceededRequest)) } returns response

        val actualResponse = interceptor.intercept(chain)

        // A request that names its own session keeps it and never waits for the stored one
        verify { sessionHolder wasNot Called }

        Truth.assertThat(actualResponse).isSameInstanceAs(response)
        Truth.assertThat(proceededRequest.captured.header(AUTHORIZATION)).isEqualTo("Bearer $OTHER_TOKEN")
    }

    private fun request(url: String) = Request.Builder().url(url).build()

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val TOKEN = "token"
        const val OTHER_TOKEN = "other-token"
        const val CATEGORIES_URL = "http://10.0.2.2:3001/categories"
        const val LOGIN_URL = "http://10.0.2.2:3001/login"
        const val LOGOUT_URL = "http://10.0.2.2:3001/logout"
        val SESSION = Session(TOKEN, User("u1", "tech1@example.com", "Tech One", UserRole.Technician))
    }
}
