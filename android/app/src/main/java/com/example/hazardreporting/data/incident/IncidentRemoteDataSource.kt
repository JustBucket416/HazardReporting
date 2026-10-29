package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dto.CreateIncidentRequestDto
import com.example.hazardreporting.data.incident.dto.IncidentDto
import com.example.hazardreporting.data.incident.dto.IncidentSummaryDto
import com.example.hazardreporting.data.incident.dto.UpdateIncidentRequestDto
import com.example.hazardreporting.data.incident.dto.WithdrawIncidentRequestDto
import com.example.hazardreporting.data.remote.RemoteErrorInterceptor
import java.io.File
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * Calls the server's incident routes. A missing connection fails with
 * [com.example.hazardreporting.domain.exception.NoConnectionException] and a rejected session with
 * [com.example.hazardreporting.domain.exception.UnauthorizedException], both thrown by [RemoteErrorInterceptor].
 * Any other refusal, e.g. a 409, propagates as a [retrofit2.HttpException]
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
    suspend fun fetchSummaries(): List<IncidentSummaryDto> = api.getSummaries()

    /**
     * Downloads the caller's incident with [serverId]
     */
    suspend fun fetchIncident(serverId: String): IncidentDto = api.getIncident(serverId)

    /**
     * Sends a report. The server answers a retried report with the incident it already has (ADR-0003)
     */
    suspend fun createIncident(request: CreateIncidentRequestDto): IncidentDto =
        api.createIncident(request)

    /**
     * Sends new report fields for the incident with [serverId]
     */
    suspend fun updateIncident(serverId: String, request: UpdateIncidentRequestDto): IncidentDto =
        api.updateIncident(serverId, request)

    /**
     * Withdraws the incident with [serverId]
     */
    suspend fun withdrawIncident(serverId: String): IncidentDto =
        api.withdrawIncident(serverId, WithdrawIncidentRequestDto(STATUS_WITHDRAWN))

    /**
     * Uploads the photo [file], declared as [mimeType]
     *
     * @return the stored photo's URL, relative to the server
     */
    suspend fun uploadPhoto(file: File, mimeType: String): String {
        val part = MultipartBody.Part.createFormData(FILE_PART, file.name, file.asRequestBody(mimeType.toMediaType()))
        return api.uploadPhoto(part).url
    }

    private companion object {
        const val STATUS_WITHDRAWN = "withdrawn"
        const val FILE_PART = "file"
    }
}
