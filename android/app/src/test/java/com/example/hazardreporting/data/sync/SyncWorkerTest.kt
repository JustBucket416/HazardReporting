package com.example.hazardreporting.data.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerifySequence
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class SyncWorkerTest {

    private val synchronizer = mockk<IncidentSynchronizer>()

    private val worker = SyncWorker(mockk<Context>(), mockk<WorkerParameters>(), synchronizer)

    @Test
    fun testDoWork_Success() = runTest {
        coJustRun { synchronizer.push() }

        val result = worker.doWork()

        coVerifySequence { synchronizer.push() }

        Truth.assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun testDoWork_NoConnection() = runTest {
        coEvery { synchronizer.push() } throws NoConnectionException()

        // Tried again with backoff once the device is online
        Truth.assertThat(worker.doWork()).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun testDoWork_ResponseNotRead() = runTest {
        coEvery { synchronizer.push() } throws IOException()

        // A connection lost while a response was read is retried too
        Truth.assertThat(worker.doWork()).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun testDoWork_Unauthorized() = runTest {
        coEvery { synchronizer.push() } throws UnauthorizedException()

        // Retrying can't help until the technician signs in again
        Truth.assertThat(worker.doWork()).isEqualTo(ListenableWorker.Result.failure())
    }
}
