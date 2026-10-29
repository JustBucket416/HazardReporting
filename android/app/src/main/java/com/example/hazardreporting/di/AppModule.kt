package com.example.hazardreporting.di

import com.example.hazardreporting.utils.CoroutineDispatchers
import com.example.hazardreporting.utils.Logger
import dagger.Module
import dagger.Provides
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Provides application-wide utilities: the [ApplicationScope] coroutine scope and the [Clock]
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Module
object AppModule {

    private const val TAG = "ApplicationScope"

    /**
     * Provides the [ApplicationScope] coroutine scope. A failing child doesn't cancel its siblings,
     * and an uncaught exception is logged instead of crashing the app
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(dispatchers: CoroutineDispatchers): CoroutineScope {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Logger.error(TAG, "Uncaught exception in the application scope", throwable)
        }
        return CoroutineScope(SupervisorJob() + dispatchers.default + exceptionHandler)
    }

    /**
     * Provides the device's wall clock, e.g. for stamping a new report's createdAt
     */
    @Provides
    fun provideClock(): Clock = Clock.systemUTC()
}
