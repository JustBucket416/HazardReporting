package com.example.hazardreporting.data.session

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds `Authorization: Bearer <token>` to every request while someone is signed in.
 * Login requests, and requests that already name a session in their `Authorization` header
 * (e.g. closing a session that was never stored), are sent as they are, without touching [SessionHolder]
 *
 * @property sessionHolder provides the token
 *
 * @author Roman Pliskin on 29.10.2026
 */
class AuthInterceptor @Inject constructor(
    private val sessionHolder: SessionHolder,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.pathSegments == listOf(SessionApi.LOGIN_PATH)) return chain.proceed(request)
        if (request.header(SessionApi.AUTHORIZATION_HEADER) != null) return chain.proceed(request)

        val token = sessionHolder.sessionBlocking()?.token ?: return chain.proceed(request)
        val authorizedRequest = request.newBuilder()
            .header(SessionApi.AUTHORIZATION_HEADER, SessionApi.bearer(token))
            .build()
        return chain.proceed(authorizedRequest)
    }
}
