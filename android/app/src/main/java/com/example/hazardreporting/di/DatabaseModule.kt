package com.example.hazardreporting.di

import android.content.Context
import androidx.room.Room
import com.example.hazardreporting.data.catalog.dao.CategoryDao
import com.example.hazardreporting.data.catalog.dao.SeverityDao
import com.example.hazardreporting.data.catalog.dao.SiteDao
import com.example.hazardreporting.data.incident.dao.IncidentDao
import com.example.hazardreporting.data.local.HazardReportingDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Provides the Room database and its DAOs
 *
 * @author Roman Pliskin on 29.10.2026
 */
@Module
object DatabaseModule {

    private const val DATABASE_NAME = "hazard-reporting.db"

    /**
     * Provides the app's Room database
     */
    @Provides
    @Singleton
    fun provideDatabase(context: Context): HazardReportingDatabase =
        Room.databaseBuilder(context, HazardReportingDatabase::class.java, DATABASE_NAME).build()

    /**
     * Provides the DAO of the `categories` table
     */
    @Provides
    fun provideCategoryDao(database: HazardReportingDatabase): CategoryDao = database.categoryDao()

    /**
     * Provides the DAO of the `severities` table
     */
    @Provides
    fun provideSeverityDao(database: HazardReportingDatabase): SeverityDao = database.severityDao()

    /**
     * Provides the DAO of the `sites` table
     */
    @Provides
    fun provideSiteDao(database: HazardReportingDatabase): SiteDao = database.siteDao()

    /**
     * Provides the DAO of the `incidents` table
     */
    @Provides
    fun provideIncidentDao(database: HazardReportingDatabase): IncidentDao = database.incidentDao()
}
