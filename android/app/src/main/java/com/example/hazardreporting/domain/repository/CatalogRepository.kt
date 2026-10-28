package com.example.hazardreporting.domain.repository

import com.example.hazardreporting.domain.model.Category
import com.example.hazardreporting.domain.model.Severity
import com.example.hazardreporting.domain.model.Site
import kotlinx.coroutines.flow.Flow

/**
 * The catalog: categories, severities and sites. It is stored on the device,
 * so it can be observed offline once it has been downloaded
 *
 * @author Roman Pliskin on 29.10.2026
 */
interface CatalogRepository {

    /**
     * Downloads the whole catalog and replaces the stored one. Nothing is stored if any download fails
     *
     * @throws com.example.hazardreporting.domain.exception.NoConnectionException if the server can't be reached
     * @throws com.example.hazardreporting.domain.exception.UnauthorizedException if the server rejects the session
     */
    suspend fun refresh()

    /**
     * Observes the stored categories, in the server's order
     */
    fun observeCategories(): Flow<List<Category>>

    /**
     * Observes the stored severities, from the lowest rank to the highest
     */
    fun observeSeverities(): Flow<List<Severity>>

    /**
     * Observes the stored sites, in the server's order
     */
    fun observeSites(): Flow<List<Site>>
}
