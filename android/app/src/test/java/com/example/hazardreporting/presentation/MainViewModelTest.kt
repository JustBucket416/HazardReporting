package com.example.hazardreporting.presentation

import com.example.hazardreporting.domain.interactor.LoginInteractor
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
import com.example.hazardreporting.presentation.navigation.IncidentList
import com.example.hazardreporting.presentation.navigation.Login
import com.example.hazardreporting.utils.CoroutineDispatchers
import com.example.hazardreporting.utils.Logger
import com.example.hazardreporting.utils.MainCoroutineRule
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @Rule
    @JvmField
    val rule = MainCoroutineRule()

    private val loginInteractor = mockk<LoginInteractor>()
    private val dispatchers = mockk<CoroutineDispatchers>()
    private val logger = mockk<Logger>()

    @Before
    fun setUp() {
        every { dispatchers.default } returns rule.testDispatcher
    }

    @Test
    fun testInit_SignedIn() = runTest {
        coEvery { loginInteractor.currentUser() } returns USER

        val viewModel = MainViewModel(loginInteractor, dispatchers, logger)

        // The splash screen stays up until the start screen is known
        Truth.assertThat(viewModel.uiState.value.startDestination).isNull()

        advanceUntilIdle()

        coVerifySequence { loginInteractor.currentUser() }

        Truth.assertThat(viewModel.uiState.value.startDestination).isEqualTo(IncidentList())
    }

    @Test
    fun testInit_SignedOut() = runTest {
        coEvery { loginInteractor.currentUser() } returns null

        val viewModel = MainViewModel(loginInteractor, dispatchers, logger)
        advanceUntilIdle()

        coVerifySequence { loginInteractor.currentUser() }

        Truth.assertThat(viewModel.uiState.value.startDestination).isEqualTo(Login)
    }

    @Test
    fun testInit_UnexpectedException() = runTest {
        val exception = IllegalStateException()

        coEvery { loginInteractor.currentUser() } throws exception
        justRun { logger.error(any(), any(), exception) }

        val viewModel = MainViewModel(loginInteractor, dispatchers, logger)
        advanceUntilIdle()

        verifySequence { logger.error("MainViewModel", any(), exception) }

        Truth.assertThat(viewModel.uiState.value.startDestination).isEqualTo(Login)
    }

    private companion object {
        val USER = User("u1", "tech1@example.com", "Tech One", UserRole.Technician)
    }
}
