package com.example.hazardreporting.domain.repository

import com.example.hazardreporting.domain.model.User

/**
 * Signs the user in and keeps their session
 *
 * @author Roman Pliskin on 29.10.2026
 */
interface SessionRepository {

    /**
     * Signs in with [email] and [password] and stores the new session
     *
     * @return the signed-in user
     * @throws com.example.hazardreporting.domain.exception.InvalidCredentialsException if the server rejects the credentials
     * @throws com.example.hazardreporting.domain.exception.NoConnectionException if the server can't be reached
     */
    suspend fun login(email: String, password: String): User

    /**
     * Returns the signed-in user, or null if nobody is signed in
     */
    suspend fun currentUser(): User?
}
