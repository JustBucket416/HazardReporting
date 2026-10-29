package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dto.IncidentSummaryDto
import retrofit2.http.GET

/**
 * Retrofit interface for the server's technician incident routes
 *
 * @author Roman Pliskin on 29.10.2026
 */
interface IncidentApi {

    /**
     * Returns the caller's incidents, without withdrawn ones, newest first
     */
    @GET("incidents/summary")
    suspend fun getSummaries(): List<IncidentSummaryDto>
}
