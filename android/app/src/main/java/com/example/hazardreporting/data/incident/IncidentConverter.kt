package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dto.IncidentSummaryDto
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.SyncState
import javax.inject.Inject

/**
 * Converts incidents from the server's DTOs to Room entities, and from entities to domain models
 *
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentConverter @Inject constructor() {

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

    private fun convertStatus(status: String): IncidentStatus = when (status) {
        STATUS_OPEN -> IncidentStatus.Open
        STATUS_ACKNOWLEDGED -> IncidentStatus.Acknowledged
        STATUS_IN_PROGRESS -> IncidentStatus.InProgress
        STATUS_RESOLVED -> IncidentStatus.Resolved
        STATUS_REJECTED -> IncidentStatus.Rejected
        STATUS_WITHDRAWN -> IncidentStatus.Withdrawn
        else -> IncidentStatus.Unknown
    }

    private companion object {
        const val STATUS_OPEN = "open"
        const val STATUS_ACKNOWLEDGED = "acknowledged"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_RESOLVED = "resolved"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_WITHDRAWN = "withdrawn"
    }
}
