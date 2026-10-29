package com.example.hazardreporting.data.sync

import com.example.hazardreporting.data.incident.ImageFormat
import com.example.hazardreporting.data.incident.IncidentConverter
import com.example.hazardreporting.data.incident.IncidentLocalDataSource
import com.example.hazardreporting.data.incident.IncidentRemoteDataSource
import com.example.hazardreporting.data.incident.PhotoLocalDataSource
import com.example.hazardreporting.data.incident.dto.CreateIncidentRequestDto
import com.example.hazardreporting.data.incident.dto.IncidentDto
import com.example.hazardreporting.data.incident.dto.PositionDto
import com.example.hazardreporting.data.incident.dto.UpdateIncidentRequestDto
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.data.incident.entity.IncidentPhotoEntity
import com.example.hazardreporting.data.incident.entity.IncidentWithPhotos
import com.example.hazardreporting.data.session.SessionHolder
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.example.hazardreporting.domain.model.SyncState
import com.example.hazardreporting.utils.CoroutineDispatchers
import com.example.hazardreporting.utils.Logger
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Tests every branch of the push (spec § 3.4). The stored rows live in memory behind the mocked
 * [IncidentLocalDataSource], which enforces the unique server id like Room does, and the mocked
 * [IncidentRemoteDataSource] answers like the server: it deduplicates POSTs by createdAt and refuses changes
 * to incidents that aren't open with 409. The converter is the real one, since what matters is what ends up
 * stored and sent
 *
 * @author Roman Pliskin on 29.10.2026
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IncidentSynchronizerTest {

    private val localDataSource = mockk<IncidentLocalDataSource>()
    private val remoteDataSource = mockk<IncidentRemoteDataSource>()
    private val photoLocalDataSource = mockk<PhotoLocalDataSource>()
    private val converter = IncidentConverter("http://10.0.2.2:3001/".toHttpUrl())
    private val syncLock = SyncLock()
    private val sessionHolder = mockk<SessionHolder>()
    private val clock = Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)
    private val logger = mockk<Logger>()
    private val dispatchers = mockk<CoroutineDispatchers>()

    /**
     * The stored rows, by local id
     */
    private val incidents = mutableMapOf<Long, IncidentEntity>()

    /**
     * The stored photo rows
     */
    private val photos = mutableListOf<IncidentPhotoEntity>()

    /**
     * Every row written with an update, in order
     */
    private val writes = mutableListOf<IncidentEntity>()

    /**
     * Paths of the photo files that exist
     */
    private val files = mutableSetOf<String>()

    /**
     * The server's incidents, by id
     */
    private val server = mutableMapOf<String, IncidentDto>()

    private lateinit var synchronizer: IncidentSynchronizer

    @Before
    fun setUp() {
        every { dispatchers.io } returns UnconfinedTestDispatcher()
        stubLocalDataSource()
        stubServer()
        every { photoLocalDataSource.formatOf(any()) } answers {
            if (firstArg<String>() in files) ImageFormat.Jpeg else null
        }
        every { photoLocalDataSource.deletePhoto(any()) } answers { files -= firstArg<String>() }
        justRun { photoLocalDataSource.deleteUnreferencedPhotos(any(), any()) }
        justRun { sessionHolder.markRejected() }
        justRun { logger.error(any(), any(), any()) }

        synchronizer = IncidentSynchronizer(
            localDataSource,
            remoteDataSource,
            photoLocalDataSource,
            converter,
            syncLock,
            sessionHolder,
            clock,
            logger,
            dispatchers,
        )
    }

    @Test
    fun testPush_NothingPending() = runTest {
        storeIncident(synced(1, "inc-1"))

        synchronizer.push()

        coVerify(exactly = 0) { remoteDataSource.createIncident(any()) }
        coVerify(exactly = 0) { remoteDataSource.updateIncident(any(), any()) }
        coVerify(exactly = 0) { remoteDataSource.withdrawIncident(any()) }
        coVerify(exactly = 0) { remoteDataSource.uploadPhoto(any(), any()) }
    }

    @Test
    fun testPush_DeletesOrphanedPhotosFirst() = runTest {
        storeIncident(report(1), PATH_A)
        storePhotoRow(incidentLocalId = 2, position = 0, localPath = null, remoteUrl = REMOTE_B)

        synchronizer.push()

        // Only files older than a week that no row refers to go, before anything is uploaded
        coVerifyOrder {
            photoLocalDataSource.deleteUnreferencedPhotos(setOf(PATH_A), NOW - Duration.ofDays(7).toMillis())
            remoteDataSource.uploadPhoto(File(PATH_A), "image/jpeg")
        }
    }

    @Test
    fun testPush_PendingCreate() = runTest {
        storeIncident(report(1), PATH_A, PATH_B)

        synchronizer.push()

        // Photos first, each URL stored and its file deleted at once; createAttempted is set before the POST
        coVerifyOrder {
            remoteDataSource.uploadPhoto(File(PATH_A), "image/jpeg")
            localDataSource.setPhotoRemoteUrl(1, PATH_A, REMOTE_A)
            photoLocalDataSource.deletePhoto(PATH_A)
            remoteDataSource.uploadPhoto(File(PATH_B), "image/jpeg")
            localDataSource.setPhotoRemoteUrl(1, PATH_B, REMOTE_B)
            photoLocalDataSource.deletePhoto(PATH_B)
            localDataSource.markCreateAttempted(1)
            remoteDataSource.createIncident(createRequest(report(1), listOf(REMOTE_A, REMOTE_B)))
        }

        Truth.assertThat(server.values.single().photoUrls).containsExactly(REMOTE_A, REMOTE_B).inOrder()
        Truth.assertThat(incidents.getValue(1)).isEqualTo(
            report(1).copy(
                serverId = "inc-1",
                updatedAt = RECEIVED_AT,
                syncState = SyncState.Synced,
                createAttempted = true,
            )
        )
        Truth.assertThat(photos.map { it.remoteUrl }).containsExactly(REMOTE_A, REMOTE_B)
        Truth.assertThat(files).isEmpty()
    }

    @Test
    fun testPush_PartialPhotoUploadThenRetry() = runTest {
        storeIncident(report(1), PATH_A, PATH_B)
        coEvery { remoteDataSource.uploadPhoto(File(PATH_B), any()) } throws NoConnectionException()

        assertPushThrows(NoConnectionException::class.java)

        // The first photo's URL is kept; nothing was POSTed, so the report may still simply be deleted
        Truth.assertThat(photos.map { it.remoteUrl }).containsExactly(REMOTE_A, null).inOrder()
        Truth.assertThat(files).containsExactly(PATH_B)
        Truth.assertThat(incidents.getValue(1).createAttempted).isFalse()
        Truth.assertThat(server).isEmpty()

        coEvery { remoteDataSource.uploadPhoto(File(PATH_B), any()) } returns REMOTE_B
        synchronizer.push()

        // The retry uploads only the photo that hadn't arrived
        coVerify(exactly = 1) { remoteDataSource.uploadPhoto(File(PATH_A), any()) }
        coVerify(exactly = 2) { remoteDataSource.uploadPhoto(File(PATH_B), any()) }
        Truth.assertThat(server.values.single().photoUrls).containsExactly(REMOTE_A, REMOTE_B).inOrder()
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
    }

    @Test
    fun testPush_LostPostResponseThenRetry() = runTest {
        storeIncident(report(1), PATH_A)
        var isResponseLost = true
        coEvery { remoteDataSource.createIncident(any()) } answers {
            val stored = serverCreate(firstArg())
            if (isResponseLost) {
                isResponseLost = false
                throw NoConnectionException()
            }
            stored
        }

        assertPushThrows(NoConnectionException::class.java)

        // The server has the report, but the device doesn't know its id; it can no longer be simply deleted
        Truth.assertThat(server).hasSize(1)
        Truth.assertThat(incidents.getValue(1).serverId).isNull()
        Truth.assertThat(incidents.getValue(1).createAttempted).isTrue()
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.PendingCreate)

        synchronizer.push()

        // The retry sends the same createdAt, which the server recognises: still one incident, whose id is adopted
        coVerify(exactly = 2) { remoteDataSource.createIncident(createRequest(report(1), listOf(REMOTE_A))) }
        Truth.assertThat(server).hasSize(1)
        Truth.assertThat(incidents.getValue(1).serverId).isEqualTo(server.keys.single())
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
    }

    @Test
    fun testPush_PendingCreateKnownByServerId() = runTest {
        // Edited while its POST was in flight: the id arrived, but the edit is still to be sent
        server["inc-1"] = serverIncident("inc-1", report(1))
        storeIncident(report(1).copy(serverId = "inc-1", title = "Edited", createAttempted = true), remote = REMOTE_A)

        synchronizer.push()

        // POSTed again; the server updates the report it has, as it is still open (ADR-0003)
        Truth.assertThat(server).hasSize(1)
        Truth.assertThat(server.getValue("inc-1").title).isEqualTo("Edited")
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
    }

    @Test
    fun testPush_EditDuringInFlightPost() = runTest {
        storeIncident(report(1), remote = REMOTE_A)
        var isFirstPost = true
        coEvery { remoteDataSource.createIncident(any()) } answers {
            val stored = serverCreate(firstArg())
            if (isFirstPost) {
                isFirstPost = false
                editStored(1, title = "Edited meanwhile")
            }
            stored
        }

        synchronizer.push()

        // The first answer only gave the row its server id, as the edit kept it pending; the edit went out next
        Truth.assertThat(writes.first()).isEqualTo(
            report(1).copy(
                serverId = "inc-1",
                title = "Edited meanwhile",
                updatedAt = RECEIVED_AT,
                localVersion = 1,
                createAttempted = true,
            )
        )
        coVerify(exactly = 2) { remoteDataSource.createIncident(any()) }
        Truth.assertThat(server).hasSize(1)
        Truth.assertThat(server.getValue("inc-1").title).isEqualTo("Edited meanwhile")
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
    }

    @Test
    fun testPush_PendingUpdate() = runTest {
        server["inc-1"] = serverIncident("inc-1", synced(1, "inc-1")).copy(priority = "high")
        storeIncident(edited(synced(1, "inc-1"), title = "New title"), remote = REMOTE_A)

        synchronizer.push()

        coVerify(exactly = 1) {
            remoteDataSource.updateIncident("inc-1", updateRequest(edited(synced(1, "inc-1"), "New title"), REMOTE_A))
        }
        Truth.assertThat(server.getValue("inc-1").title).isEqualTo("New title")
        Truth.assertThat(incidents.getValue(1)).isEqualTo(
            edited(synced(1, "inc-1"), title = "New title").copy(
                syncState = SyncState.Synced,
                priority = "high",
                updatedAt = server.getValue("inc-1").updatedAt,
            )
        )
    }

    @Test
    fun testPush_PendingUpdateWithNewPhoto() = runTest {
        server["inc-1"] = serverIncident("inc-1", synced(1, "inc-1"))
        storeIncident(edited(synced(1, "inc-1"), title = "With a second photo"))
        storePhotoRow(incidentLocalId = 1, position = 0, localPath = null, remoteUrl = REMOTE_A)
        storePhotoRow(incidentLocalId = 1, position = 1, localPath = PATH_B, remoteUrl = null)
        files += PATH_B

        synchronizer.push()

        // Only the new photo is uploaded, and the PATCH carries both in order
        coVerify(exactly = 1) { remoteDataSource.uploadPhoto(any(), any()) }
        Truth.assertThat(server.getValue("inc-1").photoUrls).containsExactly(REMOTE_A, REMOTE_B).inOrder()
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
    }

    @Test
    fun testPush_EditDuringInFlightPatchStaysPending() = runTest {
        server["inc-1"] = serverIncident("inc-1", synced(1, "inc-1"))
        storeIncident(edited(synced(1, "inc-1"), title = "First edit"), remote = REMOTE_A)
        var patches = 0
        coEvery { remoteDataSource.updateIncident("inc-1", any()) } answers {
            patches++
            if (patches > 1) throw NoConnectionException()
            val stored = serverUpdate(firstArg(), secondArg())
            editStored(1, title = "Second edit")
            stored
        }

        assertPushThrows(NoConnectionException::class.java)

        // The server accepted the first edit, but the second one, made during the PATCH, isn't lost
        Truth.assertThat(server.getValue("inc-1").title).isEqualTo("First edit")
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.PendingUpdate)
        Truth.assertThat(incidents.getValue(1).title).isEqualTo("Second edit")
        Truth.assertThat(incidents.getValue(1).localVersion).isEqualTo(2)
    }

    @Test
    fun testPush_EditDuringInFlightPatchSentInSamePush() = runTest {
        server["inc-1"] = serverIncident("inc-1", synced(1, "inc-1"))
        storeIncident(edited(synced(1, "inc-1"), title = "First edit"), remote = REMOTE_A)
        var isFirstPatch = true
        coEvery { remoteDataSource.updateIncident("inc-1", any()) } answers {
            val stored = serverUpdate(firstArg(), secondArg())
            if (isFirstPatch) {
                isFirstPatch = false
                editStored(1, title = "Second edit")
            }
            stored
        }

        synchronizer.push()

        // Scheduling a push during a push has no effect, so the push itself sends the newer version
        coVerify(exactly = 2) { remoteDataSource.updateIncident("inc-1", any()) }
        Truth.assertThat(server.getValue("inc-1").title).isEqualTo("Second edit")
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
    }

    @Test
    fun testPush_PendingUpdateConflict() = runTest {
        server["inc-1"] = serverIncident("inc-1", synced(1, "inc-1")).copy(status = "acknowledged", priority = "urgent")
        storeIncident(edited(synced(1, "inc-1"), title = "Too late"), PATH_B)

        synchronizer.push()

        // The edit is dropped, the server's version overwrites the row, and the notice is raised (ADR-0004)
        coVerifyOrder {
            remoteDataSource.updateIncident("inc-1", any())
            remoteDataSource.fetchIncident("inc-1")
        }
        val stored = incidents.getValue(1)
        Truth.assertThat(stored.title).isEqualTo("Leaking valve 1")
        Truth.assertThat(stored.status).isEqualTo("acknowledged")
        Truth.assertThat(stored.priority).isEqualTo("urgent")
        Truth.assertThat(stored.syncState).isEqualTo(SyncState.Synced)
        Truth.assertThat(stored.statusChangeNoticePending).isTrue()
        Truth.assertThat(photos.map { it.remoteUrl to it.localPath }).containsExactly(REMOTE_A to null)
    }

    @Test
    fun testPush_ConflictThenNoConnection() = runTest {
        server["inc-1"] = serverIncident("inc-1", synced(1, "inc-1")).copy(status = "acknowledged")
        storeIncident(edited(synced(1, "inc-1"), title = "Too late"), remote = REMOTE_A)
        coEvery { remoteDataSource.fetchIncident("inc-1") } throws NoConnectionException()

        assertPushThrows(NoConnectionException::class.java)

        // Left pending, so the next push gets the 409 again and finishes the job
        Truth.assertThat(incidents.getValue(1)).isEqualTo(edited(synced(1, "inc-1"), title = "Too late"))
    }

    @Test
    fun testPush_PendingWithdrawNeverSent() = runTest {
        storeIncident(report(1).copy(syncState = SyncState.PendingWithdraw, localVersion = 1), PATH_A, PATH_B)

        synchronizer.push()

        // Never POSTed, so it is deleted with its files and nothing is sent (ADR-0003)
        Truth.assertThat(incidents).isEmpty()
        Truth.assertThat(photos).isEmpty()
        Truth.assertThat(files).isEmpty()
        coVerify(exactly = 0) { remoteDataSource.uploadPhoto(any(), any()) }
        coVerify(exactly = 0) { remoteDataSource.createIncident(any()) }
        coVerify(exactly = 0) { remoteDataSource.withdrawIncident(any()) }
    }

    @Test
    fun testPush_UnsentWithdrawalsGoFirst() = runTest {
        storeIncident(report(1), remote = REMOTE_A)
        storeIncident(report(2).copy(syncState = SyncState.PendingWithdraw), PATH_B)
        coEvery { remoteDataSource.createIncident(any()) } throws NoConnectionException()

        assertPushThrows(NoConnectionException::class.java)

        // Needs no connection, so it isn't held up by the report that couldn't be sent
        Truth.assertThat(incidents.keys).containsExactly(1L)
    }

    @Test
    fun testPush_PendingWithdrawSynced() = runTest {
        server["inc-1"] = serverIncident("inc-1", synced(1, "inc-1"))
        storeIncident(synced(1, "inc-1").copy(syncState = SyncState.PendingWithdraw, localVersion = 1), PATH_B)

        synchronizer.push()

        // Only the withdrawal is sent; photos of a dropped edit aren't uploaded for it
        coVerify(exactly = 0) { remoteDataSource.uploadPhoto(any(), any()) }
        coVerify(exactly = 1) { remoteDataSource.withdrawIncident("inc-1") }
        Truth.assertThat(server.getValue("inc-1").status).isEqualTo("withdrawn")
        Truth.assertThat(incidents.getValue(1).status).isEqualTo("withdrawn")
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
    }

    @Test
    fun testPush_PendingWithdrawPostFirst() = runTest {
        // A POST was sent, but its response was lost
        storeIncident(report(1).copy(syncState = SyncState.PendingWithdraw, createAttempted = true), PATH_A)

        synchronizer.push()

        // POSTed first, so no Open incident is left behind, then withdrawn
        coVerifyOrder {
            remoteDataSource.uploadPhoto(File(PATH_A), "image/jpeg")
            remoteDataSource.createIncident(createRequest(report(1), listOf(REMOTE_A)))
            remoteDataSource.withdrawIncident("inc-1")
        }
        Truth.assertThat(server.values.single().status).isEqualTo("withdrawn")
        Truth.assertThat(incidents.getValue(1).serverId).isEqualTo("inc-1")
        Truth.assertThat(incidents.getValue(1).status).isEqualTo("withdrawn")
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
    }

    @Test
    fun testPush_PendingWithdrawPostFirstThenNoConnection() = runTest {
        storeIncident(report(1).copy(syncState = SyncState.PendingWithdraw, createAttempted = true), remote = REMOTE_A)
        coEvery { remoteDataSource.withdrawIncident(any()) } throws NoConnectionException()

        assertPushThrows(NoConnectionException::class.java)

        // The id is kept, so the retry only PATCHes
        Truth.assertThat(incidents.getValue(1).serverId).isEqualTo("inc-1")
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.PendingWithdraw)
    }

    @Test
    fun testPush_RetriedWithdrawalAlreadyWithdrawn() = runTest {
        // The first PATCH succeeded, but its response was lost
        server["inc-1"] = serverIncident("inc-1", synced(1, "inc-1")).copy(status = "withdrawn")
        storeIncident(withdrawn(synced(1, "inc-1")), remote = REMOTE_A)

        synchronizer.push()

        // The 409 means the withdrawal already happened: synced, without a notice
        coVerify(exactly = 1) { remoteDataSource.fetchIncident("inc-1") }
        Truth.assertThat(incidents.getValue(1).status).isEqualTo("withdrawn")
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
        Truth.assertThat(incidents.getValue(1).statusChangeNoticePending).isFalse()
    }

    @Test
    fun testPush_PendingWithdrawConflict() = runTest {
        server["inc-1"] = serverIncident("inc-1", synced(1, "inc-1")).copy(status = "in_progress")
        storeIncident(withdrawn(synced(1, "inc-1")), remote = REMOTE_A)

        synchronizer.push()

        Truth.assertThat(incidents.getValue(1).status).isEqualTo("in_progress")
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
        Truth.assertThat(incidents.getValue(1).statusChangeNoticePending).isTrue()
    }

    @Test
    fun testPush_WithdrawDuringInFlightPatch() = runTest {
        server["inc-1"] = serverIncident("inc-1", synced(1, "inc-1"))
        storeIncident(edited(synced(1, "inc-1"), title = "Edit"), remote = REMOTE_A)
        coEvery { remoteDataSource.updateIncident("inc-1", any()) } answers {
            val stored = serverUpdate(firstArg(), secondArg())
            incidents[1] = incidents.getValue(1).let {
                it.copy(syncState = SyncState.PendingWithdraw, localVersion = it.localVersion + 1)
            }
            stored
        }

        synchronizer.push()

        // The edit's answer doesn't mark the row synced, so the withdrawal still goes out
        coVerifyOrder {
            remoteDataSource.updateIncident("inc-1", any())
            remoteDataSource.withdrawIncident("inc-1")
        }
        Truth.assertThat(server.getValue("inc-1").status).isEqualTo("withdrawn")
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
    }

    @Test
    fun testPush_PostAnsweredWithMovedOnIncident() = runTest {
        // Reported, the response lost, then acknowledged; the report was edited on the device meanwhile
        server["inc-1"] = serverIncident("inc-1", report(1)).copy(status = "acknowledged")
        val editedOffline = report(1).copy(title = "Edited offline", createAttempted = true, localVersion = 1)
        storeIncident(editedOffline, remote = REMOTE_A)

        synchronizer.push()

        // The server kept its version instead of the edit, which is reported like a 409
        Truth.assertThat(server.getValue("inc-1").title).isEqualTo(report(1).title)
        Truth.assertThat(incidents.getValue(1).title).isEqualTo(report(1).title)
        Truth.assertThat(incidents.getValue(1).serverId).isEqualTo("inc-1")
        Truth.assertThat(incidents.getValue(1).status).isEqualTo("acknowledged")
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
        Truth.assertThat(incidents.getValue(1).statusChangeNoticePending).isTrue()
    }

    @Test
    fun testPush_PostAnsweredWithMovedOnSameReport() = runTest {
        // Reported, the response lost, then acknowledged; nothing changed on the device
        server["inc-1"] = serverIncident("inc-1", report(1)).copy(status = "acknowledged")
        storeIncident(report(1).copy(createAttempted = true), remote = REMOTE_A)

        synchronizer.push()

        // The server has exactly this report, so nothing was refused
        Truth.assertThat(incidents.getValue(1).status).isEqualTo("acknowledged")
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
        Truth.assertThat(incidents.getValue(1).statusChangeNoticePending).isFalse()
    }

    @Test
    fun testPush_Unauthorized() = runTest {
        storeIncident(report(1), remote = REMOTE_A)
        storeIncident(report(2), remote = REMOTE_B)
        coEvery { remoteDataSource.createIncident(any()) } throws UnauthorizedException()

        assertPushThrows(UnauthorizedException::class.java)

        // Stops at once and marks the session as rejected; nothing is lost
        verify(exactly = 1) { sessionHolder.markRejected() }
        coVerify(exactly = 1) { remoteDataSource.createIncident(any()) }
        Truth.assertThat(incidents.values.map { it.syncState }).containsExactly(
            SyncState.PendingCreate,
            SyncState.PendingCreate,
        )
    }

    @Test
    fun testPush_UnauthorizedOnUpload() = runTest {
        storeIncident(report(1), PATH_A)
        coEvery { remoteDataSource.uploadPhoto(any(), any()) } throws UnauthorizedException()

        assertPushThrows(UnauthorizedException::class.java)

        verify(exactly = 1) { sessionHolder.markRejected() }
        Truth.assertThat(files).containsExactly(PATH_A)
    }

    @Test
    fun testPush_NoConnection() = runTest {
        storeIncident(report(1), remote = REMOTE_A)
        storeIncident(report(2), remote = REMOTE_B)
        coEvery { remoteDataSource.createIncident(any()) } throws NoConnectionException()

        assertPushThrows(NoConnectionException::class.java)

        // Stops for a retry; the session is fine
        coVerify(exactly = 1) { remoteDataSource.createIncident(any()) }
        verify(exactly = 0) { sessionHolder.markRejected() }
        Truth.assertThat(incidents.values.map { it.syncState }).containsExactly(
            SyncState.PendingCreate,
            SyncState.PendingCreate,
        )
    }

    @Test
    fun testPush_BadRequestSkipsIncident() = runTest {
        storeIncident(report(1), remote = REMOTE_A)
        storeIncident(report(2), remote = REMOTE_B)
        coEvery { remoteDataSource.createIncident(match { it.createdAt == report(1).createdAt }) } throws
            httpException(400)

        synchronizer.push()

        // The refused report stays pending and is logged; the next one is still sent, and nothing is retried in a loop
        coVerify(exactly = 1) { remoteDataSource.createIncident(match { it.createdAt == report(1).createdAt }) }
        verify(exactly = 1) { logger.error("IncidentSynchronizer", any(), ofType(HttpException::class)) }
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.PendingCreate)
        Truth.assertThat(incidents.getValue(2).syncState).isEqualTo(SyncState.Synced)
    }

    @Test
    fun testPush_RefusedPhotoSkipsIncident() = runTest {
        storeIncident(report(1), PATH_A)
        storeIncident(report(2), remote = REMOTE_B)
        coEvery { remoteDataSource.uploadPhoto(File(PATH_A), any()) } throws httpException(400)

        synchronizer.push()

        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.PendingCreate)
        Truth.assertThat(incidents.getValue(1).createAttempted).isFalse()
        Truth.assertThat(files).containsExactly(PATH_A)
        Truth.assertThat(incidents.getValue(2).syncState).isEqualTo(SyncState.Synced)
    }

    @Test
    fun testPush_ServerErrorSkipsIncident() = runTest {
        server["inc-1"] = serverIncident("inc-1", synced(1, "inc-1"))
        storeIncident(edited(synced(1, "inc-1"), title = "Edit"), remote = REMOTE_A)
        storeIncident(report(2), remote = REMOTE_B)
        coEvery { remoteDataSource.updateIncident("inc-1", any()) } throws httpException(500)

        synchronizer.push()

        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.PendingUpdate)
        Truth.assertThat(incidents.getValue(2).syncState).isEqualTo(SyncState.Synced)
    }

    @Test
    fun testPush_MissingPhotoFileSkipsIncident() = runTest {
        storeIncident(report(1))
        storePhotoRow(incidentLocalId = 1, position = 0, localPath = PATH_A, remoteUrl = null)
        storeIncident(report(2), remote = REMOTE_B)

        synchronizer.push()

        // It can never be uploaded, so nothing is sent for it; the technician can still edit the photo away
        coVerify(exactly = 0) { remoteDataSource.uploadPhoto(File(PATH_A), any()) }
        verify(exactly = 1) { logger.error("IncidentSynchronizer", any(), null) }
        Truth.assertThat(incidents.getValue(1).createAttempted).isFalse()
        Truth.assertThat(incidents.getValue(2).syncState).isEqualTo(SyncState.Synced)
    }

    @Test
    fun testPush_ReportSavedDuringPushIsSent() = runTest {
        storeIncident(report(1), remote = REMOTE_A)
        coEvery { remoteDataSource.createIncident(match { it.createdAt == report(1).createdAt }) } answers {
            storeIncident(report(2), remote = REMOTE_B)
            serverCreate(firstArg())
        }

        synchronizer.push()

        Truth.assertThat(server).hasSize(2)
        Truth.assertThat(incidents.values.map { it.syncState }).containsExactly(SyncState.Synced, SyncState.Synced)
    }

    @Test
    fun testPush_ServerIdHeldBySyncedCopy() = runTest {
        // A copy of the same server incident, e.g. left by a pull of an older version
        server["inc-1"] = serverIncident("inc-1", report(1))
        storeIncident(synced(9, "inc-1").copy(createdAt = report(1).createdAt))
        storeIncident(report(1).copy(createAttempted = true), remote = REMOTE_A)

        synchronizer.push()

        // Two rows never share a server id: the copy without a change of its own goes
        Truth.assertThat(incidents.keys).containsExactly(1L)
        Truth.assertThat(incidents.getValue(1).serverId).isEqualTo("inc-1")
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
    }

    @Test
    fun testPush_ServerIdHeldByPendingRow() = runTest {
        server["inc-1"] = serverIncident("inc-1", report(1))
        storeIncident(
            synced(9, "inc-1").copy(createdAt = report(1).createdAt, syncState = SyncState.PendingUpdate),
            remote = REMOTE_B,
        )
        storeIncident(report(1).copy(createAttempted = true), remote = REMOTE_A)

        synchronizer.push()

        // Neither change is thrown away and no two rows share the id; the clash is logged
        Truth.assertThat(incidents.values.mapNotNull { it.serverId }).containsExactly("inc-1")
        Truth.assertThat(incidents.getValue(1).serverId).isNull()
        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.PendingCreate)
        verify { logger.error("IncidentSynchronizer", match { it.contains("held by incident 9") }, null) }
    }

    @Test
    fun testPush_WaitsForSyncLock() = runTest {
        storeIncident(report(1), remote = REMOTE_A)
        val pull = CompletableDeferred<Unit>()
        val pulling = launch(UnconfinedTestDispatcher(testScheduler)) { syncLock.withLock { pull.await() } }

        val pushing = launch(UnconfinedTestDispatcher(testScheduler)) { synchronizer.push() }

        // Nothing is read or sent while a pull or a wipe holds the lock
        coVerify(exactly = 0) { localDataSource.getPendingIncidents() }

        pull.complete(Unit)
        pulling.join()
        pushing.join()

        Truth.assertThat(incidents.getValue(1).syncState).isEqualTo(SyncState.Synced)
    }

    private suspend fun assertPushThrows(type: Class<out Exception>) {
        val thrown = try {
            synchronizer.push()
            null
        } catch (e: Exception) {
            e
        }
        Truth.assertThat(thrown).isInstanceOf(type)
    }

    private fun stubLocalDataSource() {
        coEvery { localDataSource.inTransaction<Any?>(any()) } coAnswers { firstArg<suspend () -> Any?>().invoke() }
        coEvery { localDataSource.getPendingIncidents() } answers {
            incidents.values.filter { it.syncState != SyncState.Synced }.sortedBy { it.localId }
        }
        coEvery { localDataSource.getIncident(any()) } answers { incidents[firstArg()] }
        coEvery { localDataSource.getIncidentWithPhotos(any()) } answers {
            incidents[firstArg()]?.let { incident ->
                IncidentWithPhotos(incident, photos.filter { it.incidentLocalId == incident.localId })
            }
        }
        coEvery { localDataSource.getIncidentByServerId(any()) } answers {
            incidents.values.find { it.serverId == firstArg<String>() }
        }
        coEvery { localDataSource.markCreateAttempted(any()) } answers {
            val localId = firstArg<Long>()
            incidents[localId] = incidents.getValue(localId).copy(createAttempted = true)
        }
        coEvery { localDataSource.updateIncident(any()) } answers {
            val incident = firstArg<IncidentEntity>()
            // Like the unique index on serverId
            check(incidents.values.none { it.localId != incident.localId && it.serverId == incident.serverId &&
                it.serverId != null }) { "UNIQUE constraint failed: incidents.serverId" }
            incidents[incident.localId] = incident
            writes += incident
        }
        coEvery { localDataSource.deleteIncidents(any()) } answers {
            val localIds = firstArg<List<Long>>()
            incidents.keys.removeAll(localIds)
            photos.removeAll { it.incidentLocalId in localIds }
        }
        coEvery { localDataSource.replacePhotos(any(), any()) } answers {
            photos.removeAll { it.incidentLocalId == firstArg<Long>() }
            photos += secondArg<List<IncidentPhotoEntity>>()
        }
        coEvery { localDataSource.setPhotoRemoteUrl(any(), any(), any()) } answers {
            photos.replaceAll {
                val matches = it.incidentLocalId == firstArg<Long>() && it.localPath == secondArg<String>()
                if (matches && it.remoteUrl == null) it.copy(remoteUrl = thirdArg()) else it
            }
        }
        coEvery { localDataSource.getPhotoLocalPaths() } answers { photos.mapNotNull { it.localPath } }
    }

    private fun stubServer() {
        coEvery { remoteDataSource.uploadPhoto(any(), any()) } answers { "/uploads/${firstArg<File>().name}" }
        coEvery { remoteDataSource.createIncident(any()) } answers { serverCreate(firstArg()) }
        coEvery { remoteDataSource.updateIncident(any(), any()) } answers { serverUpdate(firstArg(), secondArg()) }
        coEvery { remoteDataSource.withdrawIncident(any()) } answers { serverWithdraw(firstArg()) }
        coEvery { remoteDataSource.fetchIncident(any()) } answers {
            server[firstArg()] ?: throw httpException(404)
        }
    }

    /**
     * Like the server's POST: a report with a createdAt it already has updates that incident while it is open,
     * and is answered with it unchanged otherwise (ADR-0003)
     */
    private fun serverCreate(request: CreateIncidentRequestDto): IncidentDto {
        val existing = server.values.find { it.createdAt == request.createdAt }
        val stored = when {
            existing == null -> IncidentDto(
                id = "inc-${server.size + 1}",
                title = request.title,
                description = request.description,
                categoryId = request.categoryId,
                severityId = request.severityId,
                siteId = request.siteId,
                locationDescription = request.locationDescription,
                position = request.position,
                photoUrls = request.photoUrls,
                status = STATUS_OPEN,
                priority = null,
                createdAt = request.createdAt,
                updatedAt = RECEIVED_AT,
            )
            existing.status != STATUS_OPEN -> return existing
            else -> existing.copy(
                title = request.title,
                description = request.description,
                categoryId = request.categoryId,
                severityId = request.severityId,
                siteId = request.siteId,
                locationDescription = request.locationDescription,
                position = request.position,
                photoUrls = request.photoUrls,
                updatedAt = existing.updatedAt + 1,
            )
        }
        server[stored.id] = stored
        return stored
    }

    private fun serverUpdate(id: String, request: UpdateIncidentRequestDto): IncidentDto {
        val existing = server[id] ?: throw httpException(404)
        if (existing.status != STATUS_OPEN) throw httpException(409)
        val stored = existing.copy(
            title = request.title,
            description = request.description,
            categoryId = request.categoryId,
            severityId = request.severityId,
            siteId = request.siteId,
            locationDescription = request.locationDescription,
            position = request.position,
            photoUrls = request.photoUrls,
            updatedAt = existing.updatedAt + 1,
        )
        server[id] = stored
        return stored
    }

    private fun serverWithdraw(id: String): IncidentDto {
        val existing = server[id] ?: throw httpException(404)
        // Withdrawing an incident that is already withdrawn is refused too
        if (existing.status != STATUS_OPEN) throw httpException(409)
        val stored = existing.copy(status = "withdrawn", updatedAt = existing.updatedAt + 1)
        server[id] = stored
        return stored
    }

    private fun serverIncident(id: String, incident: IncidentEntity): IncidentDto = IncidentDto(
        id = id,
        title = incident.title,
        description = incident.description.orEmpty(),
        categoryId = incident.categoryId,
        severityId = incident.severityId,
        siteId = incident.siteId,
        locationDescription = incident.locationDescription.orEmpty(),
        position = PositionDto(53.5, -2.2, 12.0),
        photoUrls = listOf(REMOTE_A),
        status = STATUS_OPEN,
        priority = null,
        createdAt = incident.createdAt,
        updatedAt = RECEIVED_AT,
    )

    /**
     * Stores [incident] with a photo for each of [localPaths], whose files exist, or else one uploaded photo [remote]
     */
    private fun storeIncident(incident: IncidentEntity, vararg localPaths: String, remote: String? = null) {
        incidents[incident.localId] = incident
        localPaths.forEachIndexed { position, path ->
            storePhotoRow(incident.localId, position, localPath = path, remoteUrl = null)
            files += path
        }
        if (remote != null) storePhotoRow(incident.localId, position = 0, localPath = null, remoteUrl = remote)
    }

    private fun storePhotoRow(incidentLocalId: Long, position: Int, localPath: String?, remoteUrl: String?) {
        photos += IncidentPhotoEntity(
            id = photos.size + 1L,
            incidentLocalId = incidentLocalId,
            position = position,
            localPath = localPath,
            remoteUrl = remoteUrl,
        )
    }

    /**
     * Edits the stored incident with [localId] the way the device does, as if the technician saved the form
     */
    private fun editStored(localId: Long, title: String) {
        val stored = incidents.getValue(localId)
        incidents[localId] = stored.copy(title = title, localVersion = stored.localVersion + 1)
    }

    private fun report(localId: Long) = IncidentEntity(
        localId = localId,
        serverId = null,
        title = "Leaking valve $localId",
        categoryId = "cat-plumbing",
        severityId = "sev-medium",
        siteId = "site-depot",
        status = STATUS_OPEN,
        createdAt = CREATED_AT + localId,
        updatedAt = null,
        description = "Dripping steadily",
        locationDescription = "Pump room B2",
        positionLat = 53.5,
        positionLng = -2.2,
        positionAccuracyM = 12.0,
        priority = null,
        detailsLoaded = true,
        syncState = SyncState.PendingCreate,
        localVersion = 0,
        createAttempted = false,
        statusChangeNoticePending = false,
    )

    private fun synced(localId: Long, serverId: String) = report(localId).copy(
        serverId = serverId,
        updatedAt = RECEIVED_AT,
        syncState = SyncState.Synced,
        createAttempted = true,
    )

    private fun edited(incident: IncidentEntity, title: String) = incident.copy(
        title = title,
        syncState = SyncState.PendingUpdate,
        localVersion = incident.localVersion + 1,
    )

    private fun withdrawn(incident: IncidentEntity) = incident.copy(
        syncState = SyncState.PendingWithdraw,
        localVersion = incident.localVersion + 1,
    )

    private fun createRequest(incident: IncidentEntity, photoUrls: List<String>) =
        converter.convertCreateRequest(incident, photoUrls)

    private fun updateRequest(incident: IncidentEntity, vararg photoUrls: String) =
        converter.convertUpdateRequest(incident, photoUrls.toList())

    private fun httpException(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    private companion object {
        const val NOW = 1_793_100_000_000L
        const val CREATED_AT = 1_793_015_100_000L
        const val RECEIVED_AT = 1_793_015_160_000L
        const val STATUS_OPEN = "open"
        const val PATH_A = "/files/photos/a.jpg"
        const val PATH_B = "/files/photos/b.jpg"
        const val REMOTE_A = "/uploads/a.jpg"
        const val REMOTE_B = "/uploads/b.jpg"
    }
}
