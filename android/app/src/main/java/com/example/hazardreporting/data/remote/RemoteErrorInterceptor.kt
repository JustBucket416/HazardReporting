package com.example.hazardreporting.data.remote

import com.example.hazardreporting.data.session.SessionApi
import com.example.hazardreporting.domain.exception.InvalidCredentialsException
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Turns the server's transport failures into domain exceptions for every call:
 * - a failure to reach the server becomes [NoConnectionException];
 * - a 401 to a login request means wrong credentials and becomes [InvalidCredentialsException];
 * - any other 401 becomes [UnauthorizedException]. When it's about the signed-in session (e.g. after the server
 *   was reset), [onUnauthorized] is called first, and the app stays signed in (ADR-0007). A request that names its
 *   own session, e.g. closing one that was never stored, isn't about the signed-in one.
 *
 * Every other response, including other HTTP errors, is passed on unchanged. The exceptions are [IOException]s,
 * since OkHttp rethrows anything else from an interceptor on its own thread. Must be added before
 * [com.example.hazardreporting.data.session.AuthInterceptor], so it sees requests before their token is added
 *
 * @property onUnauthorized called on OkHttp's thread whenever the server rejects the signed-in session
 *
 * @author Roman Pliskin on 29.10.2026
 */
class RemoteErrorInterceptor(
    private val onUnauthorized: () -> Unit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            throw NoConnectionException(e)
        }
        if (response.code != HTTP_UNAUTHORIZED) return response

        response.close()
        if (request.url.pathSegments == listOf(SessionApi.LOGIN_PATH)) throw InvalidCredentialsException()
        if (request.header(SessionApi.AUTHORIZATION_HEADER) == null) onUnauthorized()
        throw UnauthorizedException()
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
