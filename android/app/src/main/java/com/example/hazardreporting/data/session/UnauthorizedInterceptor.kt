package com.example.hazardreporting.data.session

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Watches every response for a 401, the server's way of rejecting the signed-in session (e.g. after it was
 * reset), and reports it through [onUnauthorized]. The response itself is passed on unchanged, and the app
 * stays signed in (ADR-0007). A 401 to a login request means wrong credentials, and one to a request that names
 * its own session (e.g. closing a session that was never stored) isn't about the signed-in one, so both are
 * left alone. Must be added before [AuthInterceptor], so it sees requests before their token is added
 *
 * @property onUnauthorized called on OkHttp's thread whenever the server rejects the signed-in session
 *
 * @author Roman Pliskin on 29.10.2026
 */
class UnauthorizedInterceptor(
    private val onUnauthorized: () -> Unit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val isAboutSignedInSession = request.url.pathSegments != listOf(SessionApi.LOGIN_PATH) &&
            request.header(SessionApi.AUTHORIZATION_HEADER) == null
        if (response.code == HTTP_UNAUTHORIZED && isAboutSignedInSession) onUnauthorized()
        return response
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
