package com.example.hazardreporting.data.sync

import com.example.hazardreporting.domain.exception.NoConnectionException
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncRepositoryImplTest {

    private val synchronizer = mockk<IncidentSynchronizer>()
    private val syncLock = SyncLock()

    private val repository = SyncRepositoryImpl(synchronizer, syncLock)

    @Test
    fun testPush() = runTest {
        coJustRun { synchronizer.push() }

        repository.push()

        coVerifySequence { synchronizer.push() }
    }

    @Test
    fun testCountPendingChanges() = runTest {
        coEvery { synchronizer.countUnsentChanges() } returns 3

        Truth.assertThat(repository.countPendingChanges()).isEqualTo(3)
    }

    @Test
    fun testWithSyncPaused() = runTest {
        val push = CompletableDeferred<Unit>()
        var hasRun = false

        val pushing = launch(UnconfinedTestDispatcher(testScheduler)) { syncLock.withLock { push.await() } }
        val wiping = launch(UnconfinedTestDispatcher(testScheduler)) { repository.withSyncPaused { hasRun = true } }

        // Waits for the push in progress
        Truth.assertThat(hasRun).isFalse()

        push.complete(Unit)
        pushing.join()
        wiping.join()

        Truth.assertThat(hasRun).isTrue()
    }

    @Test
    fun testPush_NoConnection() {
        coEvery { synchronizer.push() } throws NoConnectionException()

        Assert.assertThrows(NoConnectionException::class.java) {
            runTest { repository.push() }
        }
    }
}
