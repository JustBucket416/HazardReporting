package com.example.hazardreporting.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hazardreporting.domain.exception.UnauthorizedException
import java.io.IOException

/**
 * Background work that pushes the device's pending changes. Background work only pushes; pulling happens
 * on sign-in and pull-to-refresh (ADR-0002). Created by [SyncWorkerFactory], so it can take injected dependencies
 *
 * @param context the application context
 * @param params parameters of this run
 * @property synchronizer does the push
 *
 * @author Roman Pliskin on 29.10.2026
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val synchronizer: IncidentSynchronizer,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        synchronizer.push()
        Result.success()
    } catch (e: UnauthorizedException) {
        // Retrying can't help until the technician signs in again; the list shows why (ADR-0007).
        // Caught first, since it's an IOException too
        Result.failure()
    } catch (e: IOException) {
        // No connection, or one lost while a response was being read: tried again with exponential backoff
        // once the device is online
        Result.retry()
    }
}
