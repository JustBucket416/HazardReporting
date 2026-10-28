package com.example.hazardreporting.data.catalog.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.hazardreporting.data.catalog.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room access to the `categories` table
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Dao
interface CategoryDao {

    /**
     * Observes every category, in the server's order
     */
    @Query("SELECT * FROM categories ORDER BY position")
    fun observeAll(): Flow<List<CategoryEntity>>

    /**
     * Replaces every stored category with [categories] in one transaction
     */
    @Transaction
    suspend fun replaceAll(categories: List<CategoryEntity>) {
        deleteAll()
        insertAll(categories)
    }

    /**
     * Inserts [categories]
     */
    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>)

    /**
     * Deletes every category
     */
    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
