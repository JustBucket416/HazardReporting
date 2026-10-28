package com.example.hazardreporting.data.catalog.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.hazardreporting.data.catalog.entity.SiteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room access to the `sites` table
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Dao
interface SiteDao {

    /**
     * Observes every site, in the server's order
     */
    @Query("SELECT * FROM sites ORDER BY position")
    fun observeAll(): Flow<List<SiteEntity>>

    /**
     * Replaces every stored site with [sites] in one transaction
     */
    @Transaction
    suspend fun replaceAll(sites: List<SiteEntity>) {
        deleteAll()
        insertAll(sites)
    }

    /**
     * Inserts [sites]
     */
    @Insert
    suspend fun insertAll(sites: List<SiteEntity>)

    /**
     * Deletes every site
     */
    @Query("DELETE FROM sites")
    suspend fun deleteAll()
}
