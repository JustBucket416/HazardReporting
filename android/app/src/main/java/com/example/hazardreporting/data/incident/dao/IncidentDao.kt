package com.example.hazardreporting.data.incident.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.hazardreporting.data.incident.entity.IncidentEntity
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
     * Returns every incident
     */
    @Query("SELECT * FROM incidents")
    suspend fun getAll(): List<IncidentEntity>

    /**
     * Inserts [incidents]
     */
    @Insert
    suspend fun insertAll(incidents: List<IncidentEntity>)

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
