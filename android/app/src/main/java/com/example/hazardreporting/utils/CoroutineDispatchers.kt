package com.example.hazardreporting.utils

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Wrapper class for coroutine dispatchers.
 * Injected wherever code picks a thread, so tests can replace the dispatchers with test ones
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Singleton
class CoroutineDispatchers @Inject constructor() {

    /**
     * Dispatcher for blocking IO: network, database and file access
     */
    val io: CoroutineDispatcher = Dispatchers.IO

    /**
     * Dispatcher for CPU work; ViewModels launch their coroutines on it
     */
    val default: CoroutineDispatcher = Dispatchers.Default

    /**
     * Dispatcher bound to the UI thread
     */
    val main: CoroutineDispatcher = Dispatchers.Main
}
