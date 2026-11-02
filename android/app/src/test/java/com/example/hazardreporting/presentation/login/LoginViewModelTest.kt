package com.example.hazardreporting.presentation.login

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.hazardreporting.domain.exception.InvalidCredentialsException
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.NotTechnicianException
import com.example.hazardreporting.domain.interactor.LoginInteractor
import com.example.hazardreporting.domain.model.User
import com.example.hazardreporting.domain.model.UserRole
import com.example.hazardreporting.utils.Logger
import com.example.hazardreporting.utils.MainCoroutineRule
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @Rule
    @JvmField
    val rule = MainCoroutineRule()

    private val savedStateHandle = SavedStateHandle()
    private val loginInteractor = mockk<LoginInteractor>()

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        mockkObject(Logger)

        viewModel = LoginViewModel(savedStateHandle, loginInteractor)
        viewModel.onEmailChange(EMAIL)
        viewModel.onPasswordChange(PASSWORD)
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
    }

    @Test
    fun testOnSubmit_Success() = runTest {
        coEvery { loginInteractor.login(EMAIL, PASSWORD) } returns USER

        viewModel.events.test {
            viewModel.onSubmit()

            Truth.assertThat(viewModel.uiState.value.isSigningIn).isTrue()

            advanceUntilIdle()

            Truth.assertThat(awaitItem()).isEqualTo(LoginEvent.LoggedIn)
        }

        coVerifySequence { loginInteractor.login(EMAIL, PASSWORD) }

        // The screen leaves while still signing in, so the button doesn't flash back
        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            LoginUiState(email = EMAIL, password = PASSWORD, isSigningIn = true, error = null)
        )
    }

    @Test
    fun testOnSubmit_InvalidCredentials() = runTest {
        coEvery { loginInteractor.login(EMAIL, PASSWORD) } throws InvalidCredentialsException()

        viewModel.onSubmit()
        advanceUntilIdle()

        coVerifySequence { loginInteractor.login(EMAIL, PASSWORD) }

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            LoginUiState(email = EMAIL, password = PASSWORD, isSigningIn = false, error = LoginError.InvalidCredentials)
        )
    }

    @Test
    fun testOnSubmit_NoConnection() = runTest {
        coEvery { loginInteractor.login(EMAIL, PASSWORD) } throws NoConnectionException()

        viewModel.onSubmit()
        advanceUntilIdle()

        coVerifySequence { loginInteractor.login(EMAIL, PASSWORD) }

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            LoginUiState(email = EMAIL, password = PASSWORD, isSigningIn = false, error = LoginError.NoConnection)
        )
    }

    @Test
    fun testOnSubmit_NotTechnician() = runTest {
        coEvery { loginInteractor.login(EMAIL, PASSWORD) } throws NotTechnicianException(UserRole.Operator)

        viewModel.onSubmit()
        advanceUntilIdle()

        coVerifySequence { loginInteractor.login(EMAIL, PASSWORD) }

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            LoginUiState(email = EMAIL, password = PASSWORD, isSigningIn = false, error = LoginError.NotTechnician)
        )
    }

    @Test
    fun testOnSubmit_UnexpectedException() = runTest {
        val exception = IllegalStateException()

        coEvery { loginInteractor.login(EMAIL, PASSWORD) } throws exception
        justRun { Logger.error(any(), any(), exception) }

        viewModel.events.test {
            viewModel.onSubmit()
            advanceUntilIdle()

            Truth.assertThat(awaitItem()).isEqualTo(LoginEvent.UnexpectedError)
        }

        verifySequence { Logger.error("LoginViewModel", any(), exception) }

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            LoginUiState(email = EMAIL, password = PASSWORD, isSigningIn = false, error = null)
        )
    }

    @Test
    fun testOnSubmit_WhileSigningIn() = runTest {
        coEvery { loginInteractor.login(EMAIL, PASSWORD) } returns USER

        viewModel.onSubmit()
        viewModel.onSubmit()
        advanceUntilIdle()

        coVerifySequence { loginInteractor.login(EMAIL, PASSWORD) }
    }

    @Test
    fun testOnSubmit_AfterError() = runTest {
        coEvery { loginInteractor.login(EMAIL, PASSWORD) } throws NoConnectionException()

        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.onSubmit()

        // Retrying clears the previous error while the new request runs
        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            LoginUiState(email = EMAIL, password = PASSWORD, isSigningIn = true, error = null)
        )
    }

    @Test
    fun testOnEmailChange_ClearsError() = runTest {
        coEvery { loginInteractor.login(EMAIL, PASSWORD) } throws InvalidCredentialsException()

        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.onEmailChange(OTHER_EMAIL)

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            LoginUiState(email = OTHER_EMAIL, password = PASSWORD, isSigningIn = false, error = null)
        )
    }

    @Test
    fun testOnPasswordChange_ClearsError() = runTest {
        coEvery { loginInteractor.login(EMAIL, PASSWORD) } throws InvalidCredentialsException()

        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.onPasswordChange(OTHER_PASSWORD)

        Truth.assertThat(viewModel.uiState.value).isEqualTo(
            LoginUiState(email = EMAIL, password = OTHER_PASSWORD, isSigningIn = false, error = null)
        )
    }

    @Test
    fun testOnEmailChange_SavesEmail() {
        viewModel.onEmailChange(OTHER_EMAIL)

        // The email survives process death; the password is never saved
        Truth.assertThat(savedStateHandle.get<String>(KEY_EMAIL)).isEqualTo(OTHER_EMAIL)
        Truth.assertThat(savedStateHandle.keys()).containsExactly(KEY_EMAIL)
    }

    @Test
    fun testInit_RestoresEmail() {
        val restoredViewModel = LoginViewModel(
            SavedStateHandle(mapOf(KEY_EMAIL to OTHER_EMAIL)), loginInteractor
        )

        Truth.assertThat(restoredViewModel.uiState.value).isEqualTo(LoginUiState(email = OTHER_EMAIL))
    }

    private companion object {
        const val KEY_EMAIL = "email"
        const val EMAIL = "tech1@example.com"
        const val PASSWORD = "password"
        const val OTHER_EMAIL = "tech2@example.com"
        const val OTHER_PASSWORD = "secret"
        val USER = User("u1", EMAIL, "Tech One", UserRole.Technician)
    }
}
