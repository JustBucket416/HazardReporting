package com.example.hazardreporting.domain.interactor

import com.example.hazardreporting.domain.exception.InvalidCredentialsException
import com.example.hazardreporting.domain.exception.NotTechnicianException
import com.example.hazardreporting.domain.model.Session
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
import com.example.hazardreporting.domain.repository.SessionRepository
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class LoginInteractorTest {

    private val sessionRepository = mockk<SessionRepository>()

    private val interactor = LoginInteractor(sessionRepository)

    @Test
    fun testLogin_Technician() = runTest {
        coEvery { sessionRepository.openSession(EMAIL, PASSWORD) } returns TECHNICIAN_SESSION
        coJustRun { sessionRepository.saveSession(TECHNICIAN_SESSION) }

        val user = interactor.login(EMAIL, PASSWORD)

        coVerifySequence {
            sessionRepository.openSession(EMAIL, PASSWORD)
            sessionRepository.saveSession(TECHNICIAN_SESSION)
        }

        Truth.assertThat(user).isEqualTo(TECHNICIAN_SESSION.user)
    }

    @Test
    fun testLogin_TrimsEmail() = runTest {
        coEvery { sessionRepository.openSession(EMAIL, PASSWORD) } returns TECHNICIAN_SESSION
        coJustRun { sessionRepository.saveSession(TECHNICIAN_SESSION) }

        interactor.login("  $EMAIL ", PASSWORD)

        coVerifySequence {
            sessionRepository.openSession(EMAIL, PASSWORD)
            sessionRepository.saveSession(TECHNICIAN_SESSION)
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

    private fun sessionOf(role: UserRole) = Session("token", User("u1", EMAIL, "Someone", role))

    private companion object {
        const val EMAIL = "tech1@example.com"
        const val PASSWORD = " password "
        val TECHNICIAN_SESSION = Session("token", User("u1", EMAIL, "Tech One", UserRole.Technician))
    }
}
