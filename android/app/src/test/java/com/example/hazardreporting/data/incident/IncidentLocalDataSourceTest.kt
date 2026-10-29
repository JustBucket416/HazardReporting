package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dao.IncidentDao
import com.example.hazardreporting.data.incident.dao.IncidentPhotoDao
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.data.incident.entity.IncidentPhotoEntity
import com.example.hazardreporting.data.incident.entity.IncidentWithPhotos
import com.example.hazardreporting.data.local.HazardReportingDatabase
import com.example.hazardreporting.domain.model.SyncState
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentLocalDataSourceTest {

    private val database = mockk<HazardReportingDatabase>()
    private val incidentDao = mockk<IncidentDao>()
    private val photoDao = mockk<IncidentPhotoDao>()

    private val dataSource = IncidentLocalDataSource(database, incidentDao, photoDao)

    @Test
    fun testObserveIncident() {
        val flow = mockk<Flow<IncidentWithPhotos?>>()

        every { incidentDao.observeWithPhotos(1L) } returns flow

        Truth.assertThat(dataSource.observeIncident(1L)).isSameInstanceAs(flow)

        verifySequence { incidentDao.observeWithPhotos(1L) }
    }

    @Test
    fun testGetIncident() = runTest {
        val incident = mockk<IncidentEntity>()

        coEvery { incidentDao.getByLocalId(1L) } returns incident

        Truth.assertThat(dataSource.getIncident(1L)).isSameInstanceAs(incident)

        coVerifySequence { incidentDao.getByLocalId(1L) }
    }

    @Test
    fun testUpdateIncident() = runTest {
        val incident = mockk<IncidentEntity>()

        coJustRun { incidentDao.update(incident) }

        dataSource.updateIncident(incident)

        coVerifySequence { incidentDao.update(incident) }
    }

    @Test
    fun testInsertIncident() = runTest {
        val incident = mockk<IncidentEntity>()

        coEvery { incidentDao.insert(incident) } returns 9L

        Truth.assertThat(dataSource.insertIncident(incident)).isEqualTo(9L)

        coVerifySequence { incidentDao.insert(incident) }
    }

    @Test
    fun testInsertPhotos() = runTest {
        val photos = listOf(mockk<IncidentPhotoEntity>())

        coJustRun { photoDao.insertAll(photos) }

        dataSource.insertPhotos(photos)

        coVerifySequence { photoDao.insertAll(photos) }
    }

    @Test
    fun testReplacePhotos() = runTest {
        val photos = listOf(mockk<IncidentPhotoEntity>())

        coJustRun { photoDao.replaceForIncident(1L, photos) }

        dataSource.replacePhotos(1L, photos)

        coVerifySequence { photoDao.replaceForIncident(1L, photos) }
    }

    @Test
    fun testObserveIncidents() {
        val flow = mockk<Flow<List<IncidentEntity>>>()

        every { incidentDao.observeAll() } returns flow

        Truth.assertThat(dataSource.observeIncidents()).isSameInstanceAs(flow)

        verifySequence { incidentDao.observeAll() }
    }

    @Test
    fun testGetIncidents() = runTest {
        val incidents = listOf(mockk<IncidentEntity>())

        coEvery { incidentDao.getAll() } returns incidents

        Truth.assertThat(dataSource.getIncidents()).isEqualTo(incidents)

        coVerifySequence { incidentDao.getAll() }
    }

    @Test
    fun testInsertIncidents() = runTest {
        val incidents = listOf(mockk<IncidentEntity>())

        coJustRun { incidentDao.insertAll(incidents) }

        dataSource.insertIncidents(incidents)

        coVerifySequence { incidentDao.insertAll(incidents) }
    }

    @Test
    fun testUpdateIncidents() = runTest {
        val incidents = listOf(mockk<IncidentEntity>())

        coJustRun { incidentDao.updateAll(incidents) }

        dataSource.updateIncidents(incidents)

        coVerifySequence { incidentDao.updateAll(incidents) }
    }

    @Test
    fun testDeleteIncidents() = runTest {
        coJustRun { incidentDao.deleteAll(listOf(1L, 2L)) }

        dataSource.deleteIncidents(listOf(1L, 2L))

        coVerifySequence { incidentDao.deleteAll(listOf(1L, 2L)) }
    }

    @Test
    fun testGetIncidentWithPhotos() = runTest {
        val incident = mockk<IncidentWithPhotos>()

        coEvery { incidentDao.getWithPhotos(1L) } returns incident

        Truth.assertThat(dataSource.getIncidentWithPhotos(1L)).isSameInstanceAs(incident)

        coVerifySequence { incidentDao.getWithPhotos(1L) }
    }

    @Test
    fun testGetIncidentByServerId() = runTest {
        val incident = mockk<IncidentEntity>()

        coEvery { incidentDao.getByServerId("inc-1") } returns incident

        Truth.assertThat(dataSource.getIncidentByServerId("inc-1")).isSameInstanceAs(incident)

        coVerifySequence { incidentDao.getByServerId("inc-1") }
    }

    @Test
    fun testGetPendingIncidents() = runTest {
        val incidents = listOf(mockk<IncidentEntity>())

        coEvery { incidentDao.getAllExcept(SyncState.Synced) } returns incidents

        Truth.assertThat(dataSource.getPendingIncidents()).isEqualTo(incidents)

        coVerifySequence { incidentDao.getAllExcept(SyncState.Synced) }
    }

    @Test
    fun testExistsCreatedAt() = runTest {
        coEvery { incidentDao.existsCreatedAt(100L) } returns true

        Truth.assertThat(dataSource.existsCreatedAt(100L)).isTrue()

        coVerifySequence { incidentDao.existsCreatedAt(100L) }
    }

    @Test
    fun testMarkCreateAttempted() = runTest {
        coJustRun { incidentDao.markCreateAttempted(1L) }

        dataSource.markCreateAttempted(1L)

        coVerifySequence { incidentDao.markCreateAttempted(1L) }
    }

    @Test
    fun testGetPhotos() = runTest {
        val photos = listOf(mockk<IncidentPhotoEntity>())

        coEvery { photoDao.getForIncident(1L) } returns photos

        Truth.assertThat(dataSource.getPhotos(1L)).isEqualTo(photos)

        coVerifySequence { photoDao.getForIncident(1L) }
    }

    @Test
    fun testGetPhotoLocalPaths() = runTest {
        coEvery { photoDao.getLocalPaths() } returns listOf("/files/photos/a.jpg")

        Truth.assertThat(dataSource.getPhotoLocalPaths()).containsExactly("/files/photos/a.jpg")

        coVerifySequence { photoDao.getLocalPaths() }
    }

    @Test
    fun testSetPhotoRemoteUrl() = runTest {
        coJustRun { photoDao.setRemoteUrl(1L, "/files/photos/a.jpg", "/uploads/a.jpg") }

        dataSource.setPhotoRemoteUrl(1L, "/files/photos/a.jpg", "/uploads/a.jpg")

        coVerifySequence { photoDao.setRemoteUrl(1L, "/files/photos/a.jpg", "/uploads/a.jpg") }
    }
}
