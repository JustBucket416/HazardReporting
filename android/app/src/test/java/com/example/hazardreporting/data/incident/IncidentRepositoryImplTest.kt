package com.example.hazardreporting.data.incident

import app.cash.turbine.test
import com.example.hazardreporting.data.incident.dto.IncidentDto
import com.example.hazardreporting.data.incident.dto.IncidentSummaryDto
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.data.incident.entity.IncidentPhotoEntity
import com.example.hazardreporting.data.incident.entity.IncidentWithPhotos
import com.example.hazardreporting.data.sync.SyncLock
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.model.Incident
import com.example.hazardreporting.domain.model.IncidentDraft
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.SyncState
import com.example.hazardreporting.utils.CoroutineDispatchers
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
    private val photoLocalDataSource = mockk<PhotoLocalDataSource>()
    private val converter = mockk<IncidentConverter>()
    private val clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
    private val syncLock = SyncLock()
    private val dispatchers = mockk<CoroutineDispatchers>()

    private lateinit var repository: IncidentRepositoryImpl

    @Before
    fun setUp() {
        every { dispatchers.io } returns UnconfinedTestDispatcher()
        coEvery { localDataSource.inTransaction<Unit>(any()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }

        repository = IncidentRepositoryImpl(
            remoteDataSource,
            localDataSource,
            photoLocalDataSource,
            converter,
            clock,
            syncLock,
            dispatchers,
        )
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
    fun testPullSummaries_WaitsForSyncLock() = runTest {
        val push = CompletableDeferred<Unit>()

        coEvery { remoteDataSource.fetchSummaries() } returns emptyList()
        coEvery { localDataSource.getIncidents() } returns emptyList()
        stubWrites()

        val pushing = launch(UnconfinedTestDispatcher(testScheduler)) { syncLock.withLock { push.await() } }
        val pulling = launch(UnconfinedTestDispatcher(testScheduler)) { repository.pullSummaries() }

        // Summaries downloaded during a push could be merged into rows the push is writing
        coVerify(exactly = 0) { remoteDataSource.fetchSummaries() }

        push.complete(Unit)
        pushing.join()
        pulling.join()

        coVerify(exactly = 1) { remoteDataSource.fetchSummaries() }
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

    @Test
    fun testObserveIncident() = runTest {
        val withPhotos = IncidentWithPhotos(SYNCED_ROW, photos = emptyList())
        val incident = mockk<Incident>()

        every { localDataSource.observeIncident(LOCAL_ID) } returns flowOf(null, withPhotos)
        every { converter.convert(withPhotos) } returns incident

        repository.observeIncident(LOCAL_ID).test {
            // Nothing is emitted while the incident isn't stored
            Truth.assertThat(awaitItem()).isSameInstanceAs(incident)
            awaitComplete()
        }

        verifySequence {
            localDataSource.observeIncident(LOCAL_ID)
            converter.convert(withPhotos)
        }
    }

    @Test
    fun testLoadDetails_NotForcedDetailsLoaded() = runTest {
        coEvery { localDataSource.getIncident(LOCAL_ID) } returns SYNCED_ROW.copy(detailsLoaded = true)

        repository.loadDetails(LOCAL_ID, force = false)

        // Downloaded details are reused: no network call
        coVerifySequence { localDataSource.getIncident(LOCAL_ID) }
    }

    @Test
    fun testLoadDetails_NotForcedDetailsNotLoaded() = runTest {
        verifyDownloadsDetails(stored = SYNCED_ROW, force = false)
    }

    @Test
    fun testLoadDetails_ForcedDetailsLoaded() = runTest {
        verifyDownloadsDetails(stored = SYNCED_ROW.copy(detailsLoaded = true), force = true)
    }

    @Test
    fun testLoadDetails_ForcedDetailsNotLoaded() = runTest {
        verifyDownloadsDetails(stored = SYNCED_ROW, force = true)
    }

    @Test
    fun testLoadDetails_UnknownToServer() = runTest {
        val unsent = SYNCED_ROW.copy(serverId = null, detailsLoaded = true, syncState = SyncState.PendingCreate)

        coEvery { localDataSource.getIncident(LOCAL_ID) } returns unsent

        repository.loadDetails(LOCAL_ID, force = true)

        // Its details are all on the device; the server has nothing to add
        coVerifySequence { localDataSource.getIncident(LOCAL_ID) }
    }

    @Test
    fun testLoadDetails_NotStored() = runTest {
        coEvery { localDataSource.getIncident(LOCAL_ID) } returns null

        repository.loadDetails(LOCAL_ID, force = true)

        coVerifySequence { localDataSource.getIncident(LOCAL_ID) }
    }

    @Test
    fun testLoadDetails_PendingChange() = runTest {
        val pending = SYNCED_ROW.copy(detailsLoaded = true, syncState = SyncState.PendingUpdate)
        val dto = mockk<IncidentDto>()
        val updated = mockk<IncidentEntity>()

        coEvery { localDataSource.getIncident(LOCAL_ID) } returns pending
        coEvery { remoteDataSource.fetchIncident("inc-1") } returns dto
        every { converter.withStatus(pending, dto) } returns updated
        coJustRun { localDataSource.updateIncident(updated) }

        repository.loadDetails(LOCAL_ID, force = true)

        // The pending report fields and photos stay; only the status and update time are taken
        coVerifySequence {
            localDataSource.getIncident(LOCAL_ID)
            remoteDataSource.fetchIncident("inc-1")
            localDataSource.inTransaction<Unit>(any())
            localDataSource.getIncident(LOCAL_ID)
            converter.withStatus(pending, dto)
            localDataSource.updateIncident(updated)
        }
    }

    @Test
    fun testLoadDetails_NoConnection() {
        coEvery { localDataSource.getIncident(LOCAL_ID) } returns SYNCED_ROW
        coEvery { remoteDataSource.fetchIncident("inc-1") } throws NoConnectionException()

        Assert.assertThrows(NoConnectionException::class.java) {
            runTest { repository.loadDetails(LOCAL_ID, force = false) }
        }

        // Nothing is written, so the details stay "not downloaded"
        coVerifySequence {
            localDataSource.getIncident(LOCAL_ID)
            remoteDataSource.fetchIncident("inc-1")
        }
    }

    @Test
    fun testReport() = runTest {
        val draft = mockk<IncidentDraft>()
        val photos = listOf(Photo.Local("/files/photos/a.jpg"))
        val entity = mockk<IncidentEntity>()
        val photoEntities = listOf(mockk<IncidentPhotoEntity>())

        coEvery { localDataSource.inTransaction<Long>(any()) } coAnswers { firstArg<suspend () -> Long>().invoke() }
        coEvery { localDataSource.existsCreatedAt(NOW) } returns false
        every { draft.photos } returns photos
        every { converter.convertNew(draft, NOW) } returns entity
        coEvery { localDataSource.insertIncident(entity) } returns 9L
        every { converter.convertPhotos(9L, photos) } returns photoEntities
        coJustRun { localDataSource.insertPhotos(photoEntities) }

        val localId = repository.report(draft)

        // createdAt is the device's clock at first save; the row and its photos land in one transaction
        coVerifySequence {
            localDataSource.inTransaction<Long>(any())
            localDataSource.existsCreatedAt(NOW)
            converter.convertNew(draft, NOW)
            localDataSource.insertIncident(entity)
            converter.convertPhotos(9L, photos)
            localDataSource.insertPhotos(photoEntities)
        }

        Truth.assertThat(localId).isEqualTo(9L)
    }

    @Test
    fun testReport_CreatedAtTaken() = runTest {
        val draft = mockk<IncidentDraft>()
        val entity = mockk<IncidentEntity>()

        coEvery { localDataSource.inTransaction<Long>(any()) } coAnswers { firstArg<suspend () -> Long>().invoke() }
        coEvery { localDataSource.existsCreatedAt(NOW) } returns true
        coEvery { localDataSource.existsCreatedAt(NOW + 1) } returns true
        coEvery { localDataSource.existsCreatedAt(NOW + 2) } returns false
        every { draft.photos } returns emptyList()
        every { converter.convertNew(draft, NOW + 2) } returns entity
        coEvery { localDataSource.insertIncident(entity) } returns 9L
        every { converter.convertPhotos(9L, emptyList()) } returns emptyList()
        coJustRun { localDataSource.insertPhotos(emptyList()) }

        repository.report(draft)

        // Two reports on the device never share a createdAt, so they can never become one server incident
        coVerifySequence {
            localDataSource.inTransaction<Long>(any())
            localDataSource.existsCreatedAt(NOW)
            localDataSource.existsCreatedAt(NOW + 1)
            localDataSource.existsCreatedAt(NOW + 2)
            converter.convertNew(draft, NOW + 2)
            localDataSource.insertIncident(entity)
            converter.convertPhotos(9L, emptyList())
            localDataSource.insertPhotos(emptyList())
        }
    }

    @Test
    fun testCreatePhotoFile() = runTest {
        every { photoLocalDataSource.createPhotoFile() } returns "/files/photos/a.jpg"

        Truth.assertThat(repository.createPhotoFile()).isEqualTo(Photo.Local("/files/photos/a.jpg"))

        verifySequence { photoLocalDataSource.createPhotoFile() }
    }

    @Test
    fun testImportPhoto() = runTest {
        every { photoLocalDataSource.importPhoto("content://media/1") } returns "/files/photos/b.png"

        Truth.assertThat(repository.importPhoto("content://media/1")).isEqualTo(Photo.Local("/files/photos/b.png"))

        verifySequence { photoLocalDataSource.importPhoto("content://media/1") }
    }

    @Test
    fun testDeletePhoto() = runTest {
        justRun { photoLocalDataSource.deletePhoto("/files/photos/a.jpg") }

        repository.deletePhoto(Photo.Local("/files/photos/a.jpg"))

        verifySequence { photoLocalDataSource.deletePhoto("/files/photos/a.jpg") }
    }

    private suspend fun verifyDownloadsDetails(stored: IncidentEntity, force: Boolean) {
        val dto = mockk<IncidentDto>()
        val updated = mockk<IncidentEntity>()
        val photos = listOf(mockk<IncidentPhotoEntity>())

        coEvery { localDataSource.getIncident(LOCAL_ID) } returns stored
        coEvery { remoteDataSource.fetchIncident("inc-1") } returns dto
        every { converter.withDetails(stored, dto) } returns updated
        every { converter.convertPhotos(LOCAL_ID, dto) } returns photos
        coJustRun { localDataSource.updateIncident(updated) }
        coJustRun { localDataSource.replacePhotos(LOCAL_ID, photos) }

        repository.loadDetails(LOCAL_ID, force)

        coVerifySequence {
            localDataSource.getIncident(LOCAL_ID)
            remoteDataSource.fetchIncident("inc-1")
            localDataSource.inTransaction<Unit>(any())
            localDataSource.getIncident(LOCAL_ID)
            converter.withDetails(stored, dto)
            localDataSource.updateIncident(updated)
            converter.convertPhotos(LOCAL_ID, dto)
            localDataSource.replacePhotos(LOCAL_ID, photos)
        }
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
        const val LOCAL_ID = 1L
        const val NOW = 1_793_100_000_000L
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
