package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dto.IncidentSummaryDto
import com.example.hazardreporting.data.remote.mapRemoteErrors
import javax.inject.Inject

/**
 * Calls the server's incident routes and maps failures to domain exceptions: a missing connection becomes
 * [com.example.hazardreporting.domain.exception.NoConnectionException] and a rejected session
 * [com.example.hazardreporting.domain.exception.UnauthorizedException]
 *
 * @property api used for making calls to the server
 *
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentRemoteDataSource @Inject constructor(
    private val api: IncidentApi,
) {

    /**
     * Downloads the caller's incident summaries
     */
    suspend fun fetchSummaries(): List<IncidentSummaryDto> = mapRemoteErrors { api.getSummaries() }
}
