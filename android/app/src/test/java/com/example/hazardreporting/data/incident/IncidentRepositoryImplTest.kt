package com.example.hazardreporting.data.incident

import app.cash.turbine.test
import com.example.hazardreporting.data.incident.dto.IncidentSummaryDto
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.SyncState
import com.example.hazardreporting.utils.CoroutineDispatchers
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IncidentRepositoryImplTest {

    private val remoteDataSource = mockk<IncidentRemoteDataSource>()
    private val localDataSource = mockk<IncidentLocalDataSource>()
    private val converter = mockk<IncidentConverter>()
    private val dispatchers = mockk<CoroutineDispatchers>()

    private lateinit var repository: IncidentRepositoryImpl

    @Before
    fun setUp() {
        every { dispatchers.io } returns UnconfinedTestDispatcher()
        coEvery { localDataSource.inTransaction<Unit>(any()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        repository = IncidentRepositoryImpl(remoteDataSource, localDataSource, converter, dispatchers)
    }

    @Test
    fun testObserveSummaries() = runTest {
        val summary = mockk<IncidentSummary>()

        every { localDataSource.observeIncidents() } returns flowOf(listOf(SYNCED_ROW))
        every { converter.convertSummary(SYNCED_ROW) } returns summary

        repository.observeSummaries().test {
            Truth.assertThat(awaitItem()).containsExactly(summary)
            awaitComplete()
        }

        verifySequence {
            localDataSource.observeIncidents()
            converter.convertSummary(SYNCED_ROW)
        }
    }

    @Test
    fun testPullSummaries_NewIncident() = runTest {
        val inserted = mockk<IncidentEntity>()

        coEvery { remoteDataSource.fetchSummaries() } returns listOf(SUMMARY)
        coEvery { localDataSource.getIncidents() } returns emptyList()
        every { converter.convert(SUMMARY) } returns inserted
        stubWrites(inserted = listOf(inserted))

        repository.pullSummaries()

        coVerifySequence {
            remoteDataSource.fetchSummaries()
            localDataSource.inTransaction<Unit>(any())
            localDataSource.getIncidents()
            converter.convert(SUMMARY)
            localDataSource.insertIncidents(listOf(inserted))
            localDataSource.updateIncidents(emptyList())
            localDataSource.deleteIncidents(emptyList())
        }
    }

    @Test
    fun testPullSummaries_SyncedIncident() = runTest {
        val updated = mockk<IncidentEntity>()

        coEvery { remoteDataSource.fetchSummaries() } returns listOf(SUMMARY)
        coEvery { localDataSource.getIncidents() } returns listOf(SYNCED_ROW)
        every { converter.withSummary(SYNCED_ROW, SUMMARY) } returns updated
        stubWrites(updated = listOf(updated))

        repository.pullSummaries()

        coVerifySequence {
            remoteDataSource.fetchSummaries()
            localDataSource.inTransaction<Unit>(any())
            localDataSource.getIncidents()
            converter.withSummary(SYNCED_ROW, SUMMARY)
            localDataSource.insertIncidents(emptyList())
            localDataSource.updateIncidents(listOf(updated))
            localDataSource.deleteIncidents(emptyList())
        }
    }

    @Test
    fun testPullSummaries_PendingUpdate() = runTest {
        val pending = SYNCED_ROW.copy(syncState = SyncState.PendingUpdate)
        val updated = mockk<IncidentEntity>()

        coEvery { remoteDataSource.fetchSummaries() } returns listOf(SUMMARY)
        coEvery { localDataSource.getIncidents() } returns listOf(pending)
        every { converter.withStatus(pending, SUMMARY) } returns updated
        stubWrites(updated = listOf(updated))

        repository.pullSummaries()

        // A row with a pending change takes only the status and update time, never the report fields
        coVerifySequence {
            remoteDataSource.fetchSummaries()
            localDataSource.inTransaction<Unit>(any())
            localDataSource.getIncidents()
            converter.withStatus(pending, SUMMARY)
            localDataSource.insertIncidents(emptyList())
            localDataSource.updateIncidents(listOf(updated))
            localDataSource.deleteIncidents(emptyList())
        }
    }

    @Test
    fun testPullSummaries_PendingWithdraw() = runTest {
        val pending = SYNCED_ROW.copy(syncState = SyncState.PendingWithdraw)
        val updated = mockk<IncidentEntity>()

        coEvery { remoteDataSource.fetchSummaries() } returns listOf(SUMMARY)
        coEvery { localDataSource.getIncidents() } returns listOf(pending)
        every { converter.withStatus(pending, SUMMARY) } returns updated
        stubWrites(updated = listOf(updated))

        repository.pullSummaries()

        coVerifySequence {
            remoteDataSource.fetchSummaries()
            localDataSource.inTransaction<Unit>(any())
            localDataSource.getIncidents()
            converter.withStatus(pending, SUMMARY)
            localDataSource.insertIncidents(emptyList())
            localDataSource.updateIncidents(listOf(updated))
            localDataSource.deleteIncidents(emptyList())
        }
    }

    @Test
    fun testPullSummaries_UnidentifiedPendingCreate() = runTest {
        // The POST reached the server, but its response didn't reach the device
        val unidentified = SYNCED_ROW.copy(localId = 2, serverId = null, syncState = SyncState.PendingCreate)
        val updated = mockk<IncidentEntity>()

        coEvery { remoteDataSource.fetchSummaries() } returns listOf(SUMMARY)
        coEvery { localDataSource.getIncidents() } returns listOf(unidentified)
        every { converter.withStatus(unidentified, SUMMARY) } returns updated
        stubWrites(updated = listOf(updated))

        repository.pullSummaries()

        // Recognised by its createdAt instead of being stored twice
        coVerifySequence {
            remoteDataSource.fetchSummaries()
            localDataSource.inTransaction<Unit>(any())
            localDataSource.getIncidents()
            converter.withStatus(unidentified, SUMMARY)
            localDataSource.insertIncidents(emptyList())
            localDataSource.updateIncidents(listOf(updated))
            localDataSource.deleteIncidents(emptyList())
        }
    }

    @Test
    fun testPullSummaries_IncidentsGoneFromServer() = runTest {
        val syncedGone = SYNCED_ROW.copy(localId = 3, serverId = "inc-gone")
        val pendingGone = SYNCED_ROW.copy(localId = 4, serverId = "inc-pending", syncState = SyncState.PendingUpdate)
        val unsent = SYNCED_ROW.copy(localId = 5, serverId = null, createdAt = 1L, syncState = SyncState.PendingCreate)

        coEvery { remoteDataSource.fetchSummaries() } returns emptyList()
        coEvery { localDataSource.getIncidents() } returns listOf(syncedGone, pendingGone, unsent)
        stubWrites(deleted = listOf(3L))

        repository.pullSummaries()

        // Only the synced row the server stopped listing goes; pending changes are never dropped by a pull
        coVerifySequence {
            remoteDataSource.fetchSummaries()
            localDataSource.inTransaction<Unit>(any())
            localDataSource.getIncidents()
            localDataSource.insertIncidents(emptyList())
            localDataSource.updateIncidents(emptyList())
            localDataSource.deleteIncidents(listOf(3L))
        }
    }

    @Test
    fun testPullSummaries_NoConnection() {
        coEvery { remoteDataSource.fetchSummaries() } throws NoConnectionException()

        Assert.assertThrows(NoConnectionException::class.java) {
            runTest { repository.pullSummaries() }
        }

        // The stored incidents stay as they were
        coVerifySequence { remoteDataSource.fetchSummaries() }
    }

    private fun stubWrites(
        inserted: List<IncidentEntity> = emptyList(),
        updated: List<IncidentEntity> = emptyList(),
        deleted: List<Long> = emptyList(),
    ) {
        coJustRun { localDataSource.insertIncidents(inserted) }
        coJustRun { localDataSource.updateIncidents(updated) }
        coJustRun { localDataSource.deleteIncidents(deleted) }
    }

    private companion object {
        const val CREATED_AT = 1_793_015_100_000L

        val SUMMARY = IncidentSummaryDto(
            id = "inc-1",
            title = "Conveyor belt fraying",
            categoryId = "cat-equipment",
            severityId = "sev-high",
            siteId = "site-north",
            status = "acknowledged",
            createdAt = CREATED_AT,
            updatedAt = 1_793_020_000_000L,
        )

        val SYNCED_ROW = IncidentEntity(
            localId = 1,
            serverId = "inc-1",
            title = "Conveyor belt fraying",
            categoryId = "cat-equipment",
            severityId = "sev-high",
            siteId = "site-north",
            status = "open",
            createdAt = CREATED_AT,
            updatedAt = 1_793_015_160_000L,
            description = null,
            locationDescription = null,
            positionLat = null,
            positionLng = null,
            positionAccuracyM = null,
            priority = null,
            detailsLoaded = false,
            syncState = SyncState.Synced,
            localVersion = 0,
            createAttempted = true,
            statusChangeNoticePending = false,
        )
    }
}
