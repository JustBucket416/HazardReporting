package com.example.hazardreporting.data.session

import com.example.hazardreporting.data.session.dto.LoginRequestDto
import com.example.hazardreporting.data.session.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for the server's session routes
 *
 * @author Roman Pliskin on 29.10.2026
 */
interface SessionApi {

    /**
     * Signs in and creates a new session
     */
    @POST(LOGIN_PATH)
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    /**
     * Deletes the session that [authorization] names. The header is passed explicitly, so a session
     * that was never stored, or was already cleared from the device, can still be closed
     *
     * @param authorization value of the `Authorization` header, made with [bearer]
     */
    @POST(LOGOUT_PATH)
    suspend fun logout(@Header(AUTHORIZATION_HEADER) authorization: String)

    /**
     * Paths and headers of the session routes
     */
    companion object {

        /**
         * Path of the login route, the only route called without a token
         */
        const val LOGIN_PATH = "login"

        /**
         * Name of the header that carries the session token
         */
        const val AUTHORIZATION_HEADER = "Authorization"

        private const val LOGOUT_PATH = "logout"
        private const val BEARER_PREFIX = "Bearer "

        /**
         * Returns the `Authorization` header value that presents [token]
         */
        fun bearer(token: String): String = "$BEARER_PREFIX$token"
    }
}
