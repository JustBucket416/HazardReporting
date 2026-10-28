package com.example.hazardreporting.data.session

import com.example.hazardreporting.data.session.dto.LoginRequestDto
import com.example.hazardreporting.data.session.dto.LoginResponseDto
import retrofit2.http.Body
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
     * Deletes the caller's session
     */
    @POST(LOGOUT_PATH)
    suspend fun logout()

    /**
     * Paths of the session routes
     */
    companion object {

        /**
         * Path of the login route, the only route called without a token
         */
        const val LOGIN_PATH = "login"

        private const val LOGOUT_PATH = "logout"
    }
}
