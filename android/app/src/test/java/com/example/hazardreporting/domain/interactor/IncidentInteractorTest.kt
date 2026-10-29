package com.example.hazardreporting.domain.interactor

import app.cash.turbine.test
import com.example.hazardreporting.domain.exception.NoConnectionException
import com.example.hazardreporting.domain.model.Incident
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.SyncState
import com.example.hazardreporting.domain.repository.CatalogRepository
import com.example.hazardreporting.domain.repository.IncidentRepository
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentInteractorTest {

    private val incidentRepository = mockk<IncidentRepository>()
    private val catalogRepository = mockk<CatalogRepository>()

    private val interactor = IncidentInteractor(incidentRepository, catalogRepository)

    @Test
    fun testObserveMyIncidents() = runTest {
        val older = summary(localId = 1, createdAt = 100)
        val newest = summary(localId = 2, createdAt = 300)
        val pendingCreate = summary(localId = 3, createdAt = 200, syncState = SyncState.PendingCreate)
        val withdrawn = summary(localId = 4, createdAt = 400, status = IncidentStatus.Withdrawn)
        val pendingWithdraw = summary(localId = 5, createdAt = 500, syncState = SyncState.PendingWithdraw)

        every { incidentRepository.observeSummaries() } returns flowOf(
            listOf(older, withdrawn, newest, pendingWithdraw, pendingCreate)
        )

        interactor.observeMyIncidents().test {
            // Withdrawn and withdrawing incidents are left out; the rest are newest first
            Truth.assertThat(awaitItem()).containsExactly(newest, pendingCreate, older).inOrder()
            awaitComplete()
        }

        verifySequence { incidentRepository.observeSummaries() }
    }

    @Test
    fun testRefreshIncidents() = runTest {
        coJustRun { catalogRepository.refresh() }
        coJustRun { incidentRepository.pullSummaries() }

        interactor.refreshIncidents()

        // The catalog comes first, so the pulled incidents can be named
        coVerifySequence {
            catalogRepository.refresh()
            incidentRepository.pullSummaries()
        }
    }

    @Test
    fun testRefreshIncidents_NoConnection() {
        coEvery { catalogRepository.refresh() } throws NoConnectionException()

        Assert.assertThrows(NoConnectionException::class.java) {
            runTest { interactor.refreshIncidents() }
        }

        coVerifySequence { catalogRepository.refresh() }
    }

    @Test
    fun testObserveIncident() = runTest {
        val incident = mockk<Incident>()

        every { incidentRepository.observeIncident(7L) } returns flowOf(incident)

        interactor.observeIncident(7L).test {
            Truth.assertThat(awaitItem()).isSameInstanceAs(incident)
            awaitComplete()
        }

        verifySequence { incidentRepository.observeIncident(7L) }
    }

    @Test
    fun testLoadIncidentDetails() = runTest {
        coJustRun { incidentRepository.loadDetails(7L, force = true) }

        interactor.loadIncidentDetails(7L, force = true)

        coVerifySequence { incidentRepository.loadDetails(7L, force = true) }
    }

    private fun summary(
        localId: Long,
        createdAt: Long,
        status: IncidentStatus = IncidentStatus.Open,
        syncState: SyncState = SyncState.Synced,
    ) = IncidentSummary(
        localId = localId,
        serverId = "inc-$localId",
        title = "Incident $localId",
        categoryId = "cat-equipment",
        severityId = "sev-high",
        siteId = "site-north",
        status = status,
        createdAt = createdAt,
        syncState = syncState,
        hasStatusChangeNotice = false,
    )
}
