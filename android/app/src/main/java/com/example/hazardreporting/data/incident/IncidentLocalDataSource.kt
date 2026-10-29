package com.example.hazardreporting.data.incident

import androidx.room.withTransaction
import com.example.hazardreporting.data.incident.dao.IncidentDao
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.data.local.HazardReportingDatabase
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Stores the incidents and their photos in Room
 *
 * @property database runs several writes as one transaction
 * @property incidentDao access to the `incidents` table
 *
 * @author Roman Pliskin on 29.10.2026
 */
class IncidentLocalDataSource @Inject constructor(
    private val database: HazardReportingDatabase,
    private val incidentDao: IncidentDao,
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
     * Returns every stored incident
     */
    suspend fun getIncidents(): List<IncidentEntity> = incidentDao.getAll()

    /**
     * Stores the new [incidents]
     */
    suspend fun insertIncidents(incidents: List<IncidentEntity>) {
        incidentDao.insertAll(incidents)
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
}
