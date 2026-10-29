package com.example.hazardreporting.di

import android.content.Context
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import com.example.hazardreporting.data.sync.SyncWorkerFactory
import dagger.Binds
import dagger.Module
import dagger.Provides

/**
 * Provides WorkManager and the [WorkerFactory] it is configured with
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Module
interface WorkModule {

    /**
     * Binds [SyncWorkerFactory] as the app's [WorkerFactory]
     */
    @Binds
    fun bindWorkerFactory(factory: SyncWorkerFactory): WorkerFactory

    /**
     * Provides the bindings that need code
     */
    companion object {

        /**
         * Provides WorkManager. It initialises itself on first use with the configuration the application
         * provides, so it must not be requested while the graph is being built
         */
        @Provides
        fun provideWorkManager(context: Context): WorkManager = WorkManager.getInstance(context)
    }
}
