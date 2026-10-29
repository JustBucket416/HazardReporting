package com.example.hazardreporting.data.incident.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.hazardreporting.data.incident.entity.IncidentEntity
import com.example.hazardreporting.data.incident.entity.IncidentWithPhotos
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
