package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dto.CreateIncidentRequestDto
import com.example.hazardreporting.data.incident.dto.IncidentDto
import com.example.hazardreporting.data.incident.dto.IncidentSummaryDto
import com.example.hazardreporting.data.incident.dto.PositionDto
import com.example.hazardreporting.data.incident.dto.UpdateIncidentRequestDto
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.data.incident.entity.IncidentPhotoEntity
import com.example.hazardreporting.data.incident.entity.IncidentWithPhotos
import com.example.hazardreporting.domain.model.Incident
import com.example.hazardreporting.domain.model.IncidentDraft
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.Position
import com.example.hazardreporting.domain.model.Priority
import com.example.hazardreporting.domain.model.SyncState
import com.google.common.truth.Truth
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentConverterTest {

    private val converter = IncidentConverter()

    @Test
    fun testConvertSummaryDto() {
        val entity = converter.convert(SUMMARY_DTO)

        Truth.assertThat(entity).isEqualTo(
            IncidentEntity(
                localId = 0,
                serverId = "inc-1",
                title = "Conveyor belt fraying",
                categoryId = "cat-equipment",
                severityId = "sev-high",
                siteId = "site-north",
                status = "open",
                createdAt = CREATED_AT,
                updatedAt = UPDATED_AT,
                description = null,
                locationDescription = null,
                positionLat = null,
                positionLng = null,
                positionAccuracyM = null,
                priority = null,
                detailsLoaded = false,
                syncState = SyncState.Synced,
                localVersion = 0,
                createAttempted = true,
                statusChangeNoticePending = false,
            )
        )
    }

    @Test
    fun testWithSummary_TakesSummaryFieldsAndKeepsDetails() {
        val entity = converter.withSummary(STORED_ENTITY, CHANGED_SUMMARY_DTO)

        Truth.assertThat(entity).isEqualTo(
            STORED_ENTITY.copy(
                title = "Changed title",
                categoryId = "cat-electrical",
                severityId = "sev-low",
                siteId = "site-depot",
                status = "acknowledged",
                updatedAt = CHANGED_UPDATED_AT,
            )
        )
    }

    @Test
    fun testWithStatus_KeepsReportFields() {
        val pending = STORED_ENTITY.copy(syncState = SyncState.PendingUpdate, localVersion = 3)

        val entity = converter.withStatus(pending, CHANGED_SUMMARY_DTO)

        // Only the status and update time come from the server; the pending report fields stay
        Truth.assertThat(entity).isEqualTo(pending.copy(status = "acknowledged", updatedAt = CHANGED_UPDATED_AT))
    }

    @Test
    fun testWithStatus_TakesServerIdOfUnidentifiedRow() {
        val unidentified = STORED_ENTITY.copy(serverId = null, syncState = SyncState.PendingCreate)

        val entity = converter.withStatus(unidentified, CHANGED_SUMMARY_DTO)

        Truth.assertThat(entity).isEqualTo(
            unidentified.copy(serverId = "inc-1", status = "acknowledged", updatedAt = CHANGED_UPDATED_AT)
        )
    }

    @Test
    fun testConvertSummary() {
        val summary = converter.convertSummary(STORED_ENTITY.copy(statusChangeNoticePending = true))

        Truth.assertThat(summary).isEqualTo(
            IncidentSummary(
                localId = LOCAL_ID,
                serverId = "inc-1",
                title = "Conveyor belt fraying",
                categoryId = "cat-equipment",
                severityId = "sev-high",
                siteId = "site-north",
                status = IncidentStatus.Open,
                createdAt = CREATED_AT,
                syncState = SyncState.Synced,
                hasStatusChangeNotice = true,
            )
        )
    }

    @Test
    fun testConvertSummary_Statuses() {
        val statuses = mapOf(
            "open" to IncidentStatus.Open,
            "acknowledged" to IncidentStatus.Acknowledged,
            "in_progress" to IncidentStatus.InProgress,
            "resolved" to IncidentStatus.Resolved,
            "rejected" to IncidentStatus.Rejected,
            "withdrawn" to IncidentStatus.Withdrawn,
            "escalated" to IncidentStatus.Unknown,
        )

        statuses.forEach { (wireStatus, status) ->
            Truth.assertThat(converter.convertSummary(STORED_ENTITY.copy(status = wireStatus)).status).isEqualTo(status)
        }
    }

    @Test
    fun testWithStatus_FullIncidentKeepsReportFields() {
        val pending = STORED_ENTITY.copy(syncState = SyncState.PendingUpdate, localVersion = 3)

        val entity = converter.withStatus(pending, INCIDENT_DTO)

        Truth.assertThat(entity).isEqualTo(pending.copy(status = "acknowledged", updatedAt = CHANGED_UPDATED_AT))
    }

    @Test
    fun testWithDetails() {
        val summaryOnly = converter.convert(SUMMARY_DTO).copy(localId = LOCAL_ID)

        val entity = converter.withDetails(summaryOnly, INCIDENT_DTO)

        Truth.assertThat(entity).isEqualTo(
            summaryOnly.copy(
                title = "Changed title",
                categoryId = "cat-electrical",
                severityId = "sev-low",
                siteId = "site-depot",
                status = "acknowledged",
                updatedAt = CHANGED_UPDATED_AT,
                description = "Fraying near the transfer point",
                locationDescription = "Line 3",
                positionLat = 53.5,
                positionLng = -2.2,
                positionAccuracyM = 10.0,
                priority = "urgent",
                detailsLoaded = true,
            )
        )
    }

    @Test
    fun testWithDetails_WithoutPosition() {
        val entity = converter.withDetails(STORED_ENTITY, INCIDENT_DTO.copy(position = null, priority = null))

        Truth.assertThat(entity.positionLat).isNull()
        Truth.assertThat(entity.positionLng).isNull()
        Truth.assertThat(entity.positionAccuracyM).isNull()
        Truth.assertThat(entity.priority).isNull()
    }

    @Test
    fun testConvertPhotos() {
        val photos = converter.convertPhotos(LOCAL_ID, INCIDENT_DTO)

        // Stored as the server sends them, relative, so they can be sent back as they are
        Truth.assertThat(photos).containsExactly(
            IncidentPhotoEntity(incidentLocalId = LOCAL_ID, position = 0, localPath = null, remoteUrl = REMOTE_A),
            IncidentPhotoEntity(incidentLocalId = LOCAL_ID, position = 1, localPath = null, remoteUrl = REMOTE_B),
        ).inOrder()
    }

    @Test
    fun testConvertIncidentWithPhotos_DetailsNotLoaded() {
        val summaryOnly = converter.convert(SUMMARY_DTO).copy(localId = LOCAL_ID)

        val incident = converter.convert(IncidentWithPhotos(summaryOnly, photos = emptyList()))

        Truth.assertThat(incident).isEqualTo(Incident(converter.convertSummary(summaryOnly), details = null))
    }

    @Test
    fun testConvertIncidentWithPhotos_WithoutPositionAndPriority() {
        val entity = STORED_ENTITY.copy(
            positionLat = null,
            positionLng = null,
            positionAccuracyM = null,
            priority = null,
        )

        val details = converter.convert(IncidentWithPhotos(entity, photos = emptyList())).details

        Truth.assertThat(details?.position).isNull()
        Truth.assertThat(details?.priority).isNull()
    }

    @Test
    fun testConvertIncidentWithPhotos_Priorities() {
        val priorities = mapOf(
            "low" to Priority.Low,
            "medium" to Priority.Medium,
            "high" to Priority.High,
            "urgent" to Priority.Urgent,
            "asap" to Priority.Unknown,
        )

        priorities.forEach { (wirePriority, priority) ->
            val withPriority = IncidentWithPhotos(STORED_ENTITY.copy(priority = wirePriority), photos = emptyList())
            Truth.assertThat(converter.convert(withPriority).details?.priority).isEqualTo(priority)
        }
    }

    @Test
    fun testConvertPhoto_AbsoluteUrl() {
        val photo = converter.convertPhoto(photoRow(localPath = null, remoteUrl = "https://cdn.example.com/a.jpg"))

        Truth.assertThat(photo).isEqualTo(Photo.Remote("https://cdn.example.com/a.jpg"))
    }

    @Test
    fun testConvertPhoto_NotUploaded() {
        val photo = converter.convertPhoto(photoRow(localPath = "/files/photos/a.jpg", remoteUrl = null))

        Truth.assertThat(photo).isEqualTo(Photo.Local("/files/photos/a.jpg"))
    }

    @Test
    fun testWithAccepted() {
        val sent = STORED_ENTITY.copy(
            serverId = null,
            status = "open",
            updatedAt = null,
            priority = null,
            syncState = SyncState.PendingCreate,
            localVersion = 2,
        )

        val entity = converter.withAccepted(sent, INCIDENT_DTO)

        // Synced with the server's id, status, priority and update time; the report fields are what was sent
        Truth.assertThat(entity).isEqualTo(
            sent.copy(
                serverId = "inc-1",
                status = "acknowledged",
                updatedAt = CHANGED_UPDATED_AT,
                priority = "urgent",
                syncState = SyncState.Synced,
            )
        )
    }

    @Test
    fun testWithServerState_KeepsPendingChange() {
        val changed = STORED_ENTITY.copy(serverId = null, syncState = SyncState.PendingCreate, localVersion = 3)

        val entity = converter.withServerState(changed, INCIDENT_DTO)

        Truth.assertThat(entity).isEqualTo(
            changed.copy(serverId = "inc-1", status = "acknowledged", updatedAt = CHANGED_UPDATED_AT)
        )
    }

    @Test
    fun testWithServerVersion() {
        val pending = STORED_ENTITY.copy(syncState = SyncState.PendingUpdate, localVersion = 4)

        val entity = converter.withServerVersion(pending, INCIDENT_DTO, hasStatusChangeNotice = true)

        // The pending change is dropped: every field is the server's, and the notice is raised
        Truth.assertThat(entity).isEqualTo(
            converter.withDetails(pending, INCIDENT_DTO).copy(
                syncState = SyncState.Synced,
                statusChangeNoticePending = true,
            )
        )
        Truth.assertThat(entity.title).isEqualTo("Changed title")
        Truth.assertThat(entity.localVersion).isEqualTo(4)
    }

    @Test
    fun testWithServerVersion_WithoutNotice() {
        val pending = STORED_ENTITY.copy(syncState = SyncState.PendingWithdraw)

        val entity = converter.withServerVersion(pending, INCIDENT_DTO, hasStatusChangeNotice = false)

        Truth.assertThat(entity.syncState).isEqualTo(SyncState.Synced)
        Truth.assertThat(entity.statusChangeNoticePending).isFalse()
    }

    @Test
    fun testConvertCreateRequest() {
        val request = converter.convertCreateRequest(STORED_ENTITY, listOf(REMOTE_A, REMOTE_B))

        Truth.assertThat(request).isEqualTo(
            CreateIncidentRequestDto(
                title = "Conveyor belt fraying",
                description = "Fraying near the transfer point",
                categoryId = "cat-equipment",
                severityId = "sev-high",
                siteId = "site-north",
                locationDescription = "Line 3",
                position = PositionDto(53.5, -2.2, 10.0),
                photoUrls = listOf(REMOTE_A, REMOTE_B),
                createdAt = CREATED_AT,
            )
        )
    }

    @Test
    fun testConvertCreateRequest_WithoutPositionOrTexts() {
        val entity = STORED_ENTITY.copy(
            description = null,
            locationDescription = null,
            positionLat = null,
            positionLng = null,
            positionAccuracyM = null,
        )

        val request = converter.convertCreateRequest(entity, listOf(REMOTE_A))

        Truth.assertThat(request.description).isEmpty()
        Truth.assertThat(request.locationDescription).isEmpty()
        Truth.assertThat(request.position).isNull()
    }

    @Test
    fun testConvertUpdateRequest() {
        val request = converter.convertUpdateRequest(STORED_ENTITY, listOf(REMOTE_B))

        // Every report field the technician owns, and no createdAt
        Truth.assertThat(request).isEqualTo(
            UpdateIncidentRequestDto(
                title = "Conveyor belt fraying",
                description = "Fraying near the transfer point",
                categoryId = "cat-equipment",
                severityId = "sev-high",
                siteId = "site-north",
                locationDescription = "Line 3",
                position = PositionDto(53.5, -2.2, 10.0),
                photoUrls = listOf(REMOTE_B),
            )
        )
    }

    @Test
    fun testConvertUpdateRequest_WithoutPosition() {
        val entity = STORED_ENTITY.copy(positionLat = null, positionLng = null, positionAccuracyM = null)

        Truth.assertThat(converter.convertUpdateRequest(entity, listOf(REMOTE_A)).position).isNull()
    }

    @Test
    fun testConvertPhoto_Neither() {
        val photo = converter.convertPhoto(photoRow(localPath = null, remoteUrl = null))

        Truth.assertThat(photo).isNull()
    }

    @Test
    fun testConvertNew() {
        val draft = IncidentDraft(
            title = "Leaking valve",
            description = "Dripping steadily",
            categoryId = "cat-plumbing",
            severityId = "sev-medium",
            siteId = "site-depot",
            locationDescription = "Pump room B2",
            position = Position(53.5, -2.2, 12.0),
            photos = listOf(Photo.Local("/files/photos/a.jpg")),
        )

        val entity = converter.convertNew(draft, createdAt = CREATED_AT)

        Truth.assertThat(entity).isEqualTo(
            IncidentEntity(
                localId = 0,
                serverId = null,
                title = "Leaking valve",
                categoryId = "cat-plumbing",
                severityId = "sev-medium",
                siteId = "site-depot",
                status = "open",
                createdAt = CREATED_AT,
                updatedAt = null,
                description = "Dripping steadily",
                locationDescription = "Pump room B2",
                positionLat = 53.5,
                positionLng = -2.2,
                positionAccuracyM = 12.0,
                priority = null,
                // Every detail of a new report is on the device
                detailsLoaded = true,
                syncState = SyncState.PendingCreate,
                localVersion = 0,
                createAttempted = false,
                statusChangeNoticePending = false,
            )
        )
    }

    @Test
    fun testConvertNew_WithoutPosition() {
        val draft = IncidentDraft("Title", "", "cat", "sev", "site", "", position = null, photos = emptyList())

        val entity = converter.convertNew(draft, createdAt = CREATED_AT)

        Truth.assertThat(entity.positionLat).isNull()
        Truth.assertThat(entity.positionLng).isNull()
        Truth.assertThat(entity.positionAccuracyM).isNull()
    }

    @Test
    fun testWithDraft() {
        val pending = STORED_ENTITY.copy(syncState = SyncState.PendingCreate, localVersion = 2)
        val draft = IncidentDraft(
            title = "Leaking valve",
            description = "Dripping steadily",
            categoryId = "cat-plumbing",
            severityId = "sev-medium",
            siteId = "site-depot",
            locationDescription = "Pump room B2",
            position = Position(53.6, -2.3, 5.0),
            photos = emptyList(),
        )

        val entity = converter.withDraft(pending, draft)

        // The report fields change; createdAt and the sync bookkeeping stay
        Truth.assertThat(entity).isEqualTo(
            pending.copy(
                title = "Leaking valve",
                categoryId = "cat-plumbing",
                severityId = "sev-medium",
                siteId = "site-depot",
                description = "Dripping steadily",
                locationDescription = "Pump room B2",
                positionLat = 53.6,
                positionLng = -2.3,
                positionAccuracyM = 5.0,
            )
        )
    }

    @Test
    fun testWithDraft_WithoutPosition() {
        val draft = IncidentDraft("Title", "", "cat", "sev", "site", "", position = null, photos = emptyList())

        val entity = converter.withDraft(STORED_ENTITY, draft)

        Truth.assertThat(entity.positionLat).isNull()
        Truth.assertThat(entity.positionLng).isNull()
        Truth.assertThat(entity.positionAccuracyM).isNull()
    }

    @Test
    fun testConvertDraftPhotos_KeepsUrlsUploadedMeanwhile() {
        val stored = listOf(
            photoRow(localPath = "/files/photos/a.jpg", remoteUrl = REMOTE_A),
            photoRow(localPath = "/files/photos/c.jpg", remoteUrl = null),
        )

        val photos = converter.convertPhotos(
            LOCAL_ID,
            listOf(Photo.Local("/files/photos/a.jpg"), Photo.Local("/files/photos/b.jpg")),
            stored,
        )

        // Opened before the upload, saved after it: the uploaded photo keeps its URL; a new one has none yet
        Truth.assertThat(photos).containsExactly(
            IncidentPhotoEntity(
                incidentLocalId = LOCAL_ID,
                position = 0,
                localPath = "/files/photos/a.jpg",
                remoteUrl = REMOTE_A,
            ),
            IncidentPhotoEntity(
                incidentLocalId = LOCAL_ID,
                position = 1,
                localPath = "/files/photos/b.jpg",
                remoteUrl = null,
            ),
        ).inOrder()
    }

    private fun photoRow(localPath: String?, remoteUrl: String?) = IncidentPhotoEntity(
        id = 1,
        incidentLocalId = LOCAL_ID,
        position = 0,
        localPath = localPath,
        remoteUrl = remoteUrl,
    )

    private companion object {
        const val REMOTE_A = "/uploads/a.jpg"
        const val REMOTE_B = "/uploads/b.png"
        const val LOCAL_ID = 7L
        const val CREATED_AT = 1_793_015_100_000L
        const val UPDATED_AT = 1_793_015_160_000L
        const val CHANGED_UPDATED_AT = 1_793_020_000_000L

        val SUMMARY_DTO = IncidentSummaryDto(
            id = "inc-1",
            title = "Conveyor belt fraying",
            categoryId = "cat-equipment",
            severityId = "sev-high",
            siteId = "site-north",
            status = "open",
            createdAt = CREATED_AT,
            updatedAt = UPDATED_AT,
        )

        val CHANGED_SUMMARY_DTO = IncidentSummaryDto(
            id = "inc-1",
            title = "Changed title",
            categoryId = "cat-electrical",
            severityId = "sev-low",
            siteId = "site-depot",
            status = "acknowledged",
            createdAt = CREATED_AT,
            updatedAt = CHANGED_UPDATED_AT,
        )

        val INCIDENT_DTO = IncidentDto(
            id = "inc-1",
            title = "Changed title",
            description = "Fraying near the transfer point",
            categoryId = "cat-electrical",
            severityId = "sev-low",
            siteId = "site-depot",
            locationDescription = "Line 3",
            position = PositionDto(53.5, -2.2, 10.0),
            photoUrls = listOf("/uploads/a.jpg", "/uploads/b.png"),
            status = "acknowledged",
            priority = "urgent",
            createdAt = CREATED_AT,
            updatedAt = CHANGED_UPDATED_AT,
        )

        val STORED_ENTITY = IncidentEntity(
            localId = LOCAL_ID,
            serverId = "inc-1",
            title = "Conveyor belt fraying",
            categoryId = "cat-equipment",
            severityId = "sev-high",
            siteId = "site-north",
            status = "open",
            createdAt = CREATED_AT,
            updatedAt = UPDATED_AT,
            description = "Fraying near the transfer point",
            locationDescription = "Line 3",
            positionLat = 53.5,
            positionLng = -2.2,
            positionAccuracyM = 10.0,
            priority = "high",
            detailsLoaded = true,
            syncState = SyncState.Synced,
            localVersion = 0,
            createAttempted = true,
            statusChangeNoticePending = false,
        )
    }
}
