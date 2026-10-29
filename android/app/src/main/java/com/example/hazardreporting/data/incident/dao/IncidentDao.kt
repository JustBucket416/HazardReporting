package com.example.hazardreporting.data.incident.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.data.incident.entity.IncidentWithPhotos
import com.example.hazardreporting.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * Room access to the `incidents` table
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Dao
interface IncidentDao {

    /**
     * Observes every incident
     */
    @Query("SELECT * FROM incidents")
    fun observeAll(): Flow<List<IncidentEntity>>

    /**
     * Observes the incident with [localId] together with its photos; emits null while there is no such incident
     */
    @Transaction
    @Query("SELECT * FROM incidents WHERE localId = :localId")
    fun observeWithPhotos(localId: Long): Flow<IncidentWithPhotos?>

    /**
     * Returns every incident
     */
    @Query("SELECT * FROM incidents")
    suspend fun getAll(): List<IncidentEntity>

    /**
     * Returns the incident with [localId], or null if there is none
     */
    @Query("SELECT * FROM incidents WHERE localId = :localId")
    suspend fun getByLocalId(localId: Long): IncidentEntity?

    /**
     * Returns the incident with [localId] together with its photos, or null if there is none
     */
    @Transaction
    @Query("SELECT * FROM incidents WHERE localId = :localId")
    suspend fun getWithPhotos(localId: Long): IncidentWithPhotos?

    /**
     * Returns the incident the server knows as [serverId], or null if there is none
     */
    @Query("SELECT * FROM incidents WHERE serverId = :serverId")
    suspend fun getByServerId(serverId: String): IncidentEntity?

    /**
     * Returns the incidents whose sync state isn't [syncState], oldest first
     */
    @Query("SELECT * FROM incidents WHERE syncState != :syncState ORDER BY localId")
    suspend fun getAllExcept(syncState: SyncState): List<IncidentEntity>

    /**
     * Returns whether an incident was created at exactly [createdAt]
     */
    @Query("SELECT EXISTS(SELECT 1 FROM incidents WHERE createdAt = :createdAt)")
    suspend fun existsCreatedAt(createdAt: Long): Boolean

    /**
     * Records that a POST of the incident with [localId] is about to be sent
     */
    @Query("UPDATE incidents SET createAttempted = 1 WHERE localId = :localId")
    suspend fun markCreateAttempted(localId: Long)

    /**
     * Inserts [incident] and returns its generated local id
     */
    @Insert
    suspend fun insert(incident: IncidentEntity): Long

    /**
     * Inserts [incidents]
     */
    @Insert
    suspend fun insertAll(incidents: List<IncidentEntity>)

    /**
     * Replaces the stored row of [incident], matched by local id
     */
    @Update
    suspend fun update(incident: IncidentEntity)

    /**
     * Replaces the stored rows of [incidents], matched by local id
     */
    @Update
    suspend fun updateAll(incidents: List<IncidentEntity>)

    /**
     * Deletes the incidents with [localIds], together with their photos
     */
    @Query("DELETE FROM incidents WHERE localId IN (:localIds)")
    suspend fun deleteAll(localIds: List<Long>)
}
