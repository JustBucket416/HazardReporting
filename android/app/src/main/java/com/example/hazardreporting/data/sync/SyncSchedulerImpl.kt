package com.example.hazardreporting.data.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.hazardreporting.domain.repository.SyncScheduler
import java.time.Duration
import javax.inject.Inject

/**
 * Implementation of [SyncScheduler] with WorkManager (spec § 3.4). Both kinds of work run [SyncWorker] only while
 * the device is online, and back off exponentially when it asks for a retry:
 * - [SYNC_ONCE], unique one-shot work that is kept rather than replaced while it is scheduled or running
 * - [SYNC_PERIODIC], unique work every 15 minutes
 *
 * @property workManager schedules the work
 *
 * @author Roman Pliskin on 29.10.2026
 */
class SyncSchedulerImpl @Inject constructor(
    private val workManager: WorkManager,
) : SyncScheduler {

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun schedulePush() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY)
            .build()
        // A push that is already running sends changes saved while it runs, too
        workManager.enqueueUniqueWork(SYNC_ONCE, ExistingWorkPolicy.KEEP, request)
    }

    override fun schedulePeriodicPush() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_INTERVAL)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY)
            .build()
        workManager.enqueueUniquePeriodicWork(SYNC_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * Names and timing of the sync work
     */
    companion object {

        /**
         * Name of the unique one-shot push
         */
        const val SYNC_ONCE = "sync-once"

        /**
         * Name of the unique periodic push
         */
        const val SYNC_PERIODIC = "sync-periodic"

        private val BACKOFF_DELAY: Duration = Duration.ofSeconds(30)
        private val PERIODIC_INTERVAL: Duration = Duration.ofMinutes(15)
    }
}
