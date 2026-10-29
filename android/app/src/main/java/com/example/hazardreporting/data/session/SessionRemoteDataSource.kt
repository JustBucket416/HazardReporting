package com.example.hazardreporting.data.session

import com.example.hazardreporting.data.remote.RemoteErrorInterceptor
import com.example.hazardreporting.data.session.dto.LoginRequestDto
import com.example.hazardreporting.data.session.dto.LoginResponseDto
import com.example.hazardreporting.domain.exception.InvalidCredentialsException
import javax.inject.Inject

/**
 * Calls the server's session routes. Their failures are domain exceptions thrown by [RemoteErrorInterceptor]
 *
 * @property api used for making calls to the server
 *
 * @author Roman Pliskin on 29.10.2026
 */
class SessionRemoteDataSource @Inject constructor(
    private val api: SessionApi,
) {

    /**
     * Signs in with [email] and [password]
     *
     * @throws InvalidCredentialsException if the server rejects the credentials
     * @throws com.example.hazardreporting.domain.exception.NoConnectionException if the server can't be reached
     */
    suspend fun login(email: String, password: String): LoginResponseDto = api.login(LoginRequestDto(email, password))

    /**
     * Deletes the session with [token] on the server
     *
     * @throws com.example.hazardreporting.domain.exception.UnauthorizedException if the server doesn't know the session
     * @throws com.example.hazardreporting.domain.exception.NoConnectionException if the server can't be reached
     */
    suspend fun logout(token: String) = api.logout(SessionApi.bearer(token))
}
