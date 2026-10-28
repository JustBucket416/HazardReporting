package com.example.hazardreporting.domain.interactor

import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
import com.example.hazardreporting.domain.repository.SessionRepository
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class LoginInteractorTest {

    private val sessionRepository = mockk<SessionRepository>()

    private val interactor = LoginInteractor(sessionRepository)

    @Test
    fun testLogin() = runTest {
        coEvery { sessionRepository.login(EMAIL, PASSWORD) } returns USER

        val user = interactor.login(EMAIL, PASSWORD)

        coVerifySequence { sessionRepository.login(EMAIL, PASSWORD) }

        Truth.assertThat(user).isEqualTo(USER)
    }

    @Test
    fun testLogin_TrimsEmail() = runTest {
        coEvery { sessionRepository.login(EMAIL, PASSWORD) } returns USER

        interactor.login("  $EMAIL ", PASSWORD)

        coVerifySequence { sessionRepository.login(EMAIL, PASSWORD) }
    }

    @Test
    fun testCurrentUser_SignedIn() = runTest {
        coEvery { sessionRepository.currentUser() } returns USER

        val user = interactor.currentUser()

        coVerifySequence { sessionRepository.currentUser() }

        Truth.assertThat(user).isEqualTo(USER)
    }

    @Test
    fun testCurrentUser_SignedOut() = runTest {
        coEvery { sessionRepository.currentUser() } returns null

        val user = interactor.currentUser()

        coVerifySequence { sessionRepository.currentUser() }

        Truth.assertThat(user).isNull()
    }

    private companion object {
        const val EMAIL = "tech1@example.com"
        const val PASSWORD = " password "
        val USER = User("u1", EMAIL, "Tech One", UserRole.Technician)
    }
}
