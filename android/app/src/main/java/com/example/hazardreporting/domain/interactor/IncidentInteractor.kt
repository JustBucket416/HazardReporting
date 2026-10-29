package com.example.hazardreporting.domain.interactor

import com.example.hazardreporting.domain.exception.UnauthorizedException
import com.example.hazardreporting.domain.exception.ValidationException
import com.example.hazardreporting.domain.model.Incident
import com.example.hazardreporting.domain.model.IncidentDraft
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.Position
import com.example.hazardreporting.domain.model.SyncState
import com.example.hazardreporting.domain.repository.CatalogRepository
import com.example.hazardreporting.domain.repository.IncidentRepository
import com.example.hazardreporting.domain.repository.LocationProvider
import com.example.hazardreporting.domain.repository.SyncRepository
import com.example.hazardreporting.domain.repository.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use cases of the technician's incidents. The rules a draft must satisfy live here and only here
 *
 * @property incidentRepository stores the incidents
 * @property catalogRepository stores the catalog the incidents refer to
 * @property syncRepository pushes pending changes to the server
 * @property locationProvider provides the device's position for a new report
 * @property syncScheduler schedules pushing saved changes to the server
 *
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentInteractor(
    private val incidentRepository: IncidentRepository,
    private val catalogRepository: CatalogRepository,
    private val syncRepository: SyncRepository,
    private val locationProvider: LocationProvider,
    private val syncScheduler: SyncScheduler,
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
     * Pushes the pending changes, then pulls the catalog and the technician's incident summaries from the server
     *
     * @throws com.example.hazardreporting.domain.exception.NoConnectionException if the server can't be reached
     * @throws UnauthorizedException if the server rejects the session
     */
    suspend fun refreshIncidents() {
        syncRepository.push()
        catalogRepository.refresh()
        incidentRepository.pullSummaries()
    }

    /**
     * Observes the incident with [localId], with its details once they are downloaded
     */
    fun observeIncident(localId: Long): Flow<Incident> = incidentRepository.observeIncident(localId)

    /**
     * Downloads the details of the incident with [localId] if [force] is set or they were never downloaded.
     * Downloaded details are reused until the technician refreshes them (ADR-0006)
     *
     * @throws com.example.hazardreporting.domain.exception.NoConnectionException if the server can't be reached
     * @throws UnauthorizedException if the server rejects the session
     */
    suspend fun loadIncidentDetails(localId: Long, force: Boolean) {
        incidentRepository.loadDetails(localId, force)
    }

    /**
     * Returns the fields of [draft] that break a rule, or an empty set if it can be saved:
     * - the title is 1 to [IncidentDraft.MAX_TITLE_LENGTH] characters after trimming
     * - the description is at most [IncidentDraft.MAX_DESCRIPTION_LENGTH] characters after trimming
     * - a category, a severity and a site are chosen
     * - there are [IncidentDraft.MIN_PHOTOS] to [IncidentDraft.MAX_PHOTOS] photos
     */
    fun validate(draft: IncidentDraft): Set<IncidentDraft.Field> = buildSet {
        val titleLength = draft.title.trim().length
        if (titleLength == 0 || titleLength > IncidentDraft.MAX_TITLE_LENGTH) add(IncidentDraft.Field.Title)
        if (draft.description.trim().length > IncidentDraft.MAX_DESCRIPTION_LENGTH) {
            add(IncidentDraft.Field.Description)
        }
        if (draft.categoryId.isNullOrBlank()) add(IncidentDraft.Field.Category)
        if (draft.severityId.isNullOrBlank()) add(IncidentDraft.Field.Severity)
        if (draft.siteId.isNullOrBlank()) add(IncidentDraft.Field.Site)
        if (draft.photos.size !in IncidentDraft.MIN_PHOTOS..IncidentDraft.MAX_PHOTOS) add(IncidentDraft.Field.Photos)
    }

    /**
     * Reports a new incident from [draft]: stores it on the device as a pending report, with its text fields
     * trimmed, and schedules a push. Never waits for the network (ADR-0002)
     *
     * @return the new incident's local id
     * @throws ValidationException if the draft breaks a rule of [validate]; nothing is stored then
     */
    suspend fun report(draft: IncidentDraft): Long {
        val invalidFields = validate(draft)
        if (invalidFields.isNotEmpty()) throw ValidationException(invalidFields)

        val localId = incidentRepository.report(draft.trimmed())
        syncScheduler.schedulePush()
        return localId
    }

    /**
     * Returns the draft the incident with [localId] is edited from: its report fields, its original position and
     * its photos. Null if no such incident is stored, or its details aren't downloaded, without which it can't
     * be edited (ADR-0006)
     */
    suspend fun getDraft(localId: Long): IncidentDraft? {
        val incident = incidentRepository.getIncident(localId) ?: return null
        val details = incident.details ?: return null
        val summary = incident.summary
        return IncidentDraft(
            title = summary.title,
            description = details.description,
            categoryId = summary.categoryId,
            severityId = summary.severityId,
            siteId = summary.siteId,
            locationDescription = details.locationDescription,
            position = details.position,
            photos = details.photos,
        )
    }

    /**
     * Edits the incident with [localId] to match [draft], with its text fields trimmed, and schedules a push.
     * Never waits for the network (ADR-0002). A synced incident gets a pending edit; a report the server hasn't
     * accepted yet stays a pending report, which then goes out with the edit. An incident being withdrawn
     * is left as it is. Whether the incident is still Open is the server's call: if an operator moved it on,
     * the push drops the edit and raises a Status Change Notice (ADR-0004)
     *
     * @throws ValidationException if the draft breaks a rule of [validate]; nothing is stored then
     */
    suspend fun edit(localId: Long, draft: IncidentDraft) {
        val invalidFields = validate(draft)
        if (invalidFields.isNotEmpty()) throw ValidationException(invalidFields)

        val incident = incidentRepository.getIncident(localId) ?: return
        val syncState = when (incident.summary.syncState) {
            SyncState.Synced, SyncState.PendingUpdate -> SyncState.PendingUpdate
            SyncState.PendingCreate -> SyncState.PendingCreate
            SyncState.PendingWithdraw -> return
        }
        incidentRepository.edit(localId, draft.trimmed(), syncState)
        syncScheduler.schedulePush()
    }

    /**
     * Withdraws the incident with [localId]: marks it as a pending withdrawal, which hides it from the list, and
     * schedules a push. A report that was never sent is then simply deleted; otherwise the server is told
     * (ADR-0003, ADR-0005). Withdrawing it again does nothing
     */
    suspend fun withdraw(localId: Long) {
        val incident = incidentRepository.getIncident(localId) ?: return
        if (incident.summary.syncState == SyncState.PendingWithdraw) return
        incidentRepository.withdraw(localId)
        syncScheduler.schedulePush()
    }

    /**
     * Clears the Status Change Notice of the incident with [localId], once the technician has read it
     */
    suspend fun dismissStatusChangeNotice(localId: Long) {
        incidentRepository.dismissStatusChangeNotice(localId)
    }

    /**
     * Returns the device's current position for a new report, or null if it isn't available
     */
    suspend fun capturePosition(): Position? = locationProvider.currentPosition()

    /**
     * Creates a path in the app's private photo storage for the camera to write a new photo to
     */
    suspend fun createPhotoFile(): Photo.Local = incidentRepository.createPhotoFile()

    /**
     * Copies the picture at [sourceUri], e.g. one picked from the gallery, into the app's private photo storage
     *
     * @return the copy, ready to attach to a draft
     */
    suspend fun importPhoto(sourceUri: String): Photo.Local = incidentRepository.importPhoto(sourceUri)

    /**
     * Deletes [photo], a file taken or imported for a draft that won't be saved with it
     */
    suspend fun discardPhoto(photo: Photo.Local) {
        incidentRepository.deletePhoto(photo)
    }

    private fun IncidentDraft.trimmed(): IncidentDraft = copy(
        title = title.trim(),
        description = description.trim(),
        locationDescription = locationDescription.trim(),
    )
}
