package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dto.CreateIncidentRequestDto
import com.example.hazardreporting.data.incident.dto.IncidentDto
import com.example.hazardreporting.data.incident.dto.IncidentSummaryDto
import com.example.hazardreporting.data.incident.dto.UpdateIncidentRequestDto
import com.example.hazardreporting.data.incident.dto.UploadResponseDto
import com.example.hazardreporting.data.incident.dto.WithdrawIncidentRequestDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Retrofit interface for the server's technician incident routes and photo uploads
 *
 * @author Roman Pliskin on 29.10.2026
 */
interface IncidentApi {

    /**
     * Returns the caller's incidents, without withdrawn ones, newest first
     */
    @GET("incidents/summary")
    suspend fun getSummaries(): List<IncidentSummaryDto>

    /**
     * Returns the caller's incident with server id [id]
     */
    @GET("incidents/{id}")
    suspend fun getIncident(@Path("id") id: String): IncidentDto

    /**
     * Reports an incident, or updates the caller's incident with the same createdAt while it is still open
     * (ADR-0003); returns the stored incident either way
     */
    @POST("incidents")
    suspend fun createIncident(@Body request: CreateIncidentRequestDto): IncidentDto

    /**
     * Changes the report fields of the caller's open incident with server id [id] and returns it
     */
    @PATCH("incidents/{id}")
    suspend fun updateIncident(@Path("id") id: String, @Body request: UpdateIncidentRequestDto): IncidentDto

    /**
     * Withdraws the caller's open incident with server id [id] and returns it
     */
    @PATCH("incidents/{id}")
    suspend fun withdrawIncident(@Path("id") id: String, @Body request: WithdrawIncidentRequestDto): IncidentDto

    /**
     * Stores a photo sent as the multipart field `file`
     */
    @Multipart
    @POST("uploads")
    suspend fun uploadPhoto(@Part file: MultipartBody.Part): UploadResponseDto
}
