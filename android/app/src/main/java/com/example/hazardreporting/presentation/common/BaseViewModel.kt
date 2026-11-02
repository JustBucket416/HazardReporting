package com.example.hazardreporting.presentation.common

import androidx.lifecycle.ViewModel
import com.example.hazardreporting.utils.Logger
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * Base class of the app's ViewModels. It owns the shared [exceptionHandler], so every ViewModel
 * launches with `viewModelScope.launch(exceptionHandler)`, on the main thread (ADR-0009)
 *
 * @author Roman Pliskin on 29.10.2026
 */
abstract class BaseViewModel : ViewModel() {

    /**
     * Handles exceptions the ViewModel doesn't catch itself: logs them and then calls [onUnexpected]
     */
    protected val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Logger.error(javaClass.simpleName, UNEXPECTED_EXCEPTION_MESSAGE, throwable)
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
