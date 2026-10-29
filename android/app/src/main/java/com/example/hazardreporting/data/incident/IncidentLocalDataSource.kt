package com.example.hazardreporting.data.incident

import androidx.room.withTransaction
import com.example.hazardreporting.data.incident.dao.IncidentDao
import com.example.hazardreporting.data.incident.dao.IncidentPhotoDao
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.data.incident.entity.IncidentPhotoEntity
import com.example.hazardreporting.data.incident.entity.IncidentWithPhotos
import com.example.hazardreporting.data.local.HazardReportingDatabase
import com.example.hazardreporting.domain.model.SyncState
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Stores the incidents and their photos in Room
 *
 * @property database runs several writes as one transaction
 * @property incidentDao access to the `incidents` table
 * @property photoDao access to the `incident_photos` table
 *
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentLocalDataSource @Inject constructor(
    private val database: HazardReportingDatabase,
    private val incidentDao: IncidentDao,
    private val photoDao: IncidentPhotoDao,
) {

    /**
     * Runs [block] in one database transaction, so it reads a consistent state and its writes land together
     */
    suspend fun <R> inTransaction(block: suspend () -> R): R = database.withTransaction(block)

    /**
     * Observes every stored incident
     */
    fun observeIncidents(): Flow<List<IncidentEntity>> = incidentDao.observeAll()

    /**
     * Observes the stored incident with [localId] together with its photos; emits null while there is none
     */
    fun observeIncident(localId: Long): Flow<IncidentWithPhotos?> = incidentDao.observeWithPhotos(localId)

    /**
     * Returns every stored incident
     */
    suspend fun getIncidents(): List<IncidentEntity> = incidentDao.getAll()

    /**
     * Returns the stored incident with [localId], or null if there is none
     */
    suspend fun getIncident(localId: Long): IncidentEntity? = incidentDao.getByLocalId(localId)

    /**
     * Returns the stored incident with [localId] together with its photos, or null if there is none
     */
    suspend fun getIncidentWithPhotos(localId: Long): IncidentWithPhotos? = incidentDao.getWithPhotos(localId)

    /**
     * Returns the stored incident the server knows as [serverId], or null if there is none
     */
    suspend fun getIncidentByServerId(serverId: String): IncidentEntity? = incidentDao.getByServerId(serverId)

    /**
     * Returns every stored incident with a pending change, oldest first
     */
    suspend fun getPendingIncidents(): List<IncidentEntity> = incidentDao.getAllExcept(SyncState.Synced)

    /**
     * Returns whether a stored incident was created at exactly [createdAt]
     */
    suspend fun existsCreatedAt(createdAt: Long): Boolean = incidentDao.existsCreatedAt(createdAt)

    /**
     * Records that a POST of the incident with [localId] is about to be sent, so a withdrawal can no longer
     * simply delete it (ADR-0003)
     */
    suspend fun markCreateAttempted(localId: Long) {
        incidentDao.markCreateAttempted(localId)
    }

    /**
     * Stores the new [incident] and returns its generated local id
     */
    suspend fun insertIncident(incident: IncidentEntity): Long = incidentDao.insert(incident)

    /**
     * Stores the new [incidents]
     */
    suspend fun insertIncidents(incidents: List<IncidentEntity>) {
        incidentDao.insertAll(incidents)
    }

    /**
     * Replaces the stored row of [incident]
     */
    suspend fun updateIncident(incident: IncidentEntity) {
        incidentDao.update(incident)
    }

    /**
     * Replaces the stored rows of [incidents]
     */
    suspend fun updateIncidents(incidents: List<IncidentEntity>) {
        incidentDao.updateAll(incidents)
    }

    /**
     * Deletes the incidents with [localIds], together with their photos
     */
    suspend fun deleteIncidents(localIds: List<Long>) {
        incidentDao.deleteAll(localIds)
    }

    /**
     * Returns every stored photo of the incident with [incidentLocalId], in no particular order
     */
    suspend fun getPhotos(incidentLocalId: Long): List<IncidentPhotoEntity> = photoDao.getForIncident(incidentLocalId)

    /**
     * Returns the local path of every stored photo that has one
     */
    suspend fun getPhotoLocalPaths(): List<String> = photoDao.getLocalPaths()

    /**
     * Stores [remoteUrl] as the server URL of the photos of the incident with [incidentLocalId] whose file is
     * [localPath], unless they already have one. Matching by file rather than by row keeps the URL when an edit
     * replaced the photo rows during the upload
     */
    suspend fun setPhotoRemoteUrl(incidentLocalId: Long, localPath: String, remoteUrl: String) {
        photoDao.setRemoteUrl(incidentLocalId, localPath, remoteUrl)
    }

    /**
     * Stores the new [photos]
     */
    suspend fun insertPhotos(photos: List<IncidentPhotoEntity>) {
        photoDao.insertAll(photos)
    }

    /**
     * Replaces every stored photo of the incident with [incidentLocalId] with [photos]
     */
    suspend fun replacePhotos(incidentLocalId: Long, photos: List<IncidentPhotoEntity>) {
        photoDao.replaceForIncident(incidentLocalId, photos)
    }
}
