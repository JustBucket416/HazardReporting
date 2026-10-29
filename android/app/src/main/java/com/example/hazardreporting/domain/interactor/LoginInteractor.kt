package com.example.hazardreporting.domain.interactor

import com.example.hazardreporting.domain.exception.NotTechnicianException
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
import com.example.hazardreporting.domain.repository.SessionRepository
import com.example.hazardreporting.domain.repository.SyncScheduler
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of signing in and out
 *
 * @property sessionRepository signs the user in and keeps their session
 * @property syncScheduler schedules the periodic push for a signed-in technician
 *
 * @author Roman Pliskin on 29.10.2026
 */
class LoginInteractor(
    private val sessionRepository: SessionRepository,
    private val syncScheduler: SyncScheduler,
) {

    /**
     * Signs in with [email], ignoring surrounding whitespace, and [password]. The app is for technicians only:
     * anyone else's new session is closed on the server and nothing is stored. A technician's pending changes
     * are pushed every 15 minutes from now on
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
        syncScheduler.schedulePeriodicPush()
        return session.user
    }

    /**
     * Returns the signed-in user, or null if nobody is signed in
     */
    suspend fun currentUser(): User? = sessionRepository.currentUser()

    /**
     * Observes whether the server rejected the session, e.g. after it was reset. Sync stops then, but the
     * technician stays signed in until they sign out themselves (ADR-0007)
     */
    fun observeSessionInvalid(): Flow<Boolean> = sessionRepository.observeSessionInvalid()
}
