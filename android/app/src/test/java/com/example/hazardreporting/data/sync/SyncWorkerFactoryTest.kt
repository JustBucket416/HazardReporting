package com.example.hazardreporting.data.sync

import android.content.Context
import androidx.work.WorkerParameters
import com.google.common.truth.Truth
import io.mockk.mockk
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class SyncWorkerFactoryTest {

    private val synchronizer = mockk<IncidentSynchronizer>()

    private val factory = SyncWorkerFactory { synchronizer }

    @Test
    fun testCreateWorker_SyncWorker() {
        val worker = factory.createWorker(mockk<Context>(), SyncWorker::class.java.name, mockk<WorkerParameters>())

        Truth.assertThat(worker).isInstanceOf(SyncWorker::class.java)
    }

    @Test
    fun testCreateWorker_UnknownWorker() {
        val worker = factory.createWorker(mockk<Context>(), "com.example.OldWorker", mockk<WorkerParameters>())

        // Left to WorkManager's default factory
        Truth.assertThat(worker).isNull()
    }
}
