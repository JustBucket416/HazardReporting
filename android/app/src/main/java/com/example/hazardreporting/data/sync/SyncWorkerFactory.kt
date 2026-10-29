package com.example.hazardreporting.data.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject
import javax.inject.Provider

/**
 * The app's [WorkerFactory], which WorkManager is configured with instead of its default one, so workers
 * get their dependencies from Dagger
 *
 * @property synchronizer provides the synchronizer each [SyncWorker] pushes with
 *
 * @author Roman Pliskin on 29.10.2026
 */
class SyncWorkerFactory @Inject constructor(
    private val synchronizer: Provider<IncidentSynchronizer>,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        SyncWorker::class.java.name -> SyncWorker(appContext, workerParameters, synchronizer.get())
        // Unknown to the app, e.g. work persisted by an older version: WorkManager's default factory tries instead
        else -> null
    }
}
