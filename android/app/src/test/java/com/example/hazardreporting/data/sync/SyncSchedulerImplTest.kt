package com.example.hazardreporting.data.sync

import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifySequence
import java.time.Duration
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class SyncSchedulerImplTest {

    private val workManager = mockk<WorkManager>()

    private val scheduler = SyncSchedulerImpl(workManager)

    @Test
    fun testSchedulePush() {
        val request = slot<OneTimeWorkRequest>()

        every { workManager.enqueueUniqueWork("sync-once", ExistingWorkPolicy.APPEND_OR_REPLACE, capture(request)) } returns
            mockk<Operation>()

        scheduler.schedulePush()

        verifySequence {
            workManager.enqueueUniqueWork("sync-once", ExistingWorkPolicy.APPEND_OR_REPLACE, any<OneTimeWorkRequest>())
        }

        // Appended after a push that is waiting or running, which may have missed the change that scheduled this one.
        // A SyncWorker run once the device is online, backing off exponentially
        val workSpec = request.captured.workSpec
        Truth.assertThat(workSpec.workerClassName).isEqualTo(SyncWorker::class.java.name)
        Truth.assertThat(workSpec.constraints.requiredNetworkType).isEqualTo(NetworkType.CONNECTED)
        Truth.assertThat(workSpec.backoffPolicy).isEqualTo(BackoffPolicy.EXPONENTIAL)
        Truth.assertThat(workSpec.backoffDelayDuration).isEqualTo(Duration.ofSeconds(30).toMillis())
        Truth.assertThat(workSpec.isPeriodic).isFalse()
    }

    @Test
    fun testCancelScheduledPushes() {
        every { workManager.cancelAllWork() } returns mockk<Operation>()

        scheduler.cancelScheduledPushes()

        verifySequence { workManager.cancelAllWork() }
    }

    @Test
    fun testSchedulePeriodicPush() {
        val request = slot<PeriodicWorkRequest>()

        every {
            workManager.enqueueUniquePeriodicWork("sync-periodic", ExistingPeriodicWorkPolicy.KEEP, capture(request))
        } returns mockk<Operation>()

        scheduler.schedulePeriodicPush()

        verifySequence {
            workManager.enqueueUniquePeriodicWork("sync-periodic", ExistingPeriodicWorkPolicy.KEEP, any())
        }

        val workSpec = request.captured.workSpec
        Truth.assertThat(workSpec.workerClassName).isEqualTo(SyncWorker::class.java.name)
        Truth.assertThat(workSpec.intervalDuration).isEqualTo(Duration.ofMinutes(15).toMillis())
        Truth.assertThat(workSpec.constraints.requiredNetworkType).isEqualTo(NetworkType.CONNECTED)
        Truth.assertThat(workSpec.backoffPolicy).isEqualTo(BackoffPolicy.EXPONENTIAL)
    }
}
