package com.example.hazardreporting.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.hazardreporting.data.catalog.dao.CategoryDao
import com.example.hazardreporting.data.catalog.dao.SeverityDao
import com.example.hazardreporting.data.catalog.dao.SiteDao
import com.example.hazardreporting.data.catalog.entity.CategoryEntity
import com.example.hazardreporting.data.catalog.entity.SeverityEntity
import com.example.hazardreporting.data.catalog.entity.SiteEntity

/**
 * The app's Room database, the source of truth for everything shown offline (ADR-0002)
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Database(
    entities = [
        CategoryEntity::class,
        SeverityEntity::class,
        SiteEntity::class,
    ],
    version = 1,
)
abstract class HazardReportingDatabase : RoomDatabase() {

    /**
     * Returns the DAO of the `categories` table
     */
    abstract fun categoryDao(): CategoryDao

    /**
     * Returns the DAO of the `severities` table
     */
    abstract fun severityDao(): SeverityDao

    /**
     * Returns the DAO of the `sites` table
     */
    abstract fun siteDao(): SiteDao
}
