package com.example.hazardreporting.data.catalog

import com.example.hazardreporting.data.catalog.dao.CategoryDao
import com.example.hazardreporting.data.catalog.dao.SeverityDao
import com.example.hazardreporting.data.catalog.dao.SiteDao
import com.example.hazardreporting.data.catalog.entity.CategoryEntity
import com.example.hazardreporting.data.catalog.entity.SeverityEntity
import com.example.hazardreporting.data.catalog.entity.SiteEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Stores the catalog in Room
 *
 * @property categoryDao access to the `categories` table
 * @property severityDao access to the `severities` table
 * @property siteDao access to the `sites` table
 *
 * @author Roman Pliskin on 29.10.2026
 */
class CatalogLocalDataSource @Inject constructor(
    private val categoryDao: CategoryDao,
    private val severityDao: SeverityDao,
    private val siteDao: SiteDao,
) {

    /**
     * Observes the stored categories, in the server's order
     */
    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    /**
     * Observes the stored severities, from the lowest rank to the highest
     */
    fun observeSeverities(): Flow<List<SeverityEntity>> = severityDao.observeAll()

    /**
     * Observes the stored sites, in the server's order
     */
    fun observeSites(): Flow<List<SiteEntity>> = siteDao.observeAll()

    /**
     * Replaces the stored categories with [categories]
     */
    suspend fun replaceCategories(categories: List<CategoryEntity>) {
        categoryDao.replaceAll(categories)
    }

    /**
     * Replaces the stored severities with [severities]
     */
    suspend fun replaceSeverities(severities: List<SeverityEntity>) {
        severityDao.replaceAll(severities)
    }

    /**
     * Replaces the stored sites with [sites]
     */
    suspend fun replaceSites(sites: List<SiteEntity>) {
        siteDao.replaceAll(sites)
    }
}
