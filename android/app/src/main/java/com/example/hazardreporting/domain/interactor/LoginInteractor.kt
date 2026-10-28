package com.example.hazardreporting.domain.interactor

import com.example.hazardreporting.domain.exception.NotTechnicianException
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
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
     * Signs in with [email], ignoring surrounding whitespace, and [password]. The app is for technicians only:
     * anyone else's new session is closed on the server and nothing is stored
     *
     * @return the signed-in technician
     * @throws com.example.hazardreporting.domain.exception.InvalidCredentialsException if the server rejects the credentials
     * @throws com.example.hazardreporting.domain.exception.NoConnectionException if the server can't be reached
     * @throws NotTechnicianException if the user isn't a technician
     */
    suspend fun login(email: String, password: String): User {
        val session = sessionRepository.openSession(email.trim(), password)
        if (session.user.role != UserRole.Technician) {
            sessionRepository.closeSession(session)
            throw NotTechnicianException(session.user.role)
        }
        sessionRepository.saveSession(session)
        return session.user
    }

    /**
     * Returns the signed-in user, or null if nobody is signed in
     */
    suspend fun currentUser(): User? = sessionRepository.currentUser()
}
