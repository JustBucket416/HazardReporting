package com.example.hazardreporting.data.incident

import androidx.room.withTransaction
import com.example.hazardreporting.data.incident.dao.IncidentDao
import com.example.hazardreporting.data.incident.dao.IncidentPhotoDao
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.data.incident.entity.IncidentPhotoEntity
import com.example.hazardreporting.data.incident.entity.IncidentWithPhotos
import com.example.hazardreporting.data.local.HazardReportingDatabase
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
