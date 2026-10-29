package com.example.hazardreporting.data.sync

import com.example.hazardreporting.domain.exception.NoConnectionException
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class SyncRepositoryImplTest {

    private val synchronizer = mockk<IncidentSynchronizer>()

    private val repository = SyncRepositoryImpl(synchronizer)

    @Test
    fun testPush() = runTest {
        coJustRun { synchronizer.push() }

        repository.push()

        coVerifySequence { synchronizer.push() }
    }

    @Test
    fun testPush_NoConnection() {
        coEvery { synchronizer.push() } throws NoConnectionException()

        Assert.assertThrows(NoConnectionException::class.java) {
            runTest { repository.push() }
        }
    }
}
