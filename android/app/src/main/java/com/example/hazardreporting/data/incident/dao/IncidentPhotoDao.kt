package com.example.hazardreporting.data.incident.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.hazardreporting.data.incident.entity.IncidentPhotoEntity

/**
 * Room access to the `incident_photos` table
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Dao
interface IncidentPhotoDao {

    /**
     * Replaces every photo of the incident with [incidentLocalId] with [photos] in one transaction
     */
    @Transaction
    suspend fun replaceForIncident(incidentLocalId: Long, photos: List<IncidentPhotoEntity>) {
        deleteForIncident(incidentLocalId)
        insertAll(photos)
    }

    /**
     * Inserts [photos]
     */
    @Insert
    suspend fun insertAll(photos: List<IncidentPhotoEntity>)

    /**
     * Deletes every photo of the incident with [incidentLocalId]
     */
    @Query("DELETE FROM incident_photos WHERE incidentLocalId = :incidentLocalId")
    suspend fun deleteForIncident(incidentLocalId: Long)
}
