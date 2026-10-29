package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dto.CreateIncidentRequestDto
import com.example.hazardreporting.data.incident.dto.IncidentDto
import com.example.hazardreporting.data.incident.dto.IncidentSummaryDto
import com.example.hazardreporting.data.incident.dto.PositionDto
import com.example.hazardreporting.data.incident.dto.UpdateIncidentRequestDto
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.data.incident.entity.IncidentPhotoEntity
import com.example.hazardreporting.data.incident.entity.IncidentWithPhotos
import com.example.hazardreporting.data.remote.BASE_URL
import com.example.hazardreporting.domain.model.Incident
import com.example.hazardreporting.domain.model.IncidentDetails
import com.example.hazardreporting.domain.model.IncidentDraft
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.Position
import com.example.hazardreporting.domain.model.Priority
import com.example.hazardreporting.domain.model.SyncState
import javax.inject.Inject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Converts incidents from the server's DTOs to Room entities, and from entities to domain models.
 * Photo URLs are stored as the server sends them, relative to it, and resolved against [BASE_URL]
 * only for the domain
 *
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentConverter @Inject constructor() {

    private val baseUrl = BASE_URL.toHttpUrl()

    /**
     * Converts a summary of an incident the device doesn't have yet to a new, synced row without details
     */
    fun convert(dto: IncidentSummaryDto): IncidentEntity = IncidentEntity(
        serverId = dto.id,
        title = dto.title,
        categoryId = dto.categoryId,
        severityId = dto.severityId,
        siteId = dto.siteId,
        status = dto.status,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        description = null,
        locationDescription = null,
        positionLat = null,
        positionLng = null,
        positionAccuracyM = null,
        priority = null,
        detailsLoaded = false,
        syncState = SyncState.Synced,
        localVersion = 0,
        // The incident came from the server, so its report obviously reached it
        createAttempted = true,
        statusChangeNoticePending = false,
    )

    /**
     * Returns [entity] with every summary field taken from [dto]. For a row without a pending change;
     * the details stay as they are until they are downloaded again (ADR-0006)
     */
    fun withSummary(entity: IncidentEntity, dto: IncidentSummaryDto): IncidentEntity = entity.copy(
        serverId = dto.id,
        title = dto.title,
        categoryId = dto.categoryId,
        severityId = dto.severityId,
        siteId = dto.siteId,
        status = dto.status,
        updatedAt = dto.updatedAt,
    )

    /**
     * Returns [entity] with only the status and update time taken from [dto]. For a row with a pending change,
     * whose report fields the server must not overwrite (ADR-0002). A row the server knows by its createdAt
     * but the device doesn't know by id yet also takes the server's id
     */
    fun withStatus(entity: IncidentEntity, dto: IncidentSummaryDto): IncidentEntity = entity.copy(
        serverId = entity.serverId ?: dto.id,
        status = dto.status,
        updatedAt = dto.updatedAt,
    )

    /**
     * Returns [entity] with only the status and update time taken from the full incident [dto].
     * For a row with a pending change, whose report fields the server must not overwrite (ADR-0002)
     */
    fun withStatus(entity: IncidentEntity, dto: IncidentDto): IncidentEntity = entity.copy(
        status = dto.status,
        updatedAt = dto.updatedAt,
    )

    /**
     * Returns [entity] with its summary and details taken from the full incident [dto], marked as having
     * its details. For a row without a pending change; the photos are converted by [convertPhotos]
     */
    fun withDetails(entity: IncidentEntity, dto: IncidentDto): IncidentEntity = entity.copy(
        serverId = dto.id,
        title = dto.title,
        categoryId = dto.categoryId,
        severityId = dto.severityId,
        siteId = dto.siteId,
        status = dto.status,
        updatedAt = dto.updatedAt,
        description = dto.description,
        locationDescription = dto.locationDescription,
        positionLat = dto.position?.lat,
        positionLng = dto.position?.lng,
        positionAccuracyM = dto.position?.accuracyM,
        priority = dto.priority,
        detailsLoaded = true,
    )

    /**
     * Returns [entity] marked as synced after the server accepted its pending change and answered with [dto]:
     * it takes the server's id, status, priority and update time. The report fields stay as they are,
     * since they are what the server accepted
     */
    fun withAccepted(entity: IncidentEntity, dto: IncidentDto): IncidentEntity = entity.copy(
        serverId = dto.id,
        status = dto.status,
        updatedAt = dto.updatedAt,
        priority = dto.priority,
        syncState = SyncState.Synced,
    )

    /**
     * Returns [entity] with the server's id, status and update time taken from [dto], keeping its sync state.
     * For a row whose pending change the server accepted while it was changed again on the device:
     * the newer change stays pending
     */
    fun withServerState(entity: IncidentEntity, dto: IncidentDto): IncidentEntity = entity.copy(
        serverId = dto.id,
        status = dto.status,
        updatedAt = dto.updatedAt,
    )

    /**
     * Returns [entity] overwritten with the server's version of the incident, [dto], with its pending change
     * dropped (ADR-0004). The photos are converted by [convertPhotos]
     *
     * @param hasStatusChangeNotice whether the technician must be told that their change wasn't applied
     */
    fun withServerVersion(entity: IncidentEntity, dto: IncidentDto, hasStatusChangeNotice: Boolean): IncidentEntity =
        withDetails(entity, dto).copy(
            syncState = SyncState.Synced,
            statusChangeNoticePending = hasStatusChangeNotice,
        )

    /**
     * Converts a stored report to the body of its `POST /incidents`, with [photoUrls] as its uploaded photos
     */
    fun convertCreateRequest(entity: IncidentEntity, photoUrls: List<String>): CreateIncidentRequestDto =
        CreateIncidentRequestDto(
            title = entity.title,
            description = entity.description.orEmpty(),
            categoryId = entity.categoryId,
            severityId = entity.severityId,
            siteId = entity.siteId,
            locationDescription = entity.locationDescription.orEmpty(),
            position = convertPositionDto(entity),
            photoUrls = photoUrls,
            createdAt = entity.createdAt,
        )

    /**
     * Converts a stored, edited incident to the body of its `PATCH /incidents/:id`, with [photoUrls] as its
     * uploaded photos
     */
    fun convertUpdateRequest(entity: IncidentEntity, photoUrls: List<String>): UpdateIncidentRequestDto =
        UpdateIncidentRequestDto(
            title = entity.title,
            description = entity.description.orEmpty(),
            categoryId = entity.categoryId,
            severityId = entity.severityId,
            siteId = entity.siteId,
            locationDescription = entity.locationDescription.orEmpty(),
            position = convertPositionDto(entity),
            photoUrls = photoUrls,
        )

    /**
     * Converts the photo URLs of the full incident [dto] to the photo rows of the incident with [incidentLocalId],
     * keeping their order
     */
    fun convertPhotos(incidentLocalId: Long, dto: IncidentDto): List<IncidentPhotoEntity> =
        dto.photoUrls.mapIndexed { position, url ->
            IncidentPhotoEntity(
                incidentLocalId = incidentLocalId,
                position = position,
                localPath = null,
                remoteUrl = url,
            )
        }

    /**
     * Converts a valid [draft] to the row of a new pending report created at [createdAt]. Its details are
     * all on the device, so they count as downloaded
     *
     * @throws IllegalStateException if the draft has no category, severity or site, which validation rules out
     */
    fun convertNew(draft: IncidentDraft, createdAt: Long): IncidentEntity = IncidentEntity(
        serverId = null,
        title = draft.title,
        categoryId = checkNotNull(draft.categoryId) { "A report needs a category" },
        severityId = checkNotNull(draft.severityId) { "A report needs a severity" },
        siteId = checkNotNull(draft.siteId) { "A report needs a site" },
        status = STATUS_OPEN,
        createdAt = createdAt,
        updatedAt = null,
        description = draft.description,
        locationDescription = draft.locationDescription,
        positionLat = draft.position?.lat,
        positionLng = draft.position?.lng,
        positionAccuracyM = draft.position?.accuracyM,
        priority = null,
        detailsLoaded = true,
        syncState = SyncState.PendingCreate,
        localVersion = 0,
        createAttempted = false,
        statusChangeNoticePending = false,
    )

    /**
     * Returns [entity] with the report fields of the valid, edited [draft]. Its createdAt and sync bookkeeping
     * stay as they are
     *
     * @throws IllegalStateException if the draft has no category, severity or site, which validation rules out
     */
    fun withDraft(entity: IncidentEntity, draft: IncidentDraft): IncidentEntity = entity.copy(
        title = draft.title,
        categoryId = checkNotNull(draft.categoryId) { "A report needs a category" },
        severityId = checkNotNull(draft.severityId) { "A report needs a severity" },
        siteId = checkNotNull(draft.siteId) { "A report needs a site" },
        description = draft.description,
        locationDescription = draft.locationDescription,
        positionLat = draft.position?.lat,
        positionLng = draft.position?.lng,
        positionAccuracyM = draft.position?.accuracyM,
    )

    /**
     * Converts a draft's [photos] to the photo rows of the incident with [incidentLocalId], keeping their order.
     * A remote photo's URL is stored relative to the server again, as the server sent it. A local photo that is
     * among the incident's [storedPhotos] keeps the server URL it got meanwhile, e.g. when an edit form opened
     * before its upload is saved after it, so it is never uploaded twice nor looked for in its deleted file
     */
    fun convertPhotos(
        incidentLocalId: Long,
        photos: List<Photo>,
        storedPhotos: List<IncidentPhotoEntity> = emptyList(),
    ): List<IncidentPhotoEntity> = photos.mapIndexed { position, photo ->
        when (photo) {
            is Photo.Local -> IncidentPhotoEntity(
                incidentLocalId = incidentLocalId,
                position = position,
                localPath = photo.path,
                remoteUrl = storedPhotos.find { it.localPath == photo.path }?.remoteUrl,
            )
            is Photo.Remote -> IncidentPhotoEntity(
                incidentLocalId = incidentLocalId,
                position = position,
                localPath = null,
                remoteUrl = relativize(photo.url),
            )
        }
    }

    /**
     * Converts a stored incident to its domain [IncidentSummary]
     */
    fun convertSummary(entity: IncidentEntity): IncidentSummary = IncidentSummary(
        localId = entity.localId,
        serverId = entity.serverId,
        title = entity.title,
        categoryId = entity.categoryId,
        severityId = entity.severityId,
        siteId = entity.siteId,
        status = convertStatus(entity.status),
        createdAt = entity.createdAt,
        syncState = entity.syncState,
        hasStatusChangeNotice = entity.statusChangeNoticePending,
    )

    /**
     * Converts a stored incident and its photos to a domain [Incident], whose details are null
     * until they are downloaded
     */
    fun convert(incidentWithPhotos: IncidentWithPhotos): Incident {
        val entity = incidentWithPhotos.incident
        val details = if (entity.detailsLoaded) {
            IncidentDetails(
                description = entity.description.orEmpty(),
                locationDescription = entity.locationDescription.orEmpty(),
                position = convertPosition(entity),
                photos = incidentWithPhotos.photos.sortedBy { it.position }.mapNotNull { convertPhoto(it) },
                priority = entity.priority?.let { convertPriority(it) },
            )
        } else {
            null
        }
        return Incident(summary = convertSummary(entity), details = details)
    }

    /**
     * Converts a stored photo to a domain [Photo]: its server URL, resolved to an absolute one, once it is
     * uploaded (its local file is deleted then), otherwise its local file. Returns null for a row with neither
     */
    fun convertPhoto(entity: IncidentPhotoEntity): Photo? = when {
        entity.remoteUrl != null -> Photo.Remote(resolve(entity.remoteUrl))
        entity.localPath != null -> Photo.Local(entity.localPath)
        else -> null
    }

    private fun convertPosition(entity: IncidentEntity): Position? {
        val lat = entity.positionLat ?: return null
        val lng = entity.positionLng ?: return null
        val accuracyM = entity.positionAccuracyM ?: return null
        return Position(lat, lng, accuracyM)
    }

    private fun convertPositionDto(entity: IncidentEntity): PositionDto? {
        val lat = entity.positionLat ?: return null
        val lng = entity.positionLng ?: return null
        val accuracyM = entity.positionAccuracyM ?: return null
        return PositionDto(lat, lng, accuracyM)
    }

    private fun resolve(url: String): String = baseUrl.resolve(url)?.toString() ?: url

    private fun relativize(url: String): String {
        val absolute = url.toHttpUrlOrNull() ?: return url
        val isOnServer = absolute.scheme == baseUrl.scheme && absolute.host == baseUrl.host &&
            absolute.port == baseUrl.port
        return if (isOnServer) absolute.encodedPath else url
    }

    private fun convertStatus(status: String): IncidentStatus = when (status) {
        STATUS_OPEN -> IncidentStatus.Open
        STATUS_ACKNOWLEDGED -> IncidentStatus.Acknowledged
        STATUS_IN_PROGRESS -> IncidentStatus.InProgress
        STATUS_RESOLVED -> IncidentStatus.Resolved
        STATUS_REJECTED -> IncidentStatus.Rejected
        STATUS_WITHDRAWN -> IncidentStatus.Withdrawn
        else -> IncidentStatus.Unknown
    }

    private fun convertPriority(priority: String): Priority = when (priority) {
        PRIORITY_LOW -> Priority.Low
        PRIORITY_MEDIUM -> Priority.Medium
        PRIORITY_HIGH -> Priority.High
        PRIORITY_URGENT -> Priority.Urgent
        else -> Priority.Unknown
    }

    private companion object {
        const val STATUS_OPEN = "open"
        const val STATUS_ACKNOWLEDGED = "acknowledged"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_RESOLVED = "resolved"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_WITHDRAWN = "withdrawn"

        const val PRIORITY_LOW = "low"
        const val PRIORITY_MEDIUM = "medium"
        const val PRIORITY_HIGH = "high"
        const val PRIORITY_URGENT = "urgent"
    }
}
