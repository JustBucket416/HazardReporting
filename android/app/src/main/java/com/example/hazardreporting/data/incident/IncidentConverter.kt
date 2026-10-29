package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dto.IncidentDto
import com.example.hazardreporting.data.incident.dto.IncidentSummaryDto
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.data.incident.entity.IncidentPhotoEntity
import com.example.hazardreporting.data.incident.entity.IncidentWithPhotos
import com.example.hazardreporting.data.remote.BaseUrl
import com.example.hazardreporting.domain.model.Incident
import com.example.hazardreporting.domain.model.IncidentDetails
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.Position
import com.example.hazardreporting.domain.model.Priority
import com.example.hazardreporting.domain.model.SyncState
import javax.inject.Inject
import okhttp3.HttpUrl

/**
 * Converts incidents from the server's DTOs to Room entities, and from entities to domain models.
 * Photo URLs are stored as the server sends them, relative to it, and resolved against [baseUrl]
 * only for the domain
 *
 * @property baseUrl the server's root, which relative photo URLs are resolved against
 *
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentConverter @Inject constructor(
    @param:BaseUrl private val baseUrl: HttpUrl,
) {

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
     * Converts a stored photo to a domain [Photo]: its local file while it has one, otherwise its server URL
     * resolved to an absolute one. Returns null for a row with neither
     */
    fun convertPhoto(entity: IncidentPhotoEntity): Photo? = when {
        entity.localPath != null -> Photo.Local(entity.localPath)
        entity.remoteUrl != null -> Photo.Remote(resolve(entity.remoteUrl))
        else -> null
    }

    private fun convertPosition(entity: IncidentEntity): Position? {
        val lat = entity.positionLat ?: return null
        val lng = entity.positionLng ?: return null
        val accuracyM = entity.positionAccuracyM ?: return null
        return Position(lat, lng, accuracyM)
    }

    private fun resolve(url: String): String = baseUrl.resolve(url)?.toString() ?: url

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
