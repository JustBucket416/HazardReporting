package com.example.hazardreporting.data.remote

import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import java.io.IOException
import retrofit2.HttpException

/**
 * @author Roman Pliskin on 29.10.2026
 */

private const val HTTP_UNAUTHORIZED = 401

/**
 * Runs a remote [call] and translates its transport failures into domain exceptions:
 * an [IOException] becomes [NoConnectionException], and an HTTP 401 becomes the exception
 * made by [onUnauthorized]. Every other exception, including other HTTP errors, propagates unchanged
 *
 * @param onUnauthorized makes the exception for an HTTP 401; [UnauthorizedException] by default
 * @param call the Retrofit call
 */
suspend fun <T> mapRemoteErrors(
    onUnauthorized: (HttpException) -> Exception = { UnauthorizedException(it) },
    call: suspend () -> T,
): T = try {
    call()
} catch (e: IOException) {
    throw NoConnectionException(e)
} catch (e: HttpException) {
    throw if (e.code() == HTTP_UNAUTHORIZED) onUnauthorized(e) else e
}
