package com.example.hazardreporting.data.incident

import com.example.hazardreporting.data.incident.dto.IncidentSummaryDto
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.domain.model.IncidentStatus
import com.example.hazardreporting.domain.model.IncidentSummary
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

    private companion object {
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
