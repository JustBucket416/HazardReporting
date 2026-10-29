package com.example.hazardreporting.presentation.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.hazardreporting.domain.exception.InvalidCredentialsException
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.NotTechnicianException
import com.example.hazardreporting.domain.interactor.LoginInteractor
import com.example.hazardreporting.presentation.common.BaseViewModel
import com.example.hazardreporting.utils.CoroutineDispatchers
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages the sign-in screen's state. The typed email survives process death in [savedStateHandle];
 * the password deliberately doesn't
 *
 * @property savedStateHandle keeps the typed email
 * @property loginInteractor used for signing in
 * @property dispatchers used for launching off the main thread
 *
 * @author Roman Pliskin on 29.10.2026
 */
class LoginViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val loginInteractor: LoginInteractor,
    private val dispatchers: CoroutineDispatchers,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState(email = savedStateHandle[KEY_EMAIL] ?: ""))
    private val _events = Channel<LoginEvent>(Channel.BUFFERED)

    /**
     * State of the sign-in screen
     */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * One-off events of the sign-in screen
     */
    val events: Flow<LoginEvent> = _events.receiveAsFlow()

    /**
     * Updates the email field and clears the previous error
     */
    fun onEmailChange(email: String) {
        savedStateHandle[KEY_EMAIL] = email
        _uiState.update { it.copy(email = email, error = null) }
    }

    /**
     * Updates the password field and clears the previous error
     */
    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    /**
     * Signs in with the entered email and password. Ignored while a sign-in is already in flight
     */
    fun onSubmit() {
        val submittedState = _uiState.getAndUpdate { it.copy(isSigningIn = true, error = null) }
        if (submittedState.isSigningIn) return

        viewModelScope.launch(exceptionHandler + dispatchers.default) {
            val error = try {
                loginInteractor.login(submittedState.email, submittedState.password)
                null
            } catch (e: InvalidCredentialsException) {
                LoginError.InvalidCredentials
            } catch (e: NoConnectionException) {
                LoginError.NoConnection
            } catch (e: NotTechnicianException) {
                LoginError.NotTechnician
            }

            if (error == null) {
                // Stays in the signing-in state while the screen navigates away
                _events.send(LoginEvent.LoggedIn)
            } else {
                _uiState.update { it.copy(isSigningIn = false, error = error) }
            }
        }
    }

    override fun onUnexpected(throwable: Throwable) {
        _uiState.update { it.copy(isSigningIn = false) }
        _events.trySend(LoginEvent.UnexpectedError)
    }

    private companion object {
        const val KEY_EMAIL = "email"
    }
}
