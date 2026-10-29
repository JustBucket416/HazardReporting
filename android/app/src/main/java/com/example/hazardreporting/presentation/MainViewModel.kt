package com.example.hazardreporting.presentation

import androidx.lifecycle.viewModelScope
import com.example.hazardreporting.domain.interactor.LoginInteractor
import com.example.hazardreporting.presentation.common.BaseViewModel
import com.example.hazardreporting.presentation.navigation.Destination
import com.example.hazardreporting.presentation.navigation.IncidentList
import com.example.hazardreporting.presentation.navigation.Login
import com.example.hazardreporting.utils.CoroutineDispatchers
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Decides where the app starts, while [MainActivity] keeps the splash screen up
 *
 * @property loginInteractor used for finding out whether someone is signed in
 * @property dispatchers used for launching off the main thread
 *
 * @author Roman Pliskin on 29.10.2026
 */
class MainViewModel @Inject constructor(
    private val loginInteractor: LoginInteractor,
    private val dispatchers: CoroutineDispatchers,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())

    /**
     * State of the app's start
     */
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(exceptionHandler + dispatchers.default) {
            val startDestination = if (loginInteractor.currentUser() != null) IncidentList() else Login
            _uiState.update { it.copy(startDestination = startDestination) }
        }
    }

    override fun onUnexpected(throwable: Throwable) {
        // Starts signed out rather than keeping the splash screen up forever
        _uiState.update { it.copy(startDestination = Login) }
    }
}

/**
 * State of the app's start
 *
 * @property startDestination the first screen, or null while it's being decided
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class MainUiState(
    val startDestination: Destination? = null,
)
