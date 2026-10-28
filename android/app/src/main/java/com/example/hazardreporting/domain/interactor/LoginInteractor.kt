package com.example.hazardreporting.domain.interactor

import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.repository.SessionRepository

/**
 * Use cases of signing in and out
 *
 * @property sessionRepository signs the user in and keeps their session
 *
 * @author Roman Pliskin on 29.10.2026
 */
class LoginInteractor(
    private val sessionRepository: SessionRepository,
) {

    /**
     * Signs in with [email], ignoring surrounding whitespace, and [password]
     *
     * @return the signed-in user
     * @throws com.example.hazardreporting.domain.exception.InvalidCredentialsException if the server rejects the credentials
     * @throws com.example.hazardreporting.domain.exception.NoConnectionException if the server can't be reached
     */
    suspend fun login(email: String, password: String): User = sessionRepository.login(email.trim(), password)

    /**
     * Returns the signed-in user, or null if nobody is signed in
     */
    suspend fun currentUser(): User? = sessionRepository.currentUser()
}
