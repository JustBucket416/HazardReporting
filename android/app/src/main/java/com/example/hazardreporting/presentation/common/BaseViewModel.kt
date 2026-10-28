package com.example.hazardreporting.presentation.common

import androidx.lifecycle.ViewModel
import com.example.hazardreporting.utils.Logger
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * Base class of the app's ViewModels. It owns the shared [exceptionHandler], so every ViewModel
 * launches with `viewModelScope.launch(exceptionHandler + dispatchers.default)` (ADR-0009)
 *
 * @property logger used for logging exceptions nobody expected
 *
 * @author Roman Pliskin on 29.10.2026
 */
abstract class BaseViewModel(
    private val logger: Logger,
) : ViewModel() {

    /**
     * Handles exceptions the ViewModel doesn't catch itself: logs them and then calls [onUnexpected]
     */
    protected val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logger.error(javaClass.simpleName, UNEXPECTED_EXCEPTION_MESSAGE, throwable)
        onUnexpected(throwable)
    }

    /**
     * Called after an unexpected [throwable] is logged. Clears any loading state
     * and tells the user that something went wrong
     */
    protected abstract fun onUnexpected(throwable: Throwable)

    private companion object {
        const val UNEXPECTED_EXCEPTION_MESSAGE = "Unexpected exception"
    }
}
