package com.example.hazardreporting.data.catalog.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.hazardreporting.data.catalog.entity.SeverityEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room access to the `severities` table
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Dao
interface SeverityDao {

    /**
     * Observes every severity, from the lowest rank to the highest
     */
    @Query("SELECT * FROM severities ORDER BY `rank`")
    fun observeAll(): Flow<List<SeverityEntity>>

    /**
     * Replaces every stored severity with [severities] in one transaction
     */
    @Transaction
    suspend fun replaceAll(severities: List<SeverityEntity>) {
        deleteAll()
        insertAll(severities)
    }

    /**
     * Inserts [severities]
     */
    @Insert
    suspend fun insertAll(severities: List<SeverityEntity>)

    /**
     * Deletes every severity
     */
    @Query("DELETE FROM severities")
    suspend fun deleteAll()
}
