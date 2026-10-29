package com.example.hazardreporting.domain.interactor

import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.NotTechnicianException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.example.hazardreporting.domain.model.LogoutResult
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
import com.example.hazardreporting.domain.repository.DeviceDataRepository
import com.example.hazardreporting.domain.repository.SessionRepository
import com.example.hazardreporting.domain.repository.SyncRepository
import com.example.hazardreporting.domain.repository.SyncScheduler
import kotlinx.coroutines.flow.Flow

/**
 * Use cases of signing in and out
 *
 * @property sessionRepository signs the user in and keeps their session
 * @property syncRepository pushes pending changes before signing out
 * @property syncScheduler schedules the periodic push for a signed-in technician, and cancels it on signing out
 * @property deviceDataRepository wiped on signing out
 *
 * @author Roman Pliskin on 29.10.2026
 */
class LoginInteractor(
    private val sessionRepository: SessionRepository,
    private val syncRepository: SyncRepository,
    private val syncScheduler: SyncScheduler,
    private val deviceDataRepository: DeviceDataRepository,
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

    /**
     * Returns how many incidents have a pending change that signing out would lose if it couldn't be sent
     */
    suspend fun countPendingChanges(): Int = syncRepository.countPendingChanges()

    /**
     * Signs out (spec § 3.2, ADR-0007). First tries to push the pending changes. If some couldn't be sent and
     * [force] isn't set, nothing else happens and their count is returned, so the technician can decide.
     * Otherwise everything on the device is wiped, in this order: scheduled pushes are cancelled and the sync
     * lock is taken, so no push or pull writes meanwhile; the database is cleared; the photo files are deleted;
     * the image caches are cleared; the session is forgotten. Finally the server is told to close the session,
     * which may fail without consequence
     *
     * @param force whether to sign out even if pending changes would be lost
     */
    suspend fun logout(force: Boolean): LogoutResult {
        try {
            syncRepository.push()
        } catch (e: NoConnectionException) {
            // Whatever couldn't be sent is counted below
        } catch (e: UnauthorizedException) {
            // Likewise; a rejected session is the usual reason to sign out and in again
        }
        if (!force) {
            val pendingCount = syncRepository.countPendingChanges()
            if (pendingCount > 0) return LogoutResult.PendingChangesRemain(pendingCount)
        }
        wipe()
        return LogoutResult.LoggedOut
    }

    private suspend fun wipe() {
        // Read first: the session is forgotten before the server is told
        val session = sessionRepository.currentSession()
        syncScheduler.cancelScheduledPushes()
        syncRepository.withSyncPaused {
            deviceDataRepository.clearDatabase()
            deviceDataRepository.deletePhotoFiles()
            deviceDataRepository.clearImageCaches()
            sessionRepository.clearSession()
        }
        if (session != null) sessionRepository.closeSession(session)
    }
}
