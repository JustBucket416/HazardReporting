package com.example.hazardreporting.domain.repository

import com.example.hazardreporting.domain.model.IncidentSummary
import kotlinx.coroutines.flow.Flow

/**
 * The technician's incidents. They are stored on the device, which is the source of truth (ADR-0002);
 * the server is only pulled from on request
 *
 * @author Roman Pliskin on 29.10.2026
 */
interface IncidentRepository {

    /**
     * Observes the summaries of every stored incident, in no particular order
     */
    fun observeSummaries(): Flow<List<IncidentSummary>>

    /**
     * Downloads the technician's incident summaries and merges them into the stored incidents. An incident
     * with a pending change keeps its report fields; only its status and update time are taken from the server
     *
     * @throws com.example.hazardreporting.domain.exception.NoConnectionException if the server can't be reached
     * @throws com.example.hazardreporting.domain.exception.UnauthorizedException if the server rejects the session
     */
    suspend fun pullSummaries()
}
