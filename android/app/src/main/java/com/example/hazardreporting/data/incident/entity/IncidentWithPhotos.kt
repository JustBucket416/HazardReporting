package com.example.hazardreporting.data.incident.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * An incident's row together with the rows of its photos, read in one Room transaction
 *
 * @property incident the incident's row
 * @property photos the rows of its photos, in no particular order
 *
 * @author Roman Pliskin on 29.10.2026
 */
data class IncidentWithPhotos(
    @Embedded
    val incident: IncidentEntity,
    @Relation(parentColumn = "localId", entityColumn = "incidentLocalId")
    val photos: List<IncidentPhotoEntity>,
)
