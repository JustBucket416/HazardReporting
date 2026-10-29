package com.example.hazardreporting.domain.interactor

import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.SyncState
import com.example.hazardreporting.domain.repository.CatalogRepository
import com.example.hazardreporting.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use cases of the technician's incidents
 *
 * @property incidentRepository stores the incidents
 * @property catalogRepository stores the catalog the incidents refer to
 *
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentInteractor(
    private val incidentRepository: IncidentRepository,
    private val catalogRepository: CatalogRepository,
) {

    /**
     * Observes the technician's incidents for their list, newest first. Withdrawn incidents, and incidents
     * withdrawn on the device but not yet on the server, are left out
     */
    fun observeMyIncidents(): Flow<List<IncidentSummary>> = incidentRepository.observeSummaries()
        .map { summaries ->
            summaries
                .filter { it.status != IncidentStatus.Withdrawn && it.syncState != SyncState.PendingWithdraw }
                .sortedByDescending { it.createdAt }
        }

    /**
     * Pulls the catalog and then the technician's incident summaries from the server.
     * Pushing pending changes first is added with the synchroniser (issue 13)
     *
     * @throws com.example.hazardreporting.domain.exception.NoConnectionException if the server can't be reached
     * @throws com.example.hazardreporting.domain.exception.UnauthorizedException if the server rejects the session
     */
    suspend fun refreshIncidents() {
        catalogRepository.refresh()
        incidentRepository.pullSummaries()
    }
}
