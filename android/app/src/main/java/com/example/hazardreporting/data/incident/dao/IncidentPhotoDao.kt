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
     * Returns every photo of the incident with [incidentLocalId], in no particular order
     */
    @Query("SELECT * FROM incident_photos WHERE incidentLocalId = :incidentLocalId")
    suspend fun getForIncident(incidentLocalId: Long): List<IncidentPhotoEntity>

    /**
     * Returns the local path of every photo that has one
     */
    @Query("SELECT localPath FROM incident_photos WHERE localPath IS NOT NULL")
    suspend fun getLocalPaths(): List<String>

    /**
     * Stores [remoteUrl] on the photos of the incident with [incidentLocalId] whose file is [localPath]
     * and that have no server URL yet
     */
    @Query(
        "UPDATE incident_photos SET remoteUrl = :remoteUrl " +
            "WHERE incidentLocalId = :incidentLocalId AND localPath = :localPath AND remoteUrl IS NULL"
    )
    suspend fun setRemoteUrl(incidentLocalId: Long, localPath: String, remoteUrl: String)

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
