package com.example.hazardreporting.domain.interactor

import com.example.hazardreporting.domain.exception.InvalidCredentialsException
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.NotTechnicianException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.example.hazardreporting.domain.model.LogoutResult
import com.example.hazardreporting.domain.model.Session
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
import com.example.hazardreporting.domain.repository.DeviceDataRepository
import com.example.hazardreporting.domain.repository.SessionRepository
import com.example.hazardreporting.domain.repository.SyncRepository
import com.example.hazardreporting.domain.repository.SyncScheduler
import com.google.common.truth.Truth
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class LoginInteractorTest {

    private val sessionRepository = mockk<SessionRepository>()
    private val syncRepository = mockk<SyncRepository>()
    private val syncScheduler = mockk<SyncScheduler>()
    private val deviceDataRepository = mockk<DeviceDataRepository>()

    private val interactor = LoginInteractor(sessionRepository, syncRepository, syncScheduler, deviceDataRepository)

    /**
     * Whether the wipe holds the sync lock right now
     */
    private var isSyncPaused = false

    @Test
    fun testLogin_Technician() = runTest {
        coEvery { sessionRepository.openSession(EMAIL, PASSWORD) } returns TECHNICIAN_SESSION
        coJustRun { sessionRepository.saveSession(TECHNICIAN_SESSION) }
        justRun { syncScheduler.schedulePeriodicPush() }

        val user = interactor.login(EMAIL, PASSWORD)

        // The technician's pending changes are pushed every 15 minutes from now on
        coVerifySequence {
            sessionRepository.openSession(EMAIL, PASSWORD)
            sessionRepository.saveSession(TECHNICIAN_SESSION)
            syncScheduler.schedulePeriodicPush()
        }

        Truth.assertThat(user).isEqualTo(TECHNICIAN_SESSION.user)
    }

    @Test
    fun testLogin_TrimsEmail() = runTest {
        coEvery { sessionRepository.openSession(EMAIL, PASSWORD) } returns TECHNICIAN_SESSION
        coJustRun { sessionRepository.saveSession(TECHNICIAN_SESSION) }
        justRun { syncScheduler.schedulePeriodicPush() }

        interactor.login("  $EMAIL ", PASSWORD)

        coVerifySequence {
            sessionRepository.openSession(EMAIL, PASSWORD)
            sessionRepository.saveSession(TECHNICIAN_SESSION)
            syncScheduler.schedulePeriodicPush()
        }
    }

    @Test
    fun testLogin_Operator() {
        val session = sessionOf(UserRole.Operator)

        coEvery { sessionRepository.openSession(EMAIL, PASSWORD) } returns session
        coJustRun { sessionRepository.closeSession(session) }

        Assert.assertThrows(NotTechnicianException::class.java) {
            runTest { interactor.login(EMAIL, PASSWORD) }
        }

        // The operator's session is closed on the server and never stored
        coVerifySequence {
            sessionRepository.openSession(EMAIL, PASSWORD)
            sessionRepository.closeSession(session)
        }
        verify { syncScheduler wasNot Called }
    }

    @Test
    fun testLogin_UnknownRole() {
        val session = sessionOf(UserRole.Unknown)

        coEvery { sessionRepository.openSession(EMAIL, PASSWORD) } returns session
        coJustRun { sessionRepository.closeSession(session) }

        Assert.assertThrows(NotTechnicianException::class.java) {
            runTest { interactor.login(EMAIL, PASSWORD) }
        }

        coVerifySequence {
            sessionRepository.openSession(EMAIL, PASSWORD)
            sessionRepository.closeSession(session)
        }
    }

    @Test
    fun testLogin_InvalidCredentials() {
        coEvery { sessionRepository.openSession(EMAIL, PASSWORD) } throws InvalidCredentialsException()

        Assert.assertThrows(InvalidCredentialsException::class.java) {
            runTest { interactor.login(EMAIL, PASSWORD) }
        }

        coVerifySequence { sessionRepository.openSession(EMAIL, PASSWORD) }
    }

    @Test
    fun testCurrentUser_SignedIn() = runTest {
        coEvery { sessionRepository.currentUser() } returns TECHNICIAN_SESSION.user

        val user = interactor.currentUser()

        coVerifySequence { sessionRepository.currentUser() }

        Truth.assertThat(user).isEqualTo(TECHNICIAN_SESSION.user)
    }

    @Test
    fun testCurrentUser_SignedOut() = runTest {
        coEvery { sessionRepository.currentUser() } returns null

        val user = interactor.currentUser()

        coVerifySequence { sessionRepository.currentUser() }

        Truth.assertThat(user).isNull()
    }

    @Test
    fun testObserveSessionInvalid() {
        val isInvalid = flowOf(true)

        every { sessionRepository.observeSessionInvalid() } returns isInvalid

        Truth.assertThat(interactor.observeSessionInvalid()).isSameInstanceAs(isInvalid)
    }

    @Test
    fun testCountPendingChanges() = runTest {
        coEvery { syncRepository.countPendingChanges() } returns 2

        Truth.assertThat(interactor.countPendingChanges()).isEqualTo(2)
    }

    @Test
    fun testLogout_EverythingSent() = runTest {
        coJustRun { syncRepository.push() }
        coEvery { syncRepository.countPendingChanges() } returns 0
        stubWipe(TECHNICIAN_SESSION)

        val result = interactor.logout(force = false)

        // Pushes first; then the wipe, in exactly this order, with the sync lock held while the device is cleared
        coVerifyOrder {
            syncRepository.push()
            syncRepository.countPendingChanges()
            syncScheduler.cancelScheduledPushes()
            syncRepository.withSyncPaused<Unit>(any())
            deviceDataRepository.clearDatabase()
            deviceDataRepository.deletePhotoFiles()
            deviceDataRepository.clearImageCaches()
            sessionRepository.clearSession()
            sessionRepository.closeSession(TECHNICIAN_SESSION)
        }

        Truth.assertThat(result).isEqualTo(LogoutResult.LoggedOut)
    }

    @Test
    fun testLogout_PendingChangesRemain() = runTest {
        coEvery { syncRepository.push() } throws NoConnectionException()
        coEvery { syncRepository.countPendingChanges() } returns 1

        val result = interactor.logout(force = false)

        // The technician decides; nothing is wiped meanwhile
        coVerifySequence {
            syncRepository.push()
            syncRepository.countPendingChanges()
        }
        verify { syncScheduler wasNot Called }
        verify { deviceDataRepository wasNot Called }
        verify { sessionRepository wasNot Called }

        Truth.assertThat(result).isEqualTo(LogoutResult.PendingChangesRemain(1))
    }

    @Test
    fun testLogout_SessionRejected() = runTest {
        // The usual reason to sign out: nothing can be sent with a rejected session
        coEvery { syncRepository.push() } throws UnauthorizedException()
        coEvery { syncRepository.countPendingChanges() } returns 2

        val result = interactor.logout(force = false)

        verify { deviceDataRepository wasNot Called }
        Truth.assertThat(result).isEqualTo(LogoutResult.PendingChangesRemain(2))
    }

    @Test
    fun testLogout_Forced() = runTest {
        coEvery { syncRepository.push() } throws NoConnectionException()
        stubWipe(TECHNICIAN_SESSION)

        val result = interactor.logout(force = true)

        // Still tries to send first, but wipes whatever is left
        coVerifyOrder {
            syncRepository.push()
            syncScheduler.cancelScheduledPushes()
            syncRepository.withSyncPaused<Unit>(any())
            deviceDataRepository.clearDatabase()
            deviceDataRepository.deletePhotoFiles()
            deviceDataRepository.clearImageCaches()
            sessionRepository.clearSession()
            sessionRepository.closeSession(TECHNICIAN_SESSION)
        }
        coVerify(exactly = 0) { syncRepository.countPendingChanges() }

        Truth.assertThat(result).isEqualTo(LogoutResult.LoggedOut)
    }

    @Test
    fun testLogout_NoSessionStored() = runTest {
        coJustRun { syncRepository.push() }
        coEvery { syncRepository.countPendingChanges() } returns 0
        stubWipe(session = null)

        interactor.logout(force = false)

        // Wiped all the same; there is no session to close on the server
        coVerify(exactly = 1) { deviceDataRepository.clearDatabase() }
        coVerify(exactly = 0) { sessionRepository.closeSession(any()) }
    }

    @Test
    fun testLogout_UnexpectedException() {
        val exception = IllegalStateException()

        coEvery { syncRepository.push() } throws exception

        val thrown = Assert.assertThrows(IllegalStateException::class.java) {
            runTest { interactor.logout(force = false) }
        }

        Truth.assertThat(thrown).isSameInstanceAs(exception)
        verify { deviceDataRepository wasNot Called }
    }

    /**
     * Stubs the wipe; every step that clears the device checks that it runs while the sync lock is held
     */
    private fun stubWipe(session: Session?) {
        coEvery { sessionRepository.currentSession() } returns session
        justRun { syncScheduler.cancelScheduledPushes() }
        coEvery { syncRepository.withSyncPaused<Unit>(any()) } coAnswers {
            isSyncPaused = true
            firstArg<suspend () -> Unit>().invoke()
            isSyncPaused = false
        }
        coEvery { deviceDataRepository.clearDatabase() } answers { check(isSyncPaused) }
        coEvery { deviceDataRepository.deletePhotoFiles() } answers { check(isSyncPaused) }
        coEvery { deviceDataRepository.clearImageCaches() } answers { check(isSyncPaused) }
        coEvery { sessionRepository.clearSession() } answers { check(isSyncPaused) }
        coJustRun { sessionRepository.closeSession(any()) }
    }

    private fun sessionOf(role: UserRole) = Session("token", User("u1", EMAIL, "Someone", role))

    private companion object {
        const val EMAIL = "tech1@example.com"
        const val PASSWORD = " password "
        val TECHNICIAN_SESSION = Session("token", User("u1", EMAIL, "Tech One", UserRole.Technician))
    }
}
