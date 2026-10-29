package com.example.hazardreporting.domain.repository

import com.example.hazardreporting.domain.model.Session
import com.example.hazardreporting.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Opens, keeps and closes the user's session. Opening a session doesn't store it, so the caller can
 * decide whether to keep it ([saveSession]) or throw it away ([closeSession])
 *
 * @author Roman Pliskin on 29.10.2026
 */
interface SessionRepository {

    /**
     * Signs in with [email] and [password] and returns the new session without storing it
     *
     * @throws com.example.hazardreporting.domain.exception.InvalidCredentialsException if the server rejects the credentials
     * @throws com.example.hazardreporting.domain.exception.NoConnectionException if the server can't be reached
     */
    suspend fun openSession(email: String, password: String): Session

    /**
     * Stores [session] as the signed-in session, so every following request is made with it
     */
    suspend fun saveSession(session: Session)

    /**
     * Closes [session] on the server. Best effort: a failure is ignored, because the session
     * is useless to the device either way. Doesn't touch the stored session
     */
    suspend fun closeSession(session: Session)

    /**
     * Returns the signed-in user, or null if nobody is signed in
     */
    suspend fun currentUser(): User?

    /**
     * Observes whether the server rejected the signed-in session, e.g. after it was reset. A rejected session
     * stays signed in (ADR-0007); signing in again replaces it
     */
    fun observeSessionInvalid(): Flow<Boolean>

    /**
     * Records that the server rejected the signed-in session
     */
    fun markSessionInvalid()
}
