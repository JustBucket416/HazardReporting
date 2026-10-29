package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dto.IncidentDto
import com.example.hazardreporting.data.incident.dto.IncidentSummaryDto
import com.example.hazardreporting.data.incident.dto.PositionDto
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.data.incident.entity.IncidentPhotoEntity
import com.example.hazardreporting.data.incident.entity.IncidentWithPhotos
import com.example.hazardreporting.domain.model.Incident
import com.example.hazardreporting.domain.model.IncidentDetails
import com.example.hazardreporting.domain.model.IncidentDraft
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.IncidentSummary
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.Position
import com.example.hazardreporting.domain.model.Priority
import com.example.hazardreporting.domain.model.SyncState
import com.google.common.truth.Truth
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentConverterTest {

    private val converter = IncidentConverter(BASE_URL.toHttpUrl())

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
    fun testConvertIncidentWithPhotos_DetailsLoaded() {
        val incident = converter.convert(
            IncidentWithPhotos(
                incident = STORED_ENTITY,
                photos = listOf(
                    IncidentPhotoEntity(2, LOCAL_ID, position = 1, localPath = "/files/photos/b.jpg", remoteUrl = null),
                    IncidentPhotoEntity(1, LOCAL_ID, position = 0, localPath = null, remoteUrl = "/uploads/a.jpg"),
                ),
            )
        )

        Truth.assertThat(incident).isEqualTo(
            Incident(
                summary = converter.convertSummary(STORED_ENTITY),
                details = IncidentDetails(
                    description = "Fraying near the transfer point",
                    locationDescription = "Line 3",
                    position = Position(53.5, -2.2, 10.0),
                    // In their stored order, with relative URLs resolved against the server's root
                    photos = listOf(
                        Photo.Remote("http://10.0.2.2:3001/uploads/a.jpg"),
                        Photo.Local("/files/photos/b.jpg"),
                    ),
                    priority = Priority.High,
                ),
            )
        )
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
    fun testConvertPhoto_RelativeUrl() {
        val photo = converter.convertPhoto(photoRow(localPath = null, remoteUrl = "/uploads/seed/photo-05.png"))

        Truth.assertThat(photo).isEqualTo(Photo.Remote("http://10.0.2.2:3001/uploads/seed/photo-05.png"))
    }

    @Test
    fun testConvertPhoto_AbsoluteUrl() {
        val photo = converter.convertPhoto(photoRow(localPath = null, remoteUrl = "https://cdn.example.com/a.jpg"))

        Truth.assertThat(photo).isEqualTo(Photo.Remote("https://cdn.example.com/a.jpg"))
    }

    @Test
    fun testConvertPhoto_LocalFileWins() {
        // Just after an upload a photo has both; the file is used until it is deleted
        val photo = converter.convertPhoto(photoRow(localPath = "/files/photos/a.jpg", remoteUrl = REMOTE_A))

        Truth.assertThat(photo).isEqualTo(Photo.Local("/files/photos/a.jpg"))
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
    fun testConvertDraftPhotos() {
        val photos = converter.convertPhotos(
            LOCAL_ID,
            listOf(
                Photo.Local("/files/photos/a.jpg"),
                Photo.Remote("http://10.0.2.2:3001/uploads/b.png"),
                Photo.Remote("https://cdn.example.com/c.jpg"),
            ),
        )

        // A server photo is stored relative again, as the server sent it; any other URL stays as it is
        Truth.assertThat(photos).containsExactly(
            IncidentPhotoEntity(
                incidentLocalId = LOCAL_ID,
                position = 0,
                localPath = "/files/photos/a.jpg",
                remoteUrl = null,
            ),
            IncidentPhotoEntity(incidentLocalId = LOCAL_ID, position = 1, localPath = null, remoteUrl = REMOTE_B),
            IncidentPhotoEntity(
                incidentLocalId = LOCAL_ID,
                position = 2,
                localPath = null,
                remoteUrl = "https://cdn.example.com/c.jpg",
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
        const val BASE_URL = "http://10.0.2.2:3001/"
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
