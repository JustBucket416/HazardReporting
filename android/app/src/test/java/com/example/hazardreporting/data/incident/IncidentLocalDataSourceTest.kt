package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dao.IncidentDao
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.data.local.HazardReportingDatabase
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

    private val dataSource = IncidentLocalDataSource(database, incidentDao)

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
}
