package com.example.hazardreporting.presentation.incidentform

import com.example.hazardreporting.domain.model.Category
import com.example.hazardreporting.domain.model.IncidentDraft
import com.example.hazardreporting.domain.model.Photo
import com.example.hazardreporting.domain.model.Position
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.domain.model.Site

/**
 * State of the report form
 *
 * @property isEditing whether the form edits an existing incident rather than reporting a new one
 * @property title text of the title field
 * @property description text of the description field
 * @property categoryId id of the chosen category, or null
 * @property severityId id of the chosen severity, or null
 * @property siteId id of the chosen site, or null
 * @property locationDescription text of the location description field
 * @property photos the attached photos, in order
 * @property categories the categories to choose from
 * @property severities the severities to choose from, from Low to Critical
 * @property sites the sites to choose from
 * @property position the captured position, or null if there is none
 * @property positionStatus where capturing the position stands
 * @property errors invalid fields whose errors show: after the first Save, or once a field lost focus
 * @property isPhotoSourceSheetVisible whether the sheet that offers Camera and Gallery shows
 * @property isDiscardDialogVisible whether the "Discard changes?" dialog shows
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class IncidentFormUiState(
    val isEditing: Boolean = false,
    val title: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val severityId: String? = null,
    val siteId: String? = null,
    val locationDescription: String = "",
    val photos: List<Photo> = emptyList(),
    val categories: List<Category> = emptyList(),
    val severities: List<Severity> = emptyList(),
    val sites: List<Site> = emptyList(),
    val position: Position? = null,
    val positionStatus: PositionStatus = PositionStatus.Locating,
    val errors: Set<IncidentDraft.Field> = emptySet(),
    val isPhotoSourceSheetVisible: Boolean = false,
    val isDiscardDialogVisible: Boolean = false,
) {

    /**
     * Whether another photo may be attached
     */
    val canAddPhoto: Boolean
        get() = photos.size < IncidentDraft.MAX_PHOTOS
}
