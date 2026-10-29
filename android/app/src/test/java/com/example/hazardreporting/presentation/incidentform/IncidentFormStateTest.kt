package com.example.hazardreporting.presentation.incidentform

import com.example.hazardreporting.domain.model.IncidentDraft
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.Position
import com.google.common.truth.Truth
import org.junit.Test

/**
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentFormStateTest {

    @Test
    fun testEncode_RoundTrip() {
        val decoded = IncidentFormState.decodeOrNull(STATE.encode())

        Truth.assertThat(decoded).isEqualTo(STATE)
    }

    @Test
    fun testDecodeOrNull_NothingSaved() {
        Truth.assertThat(IncidentFormState.decodeOrNull(null)).isNull()
        Truth.assertThat(IncidentFormState.decodeOrNull("")).isNull()
    }

    @Test
    fun testDecodeOrNull_Unreadable() {
        // A state this version can't read starts the form afresh
        Truth.assertThat(IncidentFormState.decodeOrNull("{\"fields\": 42}")).isNull()
    }

    @Test
    fun testToDraft() {
        Truth.assertThat(STATE.toDraft()).isEqualTo(
            IncidentDraft(
                title = "Leaking valve",
                description = "Dripping",
                categoryId = "cat-plumbing",
                severityId = "sev-medium",
                siteId = "site-depot",
                locationDescription = "Pump room",
                position = Position(53.5, -2.2, 12.0),
                photos = listOf(Photo.Local("/files/photos/a.jpg"), Photo.Remote("http://10.0.2.2:3001/uploads/b.png")),
            )
        )
    }

    @Test
    fun testHasUnsavedChanges() {
        Truth.assertThat(IncidentFormState().hasUnsavedChanges).isFalse()
        // Capturing the position or touching a field isn't a change the technician could lose
        Truth.assertThat(
            IncidentFormState(position = FormPosition(1.0, 2.0, 3.0), touchedFields = setOf(IncidentDraft.Field.Title))
                .hasUnsavedChanges
        ).isFalse()
        Truth.assertThat(IncidentFormState(fields = IncidentFormFields(title = "A")).hasUnsavedChanges).isTrue()
    }

    private companion object {
        val STATE = IncidentFormState(
            fields = IncidentFormFields(
                title = "Leaking valve",
                description = "Dripping",
                categoryId = "cat-plumbing",
                severityId = "sev-medium",
                siteId = "site-depot",
                locationDescription = "Pump room",
                photos = listOf(
                    FormPhoto.Local("/files/photos/a.jpg"),
                    FormPhoto.Remote("http://10.0.2.2:3001/uploads/b.png"),
                ),
            ),
            position = FormPosition(53.5, -2.2, 12.0),
            positionStatus = PositionStatus.Captured,
            touchedFields = setOf(IncidentDraft.Field.Title),
            isSaveAttempted = true,
            addedPhotoPaths = listOf("/files/photos/a.jpg"),
            pendingCameraPhotoPath = "/files/photos/c.jpg",
        )
    }
}
