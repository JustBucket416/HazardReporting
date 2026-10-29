package com.example.hazardreporting.domain.repository

import com.example.hazardreporting.domain.model.Incident
import com.example.hazardreporting.domain.model.IncidentDraft
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.Photo
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

    /**
     * Observes the stored incident with [localId]. Emits nothing while no such incident is stored,
     * e.g. after it was deleted
     */
    fun observeIncident(localId: Long): Flow<Incident>

    /**
     * Downloads the details of the incident with [localId] and stores them, but only when [force] is set
     * or its details were never downloaded (ADR-0006). Nothing is downloaded for an incident the server
     * doesn't know yet, whose details are all on the device. An incident with a pending change keeps
     * its report fields; only its status and update time are taken from the server
     *
     * @throws com.example.hazardreporting.domain.exception.NoConnectionException if the server can't be reached
     * @throws com.example.hazardreporting.domain.exception.UnauthorizedException if the server rejects the session
     */
    suspend fun loadDetails(localId: Long, force: Boolean)

    /**
     * Stores a new incident from the valid [draft] as a pending report, with its createdAt set to now
     *
     * @return the new incident's local id
     */
    suspend fun report(draft: IncidentDraft): Long

    /**
     * Creates a path for a new photo in the app's private photo storage, for the camera to write to.
     * The file itself is created by whoever writes it
     */
    suspend fun createPhotoFile(): Photo.Local

    /**
     * Copies the picture at [sourceUri], e.g. one picked from the gallery, into the app's private photo storage
     *
     * @return the copy
     */
    suspend fun importPhoto(sourceUri: String): Photo.Local

    /**
     * Deletes the file of [photo] from the app's private photo storage. A file outside it is left alone
     */
    suspend fun deletePhoto(photo: Photo.Local)
}
